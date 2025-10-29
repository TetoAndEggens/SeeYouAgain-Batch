package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.tasklet;

import java.util.List;
import java.util.Map;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto.AbandonedAnimalPublicDataDto;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.parameter.AbandonedAnimalDataLoadJobParameter;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.service.AbandonedAnimalProfileService;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.service.AbandonedAnimalPublicDataService;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.service.AbandonedAnimalService;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.service.BreedTypeService;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.service.CenterLocationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class AbandonedAnimalDataLoadTasklet implements Tasklet {

	private final AbandonedAnimalPublicDataService abandonedAnimalPublicDataService;
	private final CenterLocationService centerLocationService;
	private final BreedTypeService breedTypeService;
	private final AbandonedAnimalService abandonedAnimalService;
	private final AbandonedAnimalProfileService abandonedAnimalProfileService;
	private final AbandonedAnimalDataLoadJobParameter abandonedAnimalDataLoadJobParameter;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		log.info("유기동물 데이터 로드 태스크렛 시작");
		log.info("파라미터 확인 - startDate: {}, endDate: {}, pageNo: {}, numOfRows: {}",
			abandonedAnimalDataLoadJobParameter.getStartDate(),
			abandonedAnimalDataLoadJobParameter.getEndDate(),
			abandonedAnimalDataLoadJobParameter.getPageNo(),
			abandonedAnimalDataLoadJobParameter.getNumOfRows());

		List<AbandonedAnimalPublicDataDto> publicDataList = abandonedAnimalPublicDataService.fetchAllData(
			abandonedAnimalDataLoadJobParameter.getStartDate(),
			abandonedAnimalDataLoadJobParameter.getEndDate(),
			abandonedAnimalDataLoadJobParameter.getNumOfRows(),
			abandonedAnimalDataLoadJobParameter.getPageNo()
		);

		if (publicDataList.isEmpty()) {
			log.info("처리할 데이터가 없습니다");
			return RepeatStatus.FINISHED;
		}

		Map<String, Long> centerLocationIdMap = centerLocationService.processCenterLocations(publicDataList);

		Map<String, Long> breedTypeIdMap = breedTypeService.processBreedTypes(publicDataList);

		Map<String, Long> abandonedAnimalIdMap = abandonedAnimalService.processAbandonedAnimals(
			publicDataList, centerLocationIdMap, breedTypeIdMap);

		abandonedAnimalProfileService.processAbandonedAnimalProfiles(publicDataList, abandonedAnimalIdMap);

		log.info("유기동물 데이터 로드 태스크렛 정상 완료");
		return RepeatStatus.FINISHED;
	}
}