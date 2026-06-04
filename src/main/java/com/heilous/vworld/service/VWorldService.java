package com.heilous.vworld.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
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
            log.info("VWorld API Response: {}", responseBody);

            return objectMapper.readValue(responseBody, VWorldLandResponse.class);

        } catch (Exception e) {
            log.error("VWorld API 호출 실패", e);
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);
        }
    }
}
