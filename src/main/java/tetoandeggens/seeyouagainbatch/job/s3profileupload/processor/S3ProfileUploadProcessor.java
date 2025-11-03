package tetoandeggens.seeyouagainbatch.job.s3profileupload.processor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tetoandeggens.seeyouagainbatch.domain.AbandonedAnimalProfile;
import tetoandeggens.seeyouagainbatch.domain.AbandonedAnimalS3Profile;
import tetoandeggens.seeyouagainbatch.domain.ImageType;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.exception.ImageNotFoundException;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ProfileUploadProcessor implements ItemProcessor<AbandonedAnimalProfile, AbandonedAnimalS3Profile> {

	private static final String S3_KEY_PREFIX = "abandoned-animal-profiles/";
	private static final String FILE_EXTENSION = ".webp";
	private static final String UNDERSCORE = "_";
	private static final String LEFT_BRACKET = "[";
	private static final String RIGHT_BRACKET = "]";
	private static final String SPACE = " ";
	private static final String ENCODED_LEFT_BRACKET = "%5B";
	private static final String ENCODED_RIGHT_BRACKET = "%5D";
	private static final String ENCODED_SPACE = "%20";
	private static final int HTTP_OK = 200;
	private static final int HTTP_NOT_FOUND = 404;

	private final S3Client s3Client;
	private final HttpClient httpClient;

	@Value("${aws.s3.bucket}")
	private String bucketName;

	@Override
	public AbandonedAnimalS3Profile process(AbandonedAnimalProfile profile) {
		try {
			String s3Key = uploadProfileToS3(profile);

			if (s3Key != null) {
				return AbandonedAnimalS3Profile.builder()
					.objectKey(s3Key)
					.abandonedAnimal(profile.getAbandonedAnimal())
					.build();
			} else {
				return null;
			}

		} catch (ImageNotFoundException e) {
			log.warn("이미지를 찾을 수 없음. Profile ID: {}, 프로필 URL 삭제 처리", profile.getId());
			profile.clearProfile();
			return null;
		} catch (Exception e) {
			log.error("프로세싱 중 예외 발생. Profile ID: {}, Error: {}", profile.getId(), e.getMessage(), e);
			return null;
		}
	}

	private String uploadProfileToS3(AbandonedAnimalProfile profile) throws ImageNotFoundException {
		String profileUrl = profile.getProfile();
		if (profileUrl == null || profileUrl.isBlank()) {
			return null;
		}

		String s3Key = generateS3Key(profile.getId());

		try {
			HttpResponse<InputStream> response = downloadImageAsStream(profileUrl);

			try (InputStream inputStream = response.body()) {
				uploadToS3(s3Key, inputStream);
			}

			return s3Key;

		} catch (ImageNotFoundException e) {
			throw e;
		} catch (Exception e) {
			log.error("S3 업로드 실패. Profile ID: {}, URL: {}, Error: {}", profile.getId(), profileUrl, e.getMessage(), e);
			return null;
		}
	}

	private String generateS3Key(Long abandonedAnimalProfileId) {
		String uuid = UUID.randomUUID().toString();
		return S3_KEY_PREFIX + abandonedAnimalProfileId + UNDERSCORE + uuid + FILE_EXTENSION;
	}

	private HttpResponse<InputStream> downloadImageAsStream(String imageUrl) throws IOException, InterruptedException, URISyntaxException, ImageNotFoundException {
		URI uri = encodeUrl(imageUrl);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(uri)
			.GET()
			.build();

		HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

		if (response.statusCode() == HTTP_NOT_FOUND) {
			throw new ImageNotFoundException("이미지를 찾을 수 없음. URL: " + imageUrl);
		}

		if (response.statusCode() != HTTP_OK) {
			throw new IOException("이미지 다운로드 실패. Status: " + response.statusCode() + ", URL: " + imageUrl);
		}

		return response;
	}

	private URI encodeUrl(String url) throws URISyntaxException {
		String encodedUrl = url.replace(LEFT_BRACKET, ENCODED_LEFT_BRACKET)
			.replace(RIGHT_BRACKET, ENCODED_RIGHT_BRACKET)
			.replace(SPACE, ENCODED_SPACE);

		return new URI(encodedUrl);
	}

	private void uploadToS3(String key, InputStream inputStream) throws IOException {
		byte[] imageBytes = inputStream.readAllBytes();

		if (imageBytes.length == 0) {
			throw new IOException("다운로드한 이미지가 비어있음");
		}

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(key)
			.contentType(ImageType.WEBP.getType())
			.build();

		s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));
	}
}