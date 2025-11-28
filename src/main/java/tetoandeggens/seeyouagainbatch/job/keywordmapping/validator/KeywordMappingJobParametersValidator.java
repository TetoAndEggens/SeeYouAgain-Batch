package tetoandeggens.seeyouagainbatch.job.keywordmapping.validator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;

@Component
public class KeywordMappingJobParametersValidator implements JobParametersValidator {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Override
	public void validate(@Nullable JobParameters parameters) throws JobParametersInvalidException {

		if (parameters == null) {
			throw new JobParametersInvalidException(
				KeywordMappingValidationErrorMessage.PARAMETERS_NULL.getMessageFormat());
		}

		validateDateParameter(parameters, KeywordMappingJobParameterKey.DATE);
	}

	private LocalDate validateDateParameter(JobParameters parameters, KeywordMappingJobParameterKey paramKey)
		throws JobParametersInvalidException {

		String dateString = parameters.getString(paramKey.getKey());
		if (dateString == null || dateString.trim().isEmpty()) {
			throw new JobParametersInvalidException(
				KeywordMappingValidationErrorMessage.PARAMETER_REQUIRED.format(paramKey.getKey())
			);
		}

		try {
			return LocalDate.parse(dateString, DATE_FORMATTER);
		} catch (DateTimeParseException e) {
			throw new JobParametersInvalidException(
				KeywordMappingValidationErrorMessage.INVALID_DATE_FORMAT.format(paramKey.getKey(), dateString)
			);
		}
	}
}