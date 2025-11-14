package tetoandeggens.seeyouagainbatch.job.animaldataload.service;

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

import tetoandeggens.seeyouagainbatch.domain.AnimalLocation;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.AnimalPublicDataDto;
import tetoandeggens.seeyouagainbatch.job.animaldataload.dto.GeoCoordinateDto;
import tetoandeggens.seeyouagainbatch.job.animaldataload.repository.AnimalLocationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnimalLocationService 단위 테스트")
class AnimalLocationServiceTest {

    @Mock
    private AnimalLocationRepository animalLocationRepository;

    @Mock
    private KakaoMapService kakaoMapService;

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @InjectMocks
    private AnimalLocationService animalLocationService;

    @Test
    @DisplayName("새로운 보호소 위치를 처리하고 ID 맵을 반환해야 한다")
    void shouldProcessNewAnimalLocations() {
        List<AnimalPublicDataDto> dataList = List.of(
                createMockDto("test-reg-001", "강남구청", "서울특별시 강남구 학동로 426"),
                createMockDto("test-reg-002", "송파구청", "서울특별시 송파구 올림픽로 326")
        );

        GeoCoordinateDto coordinate = new GeoCoordinateDto(37.5665, 126.9780);

        when(kakaoMapService.searchCoordinates(anyString())).thenReturn(coordinate);
        when(animalLocationRepository.findByCenterNoIn(any(Set.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(
                        createAnimalLocation(1L, "test-reg-001", "강남구청"),
                        createAnimalLocation(2L, "test-reg-002", "송파구청")
                ));

        when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
                .thenReturn(new int[]{1, 1});

        Map<String, Long> result = animalLocationService.processAnimalLocations(dataList);

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("test-reg-001", "test-reg-002");
        assertThat(result.get("test-reg-001")).isEqualTo(1L);
        assertThat(result.get("test-reg-002")).isEqualTo(2L);

        verify(animalLocationRepository, times(2)).findByCenterNoIn(any(Set.class));
        verify(kakaoMapService, times(2)).searchCoordinates(anyString());
        verify(namedParameterJdbcTemplate, times(1)).batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    @Test
    @DisplayName("기존 보호소 위치만 있으면 ID 맵을 반환하고 insert는 실행하지 않아야 한다")
    void shouldReturnExistingAnimalLocationsWithoutInsert() {
        List<AnimalPublicDataDto> dataList = List.of(
                createMockDto("test-reg-001", "강남구청", "서울특별시 강남구 학동로 426")
        );

        when(animalLocationRepository.findByCenterNoIn(any(Set.class)))
                .thenReturn(List.of(createAnimalLocation(1L, "test-reg-001", "강남구청")));

        Map<String, Long> result = animalLocationService.processAnimalLocations(dataList);

        assertThat(result).hasSize(1);
        assertThat(result.get("test-reg-001")).isEqualTo(1L);

        verify(animalLocationRepository, times(1)).findByCenterNoIn(any(Set.class));
        verify(kakaoMapService, never()).searchCoordinates(anyString());
        verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    @Test
    @DisplayName("기존과 새로운 보호소가 혼합되어 있으면 새로운 것만 insert해야 한다")
    void shouldProcessMixedAnimalLocations() {
        List<AnimalPublicDataDto> dataList = List.of(
                createMockDto("test-reg-001", "강남구청", "서울특별시 강남구 학동로 426"),
                createMockDto("test-reg-002", "송파구청", "서울특별시 송파구 올림픽로 326")
        );

        GeoCoordinateDto coordinate = new GeoCoordinateDto(37.5665, 126.9780);

        when(kakaoMapService.searchCoordinates(anyString())).thenReturn(coordinate);
        when(animalLocationRepository.findByCenterNoIn(any(Set.class)))
                .thenReturn(List.of(createAnimalLocation(1L, "test-reg-001", "강남구청")))
                .thenReturn(List.of(createAnimalLocation(2L, "test-reg-002", "송파구청")));

        when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
                .thenReturn(new int[]{1});

        Map<String, Long> result = animalLocationService.processAnimalLocations(dataList);

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("test-reg-001", "test-reg-002");

        verify(animalLocationRepository, times(2)).findByCenterNoIn(any(Set.class));
        verify(kakaoMapService, times(1)).searchCoordinates(anyString());
        verify(namedParameterJdbcTemplate, times(1)).batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    @Test
    @DisplayName("빈 리스트로 처리하면 빈 맵을 반환해야 한다")
    void shouldReturnEmptyMapWhenListIsEmpty() {
        List<AnimalPublicDataDto> emptyList = Collections.emptyList();

        when(animalLocationRepository.findByCenterNoIn(any(Set.class)))
                .thenReturn(Collections.emptyList());

        Map<String, Long> result = animalLocationService.processAnimalLocations(emptyList);

        assertThat(result).isEmpty();
        verify(animalLocationRepository, times(1)).findByCenterNoIn(any(Set.class));
        verify(kakaoMapService, never()).searchCoordinates(anyString());
        verify(namedParameterJdbcTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    @Test
    @DisplayName("null 또는 빈 careRegNo는 무시되어야 한다")
    void shouldIgnoreNullOrBlankCareRegNo() {
        List<AnimalPublicDataDto> dataList = new ArrayList<>();
        dataList.add(createMockDto(null, "강남구청", "서울특별시 강남구 학동로 426"));
        dataList.add(createMockDto("", "송파구청", "서울특별시 송파구 올림픽로 326"));
        dataList.add(createMockDto("   ", "성남시청", "경기도 성남시 중원구 성남대로 997"));
        dataList.add(createMockDto("test-reg-001", "강남구청", "서울특별시 강남구 학동로 426"));

        GeoCoordinateDto coordinate = new GeoCoordinateDto(37.5665, 126.9780);

        when(kakaoMapService.searchCoordinates(anyString())).thenReturn(coordinate);
        when(animalLocationRepository.findByCenterNoIn(any(Set.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(createAnimalLocation(1L, "test-reg-001", "강남구청")));

        when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
                .thenReturn(new int[]{1});

        Map<String, Long> result = animalLocationService.processAnimalLocations(dataList);

        assertThat(result).hasSize(1);
        assertThat(result).containsKey("test-reg-001");

        verify(animalLocationRepository, times(2)).findByCenterNoIn(any(Set.class));
        verify(namedParameterJdbcTemplate, times(1)).batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    @Test
    @DisplayName("동일한 careRegNo가 여러 개 있어도 중복 제거되어 한 번만 처리되어야 한다")
    void shouldDeduplicateSameCareRegNo() {
        List<AnimalPublicDataDto> dataList = List.of(
                createMockDto("test-reg-001", "강남구청", "서울특별시 강남구 학동로 426"),
                createMockDto("test-reg-001", "강남구청", "서울특별시 강남구 학동로 426"),
                createMockDto("test-reg-001", "강남구청", "서울특별시 강남구 학동로 426")
        );

        GeoCoordinateDto coordinate = new GeoCoordinateDto(37.5665, 126.9780);

        when(kakaoMapService.searchCoordinates(anyString())).thenReturn(coordinate);
        when(animalLocationRepository.findByCenterNoIn(any(Set.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(createAnimalLocation(1L, "test-reg-001", "강남구청")));

        when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
                .thenReturn(new int[]{1});

        Map<String, Long> result = animalLocationService.processAnimalLocations(dataList);

        assertThat(result).hasSize(1);
        assertThat(result.get("test-reg-001")).isEqualTo(1L);

        verify(animalLocationRepository, times(2)).findByCenterNoIn(argThat(set -> set.size() == 1));
        verify(kakaoMapService, times(1)).searchCoordinates(anyString());
        verify(namedParameterJdbcTemplate, times(1))
                .batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    @Test
    @DisplayName("주소가 없는 보호소는 건너뛰어야 한다")
    void shouldSkipCenterWithoutAddress() {
        List<AnimalPublicDataDto> dataList = List.of(
                createMockDto("test-reg-001", "강남구청", null),
                createMockDto("test-reg-002", "송파구청", ""),
                createMockDto("test-reg-003", "성남시청", "경기도 성남시 중원구 성남대로 997")
        );

        GeoCoordinateDto coordinate = new GeoCoordinateDto(37.5665, 126.9780);

        when(kakaoMapService.searchCoordinates(anyString())).thenReturn(coordinate);
        when(animalLocationRepository.findByCenterNoIn(any(Set.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(createAnimalLocation(3L, "test-reg-003", "성남시청")));

        when(namedParameterJdbcTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
                .thenReturn(new int[]{1});

        Map<String, Long> result = animalLocationService.processAnimalLocations(dataList);

        assertThat(result).hasSize(1);
        assertThat(result).containsKey("test-reg-003");

        verify(kakaoMapService, times(1)).searchCoordinates(anyString());
        verify(namedParameterJdbcTemplate, times(1))
                .batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    private AnimalPublicDataDto createMockDto(String careRegNo, String careNm, String careAddr) {
        return new AnimalPublicDataDto(
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, careNm, null, careAddr,
                careRegNo, null, null
        );
    }

    private AnimalLocation createAnimalLocation(Long id, String centerNo, String name) {
        return AnimalLocation.builder()
                .id(id)
                .centerNo(centerNo)
                .name(name)
                .address("테스트 주소")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();
    }
}