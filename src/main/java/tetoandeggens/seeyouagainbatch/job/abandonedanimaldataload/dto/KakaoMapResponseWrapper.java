package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class KakaoMapResponseWrapper {

    @JsonProperty("documents")
    private List<KakaoMapDocumentDto> documents;
}