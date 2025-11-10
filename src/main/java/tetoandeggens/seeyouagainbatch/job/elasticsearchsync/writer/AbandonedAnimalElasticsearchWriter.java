package tetoandeggens.seeyouagainbatch.job.elasticsearchsync.writer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbatch.domain.AbandonedAnimal;
import tetoandeggens.seeyouagainbatch.job.elasticsearchsync.domain.AbandonedAnimalDocument;

@Component
@RequiredArgsConstructor
public class AbandonedAnimalElasticsearchWriter implements ItemWriter<AbandonedAnimal> {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void write(Chunk<? extends AbandonedAnimal> chunk) throws Exception {
        BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

        for (AbandonedAnimal animal : chunk.getItems()) {
            AbandonedAnimalDocument document = AbandonedAnimalDocument.builder()
                .id(animal.getId())
                .breedName(animal.getBreedType() != null ? animal.getBreedType().getName() : null)
                .color(animal.getColor())
                .specialMark(animal.getSpecialMark())
                .build();

            bulkRequest.operations(op -> op
                .index(idx -> idx
                    .index("abandoned_animal")
                    .id(String.valueOf(document.getId()))
                    .document(document)
                )
            );
        }

        elasticsearchClient.bulk(bulkRequest.build());
    }
}
