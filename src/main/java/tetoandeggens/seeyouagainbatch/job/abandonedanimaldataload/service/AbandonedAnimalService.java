package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.constant.AbandonedAnimalEntityField;
import tetoandeggens.seeyouagainbatch.domain.AbandonedAnimal;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.converter.AbandonedAnimalConverter;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto.AbandonedAnimalPublicDataDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbandonedAnimalService {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final AbandonedAnimalConverter abandonedAnimalConverter;

	@Transactional
	public Map<String, Long> processAbandonedAnimals(List<AbandonedAnimalPublicDataDto> publicDataList,
		Map<String, Long> centerLocationIdMap,
		Map<String, Long> breedTypeIdMap) {
		List<AbandonedAnimal> abandonedAnimals = abandonedAnimalConverter.convertToEntities(
			publicDataList, centerLocationIdMap, breedTypeIdMap);

		bulkInsert(abandonedAnimals);

		return getAbandonedAnimalIdMap(abandonedAnimals);
	}

	private void bulkInsert(List<AbandonedAnimal> abandonedAnimals) {
		if (abandonedAnimals.isEmpty()) {
			log.info("저장할 유기동물 데이터가 없습니다");
			return;
		}

		String sql = "INSERT INTO abandoned_animal (" +
			"desertion_no, happen_date, happen_place, city, town, species, color, birth, weight, " +
			"notice_no, notice_start_date, notice_end_date, process_state, " +
			"sex, neutered_state, special_mark, center_phone, final_updated_at, " +
			"center_location_id, breed_type_id, created_at, updated_at) " +
			"VALUES (" +
			":desertion_no, :happen_date, :happen_place, :city, :town, :species, :color, :birth, :weight, " +
			":notice_no, :notice_start_date, :notice_end_date, :process_state, " +
			":sex, :neutered_state, :special_mark, :center_phone, :final_updated_at, " +
			":center_location_id, :breed_type_id, NOW(), NOW())";

		SqlParameterSource[] batchParams = new SqlParameterSource[abandonedAnimals.size()];
		for (int i = 0; i < abandonedAnimals.size(); i++) {
			batchParams[i] = createParameterSource(abandonedAnimals.get(i));
		}

		namedParameterJdbcTemplate.batchUpdate(sql, batchParams);
		log.info("{}마리의 유기동물 정보를 Bulk Insert 완료", abandonedAnimals.size());
	}

	private Map<String, Long> getAbandonedAnimalIdMap(List<AbandonedAnimal> abandonedAnimals) {
		if (abandonedAnimals.isEmpty()) {
			return Map.of();
		}

		List<String> desertionNos = new ArrayList<>();
		for (AbandonedAnimal animal : abandonedAnimals) {
			desertionNos.add(animal.getDesertionNo());
		}

		String selectSql = "SELECT abandoned_animal_id, desertion_no " +
			"FROM abandoned_animal " +
			"WHERE desertion_no IN (:desertionNos)";

		MapSqlParameterSource selectParams = new MapSqlParameterSource()
			.addValue("desertionNos", desertionNos);

		return namedParameterJdbcTemplate.query(
			selectSql,
			selectParams,
			rs -> {
				Map<String, Long> resultMap = new java.util.HashMap<>();
				while (rs.next()) {
					resultMap.put(rs.getString("desertion_no"), rs.getLong("abandoned_animal_id"));
				}
				return resultMap;
			}
		);
	}

	private MapSqlParameterSource createParameterSource(AbandonedAnimal animal) {
		return new MapSqlParameterSource()
			.addValue(AbandonedAnimalEntityField.DESERTION_NO.getColumnName(), animal.getDesertionNo())
			.addValue(AbandonedAnimalEntityField.HAPPEN_DATE.getColumnName(), animal.getHappenDate())
			.addValue(AbandonedAnimalEntityField.HAPPEN_PLACE.getColumnName(), animal.getHappenPlace())
			.addValue(AbandonedAnimalEntityField.CITY.getColumnName(), animal.getCity())
			.addValue(AbandonedAnimalEntityField.TOWN.getColumnName(), animal.getTown())
			.addValue(AbandonedAnimalEntityField.SPECIES.getColumnName(), animal.getSpecies().name())
			.addValue(AbandonedAnimalEntityField.COLOR.getColumnName(), animal.getColor())
			.addValue(AbandonedAnimalEntityField.BIRTH.getColumnName(), animal.getBirth())
			.addValue(AbandonedAnimalEntityField.WEIGHT.getColumnName(), animal.getWeight())
			.addValue(AbandonedAnimalEntityField.NOTICE_NO.getColumnName(), animal.getNoticeNo())
			.addValue(AbandonedAnimalEntityField.NOTICE_START_DATE.getColumnName(), animal.getNoticeStartDate())
			.addValue(AbandonedAnimalEntityField.NOTICE_END_DATE.getColumnName(), animal.getNoticeEndDate())
			.addValue(AbandonedAnimalEntityField.PROCESS_STATE.getColumnName(), animal.getProcessState())
			.addValue(AbandonedAnimalEntityField.SEX.getColumnName(), animal.getSex().name())
			.addValue(AbandonedAnimalEntityField.NEUTERED_STATE.getColumnName(), animal.getNeuteredState().name())
			.addValue(AbandonedAnimalEntityField.SPECIAL_MARK.getColumnName(), animal.getSpecialMark())
			.addValue(AbandonedAnimalEntityField.CENTER_PHONE.getColumnName(), animal.getCenterPhone())
			.addValue(AbandonedAnimalEntityField.FINAL_UPDATED_AT.getColumnName(), animal.getFinalUpdatedAt())
			.addValue(AbandonedAnimalEntityField.CENTER_LOCATION_ID.getColumnName(), animal.getCenterLocation().getId())
			.addValue(AbandonedAnimalEntityField.BREED_TYPE_ID.getColumnName(), animal.getBreedType().getId());
	}
}