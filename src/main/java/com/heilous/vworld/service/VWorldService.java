package com.heilous.vworld.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import com.heilous.vworld.dto.AddressLandResponse;
import com.heilous.vworld.dto.KakaoAddressResponse;
import com.heilous.vworld.dto.LandPriceWfsResponse;
import com.heilous.vworld.dto.PossessionAttrResponse;
import com.heilous.vworld.dto.VWorldLandRequest;
import com.heilous.vworld.dto.VWorldLandResponse;
import com.heilous.vworld.dto.VWorldWfsResponse;
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

    private static final String API_URL            = "http://api.vworld.kr/ned/data/ladfrlList";
    private static final String WFS_URL            = "http://api.vworld.kr/ned/wfs/getLandUseWFS";
    private static final String POSSESSION_URL     = "https://api.vworld.kr/ned/data/getPossessionAttr";
    private static final String LAND_PRICE_WFS_URL = "http://api.vworld.kr/ned/wfs/getReferLandPriceWFS";

    public VWorldLandResponse getLandInfo(VWorldLandRequest request) {
        try {
            String urlString = UriComponentsBuilder.fromHttpUrl(API_URL)
                    .queryParam("key", apiKey).queryParam("domain", apiDomain)
                    .queryParam("pnu", request.getPnu()).queryParam("format", request.getFormat())
                    .queryParam("numOfRows", request.getNumOfRows()).queryParam("pageNo", request.getPageNo())
                    .build().toUriString();

            return objectMapper.readValue(call(urlString, 3_000, 5_000), VWorldLandResponse.class);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("VWorld API 호출 실패", e);
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);
        }
    }

    public AddressLandResponse getLandInfoByAddress(String address) {
        KakaoAddressResponse kakaoResponse = kakaoAddressService.searchAddress(address);

        if (kakaoResponse.getDocuments() == null || kakaoResponse.getDocuments().isEmpty()) {
            throw new CustomException(GlobalErrorCode.KAKAO_ADDRESS_NOT_FOUND);
        }

        KakaoAddressResponse.Document doc = kakaoResponse.getDocuments().get(0);
        if (doc.getAddress() == null) throw new CustomException(GlobalErrorCode.KAKAO_ADDRESS_NOT_FOUND);

        String pnu = kakaoAddressService.buildPnu(doc.getAddress());
        log.info("생성된 PNU: {}", pnu);

        VWorldLandRequest request = new VWorldLandRequest();
        request.setPnu(pnu);
        VWorldLandResponse landInfo = getLandInfo(request);

        KakaoAddressResponse.RoadAddress roadAddress = doc.getRoadAddress();
        Double x = null, y = null;
        try { x = doc.getX() != null ? Double.parseDouble(doc.getX()) : null; } catch (NumberFormatException ignored) {}
        try { y = doc.getY() != null ? Double.parseDouble(doc.getY()) : null; } catch (NumberFormatException ignored) {}

        return AddressLandResponse.builder()
                .pnu(pnu).addressName(doc.getAddressName()).x(x).y(y)
                .zoneNo(roadAddress != null ? roadAddress.getZoneNo() : null)
                .buildingName(roadAddress != null ? roadAddress.getBuildingName() : null)
                .landInfo(landInfo).build();
    }

    public PossessionAttrResponse getPossessionAttr(String pnu, int numOfRows) {
        try {
            String urlString = UriComponentsBuilder.fromHttpUrl(POSSESSION_URL)
                    .queryParam("key", apiKey).queryParam("domain", apiDomain)
                    .queryParam("pnu", pnu).queryParam("format", "json")
                    .queryParam("numOfRows", numOfRows).queryParam("pageNo", 1)
                    .build().toUriString();

            return objectMapper.readValue(call(urlString, 5_000, 10_000), PossessionAttrResponse.class);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("VWorld PossessionAttr API 호출 실패", e);
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);
        }
    }

    public VWorldWfsResponse getLandUseByPnu(String pnu) {
        try {
            String urlString = UriComponentsBuilder.fromHttpUrl(WFS_URL)
                    .queryParam("key", apiKey).queryParam("domain", apiDomain)
                    .queryParam("typename", "dt_d154").queryParam("pnu", pnu)
                    .queryParam("maxFeatures", "1").queryParam("resultType", "results")
                    .queryParam("srsName", "EPSG:4326").queryParam("output", "application/json")
                    .build().toUriString();

            return objectMapper.readValue(call(urlString, 5_000, 10_000), VWorldWfsResponse.class);
        } catch (Exception e) {
            log.error("VWorld WFS API 호출 실패", e);
            return null;
        }
    }

    public LandPriceWfsResponse getLandPriceByPnu(String pnu) {
        try {
            String urlString = UriComponentsBuilder.fromHttpUrl(LAND_PRICE_WFS_URL)
                    .queryParam("key", apiKey).queryParam("domain", apiDomain)
                    .queryParam("typename", "dt_d152").queryParam("pnu", pnu)
                    .queryParam("maxFeatures", "1").queryParam("resultType", "results")
                    .queryParam("srsName", "EPSG:4326").queryParam("output", "application/json")
                    .build().toUriString();

            return objectMapper.readValue(call(urlString, 5_000, 10_000), LandPriceWfsResponse.class);
        } catch (Exception e) {
            log.error("VWorld LandPrice WFS 호출 실패", e);
            return null;
        }
    }

    private String call(String urlString, int connectTimeout, int readTimeout) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);

        int responseCode = conn.getResponseCode();
        log.info("VWorld API Response code: {}", responseCode);

        BufferedReader rd = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode <= 300
                        ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) sb.append(line);
        rd.close();
        conn.disconnect();
        return sb.toString();
    }
}
