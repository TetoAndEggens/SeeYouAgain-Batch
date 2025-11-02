package tetoandeggens.seeyouagainbatch.job.s3profileupload.processor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ProfileUploadProcessor implements ItemProcessor<AbandonedAnimalProfile, AbandonedAnimalS3Profile> {

	private static final String S3_KEY_PREFIX = "abandoned-animal-profiles/";

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

		} catch (Exception e) {
			log.error("프로세싱 중 예외 발생. Profile ID: {}, Error: {}", profile.getId(), e.getMessage(), e);
			return null;
		}
	}

	private String uploadProfileToS3(AbandonedAnimalProfile profile) {
		String profileUrl = profile.getProfile();
		if (profileUrl == null || profileUrl.isBlank()) {
			return null;
		}

		String s3Key = generateS3Key(profile.getId());

		try {
			HttpResponse<InputStream> response = downloadImageAsStream(profileUrl);
			long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(0);

			try (InputStream inputStream = response.body()) {
				uploadToS3(s3Key, inputStream, contentLength);
			}

			log.info("S3 업로드 완료. Key: {}, Original URL: {}", s3Key, profileUrl);
			return s3Key;

		} catch (Exception e) {
			log.error("S3 업로드 실패. Profile ID: {}, URL: {}, Error: {}", profile.getId(), profileUrl, e.getMessage(), e);
			return null;
		}
	}

	private String generateS3Key(Long abandonedAnimalProfileId) {
		String uuid = UUID.randomUUID().toString();
		return S3_KEY_PREFIX + abandonedAnimalProfileId + "_" + uuid + ".webp";
	}

	private HttpResponse<InputStream> downloadImageAsStream(String imageUrl) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(imageUrl))
			.GET()
			.build();

		HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

		if (response.statusCode() != 200) {
			throw new IOException("이미지 다운로드 실패. Status: " + response.statusCode());
		}

		return response;
	}

	private void uploadToS3(String key, InputStream inputStream, long contentLength) {
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(key)
			.contentType(ImageType.WEBP.getType())
			.build();

		s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
	}
}