package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import tetoandeggens.seeyouagainbatch.domain.BreedType;
import tetoandeggens.seeyouagainbatch.domain.Species;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.dto.AbandonedAnimalPublicDataDto;
import tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.repository.BreedTypeRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BreedTypeService 단위 테스트")
class BreedTypeServiceTest {

    @Mock
    private BreedTypeRepository breedTypeRepository;

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @InjectMocks
    private BreedTypeService breedTypeService;

    @Test
    @DisplayName("새로운 품종 유형을 처리하고 ID 맵을 반환해야 한다")
    void shouldProcessNewBreedTypes() {
        List<AbandonedAnimalPublicDataDto> dataList = List.of(
                createMockDto("417000", "믹스견", "417000"),
                createMockDto("422400", "코리안숏헤어", "422400")
        );

        when(breedTypeRepository.findByCodeIn(any(Set.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(
                        createBreedType(1L, "417000", "믹스견", Species.DOG),
                        createBreedType(2L, "422400", "코리안숏헤어", Species.CAT)
                ));

        when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
                .thenReturn(new int[]{1, 1});

        Map<String, Long> result = breedTypeService.processBreedTypes(dataList);

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("417000", "422400");
        assertThat(result.get("417000")).isEqualTo(1L);
        assertThat(result.get("422400")).isEqualTo(2L);

        verify(breedTypeRepository, times(2)).findByCodeIn(any(Set.class));
        verify(namedParameterJdbcTemplate, times(1)).batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    @Test
    @DisplayName("기존 품종 유형만 있으면 ID 맵을 반환하고 insert는 실행하지 않아야 한다")
    void shouldReturnExistingBreedTypesWithoutInsert() {
        List<AbandonedAnimalPublicDataDto> dataList = List.of(
                createMockDto("417000", "믹스견", "417000")
        );

        when(breedTypeRepository.findByCodeIn(any(Set.class)))
                .thenReturn(List.of(createBreedType(1L, "417000", "믹스견", Species.DOG)));

        Map<String, Long> result = breedTypeService.processBreedTypes(dataList);

        assertThat(result).hasSize(1);
        assertThat(result.get("417000")).isEqualTo(1L);

        verify(breedTypeRepository, times(1)).findByCodeIn(any(Set.class));
        verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    @Test
    @DisplayName("기존과 새로운 품종이 혼합되어 있으면 새로운 것만 insert해야 한다")
    void shouldProcessMixedBreedTypes() {
        List<AbandonedAnimalPublicDataDto> dataList = List.of(
                createMockDto("417000", "믹스견", "417000"),
                createMockDto("422400", "코리안숏헤어", "422400")
        );

        when(breedTypeRepository.findByCodeIn(any(Set.class)))
                .thenReturn(List.of(createBreedType(1L, "417000", "믹스견", Species.DOG)))
                .thenReturn(List.of(createBreedType(2L, "422400", "코리안숏헤어", Species.CAT)));

        when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
                .thenReturn(new int[]{1});

        Map<String, Long> result = breedTypeService.processBreedTypes(dataList);

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("417000", "422400");

        verify(breedTypeRepository, times(2)).findByCodeIn(any(Set.class));
        verify(namedParameterJdbcTemplate, times(1)).batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    @Test
    @DisplayName("빈 리스트로 처리하면 빈 맵을 반환해야 한다")
    void shouldReturnEmptyMapWhenListIsEmpty() {
        List<AbandonedAnimalPublicDataDto> emptyList = Collections.emptyList();

        when(breedTypeRepository.findByCodeIn(any(Set.class)))
                .thenReturn(Collections.emptyList());

        Map<String, Long> result = breedTypeService.processBreedTypes(emptyList);

        assertThat(result).isEmpty();
        verify(breedTypeRepository, times(1)).findByCodeIn(any(Set.class));
        verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    @Test
    @DisplayName("null 또는 빈 kindCd는 무시되어야 한다")
    void shouldIgnoreNullOrBlankKindCd() {
        List<AbandonedAnimalPublicDataDto> dataList = new ArrayList<>();
        dataList.add(createMockDto(null, "믹스견", "417000"));
        dataList.add(createMockDto("", "코리안숏헤어", "422400"));
        dataList.add(createMockDto("   ", "리트리버", "417000"));
        dataList.add(createMockDto("417000", "믹스견", "417000"));

        when(breedTypeRepository.findByCodeIn(any(Set.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(createBreedType(1L, "417000", "믹스견", Species.DOG)));

        when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
                .thenReturn(new int[]{1});

        Map<String, Long> result = breedTypeService.processBreedTypes(dataList);

        assertThat(result).hasSize(1);
        assertThat(result).containsKey("417000");

        verify(breedTypeRepository, times(2)).findByCodeIn(any(Set.class));
        verify(namedParameterJdbcTemplate, times(1)).batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    @Test
    @DisplayName("동일한 kindCd가 여러 개 있어도 중복 제거되어 한 번만 처리되어야 한다")
    void shouldDeduplicateSameKindCd() {
        List<AbandonedAnimalPublicDataDto> dataList = List.of(
                createMockDto("417000", "믹스견", "417000"),
                createMockDto("417000", "믹스견", "417000"),
                createMockDto("417000", "믹스견", "417000")
        );

        when(breedTypeRepository.findByCodeIn(any(Set.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(createBreedType(1L, "417000", "믹스견", Species.DOG)));

        when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
                .thenReturn(new int[]{1});

        Map<String, Long> result = breedTypeService.processBreedTypes(dataList);

        assertThat(result).hasSize(1);
        assertThat(result.get("417000")).isEqualTo(1L);

        verify(breedTypeRepository, times(2)).findByCodeIn(argThat(set -> set.size() == 1));
        verify(namedParameterJdbcTemplate, times(1))
                .batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    private AbandonedAnimalPublicDataDto createMockDto(String kindCd, String kindNm, String upKindCd) {
        return new AbandonedAnimalPublicDataDto(
                null, null, null, null,
                upKindCd, null, kindNm, kindCd,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null
        );
    }

    private BreedType createBreedType(Long id, String code, String name, Species species) {
        return BreedType.builder()
                .id(id)
                .code(code)
                .name(name)
                .type(species)
                .build();
    }
}