package com.fineasy.dto.response;

public record SectorComparisonResponse(
        String stockCode,
        String stockName,
        String sector,
        Double currentPer,
        Double currentPbr,
        Double sectorAvgPer,
        Double sectorAvgPbr,
        String perEvaluation,
        String pbrEvaluation,
        int peerCount
) {
}
