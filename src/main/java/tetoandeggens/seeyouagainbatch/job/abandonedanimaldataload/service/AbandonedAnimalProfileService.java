package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.service;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.constant.AbandonedAnimalProfileEntityField;
import tetoandeggens.seeyouagainbatch.domain.AbandonedAnimalProfile;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.converter.AbandonedAnimalConverter;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto.AbandonedAnimalPublicDataDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbandonedAnimalProfileService {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final AbandonedAnimalConverter abandonedAnimalConverter;

	@Transactional
	public void processAbandonedAnimalProfiles(List<AbandonedAnimalPublicDataDto> publicDataList,
		Map<String, Long> abandonedAnimalIdMap) {
		List<AbandonedAnimalProfile> profiles = abandonedAnimalConverter.convertToProfiles(
			publicDataList, abandonedAnimalIdMap);

		bulkInsertProfiles(profiles);
	}

	private void bulkInsertProfiles(List<AbandonedAnimalProfile> profiles) {
		if (profiles.isEmpty()) {
			log.info("저장할 프로필 이미지가 없습니다");
			return;
		}

		String insertSql = "INSERT INTO abandoned_animal_profile (profile, abandoned_animal_id, created_at, updated_at) " +
			"VALUES (:profile, :abandoned_animal_id, NOW(), NOW())";

		SqlParameterSource[] batchParams = new SqlParameterSource[profiles.size()];
		for (int i = 0; i < profiles.size(); i++) {
			AbandonedAnimalProfile profile = profiles.get(i);
			batchParams[i] = new MapSqlParameterSource()
				.addValue(AbandonedAnimalProfileEntityField.PROFILE.getColumnName(), profile.getProfile())
				.addValue(AbandonedAnimalProfileEntityField.ABANDONED_ANIMAL_ID.getColumnName(), profile.getAbandonedAnimal().getId());
		}

		namedParameterJdbcTemplate.batchUpdate(insertSql, batchParams);
		log.info("{}개의 프로필 이미지를 Bulk Insert 완료", profiles.size());
	}
}
