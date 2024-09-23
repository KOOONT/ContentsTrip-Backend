package com.Kooont.HeritageLoad.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HeritageResponseDto {
    private int totalCnt; // 전체 항목 수
    private List<HeritageItemDto> heritageItems;
}
