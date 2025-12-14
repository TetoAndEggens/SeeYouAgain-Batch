package tetoandeggens.seeyouagainbatch.job.s3profileupload.writer;

import java.util.List;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.constant.AnimalS3ProfileEntityField;
import tetoandeggens.seeyouagainbatch.domain.AnimalS3Profile;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.dto.ProfileImageData;
import tetoandeggens.seeyouagainbatch.job.s3profileupload.service.ParallelS3UploadService;

@Slf4j
@Component
public class S3ProfileUploadWriter implements ItemWriter<ProfileImageData> {

	private final ParallelS3UploadService parallelS3UploadService;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final String cloudfrontDomain;

	public S3ProfileUploadWriter(
		ParallelS3UploadService parallelS3UploadService,
		@Qualifier("businessNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
		@Value("${cloudfront.domain}") String cloudfrontDomain
	) {
		this.parallelS3UploadService = parallelS3UploadService;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.cloudfrontDomain = cloudfrontDomain;
	}

	@Override
	public void write(Chunk<? extends ProfileImageData> chunk) {
		List<? extends ProfileImageData> items = chunk.getItems().stream()
			.filter(item -> item != null && item.getImageBytes() != null)
			.toList();

		if (items.isEmpty()) {
			return;
		}

		parallelS3UploadService.uploadBatch(items);

		List<AnimalS3Profile> s3Profiles = items.stream()
			.map(imageData -> AnimalS3Profile.builder()
				.profile(cloudfrontDomain + imageData.getS3Key())
				.animal(imageData.getProfile().getAnimal())
				.build())
			.toList();

		bulkInsertS3Profiles(s3Profiles);
	}

	private void bulkInsertS3Profiles(List<AnimalS3Profile> profiles) {
		String insertSql = "INSERT INTO animal_s3_profile (profile, image_type, animal_id, is_deleted, created_at, updated_at) " +
			"VALUES (:profile, :image_type, :animal_id, false, NOW(), NOW())";

		SqlParameterSource[] batchParams = new SqlParameterSource[profiles.size()];
		for (int i = 0; i < profiles.size(); i++) {
			AnimalS3Profile profile = profiles.get(i);
			batchParams[i] = new MapSqlParameterSource()
				.addValue(AnimalS3ProfileEntityField.PROFILE.getColumnName(), profile.getProfile())
				.addValue(AnimalS3ProfileEntityField.IMAGE_TYPE.getColumnName(), profile.getImageType().name())
				.addValue(AnimalS3ProfileEntityField.ANIMAL_ID.getColumnName(), profile.getAnimal().getId());
		}

		namedParameterJdbcTemplate.batchUpdate(insertSql, batchParams);
		log.info("{}개의 S3 프로필을 Bulk Insert 완료", profiles.size());
	}
}