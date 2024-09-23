package com.Kooont.HeritageLoad.service;

import com.Kooont.HeritageLoad.dto.GungListItemDto;
import com.Kooont.HeritageLoad.dto.GungListResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

@Service
public class GungListService {

    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(GungListService.class);

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService1/searchKeyword1";
    private static final String SERVICE_KEY = "";

    public GungListService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<GungListItemDto> searchGungListItemsByKeywords(List<String> keywords) throws URISyntaxException, UnsupportedEncodingException {
        List<GungListItemDto> GungListItems = new ArrayList<>();

        for (String keyword : keywords) {
            // URL 생성
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
            String encodedServiceKey = URLEncoder.encode(SERVICE_KEY, StandardCharsets.UTF_8.toString());

            String url = BASE_URL
                    + "?serviceKey=" + encodedServiceKey
                    + "&numOfRows=1&pageNo=1&MobileOS=ETC&MobileApp=HeritageLoad&_type=json"
                    + "&keyword=" + encodedKeyword;

            logger.info("Raw API URL: {}", url);

            // HTTP 헤더 설정 (Accept: application/json)
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // API 요청을 전송하고 응답을 받음
            URI uri = new URI(url);
            String jsonString = restTemplate.getForObject(uri, String.class);


            logger.info("Raw API Response: {}", jsonString);

            // 응답 처리 로직 (필요시 DTO 변환 추가)
            // GungListResponseDto dtoResponse = objectMapper.readValue(rawResponse, GungListResponseDto.class);
            // if (dtoResponse != null && dtoResponse.getResponse() != null) {
            //     GungListItems.addAll(dtoResponse.getResponse().getBody().getItems().getItem());
            // }

        }

        return GungListItems;
    }
}