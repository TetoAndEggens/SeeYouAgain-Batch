package tetoandeggens.seeyouagainbatch.job.keywordmapping.writer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import tetoandeggens.seeyouagainbatch.domain.Animal;
import tetoandeggens.seeyouagainbatch.domain.AnimalByKeyword;
import tetoandeggens.seeyouagainbatch.domain.KeywordCategoryType;
import tetoandeggens.seeyouagainbatch.domain.KeywordType;
import tetoandeggens.seeyouagainbatch.domain.NotificationKeyword;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordMappingWriter 단위 테스트")
class KeywordMappingWriterTest {

	@Mock
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private KeywordMappingWriter writer;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	@Captor
	private ArgumentCaptor<SqlParameterSource[]> paramsCaptor;

	@BeforeEach
	void setUp() {
		writer = new KeywordMappingWriter(namedParameterJdbcTemplate);
	}

	@Test
	@DisplayName("여러 개의 매칭 데이터를 bulk insert 한다")
	void shouldBulkInsertMultipleMappings() {
		List<AnimalByKeyword> mappings1 = List.of(
			createMapping(1L, 1L),
			createMapping(1L, 2L)
		);
		List<AnimalByKeyword> mappings2 = List.of(
			createMapping(2L, 3L)
		);

		Chunk<List<AnimalByKeyword>> chunk = new Chunk<>(List.of(mappings1, mappings2));

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1, 1, 1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(sqlCaptor.capture(), paramsCaptor.capture());

		String sql = sqlCaptor.getValue();
		assertThat(sql).contains("INSERT INTO animal_by_keyword");
		assertThat(sql).contains("notification_keyword_id");
		assertThat(sql).contains("animal_id");
		assertThat(sql).contains("created_at");
		assertThat(sql).contains("updated_at");

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(3);
	}

	@Test
	@DisplayName("빈 리스트가 포함된 chunk는 필터링하고 유효한 항목만 insert 한다")
	void shouldFilterEmptyListsAndInsertValidOnes() {
		List<AnimalByKeyword> mappings1 = List.of(
			createMapping(1L, 1L)
		);
		List<AnimalByKeyword> emptyMappings = new ArrayList<>();
		List<AnimalByKeyword> mappings2 = List.of(
			createMapping(2L, 2L)
		);

		Chunk<List<AnimalByKeyword>> chunk = new Chunk<>(List.of(mappings1, emptyMappings, mappings2));

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1, 1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(2);
	}

	@Test
	@DisplayName("빈 chunk는 insert를 수행하지 않는다")
	void shouldNotInsertWhenChunkIsEmpty() {
		Chunk<List<AnimalByKeyword>> emptyChunk = new Chunk<>();

		writer.write(emptyChunk);

		verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
	}

	@Test
	@DisplayName("모든 항목이 빈 리스트인 경우 insert를 수행하지 않는다")
	void shouldNotInsertWhenAllItemsAreEmpty() {
		List<AnimalByKeyword> emptyMappings1 = new ArrayList<>();
		List<AnimalByKeyword> emptyMappings2 = new ArrayList<>();

		Chunk<List<AnimalByKeyword>> chunk = new Chunk<>(List.of(emptyMappings1, emptyMappings2));

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
	}

	@Test
	@DisplayName("null 리스트가 포함된 경우 필터링하고 유효한 항목만 insert 한다")
	void shouldFilterNullListsAndInsertValidOnes() {
		List<AnimalByKeyword> mappings = List.of(
			createMapping(1L, 1L),
			createMapping(1L, 2L)
		);

		Chunk<List<AnimalByKeyword>> chunk = new Chunk<>(Arrays.asList(mappings, null));

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1, 1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(2);
	}

	@Test
	@DisplayName("단일 매칭 데이터도 정상적으로 insert 한다")
	void shouldInsertSingleMapping() {
		List<AnimalByKeyword> mappings = List.of(
			createMapping(1L, 1L)
		);

		Chunk<List<AnimalByKeyword>> chunk = new Chunk<>(List.of(mappings));

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(new int[]{1});

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(1);
	}

	@Test
	@DisplayName("대량의 매칭 데이터를 정상적으로 insert 한다")
	void shouldInsertLargeNumberOfMappings() {
		List<AnimalByKeyword> largeMappings = new ArrayList<>();
		for (int i = 1; i <= 100; i++) {
			largeMappings.add(createMapping((long) i, (long) i));
		}

		Chunk<List<AnimalByKeyword>> chunk = new Chunk<>(List.of(largeMappings));

		int[] expectedResult = new int[100];
		for (int i = 0; i < 100; i++) {
			expectedResult[i] = 1;
		}

		when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
			.thenReturn(expectedResult);

		writer.write(chunk);

		verify(namedParameterJdbcTemplate, times(1))
			.batchUpdate(anyString(), paramsCaptor.capture());

		SqlParameterSource[] params = paramsCaptor.getValue();
		assertThat(params).hasSize(100);
	}

	private AnimalByKeyword createMapping(Long notificationKeywordId, Long animalId) {
		NotificationKeyword notificationKeyword = mock(NotificationKeyword.class);
		when(notificationKeyword.getId()).thenReturn(notificationKeywordId);

		Animal animal = new Animal(animalId);

		return AnimalByKeyword.builder()
			.notificationKeyword(notificationKeyword)
			.animal(animal)
			.build();
	}
}
