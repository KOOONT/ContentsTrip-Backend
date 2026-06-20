package com.Kooont.HeritageLoad.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GungListItemDto {

    private String addr1;
    private String addr2;
    private String areacode;
    private String contentid;
    private String title;
    private String mapx;
    private String mapy;
    private String firstimage;
    private String firstimage2;
    private String modifiedtime;
}
