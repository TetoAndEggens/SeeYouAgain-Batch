package tetoandeggens.seeyouagainbatch.job.animaldataload.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.AnimalApiResponseWrapper;

@FeignClient(
        name = "publicDataApiClient",
        url = "${public-data.api.base-url}"
)
public interface PublicDataApiClient {

    @GetMapping("/1543061/abandonmentPublicService_v2/abandonmentPublic_v2")
    AnimalApiResponseWrapper fetchAnimals(
            @RequestParam("serviceKey") String serviceKey,
            @RequestParam("bgnde") String bgnde,
            @RequestParam("endde") String endde,
            @RequestParam("numOfRows") int numOfRows,
            @RequestParam("_type") String type,
            @RequestParam("pageNo") int pageNo
    );
}