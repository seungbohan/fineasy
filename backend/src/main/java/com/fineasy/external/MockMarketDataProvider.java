package com.fineasy.external;

import com.fineasy.service.MarketDataProvider;
import com.fineasy.entity.MarketIndex;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "data-provider.type", havingValue = "mock", matchIfMissing = true)
public class MockMarketDataProvider implements MarketDataProvider {

    @Override
    public List<MarketIndex> getMarketIndices() {
        return List.of(
                new MarketIndex("KOSPI", "코스피", 2650.33, 15.22, 0.58,
                        List.of(2620.0, 2630.0, 2640.0, 2645.0, 2650.33), Instant.now()),
                new MarketIndex("KOSDAQ", "코스닥", 870.15, -3.42, -0.39,
                        List.of(880.0, 875.0, 872.0, 873.0, 870.15), Instant.now()),
                new MarketIndex("NASDAQ", "나스닥", 16274.94, 85.33, 0.53,
                        List.of(16100.0, 16150.0, 16200.0, 16250.0, 16274.94), Instant.now()),
                new MarketIndex("SP500", "S&P 500", 5088.80, 25.60, 0.51,
                        List.of(5040.0, 5055.0, 5070.0, 5080.0, 5088.80), Instant.now()),
                new MarketIndex("DJI", "다우존스", 39131.53, 134.21, 0.34,
                        List.of(38900.0, 38950.0, 39000.0, 39050.0, 39131.53), Instant.now()),
                new MarketIndex("SOX", "필라델피아반도체", 4918.72, 62.38, 1.28,
                        List.of(4800.0, 4830.0, 4870.0, 4900.0, 4918.72), Instant.now())
        );
    }

    @Override
    public String getMarketSummary() {
        return "오늘 국내 증시는 반도체 관련주 강세에 힘입어 코스피가 소폭 상승세를 보이고 있습니다. " +
                "미국 시장에서 나스닥과 S&P 500이 동반 상승하며 긍정적인 영향을 미쳤습니다. " +
                "다만 코스닥은 소형주 차익실현 매물로 인해 약보합세를 기록 중입니다. " +
                "주요 거시경제 지표인 미국 소비자물가지수(CPI) 발표를 앞두고 관망세가 이어지고 있어, " +
                "이번 주 후반 변동성 확대에 대비할 필요가 있습니다.";
    }
}
