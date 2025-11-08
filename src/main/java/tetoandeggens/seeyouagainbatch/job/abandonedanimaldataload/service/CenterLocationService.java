package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.service;

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
import tetoandeggens.seeyouagainbatch.constant.CenterLocationEntityField;
import tetoandeggens.seeyouagainbatch.domain.CenterLocation;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto.AbandonedAnimalPublicDataDto;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto.GeoCoordinateDto;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.repository.CenterLocationRepository;

@Slf4j
@Service
public class CenterLocationService {

	private final CenterLocationRepository centerLocationRepository;
	private final KakaoMapService kakaoMapService;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public CenterLocationService(
		CenterLocationRepository centerLocationRepository,
		KakaoMapService kakaoMapService,
		@Qualifier("businessNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate
	) {
		this.centerLocationRepository = centerLocationRepository;
		this.kakaoMapService = kakaoMapService;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	@Transactional
	public Map<String, Long> processCenterLocations(List<AbandonedAnimalPublicDataDto> publicDataList) {
		Set<String> uniqueCareRegNos = new HashSet<>();
		for (AbandonedAnimalPublicDataDto dto : publicDataList) {
			String careRegNo = dto.getCareRegNo();
			if (careRegNo != null && !careRegNo.isBlank()) {
				uniqueCareRegNos.add(careRegNo);
			}
		}

		log.info("{}개의 고유한 보호센터 위치 처리 중", uniqueCareRegNos.size());

		List<CenterLocation> existingCenterLocations = centerLocationRepository.findByCenterNoIn(uniqueCareRegNos);

		Map<String, Long> centerLocationIdMap = new HashMap<>();
		for (CenterLocation location : existingCenterLocations) {
			centerLocationIdMap.put(location.getCenterNo(), location.getId());
		}

		Set<String> newCareRegNos = new HashSet<>();
		for (String regNo : uniqueCareRegNos) {
			if (!centerLocationIdMap.containsKey(regNo)) {
				newCareRegNos.add(regNo);
			}
		}

		if (!newCareRegNos.isEmpty()) {
			log.info("{}개의 새로운 보호센터 위치 생성 중", newCareRegNos.size());
			List<CenterLocation> newCenterLocations = new ArrayList<>();

			for (String newCareRegNo : newCareRegNos) {
				AbandonedAnimalPublicDataDto publicData = findDtoByCareRegNo(publicDataList, newCareRegNo);

				if (publicData != null && publicData.getCareAddr() != null && !publicData.getCareAddr().isBlank()) {
					GeoCoordinateDto coordinate = kakaoMapService.searchCoordinates(publicData.getCareAddr());
					CenterLocation centerLocation = CenterLocation.builder()
						.name(publicData.getCareNm())
						.address(publicData.getCareAddr())
						.centerNo(newCareRegNo)
						.latitude(coordinate.getLatitude())
						.longitude(coordinate.getLongitude())
						.build();
					newCenterLocations.add(centerLocation);
				}
			}

			bulkInsertCenterLocations(newCenterLocations);

			Set<String> insertedCenterNos = new HashSet<>();
			for (CenterLocation location : newCenterLocations) {
				insertedCenterNos.add(location.getCenterNo());
			}

			List<CenterLocation> savedCenterLocations = centerLocationRepository.findByCenterNoIn(insertedCenterNos);
			for (CenterLocation loc : savedCenterLocations) {
				centerLocationIdMap.put(loc.getCenterNo(), loc.getId());
			}

			log.info("{}개의 새로운 보호센터 위치 생성 완료", savedCenterLocations.size());
		}

		return centerLocationIdMap;
	}

	private void bulkInsertCenterLocations(List<CenterLocation> centerLocations) {
		String sql =
			"INSERT INTO center_location (name, address, center_no, coordinates, created_at, updated_at) " +
				"VALUES (:name, :address, :center_no, ST_GeomFromText(:coordinates, 4326), NOW(), NOW())";

		SqlParameterSource[] batchParams = new SqlParameterSource[centerLocations.size()];
		for (int i = 0; i < centerLocations.size(); i++) {
			CenterLocation location = centerLocations.get(i);
			// SRID 4326: POINT(latitude longitude) 형식
			String wkt = String.format("POINT(%f %f)",
				location.getCoordinates().getY(),
				location.getCoordinates().getX());

			batchParams[i] = new MapSqlParameterSource()
				.addValue(CenterLocationEntityField.NAME.getColumnName(), location.getName())
				.addValue(CenterLocationEntityField.ADDRESS.getColumnName(), location.getAddress())
				.addValue(CenterLocationEntityField.CENTER_NO.getColumnName(), location.getCenterNo())
				.addValue(CenterLocationEntityField.COORDINATES.getColumnName(), wkt);
		}

		namedParameterJdbcTemplate.batchUpdate(sql, batchParams);
	}

	private AbandonedAnimalPublicDataDto findDtoByCareRegNo(List<AbandonedAnimalPublicDataDto> dtoList,
		String careRegNo) {
		for (AbandonedAnimalPublicDataDto dto : dtoList) {
			if (careRegNo.equals(dto.getCareRegNo())) {
				return dto;
			}
		}
		return null;
	}
}