package com.heilous;

import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.land.dto.LandDetailResponse;
import com.heilous.land.dto.LandFilterRequest;
import com.heilous.land.dto.LandResponse;
import com.heilous.land.dto.RegionStatsResponse;
import com.heilous.land.service.LandService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LandQueryApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private LandService landService;

    @Test
    void mapLandListReturnsCoordinates() throws Exception {
        when(landService.getAllLands()).thenReturn(List.of(landSummary()));

        mockMvc.perform(get("/api/lands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].id").value(101))
                .andExpect(jsonPath("$.data[0].x").value(126.978))
                .andExpect(jsonPath("$.data[0].y").value(37.5665))
                .andExpect(jsonPath("$.data[0].transactionType").value("SALE"));
    }

    @Test
    void mapRegionStatisticsReturnsGroupedCounts() throws Exception {
        RegionStatsResponse.SigunguStats sigungu = RegionStatsResponse.SigunguStats.builder()
                .sigungu("종로구").saleCount(3).leaseCount(1).businessHopeCount(2).build();
        when(landService.getRegionStats()).thenReturn(List.of(
                RegionStatsResponse.builder().sido("서울특별시").sigungus(List.of(sigungu)).build()
        ));

        mockMvc.perform(get("/api/lands/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sido").value("서울특별시"))
                .andExpect(jsonPath("$.data[0].sigungus[0].sigungu").value("종로구"))
                .andExpect(jsonPath("$.data[0].sigungus[0].saleCount").value(3));
    }

    @Test
    void landFilterPassesConditionsAndReturnsMatchedLands() throws Exception {
        when(landService.getLandsByFilter(any())).thenReturn(List.of(landSummary()));

        mockMvc.perform(post("/api/lands/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"transactionType\":\"SALE\",\"saleMinPrice\":1000000,\"sido\":\"서울특별시\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].address").value("서울특별시 종로구 세종대로 1"));

        ArgumentCaptor<LandFilterRequest> captor = ArgumentCaptor.forClass(LandFilterRequest.class);
        verify(landService).getLandsByFilter(captor.capture());
        assertThat(captor.getValue().getStatus().name()).isEqualTo("APPROVED");
        assertThat(captor.getValue().getTransactionType().name()).isEqualTo("SALE");
        assertThat(captor.getValue().getSaleMinPrice()).isEqualTo(1_000_000L);
        assertThat(captor.getValue().getSido()).isEqualTo("서울특별시");
    }

    @Test
    void landFilterRejectsMalformedEnum() throws Exception {
        mockMvc.perform(post("/api/lands/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionType\":\"INVALID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("INVALID_FORMAT"));
    }

    @Test
    void landDetailReturnsFullLandInformation() throws Exception {
        when(landService.getLand(101L)).thenReturn(LandDetailResponse.builder()
                .id(101L).ownerEmail("owner@example.com").address("서울특별시 종로구 세종대로 1")
                .area(123.4).pnu("1111010100100010000").status("APPROVED").transactionType("SALE")
                .x(126.9780).y(37.5665).description("테스트 토지")
                .landImagePaths(List.of("land-1.jpg")).landZones(List.of()).landEtcs(List.of()).build());

        mockMvc.perform(get("/api/lands/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.ownerEmail").value("owner@example.com"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.landImagePaths[0]").value("land-1.jpg"));
    }

    @Test
    void missingLandReturnsNotFoundError() throws Exception {
        when(landService.getLand(999L)).thenThrow(new CustomException(GlobalErrorCode.LAND_NOT_FOUND));

        mockMvc.perform(get("/api/lands/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.data.code").value("LAND_001"));
    }

    private LandResponse landSummary() {
        return LandResponse.builder().id(101L).address("서울특별시 종로구 세종대로 1")
                .area(123.4).pnu("1111010100100010000").desiredPrice(1_000_000L)
                .transactionType("SALE").landImagePaths(List.of("land-1.jpg"))
                .x(126.9780).y(37.5665).lcCodeNm("대").regstrSeCodeNm("일반").build();
    }
}
