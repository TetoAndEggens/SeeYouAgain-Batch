package tetoandeggens.seeyouagainbatch.job.animaldataload.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.constant.AnimalLocationEntityField;
import tetoandeggens.seeyouagainbatch.domain.AnimalLocation;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.AnimalPublicDataDto;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.GeoCoordinateDto;
import tetoandeggens.seeyouagainbatch.job.animaldataload.repository.AnimalLocationRepository;

@Slf4j
@Service
public class AnimalLocationService {

	private final AnimalLocationRepository animalLocationRepository;
	private final KakaoMapService kakaoMapService;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public AnimalLocationService(
		AnimalLocationRepository animalLocationRepository,
		KakaoMapService kakaoMapService,
		@Qualifier("businessNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate
	) {
		this.animalLocationRepository = animalLocationRepository;
		this.kakaoMapService = kakaoMapService;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	@Transactional
	public Map<String, Long> processAnimalLocations(List<AnimalPublicDataDto> publicDataList) {
		Set<String> uniqueCareRegNos = new HashSet<>();
		for (AnimalPublicDataDto dto : publicDataList) {
			String careRegNo = dto.getCareRegNo();
			if (careRegNo != null && !careRegNo.isBlank()) {
				uniqueCareRegNos.add(careRegNo);
			}
		}

		log.info("{}개의 고유한 보호센터 위치 처리 중", uniqueCareRegNos.size());

		List<AnimalLocation> existingAnimalLocations = animalLocationRepository.findByCenterNoIn(uniqueCareRegNos);

		Map<String, Long> animalLocationIdMap = new HashMap<>();
		for (AnimalLocation location : existingAnimalLocations) {
			animalLocationIdMap.put(location.getCenterNo(), location.getId());
		}

		Set<String> newCareRegNos = new HashSet<>();
		for (String regNo : uniqueCareRegNos) {
			if (!animalLocationIdMap.containsKey(regNo)) {
				newCareRegNos.add(regNo);
			}
		}

		if (!newCareRegNos.isEmpty()) {
			log.info("{}개의 새로운 보호센터 위치 생성 중", newCareRegNos.size());
			List<AnimalLocation> newAnimalLocations = new ArrayList<>();

			for (String newCareRegNo : newCareRegNos) {
				AnimalPublicDataDto publicData = findDtoByCareRegNo(publicDataList, newCareRegNo);

				if (publicData != null && publicData.getCareAddr() != null && !publicData.getCareAddr().isBlank()) {
					GeoCoordinateDto coordinate = kakaoMapService.searchCoordinates(publicData.getCareAddr());
					AnimalLocation animalLocation = AnimalLocation.builder()
						.name(publicData.getCareNm())
						.address(publicData.getCareAddr())
						.centerNo(newCareRegNo)
						.latitude(coordinate.getLatitude())
						.longitude(coordinate.getLongitude())
						.build();
					newAnimalLocations.add(animalLocation);
				}
			}

			bulkInsertAnimalLocations(newAnimalLocations);

			Set<String> insertedCenterNos = new HashSet<>();
			for (AnimalLocation location : newAnimalLocations) {
				insertedCenterNos.add(location.getCenterNo());
			}

			List<AnimalLocation> savedAnimalLocations = animalLocationRepository.findByCenterNoIn(insertedCenterNos);
			for (AnimalLocation loc : savedAnimalLocations) {
				animalLocationIdMap.put(loc.getCenterNo(), loc.getId());
			}

			log.info("{}개의 새로운 보호센터 위치 생성 완료", savedAnimalLocations.size());
		}

		return animalLocationIdMap;
	}

	private void bulkInsertAnimalLocations(List<AnimalLocation> animalLocations) {
		String sql =
			"INSERT INTO animal_location (name, address, center_no, coordinates, created_at, updated_at) " +
				"VALUES (:name, :address, :center_no, ST_GeomFromText(:coordinates, 4326), NOW(), NOW())";

		SqlParameterSource[] batchParams = new SqlParameterSource[animalLocations.size()];
		for (int i = 0; i < animalLocations.size(); i++) {
			AnimalLocation location = animalLocations.get(i);
			String wkt = String.format("POINT(%f %f)",
				location.getCoordinates().getY(),
				location.getCoordinates().getX());

			batchParams[i] = new MapSqlParameterSource()
				.addValue(AnimalLocationEntityField.NAME.getColumnName(), location.getName())
				.addValue(AnimalLocationEntityField.ADDRESS.getColumnName(), location.getAddress())
				.addValue(AnimalLocationEntityField.CENTER_NO.getColumnName(), location.getCenterNo())
				.addValue(AnimalLocationEntityField.COORDINATES.getColumnName(), wkt);
		}

		namedParameterJdbcTemplate.batchUpdate(sql, batchParams);
	}

	private AnimalPublicDataDto findDtoByCareRegNo(List<AnimalPublicDataDto> dtoList,
		String careRegNo) {
		for (AnimalPublicDataDto dto : dtoList) {
			if (careRegNo.equals(dto.getCareRegNo())) {
				return dto;
			}
		}
		return null;
	}
}