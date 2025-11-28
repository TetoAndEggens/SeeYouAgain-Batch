package tetoandeggens.seeyouagainbatch.job.keywordmapping.writer;

import java.time.LocalDateTime;
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
import tetoandeggens.seeyouagainbatch.constant.AnimalByKeywordEntityField;
import tetoandeggens.seeyouagainbatch.domain.AnimalByKeyword;

@Slf4j
@Component
public class KeywordMappingWriter implements ItemWriter<List<AnimalByKeyword>> {

	private static final String PARAM_NOTIFICATION_KEYWORD_ID = "notificationKeywordId";
	private static final String PARAM_ANIMAL_ID = "animalId";
	private static final String PARAM_CREATED_AT = "createdAt";
	private static final String PARAM_UPDATED_AT = "updatedAt";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public KeywordMappingWriter(
		@Qualifier("businessNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate
	) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	@Override
	public void write(Chunk<? extends List<AnimalByKeyword>> chunk) {
		List<AnimalByKeyword> allMappings = new ArrayList<>();

		for (List<AnimalByKeyword> mappings : chunk.getItems()) {
			if (mappings != null && !mappings.isEmpty()) {
				allMappings.addAll(mappings);
			}
		}

		if (allMappings.isEmpty()) {
			return;
		}

		bulkInsertMappings(allMappings);
		log.info("{}개의 매칭 데이터를 DB에 저장 완료", allMappings.size());
	}

	private void bulkInsertMappings(List<AnimalByKeyword> mappings) {
		String insertSql = "INSERT INTO animal_by_keyword " +
			"(" + AnimalByKeywordEntityField.NOTIFICATION_KEYWORD_ID.getColumnName() + ", " +
			AnimalByKeywordEntityField.ANIMAL_ID.getColumnName() + ", " +
			AnimalByKeywordEntityField.CREATED_AT.getColumnName() + ", " +
			AnimalByKeywordEntityField.UPDATED_AT.getColumnName() + ") " +
			"VALUES (:" + PARAM_NOTIFICATION_KEYWORD_ID + ", :" + PARAM_ANIMAL_ID + ", :" + PARAM_CREATED_AT + ", :" + PARAM_UPDATED_AT + ")";

		SqlParameterSource[] batchParams = new SqlParameterSource[mappings.size()];
		LocalDateTime now = LocalDateTime.now();

		for (int i = 0; i < mappings.size(); i++) {
			AnimalByKeyword mapping = mappings.get(i);
			batchParams[i] = new MapSqlParameterSource()
				.addValue(PARAM_NOTIFICATION_KEYWORD_ID, mapping.getNotificationKeyword().getId())
				.addValue(PARAM_ANIMAL_ID, mapping.getAnimal().getId())
				.addValue(PARAM_CREATED_AT, now)
				.addValue(PARAM_UPDATED_AT, now);
		}

		namedParameterJdbcTemplate.batchUpdate(insertSql, batchParams);
	}
}
