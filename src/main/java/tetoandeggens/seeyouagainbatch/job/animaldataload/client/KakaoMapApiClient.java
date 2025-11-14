package tetoandeggens.seeyouagainbatch.job.animaldataload.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.KakaoMapResponseWrapper;

@FeignClient(
        name = "kakaoMapApiClient",
        url = "${kakao.api.base-url}"
)
public interface KakaoMapApiClient {

    @GetMapping("/v2/local/search/address")
    KakaoMapResponseWrapper searchCoordinates(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("query") String address
    );
}