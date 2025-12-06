package tetoandeggens.seeyouagainbatch.job.keywordnotification.writer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import com.querydsl.jpa.impl.JPAQueryFactory;

import tetoandeggens.seeyouagainbatch.domain.KeywordCategoryType;
import tetoandeggens.seeyouagainbatch.domain.KeywordType;
import tetoandeggens.seeyouagainbatch.job.keywordnotification.dto.KeywordNotificationDto;
import tetoandeggens.seeyouagainbatch.job.keywordnotification.parameter.KeywordNotificationJobParameter;
import tetoandeggens.seeyouagainbatch.job.keywordnotification.service.FcmService;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordNotificationWriter 단위 테스트")
class KeywordNotificationWriterTest {

	@Mock
	private JPAQueryFactory queryFactory;

	@Mock
	private FcmService fcmService;

	@Mock
	private KeywordNotificationJobParameter jobParameter;

	private KeywordNotificationWriter writer;

	@BeforeEach
	void setUp() {
		writer = new KeywordNotificationWriter(queryFactory, fcmService, jobParameter);
	}

	@Test
	@DisplayName("동물 수가 0이면 FCM 전송을 하지 않아야 한다")
	void shouldNotSendWhenAnimalCountIsZero() {
		when(jobParameter.getDate()).thenReturn("20250115");

		KeywordNotificationDto dto = new KeywordNotificationDto(
			1L, "믹스견", 0L, KeywordType.ABANDONED, KeywordCategoryType.BREED
		);

		Chunk<KeywordNotificationDto> chunk = new Chunk<>(List.of(dto));

		writer.write(chunk);

		verify(fcmService, never()).sendMulticastNotification(
			any(), any(), any(), any(), any(), any(), any(), any()
		);
	}

	@Test
	@DisplayName("빈 chunk는 처리하지 않아야 한다")
	void shouldNotProcessEmptyChunk() {
		when(jobParameter.getDate()).thenReturn("20250115");

		Chunk<KeywordNotificationDto> emptyChunk = new Chunk<>();

		writer.write(emptyChunk);

		verify(fcmService, never()).sendMulticastNotification(
			any(), any(), any(), any(), any(), any(), any(), any()
		);
	}

	@Test
	@DisplayName("null chunk는 처리하지 않아야 한다")
	void shouldNotProcessNullChunk() {
		when(jobParameter.getDate()).thenReturn("20250115");

		Chunk<KeywordNotificationDto> nullChunk = new Chunk<>(new ArrayList<>());

		writer.write(nullChunk);

		verify(fcmService, never()).sendMulticastNotification(
			any(), any(), any(), any(), any(), any(), any(), any()
		);
	}
}
