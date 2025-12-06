package tetoandeggens.seeyouagainbatch.job.keywordnotification.writer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbatch.domain.QFcmToken;
import tetoandeggens.seeyouagainbatch.domain.QNotificationKeyword;
import tetoandeggens.seeyouagainbatch.job.keywordnotification.dto.KeywordNotificationDto;
import tetoandeggens.seeyouagainbatch.job.keywordnotification.parameter.KeywordNotificationJobParameter;
import tetoandeggens.seeyouagainbatch.job.keywordnotification.service.FcmService;

@Component
@RequiredArgsConstructor
public class KeywordNotificationWriter implements ItemWriter<KeywordNotificationDto> {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

	private final JPAQueryFactory queryFactory;
	private final FcmService fcmService;
	private final KeywordNotificationJobParameter jobParameter;

	@Override
	public void write(Chunk<? extends KeywordNotificationDto> chunk) {
		LocalDate targetDate = LocalDate.parse(jobParameter.getDate(), DATE_FORMATTER);
		String displayDate = targetDate.format(DISPLAY_DATE_FORMATTER);

		for (KeywordNotificationDto item : chunk.getItems()) {
			if (item.getAnimalCount() == 0) {
				continue;
			}

			List<String> fcmTokens = getFcmTokensByNotificationKeywordId(item.getNotificationKeywordId());

			if (fcmTokens.isEmpty()) {
				continue;
			}

			String title = displayDate + " - " + item.getKeyword();
			String body = "새로 업로드된 동물의 수는 총 " + item.getAnimalCount() + "마리입니다.";

			String startDate = targetDate.toString();
			String endDate = targetDate.toString();

			fcmService.sendMulticastNotification(fcmTokens, title, body, item.getKeyword(), item.getKeywordType(),
				item.getKeywordCategoryType(), startDate, endDate);
		}
	}

	private List<String> getFcmTokensByNotificationKeywordId(Long notificationKeywordId) {
		QNotificationKeyword nk = QNotificationKeyword.notificationKeyword;
		QFcmToken fcmToken = QFcmToken.fcmToken;

		return queryFactory
			.select(fcmToken.token)
			.from(nk)
			.join(fcmToken).on(nk.member.id.eq(fcmToken.member.id))
			.where(
				nk.id.eq(notificationKeywordId),
				nk.member.isPushEnabled.eq(true),
				nk.member.isDeleted.eq(false)
			)
			.fetch();
	}
}