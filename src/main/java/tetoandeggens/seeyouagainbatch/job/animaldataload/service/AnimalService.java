package tetoandeggens.seeyouagainbatch.job.animaldataload.service;

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
import tetoandeggens.seeyouagainbatch.constant.AnimalEntityField;
import tetoandeggens.seeyouagainbatch.domain.Animal;
import tetoandeggens.seeyouagainbatch.job.animaldataload.converter.AnimalConverter;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.AnimalPublicDataDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimalService {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final AnimalConverter animalConverter;

	@Transactional
	public Map<String, Long> processAnimals(List<AnimalPublicDataDto> publicDataList,
		Map<String, Long> animalLocationIdMap,
		Map<String, Long> breedTypeIdMap) {
		List<Animal> animals = animalConverter.convertToEntities(
			publicDataList, animalLocationIdMap, breedTypeIdMap);

		bulkInsert(animals);

		return getAnimalIdMap(animals);
	}

	private void bulkInsert(List<Animal> animals) {
		if (animals.isEmpty()) {
			log.info("저장할 동물 데이터가 없습니다");
			return;
		}

		String sql = "INSERT INTO animal (" +
			"animal_type, desertion_no, happen_date, happen_place, city, town, species, color, birth, weight, " +
			"notice_no, notice_start_date, notice_end_date, process_state, " +
			"sex, neutered_state, special_mark, center_phone, final_updated_at, " +
			"animal_location_id, breed_type_id, is_deleted, created_at, updated_at) " +
			"VALUES (" +
			":animal_type, :desertion_no, :happen_date, :happen_place, :city, :town, :species, :color, :birth, :weight, " +
			":notice_no, :notice_start_date, :notice_end_date, :process_state, " +
			":sex, :neutered_state, :special_mark, :center_phone, :final_updated_at, " +
			":animal_location_id, :breed_type_id, false, NOW(), NOW())";


		SqlParameterSource[] batchParams = new SqlParameterSource[animals.size()];
		for (int i = 0; i < animals.size(); i++) {
			batchParams[i] = createParameterSource(animals.get(i));
		}

		namedParameterJdbcTemplate.batchUpdate(sql, batchParams);
		log.info("{}마리의 동물 정보를 Bulk Insert 완료", animals.size());
	}

	private Map<String, Long> getAnimalIdMap(List<Animal> animals) {
		if (animals.isEmpty()) {
			return Map.of();
		}

		List<String> desertionNos = new ArrayList<>();
		for (Animal animal : animals) {
			desertionNos.add(animal.getDesertionNo());
		}

		String selectSql = "SELECT animal_id, desertion_no " +
			"FROM animal " +
			"WHERE desertion_no IN (:desertionNos)";

		MapSqlParameterSource selectParams = new MapSqlParameterSource()
			.addValue("desertionNos", desertionNos);

		return namedParameterJdbcTemplate.query(
			selectSql,
			selectParams,
			rs -> {
				Map<String, Long> resultMap = new java.util.HashMap<>();
				while (rs.next()) {
					resultMap.put(rs.getString("desertion_no"), rs.getLong("animal_id"));
				}
				return resultMap;
			}
		);
	}

	private MapSqlParameterSource createParameterSource(Animal animal) {
		return new MapSqlParameterSource()
			.addValue(AnimalEntityField.ANIMAL_TYPE.getColumnName(), animal.getAnimalType().name())
			.addValue(AnimalEntityField.DESERTION_NO.getColumnName(), animal.getDesertionNo())
			.addValue(AnimalEntityField.HAPPEN_DATE.getColumnName(), animal.getHappenDate())
			.addValue(AnimalEntityField.HAPPEN_PLACE.getColumnName(), animal.getHappenPlace())
			.addValue(AnimalEntityField.CITY.getColumnName(), animal.getCity())
			.addValue(AnimalEntityField.TOWN.getColumnName(), animal.getTown())
			.addValue(AnimalEntityField.SPECIES.getColumnName(), animal.getSpecies().name())
			.addValue(AnimalEntityField.COLOR.getColumnName(), animal.getColor())
			.addValue(AnimalEntityField.BIRTH.getColumnName(), animal.getBirth())
			.addValue(AnimalEntityField.WEIGHT.getColumnName(), animal.getWeight())
			.addValue(AnimalEntityField.NOTICE_NO.getColumnName(), animal.getNoticeNo())
			.addValue(AnimalEntityField.NOTICE_START_DATE.getColumnName(), animal.getNoticeStartDate())
			.addValue(AnimalEntityField.NOTICE_END_DATE.getColumnName(), animal.getNoticeEndDate())
			.addValue(AnimalEntityField.PROCESS_STATE.getColumnName(), animal.getProcessState())
			.addValue(AnimalEntityField.SEX.getColumnName(), animal.getSex().name())
			.addValue(AnimalEntityField.NEUTERED_STATE.getColumnName(), animal.getNeuteredState().name())
			.addValue(AnimalEntityField.SPECIAL_MARK.getColumnName(), animal.getSpecialMark())
			.addValue(AnimalEntityField.CENTER_PHONE.getColumnName(), animal.getCenterPhone())
			.addValue(AnimalEntityField.FINAL_UPDATED_AT.getColumnName(), animal.getFinalUpdatedAt())
			.addValue(AnimalEntityField.ANIMAL_LOCATION_ID.getColumnName(), animal.getAnimalLocation().getId())
			.addValue(AnimalEntityField.BREED_TYPE_ID.getColumnName(), animal.getBreedType().getId());
	}
}