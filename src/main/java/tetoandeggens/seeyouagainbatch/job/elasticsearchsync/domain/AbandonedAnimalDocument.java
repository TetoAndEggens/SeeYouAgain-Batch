package tetoandeggens.seeyouagainbatch.job.elasticsearchsync.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import lombok.Builder;
import lombok.Getter;

@Getter
@Document(indexName = "abandoned_animal")
public class AbandonedAnimalDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String breedName;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String color;

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer"),
        otherFields = {
            @InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "ngram_analyzer")
        }
    )
    private String specialMark;

    @Builder
    public AbandonedAnimalDocument(
        Long id,
        String breedName,
        String color,
        String specialMark
    ) {
        this.id = id;
        this.breedName = breedName;
        this.color = color;
        this.specialMark = specialMark;
    }
}