package tetoandeggens.seeyouagainbatch.job.animaldataload.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tetoandeggens.seeyouagainbatch.job.animaldataload.client.PublicDataApiClient;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.AnimalApiResponseWrapper;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.AnimalPublicDataDto;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimalPublicDataService {

    private static final String RESPONSE_TYPE = "json";

    private final PublicDataApiClient publicDataApiClient;

    @Value("${public-data.api.service-key}")
    private String serviceKey;

    public List<AnimalPublicDataDto> fetchAllData(String startDate, String endDate,
                                                            int numOfRows, int startPageNo) {
        log.info("공공 API 데이터 조회 시작 - startDate: {}, endDate: {}, numOfRows: {}, startPageNo: {}",
                startDate, endDate, numOfRows, startPageNo);

        List<AnimalPublicDataDto> allData = new ArrayList<>();
        int currentPage = startPageNo;

        while (true) {
            try {
                log.info("페이지 {} 조회 중 (numOfRows: {})", currentPage, numOfRows);

                AnimalApiResponseWrapper response = publicDataApiClient.fetchAnimals(
                        serviceKey,
                        startDate,
                        endDate,
                        numOfRows,
                        RESPONSE_TYPE,
                        currentPage
                );

                if (!isValidResponse(response)) {
                    log.warn("페이지 {}에서 빈 응답 또는 null 응답 수신", currentPage);
                    break;
                }

                List<AnimalPublicDataDto> items = response.getResponse().getBody().getItems().getItem();

                log.info("페이지 {}에서 조회된 items 개수: {}", currentPage, items.size());
                items.forEach(item -> log.info("desertionNo: {}, happenDt: {}, happenPlace: {}, kindFullNm: {}, upKindNm: {}, " +
                        "age: {}, weight: {}, processState: {}, sexCd: {}, neuterYn: {}, careNm: {}, careTel: {}, careAddr: {}",
                        item.getDesertionNo(), item.getHappenDt(), item.getHappenPlace(), item.getKindFullNm(), item.getUpKindNm(),
                        item.getAge(), item.getWeight(), item.getProcessState(), item.getSexCd(), item.getNeuterYn(),
                        item.getCareNm(), item.getCareTel(), item.getCareAddr()));

                if (items.isEmpty()) {
                    log.info("페이지 {}에 더 이상 데이터 없음", currentPage);
                    break;
                }

                allData.addAll(items);
                log.info("페이지 {}에서 {}건 조회 완료", currentPage, items.size());

                if (items.size() < numOfRows) {
                    log.info("마지막 페이지 도달. 조회 건수: {}", items.size());
                    break;
                }

                currentPage++;
            } catch (Exception e) {
                log.error("페이지 {} 공공 API 데이터 조회 중 오류 발생", currentPage, e);
                break;
            }
        }

        log.info("공공 API 데이터 조회 완료. 총 {}건", allData.size());
        return allData;
    }

    private boolean isValidResponse(AnimalApiResponseWrapper response) {
        return response != null
                && response.getResponse() != null
                && response.getResponse().getBody() != null
                && response.getResponse().getBody().getItems() != null
                && response.getResponse().getBody().getItems().getItem() != null;
    }
}