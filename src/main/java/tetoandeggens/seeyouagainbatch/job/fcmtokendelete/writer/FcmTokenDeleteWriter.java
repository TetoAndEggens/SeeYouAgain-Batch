package tetoandeggens.seeyouagainbatch.job.fcmtokendelete.writer;

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
import tetoandeggens.seeyouagainbatch.domain.FcmToken;

@Slf4j
@Component
public class FcmTokenDeleteWriter implements ItemWriter<FcmToken> {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public FcmTokenDeleteWriter(
		@Qualifier("businessNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate
	) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	@Override
	public void write(Chunk<? extends FcmToken> chunk) {
		List<FcmToken> validItems = new ArrayList<>();
		for (FcmToken item : chunk.getItems()) {
			if (item != null && item.getId() != null) {
				validItems.add(item);
			}
		}

		if (validItems.isEmpty()) {
			return;
		}

		bulkDeleteFcmTokens(validItems);
	}

	private void bulkDeleteFcmTokens(List<? extends FcmToken> tokens) {
		String deleteSql = "DELETE FROM fcm_token WHERE fcm_token_id = :id";

		SqlParameterSource[] batchParams = new SqlParameterSource[tokens.size()];
		for (int i = 0; i < tokens.size(); i++) {
			FcmToken token = tokens.get(i);
			batchParams[i] = new MapSqlParameterSource()
				.addValue("id", token.getId());
		}

		namedParameterJdbcTemplate.batchUpdate(deleteSql, batchParams);
		log.info("{}개의 FCM 토큰을 DB에서 Hard Delete 완료", tokens.size());
	}
}