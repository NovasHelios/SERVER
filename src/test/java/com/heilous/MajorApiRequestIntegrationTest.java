package com.heilous;

import com.heilous.auth.dto.LoginResponse;
import com.heilous.auth.service.AuthService;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.global.auth.JwtProvider;
import com.heilous.land.service.LandService;
import com.heilous.wish.service.WishService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MajorApiRequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockBean
    private AuthService authService;

    @MockBean
    private LandService landService;

    @MockBean
    private WishService wishService;

    @Test
    void loginReturnsTokenForValidRequest() throws Exception {
        when(authService.login(any())).thenReturn(
                new LoginResponse("test-access-token", "company@example.com", "COMPANY")
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"company@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.data.role").value("COMPANY"));
    }

    @Test
    void loginRejectsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    void loginReturnsAuthenticationErrorForInvalidCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new CustomException(GlobalErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"company@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.data.code").value("AUTH_001"));
    }

    @Test
    void publicLandListReturnsSuccessWithoutAuthentication() throws Exception {
        when(landService.getAllLands()).thenReturn(List.of());

        mockMvc.perform(get("/api/lands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void landFilterRejectsInvalidEnumValue() throws Exception {
        mockMvc.perform(post("/api/lands/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.code").value("INVALID_FORMAT"));
    }

    @Test
    void wishListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/wishes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.data.code").value("AUTH_009"));
    }

    @Test
    void wishCanBeAddedWithValidJwt() throws Exception {
        String token = jwtProvider.createToken("company@example.com", "COMPANY");
        doNothing().when(wishService).addWish(eq(42L), eq("company@example.com"));

        mockMvc.perform(post("/api/wishes/42")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("찜 목록에 추가되었습니다."));
    }
}
