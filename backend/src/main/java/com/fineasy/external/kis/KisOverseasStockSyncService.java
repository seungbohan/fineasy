package com.fineasy.external.kis;

import com.fineasy.entity.Market;
import com.fineasy.entity.StockEntity;
import com.fineasy.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "data-provider.type", havingValue = "kis")
public class KisOverseasStockSyncService {

    private static final Logger log = LoggerFactory.getLogger(KisOverseasStockSyncService.class);

    private final StockRepository stockRepository;

    public KisOverseasStockSyncService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    private static final Map<String, String> KOREAN_NAMES = Map.ofEntries(
            Map.entry("AAPL", "애플"), Map.entry("MSFT", "마이크로소프트"),
            Map.entry("NVDA", "엔비디아"), Map.entry("GOOGL", "알파벳(구글)"),
            Map.entry("AMZN", "아마존"), Map.entry("META", "메타(페이스북)"),
            Map.entry("TSLA", "테슬라"), Map.entry("AVGO", "브로드컴"),
            Map.entry("COST", "코스트코"), Map.entry("NFLX", "넷플릭스"),
            Map.entry("AMD", "AMD"), Map.entry("ADBE", "어도비"),
            Map.entry("QCOM", "퀄컴"), Map.entry("INTC", "인텔"),
            Map.entry("INTU", "인튜이트"), Map.entry("CSCO", "시스코"),
            Map.entry("TXN", "텍사스인스트루먼트"), Map.entry("CMCSA", "컴캐스트"),
            Map.entry("PEP", "펩시코"), Map.entry("TMUS", "T모바일"),
            Map.entry("ISRG", "인튜이티브서지컬"), Map.entry("AMGN", "암젠"),
            Map.entry("HON", "허니웰"), Map.entry("AMAT", "어플라이드머티리얼즈"),
            Map.entry("BKNG", "부킹홀딩스"), Map.entry("LRCX", "램리서치"),
            Map.entry("MU", "마이크론"), Map.entry("MRVL", "마벨테크놀로지"),
            Map.entry("PANW", "팔로알토네트웍스"), Map.entry("SNPS", "시놉시스"),
            Map.entry("KLAC", "KLA"), Map.entry("CRWD", "크라우드스트라이크"),
            Map.entry("PYPL", "페이팔"), Map.entry("MELI", "메르카도리브레"),
            Map.entry("COIN", "코인베이스"), Map.entry("PLTR", "팔란티어"),
            Map.entry("BRK.B", "버크셔해서웨이"), Map.entry("LLY", "일라이릴리"),
            Map.entry("JPM", "JP모건"), Map.entry("V", "비자"),
            Map.entry("UNH", "유나이티드헬스"), Map.entry("MA", "마스터카드"),
            Map.entry("XOM", "엑슨모빌"), Map.entry("JNJ", "존슨앤존슨"),
            Map.entry("PG", "P&G"), Map.entry("HD", "홈디포"),
            Map.entry("WMT", "월마트"), Map.entry("CVX", "셰브론"),
            Map.entry("BAC", "뱅크오브아메리카"), Map.entry("KO", "코카콜라"),
            Map.entry("MRK", "머크"), Map.entry("ABBV", "애브비"),
            Map.entry("CRM", "세일즈포스"), Map.entry("DIS", "디즈니"),
            Map.entry("NKE", "나이키"), Map.entry("BA", "보잉"),
            Map.entry("GS", "골드만삭스"), Map.entry("CAT", "캐터필러"),
            Map.entry("IBM", "IBM"), Map.entry("GE", "GE에어로스페이스"),
            Map.entry("MMM", "3M"), Map.entry("UPS", "UPS"),
            Map.entry("NEE", "넥스트에라에너지"), Map.entry("T", "AT&T"),
            Map.entry("VZ", "버라이즌"), Map.entry("UBER", "우버"),
            Map.entry("NOW", "서비스나우"), Map.entry("ARM", "ARM홀딩스"),
            Map.entry("SMCI", "슈퍼마이크로"), Map.entry("TSM", "TSMC"),
            Map.entry("ASML", "ASML")
    );

    private static String displayName(String symbol, String engName) {
        String kr = KOREAN_NAMES.get(symbol);
        return kr != null ? kr + " " + engName : engName;
    }

    private static final List<OverseasStockDef> OVERSEAS_STOCKS = List.of(

            new OverseasStockDef("AAPL", displayName("AAPL", "Apple"), Market.NASDAQ, "Technology"),
            new OverseasStockDef("MSFT", displayName("MSFT", "Microsoft"), Market.NASDAQ, "Technology"),
            new OverseasStockDef("NVDA", displayName("NVDA", "NVIDIA"), Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("GOOGL", displayName("GOOGL", "Alphabet"), Market.NASDAQ, "Technology"),
            new OverseasStockDef("AMZN", displayName("AMZN", "Amazon"), Market.NASDAQ, "E-Commerce"),
            new OverseasStockDef("META", displayName("META", "Meta Platforms"), Market.NASDAQ, "Social Media"),
            new OverseasStockDef("TSLA", displayName("TSLA", "Tesla"), Market.NASDAQ, "EV"),
            new OverseasStockDef("AVGO", displayName("AVGO", "Broadcom"), Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("COST", displayName("COST", "Costco"), Market.NASDAQ, "Retail"),
            new OverseasStockDef("NFLX", displayName("NFLX", "Netflix"), Market.NASDAQ, "Entertainment"),
            new OverseasStockDef("AMD", displayName("AMD", "AMD"), Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("ADBE", displayName("ADBE", "Adobe"), Market.NASDAQ, "Software"),
            new OverseasStockDef("QCOM", displayName("QCOM", "Qualcomm"), Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("INTC", displayName("INTC", "Intel"), Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("INTU", displayName("INTU", "Intuit"), Market.NASDAQ, "Software"),
            new OverseasStockDef("CSCO", displayName("CSCO", "Cisco Systems"), Market.NASDAQ, "Networking"),
            new OverseasStockDef("TXN", displayName("TXN", "Texas Instruments"), Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("CMCSA", displayName("CMCSA", "Comcast"), Market.NASDAQ, "Media"),
            new OverseasStockDef("PEP", displayName("PEP", "PepsiCo"), Market.NASDAQ, "Food & Beverage"),
            new OverseasStockDef("TMUS", displayName("TMUS", "T-Mobile US"), Market.NASDAQ, "Telecom"),
            new OverseasStockDef("ISRG", displayName("ISRG", "Intuitive Surgical"), Market.NASDAQ, "Healthcare"),
            new OverseasStockDef("AMGN", displayName("AMGN", "Amgen"), Market.NASDAQ, "Biotech"),
            new OverseasStockDef("HON", displayName("HON", "Honeywell"), Market.NASDAQ, "Industrial"),
            new OverseasStockDef("AMAT", displayName("AMAT", "Applied Materials"), Market.NASDAQ, "Semiconductor Equipment"),
            new OverseasStockDef("BKNG", displayName("BKNG", "Booking Holdings"), Market.NASDAQ, "Travel"),
            new OverseasStockDef("LRCX", displayName("LRCX", "Lam Research"), Market.NASDAQ, "Semiconductor Equipment"),
            new OverseasStockDef("MU", displayName("MU", "Micron Technology"), Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("MRVL", displayName("MRVL", "Marvell Technology"), Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("PANW", displayName("PANW", "Palo Alto Networks"), Market.NASDAQ, "Cybersecurity"),
            new OverseasStockDef("SNPS", displayName("SNPS", "Synopsys"), Market.NASDAQ, "EDA"),
            new OverseasStockDef("KLAC", displayName("KLAC", "KLA Corporation"), Market.NASDAQ, "Semiconductor Equipment"),
            new OverseasStockDef("CRWD", displayName("CRWD", "CrowdStrike"), Market.NASDAQ, "Cybersecurity"),
            new OverseasStockDef("PYPL", displayName("PYPL", "PayPal"), Market.NASDAQ, "Fintech"),
            new OverseasStockDef("MELI", displayName("MELI", "MercadoLibre"), Market.NASDAQ, "E-Commerce"),
            new OverseasStockDef("COIN", displayName("COIN", "Coinbase"), Market.NASDAQ, "Crypto"),
            new OverseasStockDef("PLTR", displayName("PLTR", "Palantir Technologies"), Market.NASDAQ, "AI/Software"),

            new OverseasStockDef("BRK.B", displayName("BRK.B", "Berkshire Hathaway B"), Market.NYSE, "Conglomerate"),
            new OverseasStockDef("LLY", displayName("LLY", "Eli Lilly"), Market.NYSE, "Pharma"),
            new OverseasStockDef("JPM", displayName("JPM", "JPMorgan Chase"), Market.NYSE, "Banking"),
            new OverseasStockDef("V", displayName("V", "Visa"), Market.NYSE, "Payments"),
            new OverseasStockDef("UNH", displayName("UNH", "UnitedHealth Group"), Market.NYSE, "Healthcare"),
            new OverseasStockDef("MA", displayName("MA", "Mastercard"), Market.NYSE, "Payments"),
            new OverseasStockDef("XOM", displayName("XOM", "Exxon Mobil"), Market.NYSE, "Energy"),
            new OverseasStockDef("JNJ", displayName("JNJ", "Johnson & Johnson"), Market.NYSE, "Pharma"),
            new OverseasStockDef("PG", displayName("PG", "Procter & Gamble"), Market.NYSE, "Consumer Goods"),
            new OverseasStockDef("HD", displayName("HD", "Home Depot"), Market.NYSE, "Retail"),
            new OverseasStockDef("WMT", displayName("WMT", "Walmart"), Market.NYSE, "Retail"),
            new OverseasStockDef("CVX", displayName("CVX", "Chevron"), Market.NYSE, "Energy"),
            new OverseasStockDef("BAC", displayName("BAC", "Bank of America"), Market.NYSE, "Banking"),
            new OverseasStockDef("KO", displayName("KO", "Coca-Cola"), Market.NYSE, "Food & Beverage"),
            new OverseasStockDef("MRK", displayName("MRK", "Merck"), Market.NYSE, "Pharma"),
            new OverseasStockDef("ABBV", displayName("ABBV", "AbbVie"), Market.NYSE, "Pharma"),
            new OverseasStockDef("CRM", displayName("CRM", "Salesforce"), Market.NYSE, "Software"),
            new OverseasStockDef("DIS", displayName("DIS", "Walt Disney"), Market.NYSE, "Entertainment"),
            new OverseasStockDef("NKE", displayName("NKE", "Nike"), Market.NYSE, "Apparel"),
            new OverseasStockDef("BA", displayName("BA", "Boeing"), Market.NYSE, "Aerospace"),
            new OverseasStockDef("GS", displayName("GS", "Goldman Sachs"), Market.NYSE, "Banking"),
            new OverseasStockDef("CAT", displayName("CAT", "Caterpillar"), Market.NYSE, "Industrial"),
            new OverseasStockDef("IBM", displayName("IBM", "IBM"), Market.NYSE, "Technology"),
            new OverseasStockDef("GE", displayName("GE", "GE Aerospace"), Market.NYSE, "Aerospace"),
            new OverseasStockDef("MMM", displayName("MMM", "3M"), Market.NYSE, "Industrial"),
            new OverseasStockDef("UPS", displayName("UPS", "United Parcel Service"), Market.NYSE, "Logistics"),
            new OverseasStockDef("NEE", displayName("NEE", "NextEra Energy"), Market.NYSE, "Utilities"),
            new OverseasStockDef("T", displayName("T", "AT&T"), Market.NYSE, "Telecom"),
            new OverseasStockDef("VZ", displayName("VZ", "Verizon"), Market.NYSE, "Telecom"),
            new OverseasStockDef("UBER", displayName("UBER", "Uber Technologies"), Market.NYSE, "Ride-Sharing"),
            new OverseasStockDef("NOW", displayName("NOW", "ServiceNow"), Market.NYSE, "Software"),
            new OverseasStockDef("ARM", displayName("ARM", "Arm Holdings"), Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("SMCI", displayName("SMCI", "Super Micro Computer"), Market.NASDAQ, "Hardware"),
            new OverseasStockDef("TSM", displayName("TSM", "Taiwan Semiconductor"), Market.NYSE, "Semiconductor"),
            new OverseasStockDef("ASML", displayName("ASML", "ASML Holdings"), Market.NASDAQ, "Semiconductor Equipment")
    );

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncOnStartup() {
        log.info("Starting overseas stock sync...");

        try {
            Map<String, StockEntity> existingByCode = stockRepository.findAll().stream()
                    .collect(Collectors.toMap(StockEntity::getStockCode, s -> s, (a, b) -> a));

            List<StockEntity> newStocks = new ArrayList<>();
            int updated = 0;
            LocalDateTime now = LocalDateTime.now();

            for (OverseasStockDef def : OVERSEAS_STOCKS) {
                StockEntity existing = existingByCode.get(def.symbol());
                if (existing == null) {
                    newStocks.add(new StockEntity(
                            null, def.symbol(), def.name(),
                            def.market(), def.sector(), true, now
                    ));
                } else if (!existing.getStockName().equals(def.name())) {
                    existing.updateName(def.name());
                    updated++;
                }
            }

            if (!newStocks.isEmpty()) {
                stockRepository.saveAll(newStocks);
            }
            log.info("Overseas stock sync completed. {} new, {} name-updated", newStocks.size(), updated);
        } catch (Exception e) {
            log.warn("Overseas stock sync failed (non-fatal): {}", e.getMessage());
        }
    }

    private record OverseasStockDef(String symbol, String name, Market market, String sector) {}
}
