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

import tetoandeggens.seeyouagainbatch.domain.Animal;
import tetoandeggens.seeyouagainbatch.domain.AnimalProfile;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.dto.ProfileImageData;

@ExtendWith(MockitoExtension.class)
class S3ProfileUploadProcessorTest {

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

		processor = new S3ProfileUploadProcessor(httpClient);
	}

	@SuppressWarnings("unchecked")
	private HttpResponse<InputStream> createSuccessfulHttpResponse() {
		HttpResponse<InputStream> response = (HttpResponse<InputStream>)mock(HttpResponse.class);

		when(response.statusCode()).thenReturn(200);
		when(response.body()).thenAnswer(invocation -> new ByteArrayInputStream(new byte[1024]));

		return response;
	}

	@Test
	@DisplayName("이미지 다운로드 성공 시 ProfileImageData를 반환한다")
	void shouldReturnProfileImageDataWhenDownloadSucceeds() throws Exception {
		HttpResponse<InputStream> response = createSuccessfulHttpResponse();
		when(httpClient.send(any(), any())).thenAnswer(invocation -> response);

		ProfileImageData result = processor.process(testProfile);

		assertThat(result).isNotNull();
		assertThat(result.getProfile()).isEqualTo(testProfile);
		assertThat(result.getImageBytes()).isNotNull();
		assertThat(result.getImageBytes()).hasSize(1024);
		assertThat(result.getS3Key()).startsWith("animal-profiles/public-data/");
		assertThat(result.getS3Key()).endsWith(".webp");
		verify(httpClient, times(1)).send(any(), any());
	}

	@Test
	@DisplayName("이미지 다운로드 실패 시 null을 반환한다")
	void shouldReturnNullWhenDownloadFails() throws Exception {
		when(httpClient.send(any(), any())).thenThrow(new RuntimeException("HTTP 요청 실패"));

		ProfileImageData result = processor.process(testProfile);

		assertThat(result).isNull();
		verify(httpClient, times(1)).send(any(), any());
	}

	@Test
	@DisplayName("다운로드 중 예외 발생 시 null을 반환한다")
	void shouldReturnNullWhenExceptionOccurs() throws Exception {
		when(httpClient.send(any(), any())).thenThrow(new RuntimeException("다운로드 실패"));

		ProfileImageData result = processor.process(testProfile);

		assertThat(result).isNull();
		verify(httpClient, times(1)).send(any(), any());
	}

	@Test
	@DisplayName("여러 프로필을 순차적으로 처리할 수 있다")
	void shouldProcessMultipleProfiles() throws Exception {
		HttpResponse<InputStream> response = createSuccessfulHttpResponse();
		when(httpClient.send(any(), any())).thenAnswer(invocation -> response);

		AnimalProfile profile1 = AnimalProfile.builder()
			.profile("http://example.com/profile1.jpg")
			.animal(testAnimal)
			.build();

		AnimalProfile profile2 = AnimalProfile.builder()
			.profile("http://example.com/profile2.jpg")
			.animal(testAnimal)
			.build();

		ProfileImageData result1 = processor.process(profile1);
		ProfileImageData result2 = processor.process(profile2);

		assertThat(result1).isNotNull();
		assertThat(result1.getS3Key()).startsWith("animal-profiles/public-data/");
		assertThat(result1.getImageBytes()).hasSize(1024);
		assertThat(result2).isNotNull();
		assertThat(result2.getS3Key()).startsWith("animal-profiles/public-data/");
		assertThat(result2.getImageBytes()).hasSize(1024);
		verify(httpClient, times(2)).send(any(), any());
	}

	@Test
	@DisplayName("profile URL이 null이면 null을 반환한다")
	void shouldReturnNullWhenProfileUrlIsNull() throws Exception {
		AnimalProfile profileWithNullUrl = AnimalProfile.builder()
			.profile(null)
			.animal(testAnimal)
			.build();

		ProfileImageData result = processor.process(profileWithNullUrl);

		assertThat(result).isNull();
		verify(httpClient, never()).send(any(), any());
	}

	@Test
	@DisplayName("profile URL이 빈 문자열이면 null을 반환한다")
	void shouldReturnNullWhenProfileUrlIsBlank() throws Exception {
		AnimalProfile profileWithBlankUrl = AnimalProfile.builder()
			.profile("   ")
			.animal(testAnimal)
			.build();

		ProfileImageData result = processor.process(profileWithBlankUrl);

		assertThat(result).isNull();
		verify(httpClient, never()).send(any(), any());
	}

	@Test
	@DisplayName("다운로드한 이미지가 비어있으면 null을 반환한다")
	void shouldReturnNullWhenDownloadedImageIsEmpty() throws Exception {
		HttpResponse<InputStream> response = (HttpResponse<InputStream>)mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(200);
		when(response.body()).thenAnswer(invocation -> new ByteArrayInputStream(new byte[0]));
		when(httpClient.send(any(), any())).thenAnswer(invocation -> response);

		ProfileImageData result = processor.process(testProfile);

		assertThat(result).isNull();
		verify(httpClient, times(1)).send(any(), any());
	}
}
