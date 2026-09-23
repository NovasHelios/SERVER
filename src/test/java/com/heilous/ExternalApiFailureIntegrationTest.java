package com.heilous;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.vworld.dto.KakaoAddressResponse;
import com.heilous.vworld.service.KakaoAddressService;
import com.heilous.vworld.service.VWorldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExternalApiFailureIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private VWorldService vWorldService;

    @Test
    void vworldServiceFailureIsReturnedAsGatewayError() throws Exception {
        when(vWorldService.getLandInfo(any())).thenThrow(new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR));

        mockMvc.perform(get("/api/vworld/land/1111010100100010000"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.data.code").value("EXT_001"));
    }

    @Test
    void invalidKakaoAddressDataIsRejected() {
        KakaoAddressService kakaoAddressService = new KakaoAddressService(new ObjectMapper());

        assertThatThrownBy(() -> kakaoAddressService.buildPnu(null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(GlobalErrorCode.KAKAO_ADDRESS_NOT_FOUND);

        KakaoAddressResponse.Address address = new KakaoAddressResponse.Address();
        address.setBCode(" ");
        assertThatThrownBy(() -> kakaoAddressService.buildPnu(address))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(GlobalErrorCode.KAKAO_ADDRESS_NOT_FOUND);
    }
}
