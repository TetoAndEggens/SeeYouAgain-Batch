package tetoandeggens.seeyouagainbatch.job.s3profiledelete.validator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;

@Component
public class S3ProfileDeleteJobParametersValidator implements JobParametersValidator {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Override
	public void validate(@Nullable JobParameters parameters) throws JobParametersInvalidException {

		if (parameters == null) {
			throw new JobParametersInvalidException(S3ProfileDeleteValidationErrorMessage.PARAMETERS_NULL.getMessageFormat());
		}

		LocalDate startDate = validateDateParameter(parameters, S3ProfileDeleteJobParameterKey.START_DATE);
		LocalDate endDate = validateDateParameter(parameters, S3ProfileDeleteJobParameterKey.END_DATE);

		validateDateRange(startDate, endDate);
	}

	private LocalDate validateDateParameter(JobParameters parameters, S3ProfileDeleteJobParameterKey paramKey)
		throws JobParametersInvalidException {

		String dateString = parameters.getString(paramKey.getKey());
		if (dateString == null || dateString.trim().isEmpty()) {
			throw new JobParametersInvalidException(
				S3ProfileDeleteValidationErrorMessage.PARAMETER_REQUIRED.format(paramKey.getKey())
			);
		}

		try {
			return LocalDate.parse(dateString, DATE_FORMATTER);
		} catch (DateTimeParseException e) {
			throw new JobParametersInvalidException(
				S3ProfileDeleteValidationErrorMessage.INVALID_DATE_FORMAT.format(paramKey.getKey(), dateString)
			);
		}
	}

	private void validateDateRange(LocalDate startDate, LocalDate endDate) throws JobParametersInvalidException {

		if (startDate.isAfter(endDate)) {
			throw new JobParametersInvalidException(
				S3ProfileDeleteValidationErrorMessage.START_DATE_AFTER_END_DATE.format(startDate, endDate)
			);
		}
	}
}