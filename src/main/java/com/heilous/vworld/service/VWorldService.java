package com.heilous.vworld.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.vworld.dto.AddressLandResponse;
import com.heilous.vworld.dto.KakaoAddressResponse;
import com.heilous.vworld.dto.VWorldLandRequest;
import com.heilous.vworld.dto.VWorldLandResponse;
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
public class VWorldService {

    private final ObjectMapper objectMapper;
    private final KakaoAddressService kakaoAddressService;

    @Value("${vworld.api.key}")
    private String apiKey;

    @Value("${vworld.api.domain:}")
    private String apiDomain;

    private static final String API_URL = "http://api.vworld.kr/ned/data/ladfrlList";

    public VWorldLandResponse getLandInfo(VWorldLandRequest request) {
        try {
            String urlString = UriComponentsBuilder.fromHttpUrl(API_URL)
                    .queryParam("key", apiKey)
                    .queryParam("domain", apiDomain)
                    .queryParam("pnu", request.getPnu())
                    .queryParam("format", request.getFormat())
                    .queryParam("numOfRows", request.getNumOfRows())
                    .queryParam("pageNo", request.getPageNo())
                    .build()
                    .toUriString();

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");
            conn.setConnectTimeout(3_000);
            conn.setReadTimeout(5_000);

            int responseCode = conn.getResponseCode();
            log.info("VWorld API Response code: {}", responseCode);

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
            return objectMapper.readValue(responseBody, VWorldLandResponse.class);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("VWorld API 호출 실패", e);
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * 주소(지번/도로명)로 카카오 API를 호출해 PNU를 생성하고,
     * 해당 PNU로 토지 임야 정보를 한 번에 조회합니다.
     */
    public AddressLandResponse getLandInfoByAddress(String address) {
        // 1. 카카오 주소 검색
        KakaoAddressResponse kakaoResponse = kakaoAddressService.searchAddress(address);

        if (kakaoResponse.getDocuments() == null || kakaoResponse.getDocuments().isEmpty()) {
            throw new CustomException(GlobalErrorCode.KAKAO_ADDRESS_NOT_FOUND);
        }

        KakaoAddressResponse.Document doc = kakaoResponse.getDocuments().get(0);

        // 지번 주소 정보가 있어야 PNU 생성 가능
        if (doc.getAddress() == null) {
            throw new CustomException(GlobalErrorCode.KAKAO_ADDRESS_NOT_FOUND);
        }

        // 2. PNU 생성
        String pnu = kakaoAddressService.buildPnu(doc.getAddress());
        log.info("생성된 PNU: {}", pnu);

        // 3. VWorld 토지 정보 조회
        VWorldLandRequest request = new VWorldLandRequest();
        request.setPnu(pnu);
        VWorldLandResponse landInfo = getLandInfo(request);

        // 4. 응답 조합
        KakaoAddressResponse.RoadAddress roadAddress = doc.getRoadAddress();

        Double x = null;
        Double y = null;
        try { x = doc.getX() != null ? Double.parseDouble(doc.getX()) : null; } catch (NumberFormatException ignored) {}
        try { y = doc.getY() != null ? Double.parseDouble(doc.getY()) : null; } catch (NumberFormatException ignored) {}

        return AddressLandResponse.builder()
                .pnu(pnu)
                .addressName(doc.getAddressName())
                .x(x)
                .y(y)
                .zoneNo(roadAddress != null ? roadAddress.getZoneNo() : null)
                .buildingName(roadAddress != null ? roadAddress.getBuildingName() : null)
                .landInfo(landInfo)
                .build();
    }
}
