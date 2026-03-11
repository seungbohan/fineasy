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

    private static final List<OverseasStockDef> OVERSEAS_STOCKS = List.of(

            new OverseasStockDef("AAPL", "Apple", Market.NASDAQ, "Technology"),
            new OverseasStockDef("MSFT", "Microsoft", Market.NASDAQ, "Technology"),
            new OverseasStockDef("NVDA", "NVIDIA", Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("GOOGL", "Alphabet", Market.NASDAQ, "Technology"),
            new OverseasStockDef("AMZN", "Amazon", Market.NASDAQ, "E-Commerce"),
            new OverseasStockDef("META", "Meta Platforms", Market.NASDAQ, "Social Media"),
            new OverseasStockDef("TSLA", "Tesla", Market.NASDAQ, "EV"),
            new OverseasStockDef("AVGO", "Broadcom", Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("COST", "Costco", Market.NASDAQ, "Retail"),
            new OverseasStockDef("NFLX", "Netflix", Market.NASDAQ, "Entertainment"),
            new OverseasStockDef("AMD", "AMD", Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("ADBE", "Adobe", Market.NASDAQ, "Software"),
            new OverseasStockDef("QCOM", "Qualcomm", Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("INTC", "Intel", Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("INTU", "Intuit", Market.NASDAQ, "Software"),
            new OverseasStockDef("CSCO", "Cisco Systems", Market.NASDAQ, "Networking"),
            new OverseasStockDef("TXN", "Texas Instruments", Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("CMCSA", "Comcast", Market.NASDAQ, "Media"),
            new OverseasStockDef("PEP", "PepsiCo", Market.NASDAQ, "Food & Beverage"),
            new OverseasStockDef("TMUS", "T-Mobile US", Market.NASDAQ, "Telecom"),
            new OverseasStockDef("ISRG", "Intuitive Surgical", Market.NASDAQ, "Healthcare"),
            new OverseasStockDef("AMGN", "Amgen", Market.NASDAQ, "Biotech"),
            new OverseasStockDef("HON", "Honeywell", Market.NASDAQ, "Industrial"),
            new OverseasStockDef("AMAT", "Applied Materials", Market.NASDAQ, "Semiconductor Equipment"),
            new OverseasStockDef("BKNG", "Booking Holdings", Market.NASDAQ, "Travel"),
            new OverseasStockDef("LRCX", "Lam Research", Market.NASDAQ, "Semiconductor Equipment"),
            new OverseasStockDef("MU", "Micron Technology", Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("MRVL", "Marvell Technology", Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("PANW", "Palo Alto Networks", Market.NASDAQ, "Cybersecurity"),
            new OverseasStockDef("SNPS", "Synopsys", Market.NASDAQ, "EDA"),
            new OverseasStockDef("KLAC", "KLA Corporation", Market.NASDAQ, "Semiconductor Equipment"),
            new OverseasStockDef("CRWD", "CrowdStrike", Market.NASDAQ, "Cybersecurity"),
            new OverseasStockDef("PYPL", "PayPal", Market.NASDAQ, "Fintech"),
            new OverseasStockDef("MELI", "MercadoLibre", Market.NASDAQ, "E-Commerce"),
            new OverseasStockDef("COIN", "Coinbase", Market.NASDAQ, "Crypto"),
            new OverseasStockDef("PLTR", "Palantir Technologies", Market.NASDAQ, "AI/Software"),

            new OverseasStockDef("BRK.B", "Berkshire Hathaway B", Market.NYSE, "Conglomerate"),
            new OverseasStockDef("LLY", "Eli Lilly", Market.NYSE, "Pharma"),
            new OverseasStockDef("JPM", "JPMorgan Chase", Market.NYSE, "Banking"),
            new OverseasStockDef("V", "Visa", Market.NYSE, "Payments"),
            new OverseasStockDef("UNH", "UnitedHealth Group", Market.NYSE, "Healthcare"),
            new OverseasStockDef("MA", "Mastercard", Market.NYSE, "Payments"),
            new OverseasStockDef("XOM", "Exxon Mobil", Market.NYSE, "Energy"),
            new OverseasStockDef("JNJ", "Johnson & Johnson", Market.NYSE, "Pharma"),
            new OverseasStockDef("PG", "Procter & Gamble", Market.NYSE, "Consumer Goods"),
            new OverseasStockDef("HD", "Home Depot", Market.NYSE, "Retail"),
            new OverseasStockDef("WMT", "Walmart", Market.NYSE, "Retail"),
            new OverseasStockDef("CVX", "Chevron", Market.NYSE, "Energy"),
            new OverseasStockDef("BAC", "Bank of America", Market.NYSE, "Banking"),
            new OverseasStockDef("KO", "Coca-Cola", Market.NYSE, "Food & Beverage"),
            new OverseasStockDef("MRK", "Merck", Market.NYSE, "Pharma"),
            new OverseasStockDef("ABBV", "AbbVie", Market.NYSE, "Pharma"),
            new OverseasStockDef("CRM", "Salesforce", Market.NYSE, "Software"),
            new OverseasStockDef("DIS", "Walt Disney", Market.NYSE, "Entertainment"),
            new OverseasStockDef("NKE", "Nike", Market.NYSE, "Apparel"),
            new OverseasStockDef("BA", "Boeing", Market.NYSE, "Aerospace"),
            new OverseasStockDef("GS", "Goldman Sachs", Market.NYSE, "Banking"),
            new OverseasStockDef("CAT", "Caterpillar", Market.NYSE, "Industrial"),
            new OverseasStockDef("IBM", "IBM", Market.NYSE, "Technology"),
            new OverseasStockDef("GE", "GE Aerospace", Market.NYSE, "Aerospace"),
            new OverseasStockDef("MMM", "3M", Market.NYSE, "Industrial"),
            new OverseasStockDef("UPS", "United Parcel Service", Market.NYSE, "Logistics"),
            new OverseasStockDef("NEE", "NextEra Energy", Market.NYSE, "Utilities"),
            new OverseasStockDef("T", "AT&T", Market.NYSE, "Telecom"),
            new OverseasStockDef("VZ", "Verizon", Market.NYSE, "Telecom"),
            new OverseasStockDef("UBER", "Uber Technologies", Market.NYSE, "Ride-Sharing"),
            new OverseasStockDef("NOW", "ServiceNow", Market.NYSE, "Software"),
            new OverseasStockDef("ARM", "Arm Holdings", Market.NASDAQ, "Semiconductor"),
            new OverseasStockDef("SMCI", "Super Micro Computer", Market.NASDAQ, "Hardware"),
            new OverseasStockDef("TSM", "Taiwan Semiconductor", Market.NYSE, "Semiconductor"),
            new OverseasStockDef("ASML", "ASML Holdings", Market.NASDAQ, "Semiconductor Equipment")
    );

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncOnStartup() {
        log.info("Starting overseas stock sync...");

        try {
            Set<String> existingCodes = stockRepository.findAll().stream()
                    .map(StockEntity::getStockCode)
                    .collect(Collectors.toSet());

            List<StockEntity> newStocks = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (OverseasStockDef def : OVERSEAS_STOCKS) {
                if (!existingCodes.contains(def.symbol())) {
                    newStocks.add(new StockEntity(
                            null, def.symbol(), def.name(),
                            def.market(), def.sector(), true, now
                    ));
                    existingCodes.add(def.symbol());
                }
            }

            if (!newStocks.isEmpty()) {
                stockRepository.saveAll(newStocks);
                log.info("Overseas stock sync completed. {} new stocks added", newStocks.size());
            } else {
                log.info("Overseas stock sync: all {} stocks already exist", OVERSEAS_STOCKS.size());
            }
        } catch (Exception e) {
            log.warn("Overseas stock sync failed (non-fatal): {}", e.getMessage());
        }
    }

    private record OverseasStockDef(String symbol, String name, Market market, String sector) {}
}
