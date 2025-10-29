package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.client.KakaoMapApiClient;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto.GeoCoordinateDto;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto.KakaoMapDocumentDto;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto.KakaoMapResponseWrapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMapService {

    private final KakaoMapApiClient kakaoMapApiClient;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    public GeoCoordinateDto searchCoordinates(String address) {
        try {
            String authorization = "KakaoAK " + kakaoApiKey;
            KakaoMapResponseWrapper response = kakaoMapApiClient.searchCoordinates(authorization, address);

            if (response.getDocuments() != null && !response.getDocuments().isEmpty()) {
                KakaoMapDocumentDto document = response.getDocuments().get(0);
                Double latitude = Double.parseDouble(document.getLatitude());
                Double longitude = Double.parseDouble(document.getLongitude());

                log.info("주소 좌표 변환 성공: {} -> 위도: {}, 경도: {}", address, latitude, longitude);
                return new GeoCoordinateDto(latitude, longitude);
            }

            log.warn("주소에 대한 좌표를 찾을 수 없음: {}", address);
            return new GeoCoordinateDto(0.0, 0.0);
        } catch (Exception e) {
            log.error("주소 좌표 변환 중 오류 발생: {}", address, e);
            return new GeoCoordinateDto(0.0, 0.0);
        }
    }
}