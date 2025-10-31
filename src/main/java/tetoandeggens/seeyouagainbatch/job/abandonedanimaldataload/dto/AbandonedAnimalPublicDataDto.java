package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AbandonedAnimalPublicDataDto {

    @JsonProperty("desertionNo")
    private String desertionNo;

    @JsonProperty("happenDt")
    private String happenDt;

    @JsonProperty("happenPlace")
    private String happenPlace;

    @JsonProperty("kindFullNm")
    private String kindFullNm;

    @JsonProperty("upKindCd")
    private String upKindCd;

    @JsonProperty("upKindNm")
    private String upKindNm;

    @JsonProperty("kindNm")
    private String kindNm;

    @JsonProperty("kindCd")
    private String kindCd;

    @JsonProperty("colorCd")
    private String colorCd;

    @JsonProperty("age")
    private String age;

    @JsonProperty("weight")
    private String weight;

    @JsonProperty("noticeNo")
    private String noticeNo;

    @JsonProperty("noticeSdt")
    private String noticeSdt;

    @JsonProperty("noticeEdt")
    private String noticeEdt;

    @JsonProperty("popfile1")
    private String popfile1;

    @JsonProperty("popfile2")
    private String popfile2;

    @JsonProperty("popfile3")
    private String popfile3;

    @JsonProperty("processState")
    private String processState;

    @JsonProperty("sexCd")
    private String sexCd;

    @JsonProperty("neuterYn")
    private String neuterYn;

    @JsonProperty("specialMark")
    private String specialMark;

    @JsonProperty("careNm")
    private String careNm;

    @JsonProperty("careTel")
    private String careTel;

    @JsonProperty("careAddr")
    private String careAddr;

    @JsonProperty("careRegNo")
    private String careRegNo;

    @JsonProperty("updTm")
    private String updTm;

    @JsonProperty("orgNm")
    private String orgNm;
}