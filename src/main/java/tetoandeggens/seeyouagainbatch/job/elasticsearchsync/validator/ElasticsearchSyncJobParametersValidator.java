package tetoandeggens.seeyouagainbatch.job.elasticsearchsync.validator;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchSyncJobParametersValidator implements JobParametersValidator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void validate(@Nullable JobParameters parameters) throws JobParametersInvalidException {

        if (parameters == null) {
            throw new JobParametersInvalidException(ElasticsearchSyncValidationErrorMessage.PARAMETERS_NULL.getMessageFormat());
        }

        LocalDate startDate = validateDateParameter(parameters, ElasticsearchSyncJobParameterKey.START_DATE);
        LocalDate endDate = validateDateParameter(parameters, ElasticsearchSyncJobParameterKey.END_DATE);

        validateDateRange(startDate, endDate);
    }

    private LocalDate validateDateParameter(JobParameters parameters, ElasticsearchSyncJobParameterKey paramKey)
        throws JobParametersInvalidException {

        String dateString = parameters.getString(paramKey.getKey());
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new JobParametersInvalidException(
                ElasticsearchSyncValidationErrorMessage.PARAMETER_REQUIRED.format(paramKey.getKey())
            );
        }

        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new JobParametersInvalidException(
                ElasticsearchSyncValidationErrorMessage.INVALID_DATE_FORMAT.format(paramKey.getKey(), dateString)
            );
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) throws JobParametersInvalidException {

        if (startDate.isAfter(endDate)) {
            throw new JobParametersInvalidException(
                ElasticsearchSyncValidationErrorMessage.START_DATE_AFTER_END_DATE.format(startDate, endDate)
            );
        }
    }
}