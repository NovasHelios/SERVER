package com.heilous.vworld.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.vworld.dto.KakaoAddressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAddressService {

    private final ObjectMapper objectMapper;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private static final String KAKAO_ADDRESS_URL = "https://dapi.kakao.com/v2/local/search/address.json";

    /**
     * 주소(지번/도로명)로 카카오 주소 검색 API를 호출합니다.
     */
    public KakaoAddressResponse searchAddress(String query) {
        try {
            String urlString = UriComponentsBuilder.fromHttpUrl(KAKAO_ADDRESS_URL)
                    .queryParam("query", query)
                    .queryParam("analyze_type", "similar")
                    .queryParam("size", 1)
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "KakaoAK " + kakaoApiKey);
            conn.setRequestProperty("Content-type", "application/json");

            int responseCode = conn.getResponseCode();
            log.info("Kakao Address API Response code: {}", responseCode);

            BufferedReader rd;
            if (responseCode >= 200 && responseCode <= 300) {
                rd = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
            } else {
                rd = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)
                );
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            conn.disconnect();

            String responseBody = sb.toString();
            log.info("Kakao Address API Response: {}", responseBody);

            return objectMapper.readValue(responseBody, KakaoAddressResponse.class);

        } catch (Exception e) {
            log.error("카카오 주소 API 호출 실패", e);
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * 카카오 주소 검색 결과의 지번 주소 정보로 PNU(19자리)를 생성합니다.
     * 구성: b_code(10) + mountain_yn(1: Y→2, N→1) + main_address_no(4, 0패딩) + sub_address_no(4, 0패딩)
     */
    public String buildPnu(KakaoAddressResponse.Address address) {
        if (address == null) {
            throw new CustomException(GlobalErrorCode.KAKAO_ADDRESS_NOT_FOUND);
        }

        String bCode = address.getBCode();
        if (bCode == null || bCode.isBlank()) {
            throw new CustomException(GlobalErrorCode.KAKAO_ADDRESS_NOT_FOUND);
        }

        String mountainCode = "Y".equalsIgnoreCase(address.getMountainYn()) ? "2" : "1";

        String mainNo = String.format("%04d",
                parseIntSafe(address.getMainAddressNo()));

        String subNo = String.format("%04d",
                parseIntSafe(address.getSubAddressNo()));

        return bCode + mountainCode + mainNo + subNo;
    }

    private int parseIntSafe(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
