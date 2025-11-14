package tetoandeggens.seeyouagainbatch.job.animaldataload.converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.domain.Animal;
import tetoandeggens.seeyouagainbatch.domain.AnimalProfile;
import tetoandeggens.seeyouagainbatch.domain.BreedType;
import tetoandeggens.seeyouagainbatch.domain.AnimalLocation;
import tetoandeggens.seeyouagainbatch.domain.NeuteredState;
import tetoandeggens.seeyouagainbatch.domain.Sex;
import tetoandeggens.seeyouagainbatch.domain.Species;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.AnimalPublicDataDto;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnimalConverter {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(
		"yyyy-MM-dd HH:mm:ss.S");

	public List<Animal> convertToEntities(List<AnimalPublicDataDto> publicDataList,
		Map<String, Long> animalLocationIdMap,
		Map<String, Long> breedTypeIdMap) {
		List<Animal> animals = new ArrayList<>();

		for (AnimalPublicDataDto publicDataDto : publicDataList) {
			Animal animal = convertToEntity(publicDataDto, animalLocationIdMap, breedTypeIdMap);
			animals.add(animal);
		}

		return animals;
	}

	public List<AnimalProfile> convertToProfiles(List<AnimalPublicDataDto> publicDataList,
		Map<String, Long> animalIdMap) {
		List<AnimalProfile> profiles = new ArrayList<>();

		for (AnimalPublicDataDto dto : publicDataList) {
			List<String> profileUrls = extractProfileUrls(dto);
			if (profileUrls.isEmpty()) {
				continue;
			}

			Long animalId = animalIdMap.get(dto.getDesertionNo());
			if (animalId == null) {
				continue;
			}

			LocalDateTime happenDateTime = parseDateTime(dto.getHappenDt());

			for (String profileUrl : profileUrls) {
				AnimalProfile profile = AnimalProfile.builder()
					.profile(profileUrl)
					.happenDate(happenDateTime.toLocalDate())
					.animal(new Animal(animalId))
					.build();
				profiles.add(profile);
			}
		}

		return profiles;
	}

	private List<String> extractProfileUrls(AnimalPublicDataDto publicDataDto) {
		List<String> urls = new ArrayList<>();
		String[] urlArray = {publicDataDto.getPopfile1(), publicDataDto.getPopfile2(), publicDataDto.getPopfile3()};

		for (String url : urlArray) {
			if (url != null && !url.isBlank()) {
				urls.add(url);
			}
		}

		return urls;
	}

	private Animal convertToEntity(AnimalPublicDataDto publicDataDto,
		Map<String, Long> animalLocationIdMap,
		Map<String, Long> breedTypeIdMap) {
		Long animalLocationId = animalLocationIdMap.get(publicDataDto.getCareRegNo());
		Long breedTypeId = breedTypeIdMap.get(publicDataDto.getKindCd());

		AnimalLocation animalLocation = AnimalLocation.builder()
			.id(animalLocationId)
			.build();
		BreedType breedType = BreedType.builder()
			.id(breedTypeId)
			.build();

		LocalDateTime happenDateTime = parseDateTime(publicDataDto.getHappenDt());
		LocalDateTime finalUpdatedAt = parseDateTime(publicDataDto.getUpdTm());

		String[] location = parseOrgNm(publicDataDto.getOrgNm());
		String city = location[0];
		String town = location[1];

		return Animal.builder()
			.desertionNo(publicDataDto.getDesertionNo())
			.happenDate(happenDateTime.toLocalDate())
			.happenPlace(publicDataDto.getHappenPlace())
			.city(city)
			.town(town)
			.species(Species.fromCode(publicDataDto.getUpKindCd()))
			.color(publicDataDto.getColorCd())
			.birth(publicDataDto.getAge())
			.weight(publicDataDto.getWeight())
			.noticeNo(publicDataDto.getNoticeNo())
			.noticeStartDate(publicDataDto.getNoticeSdt())
			.noticeEndDate(publicDataDto.getNoticeEdt())
			.processState(publicDataDto.getProcessState())
			.sex(Sex.fromCode(publicDataDto.getSexCd()))
			.neuteredState(NeuteredState.fromCode(publicDataDto.getNeuterYn()))
			.specialMark(publicDataDto.getSpecialMark())
			.centerPhone(publicDataDto.getCareTel())
			.finalUpdatedAt(finalUpdatedAt)
			.animalLocation(animalLocation)
			.breedType(breedType)
			.build();
	}

	private LocalDateTime parseDateTime(String dateTimeStr) {
		if (dateTimeStr == null || dateTimeStr.isBlank()) {
			return null;
		}

		try {
			if (dateTimeStr.contains(" ")) {
				return LocalDateTime.parse(dateTimeStr, SQL_TIMESTAMP_FORMATTER);
			} else {
				return LocalDate.parse(dateTimeStr, DATE_FORMATTER).atStartOfDay();
			}
		} catch (Exception e) {
			log.warn("날짜/시간 파싱 실패 - 원본값: '{}', 에러: {}", dateTimeStr, e.getMessage());
			return null;
		}
	}

	private String[] parseOrgNm(String orgNm) {
		if (orgNm == null || orgNm.isEmpty()) {
			return new String[] {"", ""};
		}

		String[] parts = orgNm.split(" ", 2);

		if (parts.length >= 2) {
			return new String[] {parts[0], parts[1]};
		} else if (parts.length == 1) {
			return new String[] {parts[0], ""};
		}

		return new String[] {"", ""};
	}
}