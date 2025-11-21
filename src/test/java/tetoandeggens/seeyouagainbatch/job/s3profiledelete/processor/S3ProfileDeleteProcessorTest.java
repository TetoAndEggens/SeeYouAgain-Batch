package tetoandeggens.seeyouagainbatch.job.s3profiledelete.processor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import tetoandeggens.seeyouagainbatch.domain.Animal;
import tetoandeggens.seeyouagainbatch.domain.AnimalS3Profile;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3ProfileDeleteProcessor 단위 테스트")
class S3ProfileDeleteProcessorTest {

	@Mock
	private S3Client s3Client;

	private S3ProfileDeleteProcessor processor;

	private static final String TEST_BUCKET = "test-bucket";
	private static final String TEST_CLOUDFRONT_DOMAIN = "https://test.cloudfront.net/";

	private Animal testAnimal;

	@BeforeEach
	void setUp() {
		testAnimal = new Animal(1L);
		processor = new S3ProfileDeleteProcessor(s3Client, TEST_BUCKET, TEST_CLOUDFRONT_DOMAIN);
	}

	@Test
	@DisplayName("S3 삭제 성공 시 AnimalS3Profile을 반환한다")
	void shouldReturnS3ProfileWhenDeleteSucceeds() throws Exception {
		AnimalS3Profile profile = createS3Profile(1L, "animal-profiles/test.webp");

		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.thenReturn(DeleteObjectResponse.builder().build());

		AnimalS3Profile result = processor.process(profile);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getProfile()).isEqualTo("animal-profiles/test.webp");
		verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("CloudFront URL에서 S3 키를 정상적으로 추출하여 삭제한다")
	void shouldExtractS3KeyFromCloudfrontUrl() throws Exception {
		AnimalS3Profile profile = createS3Profile(1L, TEST_CLOUDFRONT_DOMAIN + "animal-profiles/test.webp");

		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.thenReturn(DeleteObjectResponse.builder().build());

		AnimalS3Profile result = processor.process(profile);

		assertThat(result).isNotNull();
		verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("S3에 파일이 존재하지 않아도(404) 프로필을 반환한다")
	void shouldReturnProfileWhenFileNotFound() throws Exception {
		AnimalS3Profile profile = createS3Profile(1L, "animal-profiles/nonexistent.webp");

		S3Exception s3Exception = (S3Exception) S3Exception.builder()
			.statusCode(404)
			.message("Not Found")
			.build();

		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.thenThrow(s3Exception);

		AnimalS3Profile result = processor.process(profile);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("S3 삭제 실패 시 null을 반환한다")
	void shouldReturnNullWhenDeleteFails() throws Exception {
		AnimalS3Profile profile = createS3Profile(1L, "animal-profiles/test.webp");

		S3Exception s3Exception = (S3Exception) S3Exception.builder()
			.statusCode(500)
			.message("Internal Server Error")
			.build();

		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.thenThrow(s3Exception);

		AnimalS3Profile result = processor.process(profile);

		assertThat(result).isNull();
		verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("처리 중 일반 예외 발생 시 null을 반환한다")
	void shouldReturnNullWhenExceptionOccurs() throws Exception {
		AnimalS3Profile profile = createS3Profile(1L, "animal-profiles/test.webp");

		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.thenThrow(new RuntimeException("Unexpected error"));

		AnimalS3Profile result = processor.process(profile);

		assertThat(result).isNull();
		verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("profile URL이 null이면 그대로 프로필을 반환한다")
	void shouldReturnProfileWhenUrlIsNull() throws Exception {
		AnimalS3Profile profile = createS3Profile(1L, null);

		AnimalS3Profile result = processor.process(profile);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("profile URL이 빈 문자열이면 그대로 프로필을 반환한다")
	void shouldReturnProfileWhenUrlIsEmpty() throws Exception {
		AnimalS3Profile profile = createS3Profile(1L, "");

		AnimalS3Profile result = processor.process(profile);

		assertThat(result).isNotNull();
		verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("profile URL이 공백 문자열이면 그대로 프로필을 반환한다")
	void shouldReturnProfileWhenUrlIsBlank() throws Exception {
		AnimalS3Profile profile = createS3Profile(1L, "   ");

		AnimalS3Profile result = processor.process(profile);

		assertThat(result).isNotNull();
		verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("HTTP URL에서 S3 키를 정상적으로 추출하여 삭제한다")
	void shouldExtractS3KeyFromHttpUrl() throws Exception {
		AnimalS3Profile profile = createS3Profile(1L, "http://example.com/animal-profiles/test.webp");

		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.thenReturn(DeleteObjectResponse.builder().build());

		AnimalS3Profile result = processor.process(profile);

		assertThat(result).isNotNull();
		verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("HTTPS URL에서 S3 키를 정상적으로 추출하여 삭제한다")
	void shouldExtractS3KeyFromHttpsUrl() throws Exception {
		AnimalS3Profile profile = createS3Profile(1L, "https://example.com/animal-profiles/test.webp");

		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.thenReturn(DeleteObjectResponse.builder().build());

		AnimalS3Profile result = processor.process(profile);

		assertThat(result).isNotNull();
		verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	@DisplayName("여러 프로필을 순차적으로 삭제할 수 있다")
	void shouldDeleteMultipleProfiles() throws Exception {
		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.thenReturn(DeleteObjectResponse.builder().build());

		AnimalS3Profile profile1 = createS3Profile(1L, "animal-profiles/test1.webp");
		AnimalS3Profile profile2 = createS3Profile(2L, "animal-profiles/test2.webp");
		AnimalS3Profile profile3 = createS3Profile(3L, "animal-profiles/test3.webp");

		AnimalS3Profile result1 = processor.process(profile1);
		AnimalS3Profile result2 = processor.process(profile2);
		AnimalS3Profile result3 = processor.process(profile3);

		assertThat(result1).isNotNull();
		assertThat(result2).isNotNull();
		assertThat(result3).isNotNull();
		verify(s3Client, times(3)).deleteObject(any(DeleteObjectRequest.class));
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
}
