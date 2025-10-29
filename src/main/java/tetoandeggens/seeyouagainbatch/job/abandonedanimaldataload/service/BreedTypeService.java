package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.constant.BreedTypeEntityField;
import tetoandeggens.seeyouagainbatch.domain.BreedType;
import tetoandeggens.seeyouagainbatch.domain.Species;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto.AbandonedAnimalPublicDataDto;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.repository.BreedTypeRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class BreedTypeService {

	private final BreedTypeRepository breedTypeRepository;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Transactional
	public Map<String, Long> processBreedTypes(List<AbandonedAnimalPublicDataDto> publicDataList) {
		Set<String> uniqueBreedCodes = new HashSet<>();
		for (AbandonedAnimalPublicDataDto dto : publicDataList) {
			String kindCd = dto.getKindCd();
			if (kindCd != null && !kindCd.isBlank()) {
				uniqueBreedCodes.add(kindCd);
			}
		}

		log.info("{}개의 고유한 품종 유형 처리 중", uniqueBreedCodes.size());

		List<BreedType> existingBreedTypes = breedTypeRepository.findByCodeIn(uniqueBreedCodes);
		Map<String, Long> breedTypeIdMap = new HashMap<>();
		for (BreedType breedType : existingBreedTypes) {
			breedTypeIdMap.put(breedType.getCode(), breedType.getId());
		}

		Set<String> newBreedTypeCodes = new HashSet<>();
		for (String code : uniqueBreedCodes) {
			if (!breedTypeIdMap.containsKey(code)) {
				newBreedTypeCodes.add(code);
			}
		}

		if (!newBreedTypeCodes.isEmpty()) {
			log.info("{}개의 새로운 품종 유형 생성 중", newBreedTypeCodes.size());

			Map<String, AbandonedAnimalPublicDataDto> codeToDataMap = new HashMap<>();
			for (AbandonedAnimalPublicDataDto dto : publicDataList) {
				if (dto.getKindCd() != null && !codeToDataMap.containsKey(dto.getKindCd())) {
					codeToDataMap.put(dto.getKindCd(), dto);
				}
			}

			List<BreedType> newBreedTypes = new ArrayList<>();
			for (String code : newBreedTypeCodes) {
				AbandonedAnimalPublicDataDto dto = codeToDataMap.get(code);
				Species species = Species.fromCode(dto != null ? dto.getUpKindCd() : null);
				BreedType breedType = BreedType.builder()
					.code(code)
					.name(dto.getKindNm())
					.type(species)
					.build();
				newBreedTypes.add(breedType);
			}

			bulkInsertBreedTypes(newBreedTypes);

			List<BreedType> savedBreedTypes = breedTypeRepository.findByCodeIn(newBreedTypeCodes);
			for (BreedType breed : savedBreedTypes) {
				breedTypeIdMap.put(breed.getCode(), breed.getId());
			}

			log.info("{}개의 새로운 품종 유형 생성 완료", savedBreedTypes.size());
		}

		return breedTypeIdMap;
	}

	private void bulkInsertBreedTypes(List<BreedType> breedTypes) {
		String sql = "INSERT INTO breed_type (code, name, type, created_at, updated_at) " +
			"VALUES (:code, :name, :type, NOW(), NOW())";

		SqlParameterSource[] batchParams = new SqlParameterSource[breedTypes.size()];
		for (int i = 0; i < breedTypes.size(); i++) {
			BreedType breedType = breedTypes.get(i);
			batchParams[i] = new MapSqlParameterSource()
				.addValue(BreedTypeEntityField.CODE.getColumnName(), breedType.getCode())
				.addValue(BreedTypeEntityField.NAME.getColumnName(), breedType.getName())
				.addValue(BreedTypeEntityField.TYPE.getColumnName(), breedType.getType().name());
		}

		namedParameterJdbcTemplate.batchUpdate(sql, batchParams);
	}
}