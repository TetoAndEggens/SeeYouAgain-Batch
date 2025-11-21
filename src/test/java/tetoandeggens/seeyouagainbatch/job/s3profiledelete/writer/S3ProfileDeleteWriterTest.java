package tetoandeggens.seeyouagainbatch.job.s3profiledelete.writer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import tetoandeggens.seeyouagainbatch.domain.Animal;
import tetoandeggens.seeyouagainbatch.domain.AnimalS3Profile;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3ProfileDeleteWriter 단위 테스트")
class S3ProfileDeleteWriterTest {

	@Mock
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@InjectMocks
	private S3ProfileDeleteWriter writer;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	@Captor
	private ArgumentCaptor<SqlParameterSource[]> paramsCaptor;

	private Animal testAnimal;

	@BeforeEach
	void setUp() {
		testAnimal = new Animal(1L);
	}

	@Test
	@DisplayName("여러 개의 S3 프로필을 bulk delete 한다")
	void shouldBulkDeleteMultipleS3Profiles() {
		List<AnimalS3Profile> profiles = List.of(
			createS3Profile(1L, "s3-key-1"),
			createS3Profile(2L, "s3-key-2"),
			createS3Profile(3L, "s3-key-3")
		);

		Chunk<AnimalS3Profile> chunk = new Chunk<>(profiles);

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1, 1, 1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(sqlCaptor.capture(), paramsCaptor.capture());

		String sql = sqlCaptor.getValue();
		assertThat(sql).contains("DELETE FROM animal_s3_profile");
		assertThat(sql).contains("animal_s3_profile_id = :id");

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(3);
	}

	@Test
	@DisplayName("null 항목은 필터링하고 유효한 항목만 delete 한다")
	void shouldFilterNullItemsAndDeleteValidOnes() {
		List<AnimalS3Profile> profiles = List.of(
			createS3Profile(1L, "s3-key-1"),
			createS3ProfileWithNullId(),
			createS3Profile(3L, "s3-key-3")
		);

		Chunk<AnimalS3Profile> chunk = new Chunk<>(profiles);

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1, 1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(2);
	}

	@Test
	@DisplayName("빈 chunk는 delete를 수행하지 않는다")
	void shouldNotDeleteWhenChunkIsEmpty() {
		Chunk<AnimalS3Profile> emptyChunk = new Chunk<>();

		writer.write(emptyChunk);

		verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
	}

	@Test
	@DisplayName("모든 항목이 유효하지 않은 경우 delete를 수행하지 않는다")
	void shouldNotDeleteWhenAllItemsAreInvalid() {
		List<AnimalS3Profile> profiles = List.of(
			createS3ProfileWithNullId(),
			createS3ProfileWithNullId()
		);

		Chunk<AnimalS3Profile> chunk = new Chunk<>(profiles);

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
	}

	@Test
	@DisplayName("단일 항목도 정상적으로 delete 한다")
	void shouldDeleteSingleItem() {
		List<AnimalS3Profile> profiles = List.of(
			createS3Profile(1L, "s3-key-1")
		);

		Chunk<AnimalS3Profile> chunk = new Chunk<>(profiles);

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(1);
	}

	@Test
	@DisplayName("chunk에 null 항목이 포함되어 있어도 필터링하여 delete 한다")
	void shouldFilterNullElementsFromChunk() {
		Chunk<AnimalS3Profile> chunk = new Chunk<>();
		chunk.add(createS3Profile(1L, "s3-key-1"));
		chunk.add(null);
		chunk.add(createS3Profile(2L, "s3-key-2"));

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1, 1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(2);
	}

	@Test
	@DisplayName("대량의 S3 프로필을 delete 할 수 있다")
	void shouldDeleteLargeNumberOfProfiles() {
		List<AnimalS3Profile> profiles = new java.util.ArrayList<>();
		for (int i = 1; i <= 100; i++) {
			profiles.add(createS3Profile((long) i, "s3-key-" + i));
		}

		Chunk<AnimalS3Profile> chunk = new Chunk<>(profiles);

		int[] results = new int[100];
		java.util.Arrays.fill(results, 1);
		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(results);

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(100);
	}

	private AnimalS3Profile createS3Profile(Long id, String profile) {
		AnimalS3Profile s3Profile = AnimalS3Profile.builder()
			.profile(profile)
			.animal(testAnimal)
			.build();

		try {
			java.lang.reflect.Field idField = AnimalS3Profile.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(s3Profile, id);
		} catch (Exception e) {
			throw new RuntimeException("Failed to set ID", e);
		}

		return s3Profile;
	}

	private AnimalS3Profile createS3ProfileWithNullId() {
		return AnimalS3Profile.builder()
			.profile("s3-key")
			.animal(testAnimal)
			.build();
	}
}
