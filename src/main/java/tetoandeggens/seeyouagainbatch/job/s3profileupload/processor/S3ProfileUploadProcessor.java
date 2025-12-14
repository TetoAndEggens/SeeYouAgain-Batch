package tetoandeggens.seeyouagainbatch.job.s3profileupload.processor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tetoandeggens.seeyouagainbatch.domain.AnimalProfile;
import tetoandeggens.seeyouagainbatch.domain.AnimalS3Profile;
import tetoandeggens.seeyouagainbatch.domain.ImageType;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.exception.ImageNotFoundException;

@Slf4j
@Component
public class S3ProfileUploadProcessor implements ItemProcessor<AnimalProfile, AnimalS3Profile> {

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
	private static final int LOG_INTERVAL = 500;

	private final S3Client s3Client;
	private final HttpClient httpClient;
	private final String bucketName;
	private final String cloudfrontDomain;
	private final ThreadMXBean threadMXBean;

	// 성능 측정용 변수
	private final AtomicInteger processCount = new AtomicInteger(0);
	private final AtomicLong totalDownloadTime = new AtomicLong(0);
	private final AtomicLong totalUploadTime = new AtomicLong(0);
	private final AtomicLong totalProcessTime = new AtomicLong(0);
	private final AtomicLong totalDownloadCpuTime = new AtomicLong(0);
	private final AtomicLong totalUploadCpuTime = new AtomicLong(0);

	public S3ProfileUploadProcessor(
		S3Client s3Client,
		HttpClient httpClient,
		@Value("${aws.s3.bucket}") String bucketName,
		@Value("${cloudfront.domain}") String cloudfrontDomain
	) {
		this.s3Client = s3Client;
		this.httpClient = httpClient;
		this.bucketName = bucketName;
		this.cloudfrontDomain = cloudfrontDomain;
		this.threadMXBean = ManagementFactory.getThreadMXBean();
	}

	@Override
	public AnimalS3Profile process(AnimalProfile profile) {
		try {
			String s3Key = uploadProfileToS3(profile);

			if (s3Key != null) {
				return AnimalS3Profile.builder()
					.profile(cloudfrontDomain + s3Key)
					.animal(profile.getAnimal())
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

	private String uploadProfileToS3(AnimalProfile profile) throws ImageNotFoundException {
		String profileUrl = profile.getProfile();
		if (profileUrl == null || profileUrl.isBlank()) {
			return null;
		}

		String s3Key = generateS3Key(profile.getId());

		// 전체 프로세스 시작 시간 측정
		long processStartTime = System.currentTimeMillis();
		long downloadTime = 0;
		long uploadTime = 0;
		long downloadCpuTime = 0;
		long uploadCpuTime = 0;

		try {
			// 다운로드 시작 시간 측정
			long downloadStartTime = System.currentTimeMillis();
			long downloadCpuStartTime = threadMXBean.getCurrentThreadCpuTime();

			HttpResponse<InputStream> response = downloadImageAsStream(profileUrl);

			long downloadCpuEndTime = threadMXBean.getCurrentThreadCpuTime();
			long downloadEndTime = System.currentTimeMillis();

			downloadTime = downloadEndTime - downloadStartTime;
			downloadCpuTime = (downloadCpuEndTime - downloadCpuStartTime) / 1_000_000; // 나노초 → 밀리초

			// S3 업로드 시작 시간 측정
			long uploadStartTime = System.currentTimeMillis();
			long uploadCpuStartTime = threadMXBean.getCurrentThreadCpuTime();

			try (InputStream inputStream = response.body()) {
				uploadToS3(s3Key, inputStream);
			}

			long uploadCpuEndTime = threadMXBean.getCurrentThreadCpuTime();
			long uploadEndTime = System.currentTimeMillis();

			uploadTime = uploadEndTime - uploadStartTime;
			uploadCpuTime = (uploadCpuEndTime - uploadCpuStartTime) / 1_000_000; // 나노초 → 밀리초

			// 전체 프로세스 종료 시간 측정
			long processEndTime = System.currentTimeMillis();
			long processTime = processEndTime - processStartTime;

			// 성능 통계 업데이트
			totalDownloadTime.addAndGet(downloadTime);
			totalUploadTime.addAndGet(uploadTime);
			totalProcessTime.addAndGet(processTime);
			totalDownloadCpuTime.addAndGet(downloadCpuTime);
			totalUploadCpuTime.addAndGet(uploadCpuTime);
			int count = processCount.incrementAndGet();

			// 500개 처리마다 성능 로그 출력
			if (count % LOG_INTERVAL == 0) {
				logPerformanceMetrics(count);
			}

			return s3Key;

		} catch (ImageNotFoundException e) {
			throw e;
		} catch (Exception e) {
			log.error("S3 업로드 실패. Profile ID: {}, URL: {}, Error: {}", profile.getId(), profileUrl, e.getMessage(), e);
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

	private void logPerformanceMetrics(int count) {
		long avgDownloadTime = totalDownloadTime.get() / count;
		long avgUploadTime = totalUploadTime.get() / count;
		long avgProcessTime = totalProcessTime.get() / count;
		long avgDownloadCpuTime = totalDownloadCpuTime.get() / count;
		long avgUploadCpuTime = totalUploadCpuTime.get() / count;

		long avgDownloadWaitTime = avgDownloadTime - avgDownloadCpuTime;
		long avgUploadWaitTime = avgUploadTime - avgUploadCpuTime;

		double downloadRatio = (double) totalDownloadTime.get() / totalProcessTime.get() * 100;
		double uploadRatio = (double) totalUploadTime.get() / totalProcessTime.get() * 100;

		log.info("성능 측정 {}건 처리 완료", count);
		log.info("- 다운로드: 전체 {}ms (CPU {}ms, 대기 {}ms)", avgDownloadTime, avgDownloadCpuTime, avgDownloadWaitTime);
		log.info("- S3 업로드: 전체 {}ms (CPU {}ms, 대기 {}ms)", avgUploadTime, avgUploadCpuTime, avgUploadWaitTime);
		log.info("- 전체 처리 시간: {}ms", avgProcessTime);
		log.info("- 비율: 다운로드 {}%, S3 업로드 {}%", String.format("%.2f", downloadRatio), String.format("%.2f", uploadRatio));
	}
}