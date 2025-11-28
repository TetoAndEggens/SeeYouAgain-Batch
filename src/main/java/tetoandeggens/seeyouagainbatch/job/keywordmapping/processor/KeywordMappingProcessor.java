package tetoandeggens.seeyouagainbatch.job.keywordmapping.processor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.domain.Animal;
import tetoandeggens.seeyouagainbatch.domain.AnimalByKeyword;
import tetoandeggens.seeyouagainbatch.domain.AnimalType;
import tetoandeggens.seeyouagainbatch.domain.KeywordCategoryType;
import tetoandeggens.seeyouagainbatch.domain.KeywordType;
import tetoandeggens.seeyouagainbatch.domain.NotificationKeyword;
import tetoandeggens.seeyouagainbatch.domain.QAnimal;
import tetoandeggens.seeyouagainbatch.domain.QBreedType;
import tetoandeggens.seeyouagainbatch.domain.QNotificationKeyword;
import tetoandeggens.seeyouagainbatch.job.keywordmapping.dto.KeywordCombinationDto;
import tetoandeggens.seeyouagainbatch.job.keywordmapping.parameter.KeywordMappingJobParameter;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordMappingProcessor implements ItemProcessor<KeywordCombinationDto, List<AnimalByKeyword>> {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final JPAQueryFactory queryFactory;
	private final KeywordMappingJobParameter jobParameter;

	@Override
	public List<AnimalByKeyword> process(KeywordCombinationDto item) {
		log.info("키워드 조합 처리 시작: keyword={}, keywordType={}, keywordCategoryType={}",
			item.getKeyword(), item.getKeywordType(), item.getKeywordCategoryType());

		LocalDate targetDate = LocalDate.parse(jobParameter.getDate(), DATE_FORMATTER);

		List<Animal> animals = findAnimalsByKeyword(item, targetDate);
		log.info("검색된 동물 수: {}", animals.size());

		if (animals.isEmpty()) {
			return new ArrayList<>();
		}

		List<NotificationKeyword> subscribers = findSubscribers(item);
		log.info("구독자 수: {}", subscribers.size());

		if (subscribers.isEmpty()) {
			return new ArrayList<>();
		}

		List<AnimalByKeyword> mappings = createMappings(subscribers, animals);
		log.info("생성된 매칭 수: {}", mappings.size());

		return mappings;
	}

	private List<Animal> findAnimalsByKeyword(KeywordCombinationDto keywordInfo, LocalDate targetDate) {
		QAnimal animal = QAnimal.animal;
		AnimalType animalType = convertToAnimalType(keywordInfo.getKeywordType());

		if (keywordInfo.getKeywordCategoryType() == KeywordCategoryType.BREED) {
			QBreedType breedType = QBreedType.breedType;

			return queryFactory
				.selectFrom(animal)
				.join(animal.breedType, breedType)
				.where(
					breedType.name.eq(keywordInfo.getKeyword()),
					animal.animalType.eq(animalType),
					animal.happenDate.eq(targetDate),
					animal.isDeleted.eq(false)
				)
				.fetch();
		} else {
			return queryFactory
				.selectFrom(animal)
				.where(
					animal.city.eq(keywordInfo.getKeyword())
						.or(animal.town.eq(keywordInfo.getKeyword())),
					animal.animalType.eq(animalType),
					animal.happenDate.eq(targetDate),
					animal.isDeleted.eq(false)
				)
				.fetch();
		}
	}

	private AnimalType convertToAnimalType(KeywordType keywordType) {
		return switch (keywordType) {
			case ABANDONED -> AnimalType.ABANDONED;
			case WITNESS -> AnimalType.WITNESS;
		};
	}

	private List<NotificationKeyword> findSubscribers(KeywordCombinationDto keywordInfo) {
		QNotificationKeyword nk = QNotificationKeyword.notificationKeyword;

		return queryFactory
			.selectFrom(nk)
			.where(
				nk.keyword.eq(keywordInfo.getKeyword()),
				nk.keywordType.eq(keywordInfo.getKeywordType()),
				nk.keywordCategoryType.eq(keywordInfo.getKeywordCategoryType())
			)
			.fetch();
	}

	private List<AnimalByKeyword> createMappings(
		List<NotificationKeyword> subscribers,
		List<Animal> animals
	) {
		List<AnimalByKeyword> mappings = new ArrayList<>();

		for (NotificationKeyword subscriber : subscribers) {
			for (Animal animal : animals) {
				AnimalByKeyword mapping = AnimalByKeyword.builder()
					.notificationKeyword(subscriber)
					.animal(animal)
					.build();
				mappings.add(mapping);
			}
		}

		return mappings;
	}
}