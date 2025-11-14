package tetoandeggens.seeyouagainbatch.job.animaldataload.validator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;

@Component
public class AnimalDataLoadJobParametersValidator implements JobParametersValidator {

    private static final int MIN_PAGE_NO = 1;
    private static final int MAX_PAGE_NO = Integer.MAX_VALUE;
    private static final int MIN_NUM_OF_ROWS = 1;
    private static final int MAX_NUM_OF_ROWS = 10000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void validate(@Nullable JobParameters parameters) throws JobParametersInvalidException {

        if (parameters == null) {
            throw new JobParametersInvalidException(AnimalDataLoadValidationErrorMessage.PARAMETERS_NULL.getMessageFormat());
        }

        LocalDate startDate = validateDateParameter(parameters, AnimalDataLoadJobParameterKey.START_DATE);
        LocalDate endDate = validateDateParameter(parameters, AnimalDataLoadJobParameterKey.END_DATE);

        validateIntegerParameter(parameters, AnimalDataLoadJobParameterKey.PAGE_NO, MIN_PAGE_NO, MAX_PAGE_NO);
        validateIntegerParameter(parameters, AnimalDataLoadJobParameterKey.NUM_OF_ROWS, MIN_NUM_OF_ROWS, MAX_NUM_OF_ROWS);

        validateDateRange(startDate, endDate);
    }

    private LocalDate validateDateParameter(JobParameters parameters, AnimalDataLoadJobParameterKey paramKey)
            throws JobParametersInvalidException {

        String dateString = parameters.getString(paramKey.getKey());
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new JobParametersInvalidException(
                    AnimalDataLoadValidationErrorMessage.PARAMETER_REQUIRED.format(paramKey.getKey())
            );
        }

        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new JobParametersInvalidException(
                    AnimalDataLoadValidationErrorMessage.INVALID_DATE_FORMAT.format(paramKey.getKey(), dateString)
            );
        }
    }

    private void validateIntegerParameter(JobParameters parameters, AnimalDataLoadJobParameterKey paramKey,
                                        int min, int max) throws JobParametersInvalidException {

        Long value = parameters.getLong(paramKey.getKey());
        if (value == null) {
            throw new JobParametersInvalidException(
                    AnimalDataLoadValidationErrorMessage.PARAMETER_REQUIRED.format(paramKey.getKey())
            );
        }

        if (value < min || value > max) {
            throw new JobParametersInvalidException(
                    AnimalDataLoadValidationErrorMessage.PARAMETER_OUT_OF_RANGE.format(paramKey.getKey(), value, min, max)
            );
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) throws JobParametersInvalidException {

        if (startDate.isAfter(endDate)) {
            throw new JobParametersInvalidException(
                    AnimalDataLoadValidationErrorMessage.START_DATE_AFTER_END_DATE.format(startDate, endDate)
            );
        }
    }
}