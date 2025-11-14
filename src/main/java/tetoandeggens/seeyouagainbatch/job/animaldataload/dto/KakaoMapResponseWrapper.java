package tetoandeggens.seeyouagainbatch.job.animaldataload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoMapResponseWrapper {

    @JsonProperty("documents")
    private List<KakaoMapDocumentDto> documents;
}