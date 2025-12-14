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
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.domain.AnimalProfile;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.dto.ProfileImageData;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.exception.ImageNotFoundException;

@Slf4j
@Component
public class S3ProfileUploadProcessor implements ItemProcessor<AnimalProfile, ProfileImageData> {

	private static final String S3_KEY_PREFIX = "animal-profiles/";
	private static final String PUBLIC_DATA_PREFIX = "public-data/";
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

	private final HttpClient httpClient;

	public S3ProfileUploadProcessor(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	public ProfileImageData process(AnimalProfile profile) {
		try {
			return downloadImageData(profile);
		} catch (ImageNotFoundException e) {
			log.warn("이미지를 찾을 수 없음. Profile ID: {}, 프로필 URL 삭제 처리", profile.getId());
			profile.clearProfile();
			return null;
		} catch (Exception e) {
			log.error("프로세싱 중 예외 발생. Profile ID: {}, Error: {}", profile.getId(), e.getMessage(), e);
			return null;
		}
	}

	private ProfileImageData downloadImageData(AnimalProfile profile) throws ImageNotFoundException {
		String profileUrl = profile.getProfile();
		if (profileUrl == null || profileUrl.isBlank()) {
			return null;
		}

		String s3Key = generateS3Key(profile.getId());

		try {
			HttpResponse<InputStream> response = downloadImageAsStream(profileUrl);

			byte[] imageBytes;
			try (InputStream inputStream = response.body()) {
				imageBytes = inputStream.readAllBytes();
			}

			if (imageBytes.length == 0) {
				throw new IOException("다운로드한 이미지가 비어있음");
			}

			return ProfileImageData.builder()
				.profile(profile)
				.imageBytes(imageBytes)
				.s3Key(s3Key)
				.build();

		} catch (ImageNotFoundException e) {
			throw e;
		} catch (Exception e) {
			log.error("이미지 다운로드 실패. Profile ID: {}, URL: {}, Error: {}", profile.getId(), profileUrl, e.getMessage(), e);
			return null;
		}
	}

	private String generateS3Key(Long animalProfileId) {
		String uuid = UUID.randomUUID().toString();
		return S3_KEY_PREFIX + PUBLIC_DATA_PREFIX + animalProfileId + UNDERSCORE + uuid + FILE_EXTENSION;
	}

	private HttpResponse<InputStream> downloadImageAsStream(String imageUrl) throws
		IOException,
		InterruptedException,
		URISyntaxException,
		ImageNotFoundException {
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

}