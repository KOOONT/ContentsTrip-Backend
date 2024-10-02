package com.Kooont.HeritageLoad.dto;

import lombok.Data;

@Data
public class RelatedAttractionDto {
    private String contentId;
    private String title;
    private String addr1;
    private String addr2;
    private String addr3;
    private String firstImage;
    private String mapX;
    private String mapY;
    private String contentTypeId;

    // 생성자
    public RelatedAttractionDto(String contentId, String title, String addr1, String addr2, String addr3, String firstImage, String mapX, String mapY, String contentTypeId) {
        this.contentId = contentId;
        this.title = title;
        this.addr1 = addr1;
        this.addr2 = addr2;
        this.addr3 = addr3;
        this.firstImage = firstImage;
        this.mapX = mapX;
        this.mapY = mapY;
        this.contentTypeId = contentTypeId;
    }

    // getters and setters
}
