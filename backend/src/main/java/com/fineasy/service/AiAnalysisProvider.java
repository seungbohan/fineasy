package com.fineasy.service;

import com.fineasy.dto.response.AnalysisReportResponse;
import com.fineasy.dto.response.PredictionResponse;

public interface AiAnalysisProvider {

    AnalysisReportResponse generateReport(String stockCode);

    PredictionResponse generatePrediction(String stockCode, String period);
}
