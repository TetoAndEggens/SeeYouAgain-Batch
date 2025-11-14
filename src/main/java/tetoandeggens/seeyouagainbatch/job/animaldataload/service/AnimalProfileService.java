package tetoandeggens.seeyouagainbatch.job.animaldataload.service;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.constant.AnimalProfileEntityField;
import tetoandeggens.seeyouagainbatch.domain.AnimalProfile;
import tetoandeggens.seeyouagainbatch.job.animaldataload.converter.AnimalConverter;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.AnimalPublicDataDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimalProfileService {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final AnimalConverter animalConverter;

	@Transactional
	public void processAnimalProfiles(List<AnimalPublicDataDto> publicDataList,
		Map<String, Long> animalIdMap) {
		List<AnimalProfile> profiles = animalConverter.convertToProfiles(
			publicDataList, animalIdMap);

		bulkInsertProfiles(profiles);
	}

	private void bulkInsertProfiles(List<AnimalProfile> profiles) {
		if (profiles.isEmpty()) {
			log.info("저장할 프로필 이미지가 없습니다");
			return;
		}

		String insertSql = "INSERT INTO animal_profile (profile, happen_date, animal_id, created_at, updated_at) " +
			"VALUES (:profile, :happen_date, :animal_id, NOW(), NOW())";

		SqlParameterSource[] batchParams = new SqlParameterSource[profiles.size()];
		for (int i = 0; i < profiles.size(); i++) {
			AnimalProfile profile = profiles.get(i);
			batchParams[i] = new MapSqlParameterSource()
				.addValue(AnimalProfileEntityField.PROFILE.getColumnName(), profile.getProfile())
				.addValue(AnimalProfileEntityField.HAPPEN_DATE.getColumnName(), profile.getHappenDate())
				.addValue(AnimalProfileEntityField.ANIMAL_ID.getColumnName(), profile.getAnimal().getId());
		}

		namedParameterJdbcTemplate.batchUpdate(insertSql, batchParams);
		log.info("{}개의 프로필 이미지를 Bulk Insert 완료", profiles.size());
	}
}