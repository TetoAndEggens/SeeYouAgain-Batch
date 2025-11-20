package tetoandeggens.seeyouagainbatch.job.s3profileupload.writer;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.constant.AnimalS3ProfileEntityField;
import tetoandeggens.seeyouagainbatch.domain.AnimalS3Profile;

@Slf4j
@Component
public class S3ProfileUploadWriter implements ItemWriter<AnimalS3Profile> {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public S3ProfileUploadWriter(@Qualifier("businessNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	@Override
	public void write(Chunk<? extends AnimalS3Profile> chunk) {
		List<? extends AnimalS3Profile> validItems = chunk.getItems().stream()
			.filter(item -> item != null && item.getProfile() != null)
			.collect(Collectors.toList());

		if (validItems.isEmpty()) {
			return;
		}

		bulkInsertS3Profiles(validItems);
	}

	private void bulkInsertS3Profiles(List<? extends AnimalS3Profile> profiles) {
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