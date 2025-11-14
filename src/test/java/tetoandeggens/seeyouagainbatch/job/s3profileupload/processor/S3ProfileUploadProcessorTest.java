package tetoandeggens.seeyouagainbatch.job.s3profileupload.processor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import tetoandeggens.seeyouagainbatch.domain.Animal;
import tetoandeggens.seeyouagainbatch.domain.AnimalProfile;
import tetoandeggens.seeyouagainbatch.domain.AnimalS3Profile;

@ExtendWith(MockitoExtension.class)
class S3ProfileUploadProcessorTest {

	@Mock
	private S3Client s3Client;

	@Mock
	private HttpClient httpClient;

	private S3ProfileUploadProcessor processor;

	private AnimalProfile testProfile;
	private Animal testAnimal;

	@BeforeEach
	void setUp() {
		testAnimal = new Animal(1L);

		testProfile = AnimalProfile.builder()
			.profile("http://example.com/profile.jpg")
			.animal(testAnimal)
			.build();

		processor = new S3ProfileUploadProcessor(s3Client, httpClient, "test-bucket", "");
	}

	@SuppressWarnings("unchecked")
	private HttpResponse<InputStream> createSuccessfulHttpResponse() {
		HttpResponse<InputStream> response = (HttpResponse<InputStream>) mock(HttpResponse.class);

		when(response.statusCode()).thenReturn(200);
		when(response.body()).thenAnswer(invocation -> new ByteArrayInputStream(new byte[1024]));

		return response;
	}

	@Test
	@DisplayName("S3 업로드 성공 시 AnimalS3Profile을 반환한다")
	void shouldReturnS3ProfileWhenUploadSucceeds() throws Exception {
		HttpResponse<InputStream> response = createSuccessfulHttpResponse();
		when(httpClient.send(any(), any())).thenAnswer(invocation -> response);
		when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
			.thenReturn(PutObjectResponse.builder().build());

		AnimalS3Profile result = processor.process(testProfile);

		assertThat(result).isNotNull();
		assertThat(result.getProfile()).startsWith("animal-profiles/");
		assertThat(result.getProfile()).endsWith(".webp");
		assertThat(result.getAnimal()).isEqualTo(testAnimal);
		verify(httpClient, times(1)).send(any(), any());
		verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
	}

	@Test
	@DisplayName("S3 업로드 실패 시 null을 반환한다")
	void shouldReturnNullWhenUploadFails() throws Exception {
		when(httpClient.send(any(), any())).thenThrow(new RuntimeException("HTTP 요청 실패"));

		AnimalS3Profile result = processor.process(testProfile);

		assertThat(result).isNull();
		verify(httpClient, times(1)).send(any(), any());
	}

	@Test
	@DisplayName("S3 업로드 중 예외 발생 시 null을 반환한다")
	void shouldReturnNullWhenExceptionOccurs() throws Exception {
		when(httpClient.send(any(), any())).thenThrow(new RuntimeException("S3 업로드 실패"));

		AnimalS3Profile result = processor.process(testProfile);

		assertThat(result).isNull();
		verify(httpClient, times(1)).send(any(), any());
	}

	@Test
	@DisplayName("여러 프로필을 순차적으로 처리할 수 있다")
	void shouldProcessMultipleProfiles() throws Exception {
		HttpResponse<InputStream> response = createSuccessfulHttpResponse();
		when(httpClient.send(any(), any())).thenAnswer(invocation -> response);
		when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
			.thenReturn(PutObjectResponse.builder().build());

		AnimalProfile profile1 = AnimalProfile.builder()
			.profile("http://example.com/profile1.jpg")
			.animal(testAnimal)
			.build();

		AnimalProfile profile2 = AnimalProfile.builder()
			.profile("http://example.com/profile2.jpg")
			.animal(testAnimal)
			.build();

		AnimalS3Profile result1 = processor.process(profile1);
		AnimalS3Profile result2 = processor.process(profile2);

		assertThat(result1).isNotNull();
		assertThat(result1.getProfile()).startsWith("animal-profiles/");
		assertThat(result2).isNotNull();
		assertThat(result2.getProfile()).startsWith("animal-profiles/");
		verify(httpClient, times(2)).send(any(), any());
		verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
	}
}
