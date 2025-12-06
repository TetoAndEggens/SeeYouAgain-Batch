package tetoandeggens.seeyouagainbatch.job.keywordnotification.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tetoandeggens.seeyouagainbatch.domain.KeywordCategoryType;
import tetoandeggens.seeyouagainbatch.domain.KeywordType;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

	public void sendMulticastNotification(List<String> tokens, String title, String body,
			String keyword, KeywordType keywordType, KeywordCategoryType keywordCategoryType,
			String startDate, String endDate) {
		if (tokens == null || tokens.isEmpty()) {
			return;
		}

		Notification notification = Notification.builder()
			.setTitle(title)
			.setBody(body)
			.build();

		Map<String, String> data = createNotificationData(keyword, keywordType, keywordCategoryType, startDate, endDate);

		MulticastMessage message = MulticastMessage.builder()
			.setNotification(notification)
			.putAllData(data)
			.addAllTokens(tokens)
			.build();

		try {
			FirebaseMessaging.getInstance().sendEachForMulticast(message);
			log.info("FCM 푸쉬 알림 전송 완료: title={}, body={}, tokenCount={}, data={}",
				title, body, tokens.size(), data);
		} catch (FirebaseMessagingException e) {
			log.error("FCM 푸쉬 알림 전송 실패: title={}, body={}", title, body, e);
		}
	}

	private Map<String, String> createNotificationData(String keyword, KeywordType keywordType,
			KeywordCategoryType keywordCategoryType, String startDate, String endDate) {
		Map<String, String> data = new HashMap<>();
		data.put("keyword", keyword);
		data.put("keywordType", keywordType.name());
		data.put("keywordCategoryType", keywordCategoryType.name());
		data.put("startDate", startDate);
		data.put("endDate", endDate);

		if (keywordCategoryType == KeywordCategoryType.LOCATION) {
			String[] parts = keyword.split(" ");
			if (parts.length >= 2) {
				data.put("city", parts[0]);
				data.put("town", parts[1]);
			} else if (parts.length == 1) {
				data.put("city", parts[0]);
				data.put("town", "");
			}
		}

		return data;
	}
}