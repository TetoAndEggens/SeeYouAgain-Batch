package tetoandeggens.seeyouagainbatch.job.elasticsearchsync.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ElasticsearchSyncJobParameterKey {

    START_DATE("startDate"),
    END_DATE("endDate");

    private final String key;
}