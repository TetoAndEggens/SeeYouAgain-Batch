package tetoandeggens.seeyouagainbatch.job.s3profiledelete.writer;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.domain.AnimalS3Profile;

@Slf4j
@Component
public class S3ProfileDeleteWriter implements ItemWriter<AnimalS3Profile> {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public S3ProfileDeleteWriter(
		@Qualifier("businessNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate
	) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	@Override
	public void write(Chunk<? extends AnimalS3Profile> chunk) {
		List<AnimalS3Profile> validItems = new ArrayList<>();
		for (AnimalS3Profile item : chunk.getItems()) {
			if (item != null && item.getId() != null) {
				validItems.add(item);
			}
		}

		if (validItems.isEmpty()) {
			return;
		}

		bulkDeleteS3Profiles(validItems);
	}

	private void bulkDeleteS3Profiles(List<? extends AnimalS3Profile> profiles) {
		String deleteSql = "DELETE FROM animal_s3_profile WHERE animal_s3_profile_id = :id";

		SqlParameterSource[] batchParams = new SqlParameterSource[profiles.size()];
		for (int i = 0; i < profiles.size(); i++) {
			AnimalS3Profile profile = profiles.get(i);
			batchParams[i] = new MapSqlParameterSource()
				.addValue("id", profile.getId());
		}

		namedParameterJdbcTemplate.batchUpdate(deleteSql, batchParams);
		log.info("{}개의 S3 프로필을 DB에서 Hard Delete 완료", profiles.size());
	}
}