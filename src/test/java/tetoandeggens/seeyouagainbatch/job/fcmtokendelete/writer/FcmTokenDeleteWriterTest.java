package tetoandeggens.seeyouagainbatch.job.fcmtokendelete.writer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import tetoandeggens.seeyouagainbatch.domain.FcmToken;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmTokenDeleteWriter 단위 테스트")
class FcmTokenDeleteWriterTest {

	@Mock
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@InjectMocks
	private FcmTokenDeleteWriter writer;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	@Captor
	private ArgumentCaptor<SqlParameterSource[]> paramsCaptor;

	@Test
	@DisplayName("여러 개의 FCM 토큰을 bulk delete 한다")
	void shouldBulkDeleteMultipleFcmTokens() {
		List<FcmToken> tokens = List.of(
			createFcmToken(1L, "token-1"),
			createFcmToken(2L, "token-2"),
			createFcmToken(3L, "token-3")
		);

		Chunk<FcmToken> chunk = new Chunk<>(tokens);

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1, 1, 1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(sqlCaptor.capture(), paramsCaptor.capture());

		String sql = sqlCaptor.getValue();
		assertThat(sql).contains("DELETE FROM fcm_token");
		assertThat(sql).contains("fcm_token_id = :id");

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(3);
	}

	@Test
	@DisplayName("null 항목은 필터링하고 유효한 항목만 delete 한다")
	void shouldFilterNullItemsAndDeleteValidOnes() {
		List<FcmToken> tokens = List.of(
			createFcmToken(1L, "token-1"),
			createFcmTokenWithNullId(),
			createFcmToken(3L, "token-3")
		);

		Chunk<FcmToken> chunk = new Chunk<>(tokens);

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1, 1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(2);
	}

	@Test
	@DisplayName("빈 chunk는 delete를 수행하지 않는다")
	void shouldNotDeleteWhenChunkIsEmpty() {
		Chunk<FcmToken> emptyChunk = new Chunk<>();

		writer.write(emptyChunk);

		verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
	}

	@Test
	@DisplayName("모든 항목이 유효하지 않은 경우 delete를 수행하지 않는다")
	void shouldNotDeleteWhenAllItemsAreInvalid() {
		List<FcmToken> tokens = List.of(
			createFcmTokenWithNullId(),
			createFcmTokenWithNullId()
		);

		Chunk<FcmToken> chunk = new Chunk<>(tokens);

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
	}

	@Test
	@DisplayName("단일 항목도 정상적으로 delete 한다")
	void shouldDeleteSingleItem() {
		List<FcmToken> tokens = List.of(
			createFcmToken(1L, "token-1")
		);

		Chunk<FcmToken> chunk = new Chunk<>(tokens);

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(1);
	}

	@Test
	@DisplayName("chunk에 null 항목이 포함되어 있어도 필터링하여 delete 한다")
	void shouldFilterNullElementsFromChunk() {
		Chunk<FcmToken> chunk = new Chunk<>();
		chunk.add(createFcmToken(1L, "token-1"));
		chunk.add(null);
		chunk.add(createFcmToken(2L, "token-2"));

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1, 1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(2);
	}

	@Test
	@DisplayName("대량의 FCM 토큰을 delete 할 수 있다")
	void shouldDeleteLargeNumberOfTokens() {
		List<FcmToken> tokens = new java.util.ArrayList<>();
		for (int i = 1; i <= 100; i++) {
			tokens.add(createFcmToken((long) i, "token-" + i));
		}

		Chunk<FcmToken> chunk = new Chunk<>(tokens);

		int[] results = new int[100];
		java.util.Arrays.fill(results, 1);
		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(results);

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(100);
	}

	private FcmToken createFcmToken(Long id, String token) {
		FcmToken fcmToken = mock(FcmToken.class);
		when(fcmToken.getId()).thenReturn(id);
		return fcmToken;
	}

	private FcmToken createFcmTokenWithNullId() {
		FcmToken fcmToken = mock(FcmToken.class);
		when(fcmToken.getId()).thenReturn(null);
		return fcmToken;
	}
}
