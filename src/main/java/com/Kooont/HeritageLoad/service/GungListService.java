package com.Kooont.HeritageLoad.service;

import com.Kooont.HeritageLoad.dto.GungListItemDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class GungListService {

    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(GungListService.class);

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2/searchKeyword2";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${data-key}")
    private String dataKey;

    public GungListService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<GungListItemDto> searchGungListItemsByKeywords(List<String> keywords) throws URISyntaxException, UnsupportedEncodingException {
        List<GungListItemDto> GungListItems = new ArrayList<>();

        for (String keyword : keywords) {
            if (dataKey == null || dataKey.isBlank()) {
                logger.warn("Skipping GungList API call because data-key is not configured");
                return GungListItems;
            }

            // URL 생성
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
            String encodedServiceKey = URLEncoder.encode(dataKey, StandardCharsets.UTF_8.toString());

            String url = BASE_URL
                    + "?serviceKey=" + encodedServiceKey
                    + "&numOfRows=1&pageNo=1&MobileOS=ETC&MobileApp=HeritageLoad&_type=json"
                    + "&keyword=" + encodedKeyword;

            logger.info("Calling GungList API for keyword: {}", keyword);

            // API 요청을 전송하고 응답을 받음
            URI uri = new URI(url);
            try {
                String jsonString = restTemplate.getForObject(uri, String.class);
                logger.info("Raw API Response: {}", jsonString);

                JsonNode itemNode = objectMapper.readTree(jsonString)
                        .path("response")
                        .path("body")
                        .path("items")
                        .path("item");

                if (itemNode.isArray()) {
                    for (JsonNode item : itemNode) {
                        GungListItems.add(objectMapper.treeToValue(item, GungListItemDto.class));
                    }
                } else if (!itemNode.isMissingNode() && !itemNode.isNull()) {
                    GungListItems.add(objectMapper.treeToValue(itemNode, GungListItemDto.class));
                }
            } catch (RestClientException | JsonProcessingException e) {
                logger.error("Error fetching GungList item for keyword: {}", keyword, e);
            }

        }

        return GungListItems;
    }
}
