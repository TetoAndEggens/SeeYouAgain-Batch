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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;

import tetoandeggens.seeyouagainbatch.domain.Animal;
import tetoandeggens.seeyouagainbatch.domain.AnimalProfile;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.dto.ProfileImageData;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.service.ParallelS3UploadService;

@ExtendWith(MockitoExtension.class)
class S3ProfileUploadWriterTest {

	@Mock
	private ParallelS3UploadService parallelS3UploadService;

	@Mock
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private S3ProfileUploadWriter writer;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	@Captor
	private ArgumentCaptor<SqlParameterSource[]> paramsCaptor;

	private Animal testAnimal;

	@BeforeEach
	void setUp() {
		testAnimal = new Animal(1L);
		writer = new S3ProfileUploadWriter(parallelS3UploadService, namedParameterJdbcTemplate, "https://cdn.example.com/");
	}

	@Test
	@DisplayName("S3 업로드 후 모든 항목을 DB에 저장한다")
	void shouldInsertAllItems() {
		List<ProfileImageData> imageDataList = List.of(
			createProfileImageData("s3-key-1", 1L),
			createProfileImageData("s3-key-2", 2L),
			createProfileImageData("s3-key-3", 3L)
		);

		Chunk<ProfileImageData> chunk = new Chunk<>(imageDataList);

		doNothing().when(parallelS3UploadService).uploadBatch(anyList());
		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1, 1, 1});

		writer.write(chunk);

		verify(parallelS3UploadService, times(1)).uploadBatch(anyList());
		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(sqlCaptor.capture(), paramsCaptor.capture());

		String sql = sqlCaptor.getValue();
		assertThat(sql).contains("INSERT INTO animal_s3_profile");
		assertThat(sql).contains("profile");
		assertThat(sql).contains("image_type");
		assertThat(sql).contains("animal_id");

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(3);
	}

	@Test
	@DisplayName("빈 chunk는 S3 업로드 및 DB 저장을 수행하지 않는다")
	void shouldNotProcessWhenChunkIsEmpty() {
		Chunk<ProfileImageData> emptyChunk = new Chunk<>();

		writer.write(emptyChunk);

		verify(parallelS3UploadService, never()).uploadBatch(anyList());
		verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
	}

	@Test
	@DisplayName("null 항목은 필터링하고 유효한 항목만 처리한다")
	void shouldFilterNullItemsAndProcessValidOnes() {
		List<ProfileImageData> imageDataList = List.of(
			createProfileImageData("s3-key-1", 1L),
			createProfileImageDataWithNullBytes()
		);

		Chunk<ProfileImageData> chunk = new Chunk<>(imageDataList);

		doNothing().when(parallelS3UploadService).uploadBatch(anyList());
		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1});

		writer.write(chunk);

		verify(parallelS3UploadService, times(1)).uploadBatch(anyList());
		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(1);
	}

	@Test
	@DisplayName("단일 항목도 정상적으로 처리한다")
	void shouldProcessSingleItem() {
		List<ProfileImageData> imageDataList = List.of(
			createProfileImageData("s3-key-1", 1L)
		);

		Chunk<ProfileImageData> chunk = new Chunk<>(imageDataList);

		doNothing().when(parallelS3UploadService).uploadBatch(anyList());
		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1});

		writer.write(chunk);

		verify(parallelS3UploadService, times(1)).uploadBatch(anyList());
		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(1);
	}

	private ProfileImageData createProfileImageData(String s3Key, Long animalId) {
		Animal animal = new Animal(animalId);
		AnimalProfile profile = AnimalProfile.builder()
			.profile("http://example.com/image.jpg")
			.animal(animal)
			.build();

		return ProfileImageData.builder()
			.profile(profile)
			.imageBytes(new byte[1024])
			.s3Key(s3Key)
			.build();
	}

	private ProfileImageData createProfileImageDataWithNullBytes() {
		AnimalProfile profile = AnimalProfile.builder()
			.profile("http://example.com/image.jpg")
			.animal(testAnimal)
			.build();

		return ProfileImageData.builder()
			.profile(profile)
			.imageBytes(null)
			.s3Key("s3-key-null")
			.build();
	}
}