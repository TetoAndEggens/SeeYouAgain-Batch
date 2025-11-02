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
import tetoandeggens.seeyouagainbatch.constant.AbandonedAnimalS3ProfileEntityField;
import tetoandeggens.seeyouagainbatch.domain.AbandonedAnimalS3Profile;

@Slf4j
@Component
public class S3ProfileUploadWriter implements ItemWriter<AbandonedAnimalS3Profile> {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public S3ProfileUploadWriter(@Qualifier("businessNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	@Override
	public void write(Chunk<? extends AbandonedAnimalS3Profile> chunk) {
		List<? extends AbandonedAnimalS3Profile> validItems = chunk.getItems().stream()
			.filter(item -> item != null && item.getObjectKey() != null)
			.collect(Collectors.toList());

		if (validItems.isEmpty()) {
			return;
		}

		bulkInsertS3Profiles(validItems);
	}

	private void bulkInsertS3Profiles(List<? extends AbandonedAnimalS3Profile> profiles) {
		String insertSql = "INSERT INTO abandoned_animal_s3_profile (object_key, image_type, abandoned_animal_id, created_at, updated_at) " +
			"VALUES (:object_key, :image_type, :abandoned_animal_id, NOW(), NOW())";

		SqlParameterSource[] batchParams = new SqlParameterSource[profiles.size()];
		for (int i = 0; i < profiles.size(); i++) {
			AbandonedAnimalS3Profile profile = profiles.get(i);
			batchParams[i] = new MapSqlParameterSource()
				.addValue(AbandonedAnimalS3ProfileEntityField.OBJECT_KEY.getColumnName(), profile.getObjectKey())
				.addValue(AbandonedAnimalS3ProfileEntityField.IMAGE_TYPE.getColumnName(), profile.getImageType().name())
				.addValue(AbandonedAnimalS3ProfileEntityField.ABANDONED_ANIMAL_ID.getColumnName(), profile.getAbandonedAnimal().getId());
		}

		namedParameterJdbcTemplate.batchUpdate(insertSql, batchParams);
		log.info("{}개의 S3 프로필을 Bulk Insert 완료", profiles.size());
	}
}