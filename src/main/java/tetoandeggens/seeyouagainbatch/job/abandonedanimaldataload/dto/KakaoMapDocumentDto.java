package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoMapDocumentDto {

    @JsonProperty("x")
    private String longitude;

    @JsonProperty("y")
    private String latitude;
}