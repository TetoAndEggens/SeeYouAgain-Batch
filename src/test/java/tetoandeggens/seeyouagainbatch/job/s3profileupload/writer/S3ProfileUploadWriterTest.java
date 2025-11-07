package tetoandeggens.seeyouagainbatch.job.s3profileupload.writer;

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

import tetoandeggens.seeyouagainbatch.domain.AbandonedAnimal;
import tetoandeggens.seeyouagainbatch.domain.AbandonedAnimalS3Profile;

@ExtendWith(MockitoExtension.class)
class S3ProfileUploadWriterTest {

	@Mock
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@InjectMocks
	private S3ProfileUploadWriter writer;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	@Captor
	private ArgumentCaptor<SqlParameterSource[]> paramsCaptor;

	private AbandonedAnimal testAnimal;

	@BeforeEach
	void setUp() {
		testAnimal = new AbandonedAnimal(1L);
	}

	@Test
	@DisplayName("여러 개의 S3 프로필을 bulk insert 한다")
	void shouldBulkInsertMultipleS3Profiles() {
		List<AbandonedAnimalS3Profile> profiles = List.of(
			createS3Profile("s3-key-1", 1L),
			createS3Profile("s3-key-2", 2L),
			createS3Profile("s3-key-3", 3L)
		);

		Chunk<AbandonedAnimalS3Profile> chunk = new Chunk<>(profiles);

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1, 1, 1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(sqlCaptor.capture(), paramsCaptor.capture());

		String sql = sqlCaptor.getValue();
		assertThat(sql).contains("INSERT INTO abandoned_animal_s3_profile");
		assertThat(sql).contains("profile");
		assertThat(sql).contains("image_type");
		assertThat(sql).contains("abandoned_animal_id");
		assertThat(sql).doesNotContain("ON DUPLICATE KEY UPDATE");

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(3);
	}

	@Test
	@DisplayName("null 항목은 필터링하고 유효한 항목만 insert 한다")
	void shouldFilterNullItemsAndInsertValidOnes() {
		List<AbandonedAnimalS3Profile> profiles = List.of(
			createS3Profile("s3-key-1", 1L),
			createS3ProfileWithNullKey(),
			createS3Profile("s3-key-3", 3L)
		);

		Chunk<AbandonedAnimalS3Profile> chunk = new Chunk<>(profiles);

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1, 1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(2);
	}

	@Test
	@DisplayName("빈 chunk는 insert를 수행하지 않는다")
	void shouldNotInsertWhenChunkIsEmpty() {
		Chunk<AbandonedAnimalS3Profile> emptyChunk = new Chunk<>();

		writer.write(emptyChunk);

		verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
	}

	@Test
	@DisplayName("모든 항목이 null인 경우 insert를 수행하지 않는다")
	void shouldNotInsertWhenAllItemsAreInvalid() {
		List<AbandonedAnimalS3Profile> profiles = List.of(
			createS3ProfileWithNullKey(),
			createS3ProfileWithNullKey()
		);

		Chunk<AbandonedAnimalS3Profile> chunk = new Chunk<>(profiles);

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
	}

	@Test
	@DisplayName("단일 항목도 정상적으로 insert 한다")
	void shouldInsertSingleItem() {
		List<AbandonedAnimalS3Profile> profiles = List.of(
			createS3Profile("s3-key-1", 1L)
		);

		Chunk<AbandonedAnimalS3Profile> chunk = new Chunk<>(profiles);

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(1);
	}

	private AbandonedAnimalS3Profile createS3Profile(String objectKey, Long animalId) {
		AbandonedAnimal animal = new AbandonedAnimal(animalId);

		return AbandonedAnimalS3Profile.builder()
			.profile(objectKey)
			.abandonedAnimal(animal)
			.build();
	}

	private AbandonedAnimalS3Profile createS3ProfileWithNullKey() {
		return AbandonedAnimalS3Profile.builder()
			.profile(null)
			.abandonedAnimal(testAnimal)
			.build();
	}
}
