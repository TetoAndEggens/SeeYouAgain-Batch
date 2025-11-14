package tetoandeggens.seeyouagainbatch.job.animaldataload.tasklet;

import java.util.List;
import java.util.Map;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.AnimalPublicDataDto;
import tetoandeggens.seeyouagainbatch.job.animaldataload.parameter.AnimalDataLoadJobParameter;
import tetoandeggens.seeyouagainbatch.job.animaldataload.service.AnimalProfileService;
import tetoandeggens.seeyouagainbatch.job.animaldataload.service.AnimalPublicDataService;
import tetoandeggens.seeyouagainbatch.job.animaldataload.service.AnimalService;
import tetoandeggens.seeyouagainbatch.job.animaldataload.service.BreedTypeService;
import tetoandeggens.seeyouagainbatch.job.animaldataload.service.AnimalLocationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnimalDataLoadTasklet implements Tasklet {

	private final AnimalPublicDataService animalPublicDataService;
	private final AnimalLocationService animalLocationService;
	private final BreedTypeService breedTypeService;
	private final AnimalService animalService;
	private final AnimalProfileService animalProfileService;
	private final AnimalDataLoadJobParameter animalDataLoadJobParameter;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		log.info("동물 데이터 로드 태스크렛 시작");
		log.info("파라미터 확인 - startDate: {}, endDate: {}, pageNo: {}, numOfRows: {}",
			animalDataLoadJobParameter.getStartDate(),
			animalDataLoadJobParameter.getEndDate(),
			animalDataLoadJobParameter.getPageNo(),
			animalDataLoadJobParameter.getNumOfRows());

		List<AnimalPublicDataDto> publicDataList = animalPublicDataService.fetchAllData(
			animalDataLoadJobParameter.getStartDate(),
			animalDataLoadJobParameter.getEndDate(),
			animalDataLoadJobParameter.getNumOfRows(),
			animalDataLoadJobParameter.getPageNo()
		);

		if (publicDataList.isEmpty()) {
			log.info("처리할 데이터가 없습니다");
			return RepeatStatus.FINISHED;
		}

		Map<String, Long> animalLocationIdMap = animalLocationService.processAnimalLocations(publicDataList);

		Map<String, Long> breedTypeIdMap = breedTypeService.processBreedTypes(publicDataList);

		Map<String, Long> animalIdMap = animalService.processAnimals(
			publicDataList, animalLocationIdMap, breedTypeIdMap);

		animalProfileService.processAnimalProfiles(publicDataList, animalIdMap);

		log.info("동물 데이터 로드 태스크렛 정상 완료");
		return RepeatStatus.FINISHED;
	}
}