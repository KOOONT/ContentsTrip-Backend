package com.Kooont.HeritageLoad.dto;

import lombok.Data;

@Data
public class TouristPopularDataDto {
    private String areaNm;  // 지역명
    private String daywkDivNm;  // 요일 구분
    private String touDivNm;  // 방문자 구분
    private double touNum;  // 방문자 수
    private String baseYmd;  // 기준 날짜
}
