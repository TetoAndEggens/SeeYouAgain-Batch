package tetoandeggens.seeyouagainbatch.job.keywordnotification.validator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;

@Component
public class KeywordNotificationJobParametersValidator implements JobParametersValidator {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Override
	public void validate(@Nullable JobParameters parameters) throws JobParametersInvalidException {

		if (parameters == null) {
			throw new JobParametersInvalidException(
				KeywordNotificationValidationErrorMessage.PARAMETERS_NULL.getMessageFormat());
		}

		validateDateParameter(parameters, KeywordNotificationJobParameterKey.DATE);
	}

	private LocalDate validateDateParameter(JobParameters parameters, KeywordNotificationJobParameterKey paramKey)
		throws JobParametersInvalidException {

		String dateString = parameters.getString(paramKey.getKey());
		if (dateString == null || dateString.trim().isEmpty()) {
			throw new JobParametersInvalidException(
				KeywordNotificationValidationErrorMessage.PARAMETER_REQUIRED.format(paramKey.getKey())
			);
		}

		try {
			return LocalDate.parse(dateString, DATE_FORMATTER);
		} catch (DateTimeParseException e) {
			throw new JobParametersInvalidException(
				KeywordNotificationValidationErrorMessage.INVALID_DATE_FORMAT.format(paramKey.getKey(), dateString)
			);
		}
	}
}