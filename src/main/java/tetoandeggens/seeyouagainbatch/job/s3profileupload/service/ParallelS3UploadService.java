package tetoandeggens.seeyouagainbatch.job.s3profileupload.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tetoandeggens.seeyouagainbatch.domain.ImageType;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.dto.ProfileImageData;

@Slf4j
@Service
public class ParallelS3UploadService {

	private final S3Client s3Client;
	private final ExecutorService executorService;
	private final String bucketName;

	public ParallelS3UploadService(
		S3Client s3Client,
		ExecutorService s3UploadExecutorService,
		@Value("${aws.s3.bucket}") String bucketName
	) {
		this.s3Client = s3Client;
		this.executorService = s3UploadExecutorService;
		this.bucketName = bucketName;
	}

	public void uploadBatch(List<? extends ProfileImageData> items) {
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		for (ProfileImageData imageData : items) {
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try {
					uploadToS3(imageData);
				} catch (Exception e) {
					log.error("S3 업로드 실패. Profile ID: {}, S3 Key: {}, Error: {}",
						imageData.getProfile().getId(),
						imageData.getS3Key(),
						e.getMessage(), e);
				}
			}, executorService);
			futures.add(future);
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
	}

	private void uploadToS3(ProfileImageData imageData) {
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(imageData.getS3Key())
			.contentType(ImageType.WEBP.getType())
			.build();

		s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageData.getImageBytes()));
	}
}