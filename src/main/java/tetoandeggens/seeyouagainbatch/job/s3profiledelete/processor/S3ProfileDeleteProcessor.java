package tetoandeggens.seeyouagainbatch.job.s3profiledelete.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import tetoandeggens.seeyouagainbatch.domain.AnimalS3Profile;

@Slf4j
@Component
public class S3ProfileDeleteProcessor implements ItemProcessor<AnimalS3Profile, AnimalS3Profile> {

	private final S3Client s3Client;
	private final String bucketName;
	private final String cloudfrontDomain;

	public S3ProfileDeleteProcessor(
		S3Client s3Client,
		@Value("${aws.s3.bucket}") String bucketName,
		@Value("${cloudfront.domain}") String cloudfrontDomain
	) {
		this.s3Client = s3Client;
		this.bucketName = bucketName;
		this.cloudfrontDomain = cloudfrontDomain;
	}

	@Override
	public AnimalS3Profile process(AnimalS3Profile s3Profile) {
		try {
			String s3Key = extractS3Key(s3Profile.getProfile());

			if (s3Key == null || s3Key.isBlank()) {
				return s3Profile;
			}

			deleteFromS3(s3Key);
			log.info("S3 파일 삭제 완료. S3Profile ID: {}, S3 Key: {}", s3Profile.getId(), s3Key);

			return s3Profile;

		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				log.warn("S3에 파일이 존재하지 않음. S3Profile ID: {}, 계속 진행", s3Profile.getId());
				return s3Profile;
			} else {
				log.error("S3 삭제 실패. S3Profile ID: {}, Error: {}",
					s3Profile.getId(), e.getMessage(), e);
				return null;
			}
		} catch (Exception e) {
			log.error("처리 중 예외 발생. S3Profile ID: {}, Error: {}",
				s3Profile.getId(), e.getMessage(), e);
			return null;
		}
	}

	private String extractS3Key(String profileUrl) {
		if (profileUrl == null || profileUrl.isBlank()) {
			return null;
		}

		if (profileUrl.startsWith(cloudfrontDomain)) {
			return profileUrl.substring(cloudfrontDomain.length());
		}

		if (profileUrl.startsWith("http://") || profileUrl.startsWith("https://")) {
			int domainEnd = profileUrl.indexOf('/', 8);
			if (domainEnd > 0) {
				return profileUrl.substring(domainEnd + 1);
			}
		}

		return profileUrl;
	}

	private void deleteFromS3(String s3Key) {
		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
			.bucket(bucketName)
			.key(s3Key)
			.build();

		s3Client.deleteObject(deleteObjectRequest);
	}
}