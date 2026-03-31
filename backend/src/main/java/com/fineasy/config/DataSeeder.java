package com.fineasy.config;

import com.fineasy.entity.*;
import com.fineasy.repository.*;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.fineasy.external.ecos.EcosDataSyncService;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final StockRepository stockRepo;
    private final StockPriceRepository priceRepo;
    private final TermCategoryRepository categoryRepo;
    private final FinancialTermRepository termRepo;
    private final MacroIndicatorRepository macroRepo;
    private final NewsArticleRepository newsRepo;
    private final LearnArticleRepository articleRepo;
    private final EtfPresetRepository etfPresetRepo;
    private final SectorContentRepository sectorContentRepo;
    private final String ecosApiKey;
    @Autowired(required = false)
    private EcosDataSyncService ecosDataSyncService;

    public DataSeeder(StockRepository stockRepo,
                      StockPriceRepository priceRepo,
                      TermCategoryRepository categoryRepo,
                      FinancialTermRepository termRepo,
                      MacroIndicatorRepository macroRepo,
                      NewsArticleRepository newsRepo,
                      LearnArticleRepository articleRepo,
                      EtfPresetRepository etfPresetRepo,
                      SectorContentRepository sectorContentRepo,
                      @Value("${ecos.api.key:}") String ecosApiKey) {
        this.stockRepo = stockRepo;
        this.priceRepo = priceRepo;
        this.categoryRepo = categoryRepo;
        this.termRepo = termRepo;
        this.macroRepo = macroRepo;
        this.newsRepo = newsRepo;
        this.articleRepo = articleRepo;
        this.etfPresetRepo = etfPresetRepo;
        this.sectorContentRepo = sectorContentRepo;
        this.ecosApiKey = ecosApiKey;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting data seeding...");
        seedStocks();
        log.info("KIS mode active - skipping mock stock price seeding (KisDailyPriceSyncService will backfill)");
        seedTermCategories();
        seedFinancialTerms();
        cleanAndSeedMacroIndicators();
        seedNews();
        seedLearnArticles();
        seedEtfPresets();
        seedSectorContents();
        log.info("Data seeding completed.");
    }

    private void seedStocks() {
        if (stockRepo.count() > 0) return;

        LocalDateTime now = LocalDateTime.now();

        List<StockEntity> featuredKr = List.of(
                new StockEntity(null, "005930", "삼성전자", Market.KRX, "반도체", true, now),
                new StockEntity(null, "000660", "SK하이닉스", Market.KRX, "반도체", true, now),
                new StockEntity(null, "373220", "LG에너지솔루션", Market.KRX, "배터리", true, now),
                new StockEntity(null, "207940", "삼성바이오로직스", Market.KRX, "바이오", true, now),
                new StockEntity(null, "005380", "현대자동차", Market.KRX, "자동차", true, now),
                new StockEntity(null, "000270", "기아", Market.KRX, "자동차", true, now),
                new StockEntity(null, "068270", "셀트리온", Market.KRX, "바이오", true, now),
                new StockEntity(null, "035420", "NAVER", Market.KRX, "인터넷", true, now),
                new StockEntity(null, "035720", "카카오", Market.KRX, "인터넷", true, now),
                new StockEntity(null, "006400", "삼성SDI", Market.KRX, "배터리", true, now),
                new StockEntity(null, "051910", "LG화학", Market.KRX, "화학", true, now),
                new StockEntity(null, "105560", "KB금융", Market.KRX, "금융", true, now),
                new StockEntity(null, "055550", "신한지주", Market.KRX, "금융", true, now),
                new StockEntity(null, "012450", "한화에어로스페이스", Market.KRX, "방산", true, now),
                new StockEntity(null, "005490", "POSCO홀딩스", Market.KRX, "철강", true, now),
                new StockEntity(null, "028260", "삼성물산", Market.KRX, "건설", true, now),
                new StockEntity(null, "017670", "SK텔레콤", Market.KRX, "통신", true, now),
                new StockEntity(null, "066570", "LG전자", Market.KRX, "전자", true, now),
                new StockEntity(null, "259960", "크래프톤", Market.KRX, "게임", true, now),
                new StockEntity(null, "012330", "현대모비스", Market.KRX, "자동차부품", true, now),

                new StockEntity(null, "247540", "에코프로비엠", Market.KOSDAQ, "소재", true, now),
                new StockEntity(null, "196170", "알테오젠", Market.KOSDAQ, "바이오", true, now),
                new StockEntity(null, "028300", "HLB", Market.KOSDAQ, "바이오", true, now),
                new StockEntity(null, "352820", "하이브", Market.KOSDAQ, "엔터", true, now),
                new StockEntity(null, "035900", "JYP Ent.", Market.KOSDAQ, "엔터", true, now)
        );
        stockRepo.saveAll(featuredKr);

        List<StockEntity> usStocks = List.of(

                new StockEntity(null, "AAPL", "Apple", Market.NASDAQ, "Technology", true, now),
                new StockEntity(null, "MSFT", "Microsoft", Market.NASDAQ, "Technology", true, now),
                new StockEntity(null, "NVDA", "NVIDIA", Market.NASDAQ, "Semiconductor", true, now),
                new StockEntity(null, "GOOGL", "Alphabet", Market.NASDAQ, "Technology", true, now),
                new StockEntity(null, "AMZN", "Amazon", Market.NASDAQ, "E-Commerce", true, now),
                new StockEntity(null, "TSLA", "Tesla", Market.NASDAQ, "EV", true, now),
                new StockEntity(null, "META", "Meta Platforms", Market.NASDAQ, "Social Media", true, now),
                new StockEntity(null, "AVGO", "Broadcom", Market.NASDAQ, "Semiconductor", true, now),
                new StockEntity(null, "NFLX", "Netflix", Market.NASDAQ, "Entertainment", true, now),
                new StockEntity(null, "AMD", "AMD", Market.NASDAQ, "Semiconductor", true, now),
                new StockEntity(null, "ADBE", "Adobe", Market.NASDAQ, "Software", true, now),
                new StockEntity(null, "QCOM", "Qualcomm", Market.NASDAQ, "Semiconductor", true, now),
                new StockEntity(null, "INTC", "Intel", Market.NASDAQ, "Semiconductor", true, now),
                new StockEntity(null, "COST", "Costco", Market.NASDAQ, "Retail", true, now),
                new StockEntity(null, "MU", "Micron Technology", Market.NASDAQ, "Semiconductor", true, now),
                new StockEntity(null, "COIN", "Coinbase", Market.NASDAQ, "Crypto", true, now),
                new StockEntity(null, "PLTR", "Palantir Technologies", Market.NASDAQ, "AI/Software", true, now),
                new StockEntity(null, "ARM", "Arm Holdings", Market.NASDAQ, "Semiconductor", true, now),
                new StockEntity(null, "SMCI", "Super Micro Computer", Market.NASDAQ, "Hardware", true, now),

                new StockEntity(null, "JPM", "JPMorgan Chase", Market.NYSE, "Banking", true, now),
                new StockEntity(null, "V", "Visa", Market.NYSE, "Payments", true, now),
                new StockEntity(null, "UNH", "UnitedHealth Group", Market.NYSE, "Healthcare", true, now),
                new StockEntity(null, "LLY", "Eli Lilly", Market.NYSE, "Pharma", true, now),
                new StockEntity(null, "XOM", "Exxon Mobil", Market.NYSE, "Energy", true, now),
                new StockEntity(null, "WMT", "Walmart", Market.NYSE, "Retail", true, now),
                new StockEntity(null, "BA", "Boeing", Market.NYSE, "Aerospace", true, now),
                new StockEntity(null, "DIS", "Walt Disney", Market.NYSE, "Entertainment", true, now),
                new StockEntity(null, "TSM", "Taiwan Semiconductor", Market.NYSE, "Semiconductor", true, now),
                new StockEntity(null, "CRM", "Salesforce", Market.NYSE, "Software", true, now),
                new StockEntity(null, "UBER", "Uber Technologies", Market.NYSE, "Ride-Sharing", true, now)
        );
        stockRepo.saveAll(usStocks);

        log.info("Seeded {} featured KR + {} US stocks (remaining KR stocks synced from KIS master)",
                featuredKr.size(), usStocks.size());
    }

    private void seedTermCategories() {
        if (categoryRepo.count() > 0) return;

        List<TermCategoryEntity> categories = List.of(
                new TermCategoryEntity(null, "주식 기초", 1),
                new TermCategoryEntity(null, "재무 지표", 2),
                new TermCategoryEntity(null, "기술적 분석", 3),
                new TermCategoryEntity(null, "채권/금리", 4),
                new TermCategoryEntity(null, "거시경제", 5),
                new TermCategoryEntity(null, "파생상품", 6),
                new TermCategoryEntity(null, "가치분석", 7)
        );
        categoryRepo.saveAll(categories);
        log.info("Seeded {} term categories", categories.size());
    }

    private void seedFinancialTerms() {
        if (termRepo.count() > 0) return;

        List<TermCategoryEntity> cats = categoryRepo.findAllByOrderByDisplayOrderAsc();
        TermCategoryEntity basics = cats.get(0);
        TermCategoryEntity financial = cats.get(1);
        TermCategoryEntity technical = cats.get(2);
        TermCategoryEntity bonds = cats.get(3);
        TermCategoryEntity macro = cats.get(4);
        TermCategoryEntity derivatives = cats.get(5);
        TermCategoryEntity valueAnalysis = cats.get(6);

        LocalDateTime now = LocalDateTime.now();

        List<FinancialTermEntity> terms = List.of(

                term("주식", "Stock", basics, Difficulty.BEGINNER,
                        "회사의 소유권을 나누어 놓은 것입니다. 주식을 사면 그 회사의 작은 주인이 됩니다.",
                        "주식(Stock)은 기업이 자금을 조달하기 위해 발행하는 유가증권입니다. 주식을 보유하면 해당 기업의 주주가 되며, 의결권, 배당 수령권 등의 권리를 갖게 됩니다.",
                        "삼성전자 주식 1주를 72,000원에 샀다면, 삼성전자의 아주 작은 부분을 소유하게 된 것입니다.", now),
                term("시가총액", "Market Capitalization", basics, Difficulty.BEGINNER,
                        "회사의 전체 가치를 나타내는 숫자입니다. 주가에 총 주식 수를 곱한 값입니다.",
                        "시가총액은 현재 주가와 발행주식수를 곱한 값으로, 시장에서 평가하는 기업의 전체 가치를 나타냅니다.",
                        "삼성전자의 시가총액이 430조원이라면, 시장이 삼성전자 전체를 430조원의 가치로 보고 있다는 뜻입니다.", now),
                term("거래량", "Trading Volume", basics, Difficulty.BEGINNER,
                        "특정 기간 동안 거래된 주식의 수입니다. 거래량이 많으면 사람들의 관심이 높다는 뜻입니다.",
                        "거래량은 일정 기간 동안 매수와 매도가 이루어진 주식의 총 수를 말합니다. 거래량이 급증하면 가격 변동의 신호가 될 수 있습니다.",
                        "오늘 삼성전자 거래량이 1,500만 주라면, 하루 동안 1,500만 주가 사고팔렸다는 의미입니다.", now),
                term("배당", "Dividend", basics, Difficulty.BEGINNER,
                        "회사가 벌어들인 이익의 일부를 주주들에게 나눠주는 것입니다.",
                        "배당(Dividend)은 기업이 영업활동으로 벌어들인 이익 중 일부를 주주에게 분배하는 것입니다. 보통 연 1회 또는 분기별로 지급됩니다.",
                        "배당수익률이 3%인 주식을 100만원어치 갖고 있다면, 1년에 약 3만원의 배당금을 받을 수 있습니다.", now),
                term("공매도", "Short Selling", basics, Difficulty.INTERMEDIATE,
                        "주식을 빌려서 먼저 팔고, 나중에 싸게 사서 갚는 투자 방법입니다.",
                        "공매도는 주가 하락이 예상될 때 주식을 빌려서 현재 가격에 매도하고, 나중에 낮은 가격에 매수하여 차익을 얻는 투자 전략입니다.",
                        "10만원인 주식을 빌려서 팔고, 8만원으로 떨어졌을 때 사서 갚으면 2만원의 이익이 생깁니다.", now),
                term("상한가", "Upper Limit Price", basics, Difficulty.BEGINNER,
                        "하루에 오를 수 있는 최대 가격입니다. 한국 주식은 전일 종가 대비 30%가 한도입니다.",
                        "상한가는 주식시장에서 하루 동안 주가가 오를 수 있는 최대 가격을 말합니다. 한국 증시에서는 전일 종가 대비 30%까지 상승할 수 있습니다.",
                        "전일 종가가 10,000원인 주식의 상한가는 13,000원(+30%)입니다.", now),
                term("가치투자", "Value Investing", basics, Difficulty.INTERMEDIATE,
                        "회사의 실제 가치보다 주가가 낮을 때 사서 장기간 보유하는 투자 전략입니다.",
                        "가치투자는 기업의 내재가치를 분석하여, 시장에서 저평가된 주식을 매수하고 장기 보유하는 투자 전략입니다. 워런 버핏이 대표적인 가치투자자입니다.",
                        "PER이 업종 평균보다 낮고 재무 상태가 좋은 회사를 찾아 투자하는 것이 가치투자의 기본입니다.", now),

                term("PER", "Price-Earnings Ratio", financial, Difficulty.BEGINNER,
                        "주가가 주당순이익의 몇 배인지 나타내는 지표입니다. 낮을수록 저평가, 높을수록 고평가로 볼 수 있습니다.",
                        "PER(주가수익비율)은 주가를 주당순이익(EPS)으로 나눈 값입니다. PER이 낮으면 이익 대비 주가가 저평가되어 있다고 해석할 수 있습니다.",
                        "삼성전자의 PER이 12배라면, 현재 주가가 연간 순이익의 12배 수준이라는 뜻입니다.", now),
                term("PBR", "Price-to-Book Ratio", financial, Difficulty.BEGINNER,
                        "주가가 회사 순자산의 몇 배인지 나타내는 지표입니다. 1배 미만이면 순자산 가치보다 주가가 낮다는 뜻입니다.",
                        "PBR(주가순자산비율)은 주가를 주당순자산(BPS)으로 나눈 값입니다. PBR 1배 미만은 회사를 청산해도 주주에게 돌아갈 돈이 더 많다는 의미입니다.",
                        "PBR이 0.8이면 회사 자산가치 대비 주가가 20% 할인된 셈입니다.", now),
                term("EPS", "Earnings Per Share", financial, Difficulty.BEGINNER,
                        "주식 1주당 벌어들이는 순이익입니다. EPS가 높을수록 수익성이 좋은 회사입니다.",
                        "EPS(주당순이익)은 당기순이익을 발행주식수로 나눈 값으로, 주식 1주가 1년간 벌어들이는 이익을 나타냅니다.",
                        "EPS가 5,000원이면 주식 1주당 5,000원의 순이익을 창출했다는 의미입니다.", now),
                term("ROE", "Return on Equity", financial, Difficulty.INTERMEDIATE,
                        "자기자본 대비 얼마나 이익을 냈는지 나타내는 지표입니다. 높을수록 효율적으로 돈을 벌고 있습니다.",
                        "ROE(자기자본이익률)은 당기순이익을 자기자본으로 나눈 비율입니다. 경영진이 주주의 돈으로 얼마나 효율적으로 이익을 창출하는지 평가하는 지표입니다.",
                        "ROE가 15%라면, 자기자본 100원으로 15원의 이익을 만들어냈다는 뜻입니다.", now),
                term("배당수익률", "Dividend Yield", financial, Difficulty.BEGINNER,
                        "주가 대비 1년간 받는 배당금의 비율입니다. 은행 이자처럼 주식을 갖고 있는 것만으로 받는 수익입니다.",
                        "배당수익률은 1주당 연간 배당금을 현재 주가로 나눈 비율입니다. 배당수익률이 높은 주식은 안정적인 수익을 원하는 투자자에게 인기입니다.",
                        "주가 50,000원인 주식의 연간 배당금이 2,000원이면 배당수익률은 4%입니다.", now),
                term("부채비율", "Debt-to-Equity Ratio", financial, Difficulty.INTERMEDIATE,
                        "자기자본 대비 부채가 얼마나 많은지 나타냅니다. 100% 미만이면 빚보다 자산이 더 많습니다.",
                        "부채비율은 총부채를 자기자본으로 나눈 비율입니다. 일반적으로 200% 이하가 안정적이며, 업종에 따라 차이가 있습니다.",
                        "부채비율이 150%라면, 자기자본 100원당 부채가 150원이라는 뜻입니다.", now),

                term("이동평균선", "Moving Average", technical, Difficulty.INTERMEDIATE,
                        "일정 기간 동안의 평균 주가를 선으로 연결한 것입니다. 주가의 추세를 파악하는 데 사용됩니다.",
                        "이동평균선(MA)은 일정 기간의 종가 평균을 날짜별로 연결한 선입니다. 5일, 20일, 60일, 120일 이동평균선이 주로 사용되며, 단기/중기/장기 추세를 파악합니다.",
                        "20일 이동평균선이 상승 중이면, 최근 20일간 주가가 전반적으로 올랐다는 뜻입니다.", now),
                term("RSI", "Relative Strength Index", technical, Difficulty.INTERMEDIATE,
                        "주가의 과매수/과매도 여부를 0~100 사이 숫자로 나타내는 지표입니다. 70 이상이면 과매수, 30 이하면 과매도입니다.",
                        "RSI(상대강도지수)는 일정 기간 동안의 상승폭과 하락폭을 비교하여 주가의 강도를 측정하는 기술적 지표입니다. 보통 14일을 기준으로 계산합니다.",
                        "RSI가 25면 최근 주가가 많이 떨어져서 반등 가능성이 있다는 신호로 해석할 수 있습니다.", now),
                term("MACD", "Moving Average Convergence Divergence", technical, Difficulty.ADVANCED,
                        "두 이동평균선의 차이를 이용하여 매수/매도 신호를 판단하는 지표입니다.",
                        "MACD는 12일 지수이동평균(EMA)에서 26일 EMA를 뺀 값입니다. MACD가 시그널선(9일 EMA)을 상향 돌파하면 매수 신호, 하향 돌파하면 매도 신호로 해석합니다.",
                        "MACD가 시그널선 위로 올라가는 것을 '골든크로스'라 하며, 상승 전환 신호로 봅니다.", now),
                term("볼린저밴드", "Bollinger Bands", technical, Difficulty.ADVANCED,
                        "주가의 변동 범위를 밴드로 표시한 것입니다. 밴드 아래에 있으면 저점, 위에 있으면 고점 가능성이 있습니다.",
                        "볼린저밴드는 20일 이동평균선을 중심으로 상하 2 표준편차만큼의 밴드를 그린 것입니다. 주가가 밴드 상단에 접근하면 과매수, 하단에 접근하면 과매도로 해석합니다.",
                        "주가가 볼린저밴드 하단을 터치하면 반등 가능성을, 상단을 터치하면 조정 가능성을 시사합니다.", now),

                term("기준금리", "Base Interest Rate", bonds, Difficulty.BEGINNER,
                        "한국은행이 결정하는 금리로, 모든 대출·예금 금리의 기준이 됩니다.",
                        "기준금리는 중앙은행이 시중 은행에 돈을 빌려줄 때 적용하는 금리입니다. 기준금리가 오르면 대출 이자가 높아지고, 내리면 대출 이자가 낮아집니다.",
                        "기준금리가 3.5%에서 3.25%로 인하되면, 시중 대출금리도 따라서 내려가는 경향이 있습니다.", now),
                term("국채", "Government Bond", bonds, Difficulty.INTERMEDIATE,
                        "정부가 돈을 빌리기 위해 발행하는 채권입니다. 가장 안전한 투자 수단 중 하나입니다.",
                        "국채는 정부가 재정 적자를 메우거나 공공사업 자금을 마련하기 위해 발행하는 채권입니다. 정부 신용으로 발행되어 가장 안전한 자산으로 평가됩니다.",
                        "미국 10년 국채 금리가 4.5%라면, 미국 정부에 10년간 돈을 빌려주면 연 4.5%의 이자를 받는다는 뜻입니다.", now),
                term("채권", "Bond", bonds, Difficulty.BEGINNER,
                        "돈을 빌려준다는 증서입니다. 정해진 기간 후에 원금과 이자를 돌려받습니다.",
                        "채권은 발행자(정부, 기업 등)가 투자자에게 돈을 빌리면서 발행하는 유가증권입니다. 만기에 원금을 상환하고, 그동안 정기적으로 이자(쿠폰)를 지급합니다.",
                        "연 이자 5%, 만기 3년짜리 채권 100만원을 사면, 매년 5만원의 이자와 3년 후 원금 100만원을 받습니다.", now),

                term("GDP", "Gross Domestic Product", macro, Difficulty.BEGINNER,
                        "한 나라에서 일정 기간 동안 생산된 모든 상품과 서비스의 총 가치입니다. 경제 규모를 나타냅니다.",
                        "GDP(국내총생산)는 일정 기간 동안 한 국가 내에서 생산된 모든 최종 재화와 서비스의 시장 가치 합계입니다. 경제 성장률은 전기 대비 GDP 변화율로 측정됩니다.",
                        "한국 GDP가 2,100조원이면, 1년간 한국에서 만들어진 모든 것의 가치가 2,100조원이라는 뜻입니다.", now),
                term("CPI", "Consumer Price Index", macro, Difficulty.INTERMEDIATE,
                        "소비자가 구입하는 물건과 서비스의 가격 변동을 측정한 지표입니다. 물가 상승률을 나타냅니다.",
                        "CPI(소비자물가지수)는 가계가 소비하는 재화와 서비스의 가격 변동을 측정하는 지수입니다. 인플레이션의 주요 지표로 사용됩니다.",
                        "CPI가 전년 대비 3% 상승했다면, 1년 전에 100만원이던 물건이 지금은 103만원이 되었다는 의미입니다.", now),
                term("인플레이션", "Inflation", macro, Difficulty.BEGINNER,
                        "물건의 가격이 전반적으로 오르는 현상입니다. 같은 돈으로 살 수 있는 것이 줄어듭니다.",
                        "인플레이션은 경제 전반적으로 물가 수준이 지속적으로 상승하는 현상입니다. 적절한 인플레이션(연 2% 내외)은 경제 성장에 긍정적이지만, 과도한 인플레이션은 경제에 해롭습니다.",
                        "물가가 매년 5%씩 오르면, 올해 100만원으로 살 수 있는 물건을 내년에는 105만원을 내야 합니다.", now),

                term("내재가치", "Intrinsic Value", valueAnalysis, Difficulty.BEGINNER,
                        "기업이 실제로 가지고 있는 가치입니다. 시장 가격과 다를 수 있으며, 내재가치보다 주가가 낮으면 저평가된 것입니다.",
                        "내재가치란 기업의 자산, 수익력, 성장 잠재력 등을 종합적으로 분석하여 산출한 기업의 본질적 가치입니다. 워런 버핏은 내재가치를 '기업의 남은 수명 동안 벌어들일 현금의 현재 가치'라고 정의했습니다.",
                        "기업의 내재가치가 주당 80,000원인데 현재 주가가 60,000원이면, 25% 저평가되어 있다고 볼 수 있습니다.", now),
                term("안전마진", "Margin of Safety", valueAnalysis, Difficulty.INTERMEDIATE,
                        "내재가치와 실제 매수 가격의 차이입니다. 안전마진이 클수록 투자 위험이 줄어듭니다.",
                        "안전마진(Margin of Safety)은 벤저민 그레이엄이 제시한 개념으로, 기업의 내재가치 대비 충분히 낮은 가격에 매수함으로써 분석 오류나 예기치 못한 상황에 대한 안전장치를 확보하는 것입니다.",
                        "내재가치가 10만원인 주식을 7만원에 사면 안전마진이 30%입니다. 분석이 다소 틀려도 손실 가능성이 줄어듭니다.", now),
                term("DCF", "Discounted Cash Flow", valueAnalysis, Difficulty.ADVANCED,
                        "미래에 벌어들일 현금을 현재 가치로 환산하여 기업의 가치를 평가하는 방법입니다.",
                        "DCF(현금흐름할인법)는 기업이 미래에 창출할 것으로 예상되는 자유현금흐름(FCF)을 적정 할인율로 현재 가치로 환산하여 기업 가치를 산출하는 방법입니다. 가장 이론적으로 정교한 가치평가 기법입니다.",
                        "기업이 향후 5년간 매년 100억원의 현금을 벌고, 할인율이 10%라면 각 해의 현금을 현재 가치로 환산해 합산합니다.", now),
                term("영업이익률", "Operating Margin", valueAnalysis, Difficulty.BEGINNER,
                        "매출에서 영업이익이 차지하는 비율입니다. 높을수록 본업에서 효율적으로 돈을 벌고 있다는 뜻입니다.",
                        "영업이익률은 영업이익을 매출액으로 나눈 비율입니다. 기업이 핵심 사업에서 얼마나 수익성 있게 운영되고 있는지를 나타내는 핵심 지표이며, 같은 업종 내에서 비교하는 것이 의미 있습니다.",
                        "매출 1,000억원에 영업이익이 150억원이면 영업이익률은 15%입니다. 같은 업종 평균이 10%라면 수익성이 우수한 기업입니다.", now),
                term("매출성장률", "Revenue Growth Rate", valueAnalysis, Difficulty.BEGINNER,
                        "전년 대비 매출이 얼마나 늘었는지 나타내는 비율입니다. 기업의 성장성을 판단하는 핵심 지표입니다.",
                        "매출성장률은 (당기 매출 - 전기 매출) / 전기 매출 x 100으로 계산합니다. 지속적으로 높은 매출성장률을 유지하는 기업은 성장주로 분류되며, 미래 가치가 현재 가격에 선반영되어 PER이 높은 경우가 많습니다.",
                        "작년 매출이 500억원이고 올해 600억원이면 매출성장률은 20%입니다. 3년 연속 20% 이상이면 고성장 기업입니다.", now),
                term("잉여현금흐름", "Free Cash Flow", valueAnalysis, Difficulty.INTERMEDIATE,
                        "영업활동으로 벌어들인 현금에서 설비 투자비를 뺀 금액입니다. 실제로 주주에게 돌아갈 수 있는 돈입니다.",
                        "잉여현금흐름(FCF)은 영업현금흐름에서 자본적 지출(CAPEX)을 차감한 값입니다. 배당이나 자사주 매입, 부채 상환에 사용할 수 있는 진정한 의미의 여유 자금이며, 기업의 재무 건전성과 주주 환원 능력을 평가하는 핵심 지표입니다.",
                        "영업현금흐름이 200억원이고 설비 투자에 50억원을 쓰면 FCF는 150억원입니다. 이 돈으로 배당을 주거나 자사주를 살 수 있습니다.", now)
        );

        termRepo.saveAll(terms);
        log.info("Seeded {} financial terms", terms.size());
    }

    private FinancialTermEntity term(String name, String nameEn, TermCategoryEntity category,
                                     Difficulty difficulty, String simple, String detailed,
                                     String example, LocalDateTime now) {
        return new FinancialTermEntity(null, name, nameEn, category, difficulty,
                simple, detailed, example, now, now);
    }

    private void cleanAndSeedMacroIndicators() {
        if (ecosDataSyncService == null) {

            seedFallbackIndicators(List.of(
                    new Object[]{"KR_BASE_RATE", "한국 기준금리", 2.50, "%", "한국은행", 0.0, true},
                    new Object[]{"USD_KRW", "원/달러 환율", 1442.0, "원/달러", "한국은행", 5.0, false},
                    new Object[]{"KR_CPI", "한국 소비자물가지수", 2.0, "%", "통계청", 0.03, false},
                    new Object[]{"WTI", "WTI 유가", 66.88, "달러/배럴", "NYMEX", 1.0, false},
                    new Object[]{"GOLD", "금 가격", 5162.0, "달러/온스", "COMEX", 30.0, false}
            ));
        }

        seedFallbackIndicators(List.of(
                new Object[]{"US_FED_RATE", "미국 기준금리", 3.625, "%", "Federal Reserve", 0.0, true},
                new Object[]{"US_CPI", "미국 소비자물가지수", 2.4, "%", "Bureau of Labor Statistics", 0.03, false},
                new Object[]{"KR_GDP", "한국 GDP 성장률", 1.0, "%", "한국은행", 0.02, false}
        ));
    }

    private void seedFallbackIndicators(List<Object[]> indicatorDefs) {
        Random random = new Random(123);
        LocalDate today = LocalDate.now();
        int historyDays = 30;
        List<MacroIndicatorEntity> batch = new ArrayList<>();

        for (Object[] def : indicatorDefs) {
            String code = (String) def[0];
            String name = (String) def[1];
            double baseValue = (double) def[2];
            String unit = (String) def[3];
            String source = (String) def[4];
            double volatility = (double) def[5];
            boolean isFixed = (boolean) def[6];

            if (macroRepo.findLatestByCode(code).isPresent()) {
                continue;
            }

            double currentValue = baseValue;

            for (int dayOffset = historyDays; dayOffset >= 0; dayOffset--) {
                LocalDate date = today.minusDays(dayOffset);

                if (!isFixed && dayOffset < historyDays) {
                    double change = (random.nextDouble() - 0.5) * 2.0 * volatility;
                    currentValue += change;
                    double lowerBound = baseValue * 0.95;
                    double upperBound = baseValue * 1.05;
                    currentValue = Math.max(lowerBound, Math.min(upperBound, currentValue));
                }

                double roundedValue = Math.round(currentValue * 100.0) / 100.0;
                batch.add(new MacroIndicatorEntity(
                        null, code, name, roundedValue, unit, date, source));
            }

            log.info("Prepared {} fallback data points for {} ({})", historyDays + 1, code, name);
        }

        if (!batch.isEmpty()) {
            macroRepo.saveAll(batch);
            log.info("Saved {} fallback macro indicator records in batch", batch.size());
        }
    }

    private void seedNews() {
        log.info("Skipping news seeding - real news collected via RSS feeds");
    }

    private void seedLearnArticles() {
        if (articleRepo.count() > 0) return;

        List<LearnArticleEntity> articles = List.of(
                new LearnArticleEntity(null, "주식이란 무엇인가",
                        "## 주식의 기본 개념\n\n주식은 기업의 소유권을 나누어 놓은 것입니다. " +
                                "주식을 매수한다는 것은 그 기업의 작은 주인이 된다는 뜻입니다.\n\n" +
                                "### 주식의 종류\n\n- **보통주**: 의결권이 있는 일반적인 주식\n" +
                                "- **우선주**: 배당금 우선 수령 권리가 있는 주식\n\n" +
                                "### 주식으로 돈을 버는 방법\n\n1. **시세차익**: 싸게 사서 비싸게 팔기\n" +
                                "2. **배당금**: 기업이 이익을 나눠주는 것\n\n" +
                                "주식 투자를 시작하기 전에 기본적인 개념을 이해하는 것이 중요합니다.",
                        ArticleCategory.BASICS, Difficulty.BEGINNER, 5, 1, true),
                new LearnArticleEntity(null, "증권 계좌 개설하는 방법",
                        "## 증권 계좌란?\n\n증권 계좌는 주식을 사고팔기 위해 필요한 전용 계좌입니다.\n\n" +
                                "### 개설 절차\n\n1. 증권사 앱 다운로드\n2. 본인인증\n3. 계좌 개설 신청\n" +
                                "4. 입금 후 거래 시작\n\n### 추천 증권사\n\n초보자에게는 수수료가 낮고 " +
                                "앱 사용이 편리한 증권사를 추천합니다.",
                        ArticleCategory.BASICS, Difficulty.BEGINNER, 3, 2, true),
                new LearnArticleEntity(null, "분산투자란 무엇인가",
                        "## 분산투자의 기본 원칙\n\n'달걀을 한 바구니에 담지 마라'라는 격언처럼, " +
                                "여러 종목에 나눠 투자하는 것을 분산투자라 합니다.\n\n" +
                                "### 분산투자의 종류\n\n- **종목 분산**: 여러 회사 주식에 투자\n" +
                                "- **업종 분산**: 다양한 산업에 투자\n" +
                                "- **자산 분산**: 주식, 채권, 부동산 등에 나눠 투자\n\n" +
                                "### 왜 분산투자가 중요한가?\n\n한 종목이 크게 떨어져도 다른 종목이 " +
                                "이를 보완해줄 수 있습니다.",
                        ArticleCategory.BASICS, Difficulty.BEGINNER, 7, 3, true),
                new LearnArticleEntity(null, "장기투자 vs 단기투자",
                        "## 투자 기간에 따른 전략\n\n### 장기투자\n\n- 1년 이상 보유\n" +
                                "- 기업의 성장에 베팅\n- 복리 효과 기대\n- 스트레스 적음\n\n" +
                                "### 단기투자\n\n- 수일~수주 보유\n- 기술적 분석 활용\n" +
                                "- 높은 수익 가능성, 높은 위험\n- 시장 모니터링 필요\n\n" +
                                "초보자에게는 장기투자가 더 적합합니다.",
                        ArticleCategory.BASICS, Difficulty.BEGINNER, 6, 4, true),
                new LearnArticleEntity(null, "리스크 관리 기초",
                        "## 투자에서 리스크란?\n\n리스크란 투자한 돈을 잃을 가능성을 말합니다.\n\n" +
                                "### 리스크 관리 방법\n\n1. **손절매 설정**: 일정 비율 이상 손실 시 매도\n" +
                                "2. **분산투자**: 리스크 분산\n3. **투자 금액 관리**: 여유 자금으로만 투자\n" +
                                "4. **감정 통제**: 공포와 탐욕에 휘둘리지 않기\n\n" +
                                "### 초보자를 위한 팁\n\n- 잃어도 괜찮은 금액만 투자하세요\n" +
                                "- 한 종목에 올인하지 마세요\n- 꾸준히 공부하세요",
                        ArticleCategory.BASICS, Difficulty.BEGINNER, 8, 5, true),
                new LearnArticleEntity(null, "경제 뉴스 읽는 방법",
                        "## 경제 뉴스 기초\n\n경제 뉴스를 읽을 때 핵심 포인트를 찾는 방법을 알아봅니다.\n\n" +
                                "### 뉴스에서 확인할 것\n\n1. **제목과 소제목**: 핵심 내용 파악\n" +
                                "2. **숫자와 데이터**: 구체적인 수치 확인\n" +
                                "3. **출처**: 신뢰할 수 있는 출처인지 확인\n" +
                                "4. **시장 반응**: 뉴스 후 주가 변동 확인\n\n" +
                                "### 주의할 점\n\n- 제목만 읽고 판단하지 않기\n" +
                                "- 여러 매체의 기사를 비교\n- 과거 비슷한 사례 참고",
                        ArticleCategory.NEWS_READING, Difficulty.INTERMEDIATE, 10, 6, true),
                new LearnArticleEntity(null, "캔들스틱 차트 기초",
                        "## 캔들스틱 차트란?\n\n캔들스틱 차트는 주가의 시가, 고가, 저가, 종가를 " +
                                "한눈에 볼 수 있는 차트입니다.\n\n### 캔들의 구성\n\n" +
                                "- **몸통(Body)**: 시가와 종가 사이\n- **꼬리(Wick)**: 고가와 저가\n" +
                                "- **양봉(빨강)**: 종가 > 시가 (상승)\n- **음봉(파랑)**: 종가 < 시가 (하락)\n\n" +
                                "### 기본 패턴\n\n- **망치형**: 하락 후 반등 신호\n" +
                                "- **도지**: 매수/매도 균형, 추세 전환 가능",
                        ArticleCategory.CHART_ANALYSIS, Difficulty.INTERMEDIATE, 8, 7, true),
                new LearnArticleEntity(null, "이동평균선 이해하기",
                        "## 이동평균선(MA)이란?\n\n일정 기간의 평균 주가를 선으로 연결한 것입니다.\n\n" +
                                "### 주요 이동평균선\n\n- **5일선(주봉선)**: 초단기 추세\n" +
                                "- **20일선(월봉선)**: 단기 추세\n- **60일선(분기선)**: 중기 추세\n" +
                                "- **120일선(반기선)**: 장기 추세\n\n### 활용법\n\n" +
                                "- **골든크로스**: 단기선이 장기선을 상향 돌파 (매수 신호)\n" +
                                "- **데드크로스**: 단기선이 장기선을 하향 돌파 (매도 신호)",
                        ArticleCategory.CHART_ANALYSIS, Difficulty.INTERMEDIATE, 7, 8, true),

                new LearnArticleEntity(null, "가치 분석이란 무엇인가",
                        "## 가치 분석의 기본 개념\n\n가치 분석(Value Analysis)은 기업의 본질적인 가치를 " +
                                "분석하여 투자 판단을 내리는 방법입니다. 주가가 오르내리는 단기적인 흐름보다 " +
                                "기업이 실제로 얼마의 가치가 있는지를 중요하게 봅니다.\n\n" +
                                "### 가치 분석 vs 기술적 분석\n\n" +
                                "| 구분 | 가치 분석 | 기술적 분석 |\n" +
                                "| --- | --- | --- |\n" +
                                "| 초점 | 기업의 내재가치 | 주가 차트 패턴 |\n" +
                                "| 기간 | 중장기 (1년 이상) | 단기~중기 |\n" +
                                "| 핵심 도구 | 재무제표, PER, PBR | 이동평균선, RSI, MACD |\n" +
                                "| 대표 투자자 | 워런 버핏 | 제시 리버모어 |\n\n" +
                                "### 가치 분석의 3가지 핵심 질문\n\n" +
                                "1. **이 기업은 돈을 잘 벌고 있는가?** - 매출, 영업이익, 순이익 확인\n" +
                                "2. **현재 주가는 적정한가?** - PER, PBR로 비교\n" +
                                "3. **앞으로도 성장할 수 있는가?** - 산업 전망, 경쟁력 분석\n\n" +
                                "### 초보자를 위한 팁\n\n가치 분석은 기업을 이해하는 것에서 시작합니다. " +
                                "복잡한 수식보다 '이 회사가 무슨 일을 하고, 어떻게 돈을 버는지'를 먼저 파악하세요.",
                        ArticleCategory.VALUE_ANALYSIS, Difficulty.BEGINNER, 8, 9, true),
                new LearnArticleEntity(null, "PER과 PBR로 저평가 종목 찾기",
                        "## PER과 PBR 활용법\n\n가치 투자에서 가장 기본이 되는 두 지표, " +
                                "PER과 PBR을 활용하여 저평가된 종목을 찾는 방법을 알아봅니다.\n\n" +
                                "### PER (주가수익비율)\n\n" +
                                "PER = 주가 / 주당순이익(EPS)\n\n" +
                                "- **PER이 낮다** → 이익 대비 주가가 싸다 (저평가 가능성)\n" +
                                "- **PER이 높다** → 이익 대비 주가가 비싸다 (고평가 또는 성장 기대)\n\n" +
                                "#### PER 해석 시 주의점\n\n" +
                                "- 같은 업종끼리 비교해야 합니다\n" +
                                "- 적자 기업은 PER 계산이 불가합니다\n" +
                                "- PER이 낮다고 무조건 좋은 것은 아닙니다 (실적 악화 우려)\n\n" +
                                "### PBR (주가순자산비율)\n\n" +
                                "PBR = 주가 / 주당순자산(BPS)\n\n" +
                                "- **PBR < 1** → 회사 순자산보다 주가가 낮음 (극도의 저평가)\n" +
                                "- **PBR = 1** → 자산가치와 주가가 같음\n" +
                                "- **PBR > 1** → 자산가치보다 주가가 높음\n\n" +
                                "### 실전 활용 예시\n\n" +
                                "같은 반도체 업종에서 A사 PER 8배, B사 PER 15배라면 " +
                                "A사가 상대적으로 저평가되어 있을 수 있습니다. 하지만 A사의 이익이 " +
                                "감소 추세인지, B사가 빠르게 성장 중인지도 함께 확인해야 합니다.",
                        ArticleCategory.VALUE_ANALYSIS, Difficulty.BEGINNER, 10, 10, true),
                new LearnArticleEntity(null, "기업 재무제표 읽는 법",
                        "## 재무제표 3대 표\n\n기업의 재무 상태를 파악하기 위한 핵심 재무제표 3가지를 알아봅니다.\n\n" +
                                "### 1. 손익계산서 (Income Statement)\n\n" +
                                "일정 기간 동안의 수익과 비용을 보여줍니다.\n\n" +
                                "```\n매출액\n- 매출원가\n= 매출총이익\n" +
                                "- 판관비\n= 영업이익 ★\n" +
                                "± 영업외손익\n= 세전이익\n" +
                                "- 법인세\n= 당기순이익 ★\n```\n\n" +
                                "**핵심 포인트**: 영업이익이 꾸준히 성장하고 있는지 확인하세요.\n\n" +
                                "### 2. 재무상태표 (Balance Sheet)\n\n" +
                                "특정 시점의 자산, 부채, 자본을 보여줍니다.\n\n" +
                                "- **자산 = 부채 + 자본** (항상 성립)\n" +
                                "- 부채비율(부채/자본)이 너무 높으면 위험\n" +
                                "- 유동비율(유동자산/유동부채)이 100% 이상이면 안전\n\n" +
                                "### 3. 현금흐름표 (Cash Flow Statement)\n\n" +
                                "실제로 현금이 어떻게 움직였는지 보여줍니다.\n\n" +
                                "- **영업활동 현금흐름**: 본업에서 번 현금 (양수가 좋음)\n" +
                                "- **투자활동 현금흐름**: 설비 투자 등 (음수가 일반적)\n" +
                                "- **재무활동 현금흐름**: 차입, 배당 등\n\n" +
                                "### 초보자 체크리스트\n\n" +
                                "1. 매출과 영업이익이 꾸준히 늘고 있는가?\n" +
                                "2. 부채비율이 200% 이하인가?\n" +
                                "3. 영업활동 현금흐름이 양수인가?\n" +
                                "4. 잉여현금흐름(FCF)이 양수인가?",
                        ArticleCategory.VALUE_ANALYSIS, Difficulty.INTERMEDIATE, 12, 11, true),
                new LearnArticleEntity(null, "배당주 투자 전략",
                        "## 배당으로 안정적인 수익 만들기\n\n" +
                                "배당주 투자는 기업이 주주에게 나눠주는 배당금으로 꾸준한 수익을 얻는 전략입니다.\n\n" +
                                "### 배당투자의 장점\n\n" +
                                "1. **정기적 현금 수입**: 은행 이자처럼 주기적으로 배당금 수령\n" +
                                "2. **복리 효과**: 배당금을 재투자하면 복리로 자산 증가\n" +
                                "3. **하방 보호**: 배당수익률이 높으면 주가 하락 시 방어 역할\n\n" +
                                "### 좋은 배당주의 조건\n\n" +
                                "- **배당수익률 3% 이상**: 시중 금리보다 높은 수익\n" +
                                "- **배당성향 30~60%**: 이익 중 적정 비율을 배당\n" +
                                "- **연속 배당 이력**: 5년 이상 꾸준히 배당한 기업\n" +
                                "- **이익 성장**: 배당을 유지·증가시킬 여력이 있는 기업\n\n" +
                                "### 배당수익률 계산\n\n" +
                                "배당수익률 = (주당 배당금 / 현재 주가) x 100\n\n" +
                                "예시: 주가 50,000원, 배당금 2,000원 → 배당수익률 4%\n\n" +
                                "### 주의할 점\n\n" +
                                "- 배당수익률만 보지 말고 기업의 재무 건전성도 확인\n" +
                                "- 일시적으로 주가가 폭락해서 배당수익률이 높아진 경우 주의\n" +
                                "- 배당 축소·중단 가능성도 항상 체크",
                        ArticleCategory.VALUE_ANALYSIS, Difficulty.BEGINNER, 8, 12, true),

                new LearnArticleEntity(null, "거시경제 지표와 주식시장의 관계",
                        "## 거시경제가 주식에 미치는 영향\n\n" +
                                "거시경제 지표는 경제 전체의 흐름을 보여주며, 주식시장에 큰 영향을 미칩니다.\n\n" +
                                "### 핵심 거시경제 지표\n\n" +
                                "#### 1. 기준금리\n" +
                                "- **금리 인하** → 대출 이자 감소 → 기업 이익 증가 → 주가 상승 요인\n" +
                                "- **금리 인상** → 대출 부담 증가 → 소비 위축 → 주가 하락 요인\n\n" +
                                "#### 2. 환율 (원/달러)\n" +
                                "- **원화 약세(환율 상승)** → 수출 기업에 유리, 수입 기업에 불리\n" +
                                "- **원화 강세(환율 하락)** → 수입 기업에 유리, 수출 기업에 불리\n\n" +
                                "#### 3. 소비자물가지수 (CPI)\n" +
                                "- 물가 상승률이 높으면 → 금리 인상 압력 → 주가에 부정적\n" +
                                "- 물가 안정 → 금리 인하 가능성 → 주가에 긍정적\n\n" +
                                "#### 4. 유가\n" +
                                "- 유가 상승 → 원가 부담 증가 → 대부분 기업에 부정적\n" +
                                "- 유가 하락 → 비용 절감 → 소비 여력 증가\n\n" +
                                "### 실전 활용 팁\n\n" +
                                "1. 금리 방향을 먼저 확인하세요 (경기의 큰 방향)\n" +
                                "2. 환율 변동은 투자 종목과 연결지어 해석하세요\n" +
                                "3. 단일 지표보다 여러 지표를 종합적으로 판단하세요",
                        ArticleCategory.MACRO_ECONOMY, Difficulty.INTERMEDIATE, 10, 13, true),
                new LearnArticleEntity(null, "뉴스 감성과 투자 판단",
                        "## 뉴스가 주가에 미치는 영향\n\n" +
                                "기업이나 경제 관련 뉴스는 투자 심리에 직접적인 영향을 미칩니다. " +
                                "뉴스를 올바르게 해석하는 능력은 가치 투자자에게 중요한 스킬입니다.\n\n" +
                                "### 뉴스 감성 분석이란?\n\n" +
                                "뉴스의 내용이 긍정적인지, 부정적인지, 중립적인지를 분석하는 것입니다.\n\n" +
                                "- **긍정적 뉴스**: 실적 호조, 신사업 진출, 수주 확대\n" +
                                "- **부정적 뉴스**: 실적 악화, 규제 강화, 소송 발생\n" +
                                "- **중립적 뉴스**: 인사 변동, 정기 공시\n\n" +
                                "### 뉴스 해석의 함정\n\n" +
                                "1. **선반영**: 이미 예상된 뉴스는 주가에 반영되어 있을 수 있습니다\n" +
                                "2. **과잉 반응**: 단기적 악재에 시장이 과도하게 반응할 수 있습니다\n" +
                                "3. **확증 편향**: 자기 생각에 맞는 뉴스만 골라 읽는 것을 조심하세요\n\n" +
                                "### 가치 투자자의 뉴스 활용법\n\n" +
                                "1. **장기적 영향을 판단**: 일시적 악재인지, 구조적 문제인지 구분\n" +
                                "2. **숫자로 검증**: 뉴스의 주장을 재무 데이터로 확인\n" +
                                "3. **역발상 기회 포착**: 과도한 공포 시 우량주 매수 기회\n" +
                                "4. **여러 출처 비교**: 한 매체의 뉴스만 믿지 말고 교차 검증\n\n" +
                                "### FinEasy의 뉴스 감성 점수\n\n" +
                                "FinEasy는 AI를 활용하여 뉴스의 감성을 0~1 사이 점수로 표시합니다.\n" +
                                "- **0.7 이상**: 긍정적\n- **0.4~0.7**: 중립적\n- **0.4 미만**: 부정적\n\n" +
                                "이 점수를 참고하되, 반드시 뉴스 내용을 직접 읽고 판단하세요.",
                        ArticleCategory.MACRO_ECONOMY, Difficulty.INTERMEDIATE, 10, 14, true)
        );
        articleRepo.saveAll(articles);
        log.info("Seeded {} learn articles", articles.size());
    }

    private void seedEtfPresets() {
        if (etfPresetRepo.count() > 0) return;

        List<EtfPresetEntity> presets = List.of(
                new EtfPresetEntity(null, "QQQ", "Invesco QQQ Trust", "인베스코 QQQ",
                        "NASDAQ", "US Large Cap Growth",
                        new BigDecimal("15.20"),
                        "나스닥 100 지수를 추종하는 대표 기술주 ETF. 애플, 마이크로소프트, 엔비디아 등 빅테크 중심."),
                new EtfPresetEntity(null, "SPY", "SPDR S&P 500 ETF", "SPDR S&P 500",
                        "NYSE", "US Large Cap Blend",
                        new BigDecimal("10.50"),
                        "S&P 500 지수를 추종하는 세계 최대 ETF. 미국 대형주 500개에 분산 투자."),
                new EtfPresetEntity(null, "SCHD", "Schwab U.S. Dividend Equity ETF", "슈왑 미국 배당주",
                        "NYSE", "US Dividend",
                        new BigDecimal("12.10"),
                        "높은 배당 수익률과 배당 성장을 겸비한 미국 우량 배당주 ETF."),
                new EtfPresetEntity(null, "VTI", "Vanguard Total Stock Market ETF", "뱅가드 전체 주식시장",
                        "NYSE", "US Total Market",
                        new BigDecimal("10.80"),
                        "미국 전체 주식시장(대형+중형+소형)에 투자하는 ETF. 약 4,000개 종목 보유."),
                new EtfPresetEntity(null, "KODEX200", "KODEX 200", "코덱스 200",
                        "KRX", "KR Large Cap",
                        new BigDecimal("7.30"),
                        "코스피 200 지수를 추종하는 국내 대표 ETF. 삼성전자, SK하이닉스 등 한국 대형주 중심."),
                new EtfPresetEntity(null, "TIGER_SP500", "TIGER S&P500", "타이거 미국S&P500",
                        "KRX", "KR Listed US Equity",
                        new BigDecimal("10.50"),
                        "국내 상장 S&P 500 추종 ETF. 원화로 미국 대형주에 투자 가능. 연금계좌 활용 가능."),
                new EtfPresetEntity(null, "KODEX_DIV", "KODEX Dividend", "코덱스 배당",
                        "KRX", "KR Dividend",
                        new BigDecimal("6.80"),
                        "국내 고배당주 중심 ETF. 금융, 통신, 유틸리티 등 배당 수익률 높은 종목 구성."),
                new EtfPresetEntity(null, "QLD", "ProShares Ultra QQQ", "프로셰어즈 울트라 QQQ",
                        "NASDAQ", "US Leveraged",
                        new BigDecimal("22.00"),
                        "나스닥 100 지수의 일일 수익률 2배를 추종하는 레버리지 ETF. 고위험·고수익 상품."),
                new EtfPresetEntity(null, "VWO", "Vanguard FTSE Emerging Markets ETF", "뱅가드 신흥시장",
                        "NYSE", "Emerging Markets",
                        new BigDecimal("5.20"),
                        "중국, 인도, 브라질 등 신흥시장 주식에 분산 투자하는 ETF."),
                new EtfPresetEntity(null, "GLD", "SPDR Gold Shares", "SPDR 금",
                        "NYSE", "Commodity",
                        new BigDecimal("8.10"),
                        "실물 금 가격을 추종하는 ETF. 인플레이션 헤지 및 포트폴리오 분산 목적.")
        );
        etfPresetRepo.saveAll(presets);
        log.info("Seeded {} ETF presets", presets.size());
    }

    private void seedSectorContents() {
        if (sectorContentRepo.count() > 0) return;

        seedSemiconductorSector();
        seedDefenseSector();
        seedSecondaryBatterySector();
        seedBioSector();
        seedFinanceSector();
        seedEnergySector();

        log.info("Seeded 6 sector contents");
    }

    private void seedSemiconductorSector() {
        SectorContentEntity sector = new SectorContentEntity(
                null, "semiconductor", "반도체", "Semiconductor",
                "반도체는 전자기기의 두뇌 역할을 하는 핵심 부품을 설계하고 제조하는 산업입니다. AI, 자율주행, IoT 등 4차 산업혁명의 기반이 되는 핵심 섹터입니다.",
                "cpu",
                "## 산업 구조\n\n" +
                        "반도체 산업은 크게 **팹리스(설계)**, **파운드리(위탁제조)**, **IDM(종합반도체)**, **OSAT(후공정)** 으로 나뉩니다.\n\n" +
                        "### 팹리스 (Fabless)\n" +
                        "반도체를 직접 설계만 하고 제조는 외주하는 기업입니다. 엔비디아, AMD, 퀄컴이 대표적입니다.\n\n" +
                        "### 파운드리 (Foundry)\n" +
                        "팹리스 기업의 설계도를 받아 반도체를 위탁 생산합니다. TSMC가 세계 시장의 약 55%를 점유하며, 삼성전자가 2위입니다.\n\n" +
                        "### IDM (Integrated Device Manufacturer)\n" +
                        "설계부터 제조까지 자체적으로 수행하는 종합 반도체 기업입니다. 인텔, 삼성전자, SK하이닉스가 해당됩니다.\n\n" +
                        "### 메모리 vs 비메모리\n" +
                        "- **메모리 반도체**: DRAM, NAND Flash (삼성전자, SK하이닉스, 마이크론)\n" +
                        "- **비메모리(시스템) 반도체**: CPU, GPU, AP, 센서 (엔비디아, 퀄컴, AMD)",
                "## 밸류체인\n\n" +
                        "```\n" +
                        "설계(EDA/IP) → 웨이퍼 제조 → 전공정(노광/식각/증착) → 후공정(패키징/테스트) → 최종 제품\n" +
                        "```\n\n" +
                        "### 1. EDA/IP (Electronic Design Automation)\n" +
                        "반도체 설계에 필요한 소프트웨어와 핵심 IP를 공급합니다. Synopsys, Cadence, ARM이 대표적입니다.\n\n" +
                        "### 2. 소재/장비\n" +
                        "웨이퍼(실리콘 기판), 포토마스크, 특수 가스 등의 소재와 노광장비(ASML), 식각장비(램리서치) 등을 공급합니다.\n\n" +
                        "### 3. 전공정 (Front-end)\n" +
                        "웨이퍼 위에 회로를 새기는 과정입니다. 노광 → 식각 → 증착 → 세정의 과정을 수백 번 반복합니다.\n\n" +
                        "### 4. 후공정 (Back-end)\n" +
                        "완성된 웨이퍼를 개별 칩으로 자르고, 패키징하여 테스트합니다. 최근 첨단 패키징(CoWoS, HBM)이 핵심 기술로 부상했습니다.\n\n" +
                        "### 5. 수요처\n" +
                        "스마트폰, PC, 서버/데이터센터, 자동차, AI 가속기 등 다양한 분야에서 반도체를 소비합니다.",
                "## 산업 동향\n\n" +
                        "### AI 반도체 수요 폭증\n" +
                        "생성형 AI의 확산으로 GPU, HBM(고대역폭메모리) 수요가 급증하고 있습니다. 엔비디아의 데이터센터 매출은 전년 대비 200% 이상 성장했습니다.\n\n" +
                        "### HBM 경쟁 심화\n" +
                        "SK하이닉스가 HBM3E로 선두를 유지하는 가운데, 삼성전자와 마이크론이 추격하고 있습니다.\n\n" +
                        "### 미중 반도체 갈등\n" +
                        "미국의 대중 반도체 수출 규제가 강화되면서 공급망 재편이 진행 중입니다. 각국이 반도체 자국 생산을 위한 대규모 보조금 정책을 추진하고 있습니다.\n\n" +
                        "### 첨단 공정 경쟁\n" +
                        "2nm 이하 공정에서 TSMC, 삼성전자, 인텔 간 치열한 기술 경쟁이 벌어지고 있습니다. GAA(Gate-All-Around) 트랜지스터 기술이 핵심입니다."
        );
        sector.addCompany(new SectorRepresentativeCompanyEntity("TSMC", "TSM", "NYSE", "세계 최대 파운드리 기업. 애플, 엔비디아 등 주요 팹리스 기업의 칩을 위탁 생산합니다.", 1));
        sector.addCompany(new SectorRepresentativeCompanyEntity("엔비디아", "NVDA", "NASDAQ", "AI GPU 시장 지배적 기업. 데이터센터용 H100/B200 칩으로 AI 혁명을 이끌고 있습니다.", 2));
        sector.addCompany(new SectorRepresentativeCompanyEntity("삼성전자", "005930", "KRX", "메모리 반도체 세계 1위, 파운드리 2위. DRAM/NAND/시스템반도체를 모두 생산하는 종합 반도체 기업입니다.", 3));
        sector.addCompany(new SectorRepresentativeCompanyEntity("SK하이닉스", "000660", "KRX", "DRAM/NAND 세계 2~3위. HBM(고대역폭메모리) 시장에서 선두를 달리고 있습니다.", 4));
        sector.addCompany(new SectorRepresentativeCompanyEntity("ASML", "ASML", "NASDAQ", "극자외선(EUV) 노광장비 독점 기업. 첨단 반도체 제조에 필수적인 장비를 공급합니다.", 5));
        sectorContentRepo.save(sector);
    }

    private void seedDefenseSector() {
        SectorContentEntity sector = new SectorContentEntity(
                null, "defense", "방산", "Defense",
                "방위산업은 국가 안보를 위한 무기체계, 군용 장비, 방어 시스템을 개발하고 생산하는 산업입니다. 지정학적 긴장 고조로 전 세계적으로 국방비 지출이 증가하고 있습니다.",
                "shield",
                "## 산업 구조\n\n" +
                        "방산 산업은 **완성 무기체계**, **부품/서브시스템**, **방산 IT/사이버**, **유지보수(MRO)** 로 구분됩니다.\n\n" +
                        "### 완성 무기체계\n" +
                        "전투기, 전차, 함정, 미사일 등 완제품을 개발·생산합니다. 개발 기간이 10년 이상, 계약 규모가 수조 원에 달합니다.\n\n" +
                        "### 부품/서브시스템\n" +
                        "레이더, 항공전자, 유도장치, 통신장비 등 무기체계의 핵심 부품을 공급합니다.\n\n" +
                        "### 방산 IT/사이버\n" +
                        "C4ISR(지휘통제통신컴퓨터정보감시정찰) 체계, 사이버 보안 솔루션 등을 제공합니다.\n\n" +
                        "### 유지보수(MRO)\n" +
                        "기존 무기체계의 성능 개량, 정비, 수명 연장 서비스를 제공합니다. 안정적인 반복 매출원입니다.",
                "## 밸류체인\n\n" +
                        "```\n" +
                        "소요 기획 → 연구개발(R&D) → 시제품 → 시험평가 → 양산 → 배치/운용 → MRO\n" +
                        "```\n\n" +
                        "### 1. 소요 기획\n" +
                        "군이 필요한 무기체계의 성능과 수량을 결정합니다.\n\n" +
                        "### 2. R&D / 시제품\n" +
                        "방산업체가 기술 개발 후 시제품을 제작합니다. ADD(국방과학연구소)와 협력하는 경우가 많습니다.\n\n" +
                        "### 3. 양산 / 수출\n" +
                        "시험평가를 통과하면 양산에 들어갑니다. 한국 방산은 K9 자주포, K2 전차, FA-50 경전투기 등으로 수출 성과를 내고 있습니다.\n\n" +
                        "### 4. MRO / 성능개량\n" +
                        "배치 후 수십 년간 운용하며 지속적인 유지보수와 성능 개량이 이루어집니다.",
                "## 산업 동향\n\n" +
                        "### 글로벌 국방비 증가\n" +
                        "러시아-우크라이나 전쟁 이후 NATO 국가들이 GDP 대비 국방비 2% 이상으로 확대하고 있습니다. 전 세계 국방비가 사상 최고치를 기록하고 있습니다.\n\n" +
                        "### 한국 방산 수출 호조\n" +
                        "폴란드, 사우디, UAE 등과의 대규모 수출 계약으로 한국 방산 수출이 급성장하고 있습니다. K9 자주포, K2 전차, 천궁 미사일 등이 수출 효자 품목입니다.\n\n" +
                        "### 무인화/자율화\n" +
                        "드론, 무인 전투 차량, 자율 함정 등 무인 무기체계 개발이 활발합니다.\n\n" +
                        "### 우주/사이버 영역 확대\n" +
                        "군사 위성, 우주 감시 체계, 사이버전 역량 강화에 대한 투자가 증가하고 있습니다."
        );
        sector.addCompany(new SectorRepresentativeCompanyEntity("한화에어로스페이스", "012450", "KRX", "K9 자주포, 항공엔진, 방산전자 등을 생산하는 한국 대표 방산기업. 폴란드 수출로 주목받고 있습니다.", 1));
        sector.addCompany(new SectorRepresentativeCompanyEntity("LIG넥스원", "079550", "KRX", "미사일(천궁), 유도무기, 항공전자 전문 기업. 정밀유도무기 분야 한국 1위입니다.", 2));
        sector.addCompany(new SectorRepresentativeCompanyEntity("록히드마틴", "LMT", "NYSE", "세계 최대 방산기업. F-35 전투기, PAC-3 미사일 등을 생산합니다.", 3));
        sector.addCompany(new SectorRepresentativeCompanyEntity("RTX (레이시온)", "RTX", "NYSE", "패트리엇 미사일, 항공엔진(프랫앤휘트니) 등을 생산하는 미국 방산/항공우주 기업입니다.", 4));
        sector.addCompany(new SectorRepresentativeCompanyEntity("한화시스템", "272210", "KRX", "방산 전자장비, 레이더, C4I 체계 개발. 도심항공교통(UAM) 분야에도 진출하고 있습니다.", 5));
        sectorContentRepo.save(sector);
    }

    private void seedSecondaryBatterySector() {
        SectorContentEntity sector = new SectorContentEntity(
                null, "secondary-battery", "이차전지", "Secondary Battery",
                "이차전지(충전식 배터리)는 전기차, ESS(에너지저장장치), 모바일기기의 핵심 부품입니다. 전기차 시대의 도래와 함께 가장 빠르게 성장하는 섹터 중 하나입니다.",
                "battery",
                "## 산업 구조\n\n" +
                        "이차전지 산업은 **양극재**, **음극재**, **분리막**, **전해질** 등 4대 핵심 소재와 **셀 제조**, **모듈/팩 조립**으로 구성됩니다.\n\n" +
                        "### 셀 제조사 (Cell Maker)\n" +
                        "소재를 조합하여 배터리 셀을 제조합니다. LG에너지솔루션, 삼성SDI, CATL, 파나소닉이 글로벌 빅4입니다.\n\n" +
                        "### 배터리 유형\n" +
                        "- **리튬이온(Li-ion)**: 현재 주류. 삼원계(NCM/NCA)와 LFP(리튬인산철)로 나뉨\n" +
                        "- **전고체 배터리**: 차세대 기술. 안전성과 에너지밀도 향상이 기대됨\n" +
                        "- **나트륨이온**: 저가형 대안으로 연구 중",
                "## 밸류체인\n\n" +
                        "```\n" +
                        "광물 채굴(리튬/니켈/코발트) → 소재 가공(양극재/음극재/전해질/분리막) → 셀 제조 → 모듈/팩 조립 → 최종 응용(EV/ESS)\n" +
                        "```\n\n" +
                        "### 1. 핵심 광물\n" +
                        "리튬, 니켈, 코발트, 망간 등이 주요 원재료입니다. 자원 편재로 공급망 리스크가 존재합니다.\n\n" +
                        "### 2. 4대 핵심 소재\n" +
                        "- **양극재**: 배터리 원가의 40% 차지. 에코프로비엠, 포스코퓨처엠 등\n" +
                        "- **음극재**: 흑연 기반. 포스코퓨처엠, 대주전자재료 등\n" +
                        "- **분리막**: 양극과 음극 사이 안전장치. SK아이이테크놀로지 등\n" +
                        "- **전해질**: 이온 이동 매개체. 솔브레인, 엔켐 등\n\n" +
                        "### 3. 셀 → 모듈 → 팩\n" +
                        "셀을 묶어 모듈로, 모듈을 묶어 팩으로 조립하여 전기차나 ESS에 장착합니다.",
                "## 산업 동향\n\n" +
                        "### 전기차 성장 지속\n" +
                        "글로벌 전기차 판매량이 연간 1,400만 대를 돌파하며 배터리 수요가 꾸준히 증가하고 있습니다.\n\n" +
                        "### LFP vs 삼원계 경쟁\n" +
                        "가격 경쟁력을 앞세운 LFP(CATL, BYD)와 에너지밀도가 높은 삼원계(LG에너지솔루션, 삼성SDI) 간 경쟁이 심화되고 있습니다.\n\n" +
                        "### 전고체 배터리 개발 경쟁\n" +
                        "삼성SDI, 토요타 등이 전고체 배터리 상용화를 목표로 연구개발 중입니다. 2027~2028년 양산이 목표입니다.\n\n" +
                        "### 북미/유럽 현지 생산\n" +
                        "IRA(인플레이션감축법) 등의 영향으로 배터리 셀 제조사들이 미국, 유럽에 대규모 공장을 건설하고 있습니다."
        );
        sector.addCompany(new SectorRepresentativeCompanyEntity("LG에너지솔루션", "373220", "KRX", "글로벌 배터리 셀 제조 2위. GM, 현대차 등에 배터리를 공급하며 북미 공장을 확대 중입니다.", 1));
        sector.addCompany(new SectorRepresentativeCompanyEntity("삼성SDI", "006400", "KRX", "프리미엄 배터리 셀 제조사. BMW, 리비안 등에 공급하며 전고체 배터리 기술을 선도하고 있습니다.", 2));
        sector.addCompany(new SectorRepresentativeCompanyEntity("CATL", "300750", "SZSE", "세계 최대 배터리 셀 제조사. LFP 배터리 기술력과 가격 경쟁력으로 시장을 주도하고 있습니다.", 3));
        sector.addCompany(new SectorRepresentativeCompanyEntity("에코프로비엠", "247540", "KOSDAQ", "양극재 전문 기업. 하이니켈 양극재 기술력으로 삼성SDI 등에 공급하고 있습니다.", 4));
        sector.addCompany(new SectorRepresentativeCompanyEntity("파나소닉", "6752", "TSE", "테슬라에 원통형 배터리를 독점 공급했던 기업. 4680 배터리 양산을 추진 중입니다.", 5));
        sectorContentRepo.save(sector);
    }

    private void seedBioSector() {
        SectorContentEntity sector = new SectorContentEntity(
                null, "bio", "바이오", "Bio/Pharma",
                "바이오/제약 산업은 신약 개발, 바이오시밀러, 위탁생산(CDMO) 등을 포괄하는 고부가가치 산업입니다. 고령화와 신기술 발전으로 장기 성장이 전망됩니다.",
                "dna",
                "## 산업 구조\n\n" +
                        "바이오 산업은 **신약 개발(혁신 제약)**, **바이오시밀러**, **CDMO(위탁생산)**, **진단/의료기기** 등으로 구분됩니다.\n\n" +
                        "### 혁신 제약 (Big Pharma)\n" +
                        "블록버스터급 신약을 개발하여 특허 기간 동안 독점 판매합니다. 화이자, 로슈, 노바티스 등이 대표적입니다.\n\n" +
                        "### 바이오시밀러\n" +
                        "특허 만료된 바이오의약품의 복제약입니다. 셀트리온이 세계적 선두 기업입니다.\n\n" +
                        "### CDMO (위탁개발생산)\n" +
                        "제약사의 의약품을 대신 개발·생산해주는 사업입니다. 삼성바이오로직스가 글로벌 1위입니다.\n\n" +
                        "### 바이오텍 (Biotech)\n" +
                        "혁신적인 파이프라인을 보유한 중소형 기업. 높은 성장 가능성과 함께 리스크도 큽니다.",
                "## 밸류체인\n\n" +
                        "```\n" +
                        "타겟 발굴 → 후보물질 탐색 → 전임상(동물실험) → 임상 1/2/3상 → 허가 심사 → 상업화 → 시판 후 조사\n" +
                        "```\n\n" +
                        "### 1. 신약 개발 과정\n" +
                        "타겟 발굴부터 시판까지 평균 10~15년, 비용 1~2조 원이 소요됩니다. 성공 확률은 약 5~10%입니다.\n\n" +
                        "### 2. 임상 시험 단계\n" +
                        "- **임상 1상**: 소수 건강인 대상 안전성 확인\n" +
                        "- **임상 2상**: 환자 대상 유효성/용량 확인 (가장 중요한 관문)\n" +
                        "- **임상 3상**: 대규모 환자 대상 효능/안전성 최종 확인\n\n" +
                        "### 3. CDMO/CMO\n" +
                        "바이오의약품 제조 공정은 복잡하여 많은 제약사가 전문 CDMO에 생산을 위탁합니다.",
                "## 산업 동향\n\n" +
                        "### GLP-1 비만 치료제 열풍\n" +
                        "노보노디스크(오젬픽/위고비), 릴리(마운자로/젭바운드)의 GLP-1 계열 약물이 블록버스터로 성장하고 있습니다.\n\n" +
                        "### ADC(항체약물접합체) 부상\n" +
                        "항암 치료의 새로운 패러다임으로 ADC 기술이 주목받고 있으며, 관련 라이선스 딜이 급증하고 있습니다.\n\n" +
                        "### AI 신약 개발\n" +
                        "AI를 활용한 약물 설계와 임상 최적화가 신약 개발 기간과 비용을 혁신적으로 단축할 것으로 기대됩니다.\n\n" +
                        "### CDMO 시장 성장\n" +
                        "바이오의약품 비중 확대에 따라 CDMO 시장이 연 10% 이상 성장하고 있습니다."
        );
        sector.addCompany(new SectorRepresentativeCompanyEntity("삼성바이오로직스", "207940", "KRX", "글로벌 CDMO 1위. 인천 송도에 세계 최대 바이오의약품 생산 시설을 보유하고 있습니다.", 1));
        sector.addCompany(new SectorRepresentativeCompanyEntity("셀트리온", "068270", "KRX", "바이오시밀러 세계 선두 기업. 램시마, 트룩시마 등으로 글로벌 시장을 개척하고 있습니다.", 2));
        sector.addCompany(new SectorRepresentativeCompanyEntity("모더나", "MRNA", "NASDAQ", "mRNA 기술 플랫폼 보유. COVID-19 백신으로 성장했으며 암 백신, 희귀질환 치료제를 개발 중입니다.", 3));
        sector.addCompany(new SectorRepresentativeCompanyEntity("화이자", "PFE", "NYSE", "세계 최대 제약사 중 하나. 다양한 치료 영역에서 블록버스터 의약품을 보유하고 있습니다.", 4));
        sector.addCompany(new SectorRepresentativeCompanyEntity("알테오젠", "196170", "KOSDAQ", "피하주사 전환 플랫폼 기술 보유. 글로벌 빅파마와 기술 라이선스 계약을 체결하고 있습니다.", 5));
        sectorContentRepo.save(sector);
    }

    private void seedFinanceSector() {
        SectorContentEntity sector = new SectorContentEntity(
                null, "finance", "금융", "Finance",
                "금융 산업은 은행, 증권, 보험, 자산운용 등을 포괄하며, 경제의 혈액 역할을 합니다. 디지털 전환과 핀테크의 부상으로 빠르게 변화하고 있습니다.",
                "landmark",
                "## 산업 구조\n\n" +
                        "금융 산업은 **은행**, **증권/투자은행**, **보험**, **자산운용**, **핀테크** 등으로 구분됩니다.\n\n" +
                        "### 은행 (Banking)\n" +
                        "예금을 받아 대출을 실행하는 전통적 금융 중개 기능을 수행합니다. 예대마진(NIM)이 핵심 수익원입니다.\n\n" +
                        "### 증권/투자은행 (Securities/IB)\n" +
                        "주식/채권 중개, IPO 주선, M&A 자문, 자산관리 등의 업무를 수행합니다.\n\n" +
                        "### 보험 (Insurance)\n" +
                        "생명보험, 손해보험으로 나뉘며, 보험료 수입과 투자 수익으로 운영됩니다.\n\n" +
                        "### 자산운용 (Asset Management)\n" +
                        "펀드, ETF, 연금 등을 운용하여 수수료 수익을 얻습니다.\n\n" +
                        "### 금융지주회사\n" +
                        "은행, 증권, 보험 등 여러 금융 자회사를 보유한 지주회사 형태가 한국 금융의 주류입니다.",
                "## 밸류체인\n\n" +
                        "```\n" +
                        "자금 조달(예금/채권) → 리스크 평가(심사/신용평가) → 자금 운용(대출/투자) → 수익 배분(이자/배당)\n" +
                        "```\n\n" +
                        "### 1. 자금 조달\n" +
                        "고객 예금, 채권 발행, 중앙은행 차입 등으로 자금을 조달합니다.\n\n" +
                        "### 2. 리스크 관리\n" +
                        "신용 리스크, 시장 리스크, 운영 리스크를 관리합니다. BIS 비율 등 건전성 지표가 중요합니다.\n\n" +
                        "### 3. 수익 구조\n" +
                        "- **이자 수익**: 대출 금리와 예금 금리의 차이(NIM)\n" +
                        "- **비이자 수익**: 수수료, 트레이딩, 자산관리 등\n" +
                        "- **투자 수익**: 채권, 주식 등 자체 투자 포트폴리오 운용",
                "## 산업 동향\n\n" +
                        "### 금리 환경 변화\n" +
                        "글로벌 금리 인하 사이클에 진입하면서 은행의 NIM 축소 압력이 있으나, 대출 수요 증가가 기대됩니다.\n\n" +
                        "### 디지털 전환 가속\n" +
                        "인터넷/모바일 뱅킹 비중이 90%를 넘어서며, 점포 수는 지속 감소하고 있습니다.\n\n" +
                        "### 주주환원 강화\n" +
                        "밸류업 프로그램 영향으로 금융지주사들의 배당성향과 자사주 매입이 확대되고 있습니다. 총주주환원율 30~40% 수준으로 개선 중입니다.\n\n" +
                        "### AI/빅데이터 활용\n" +
                        "AI 기반 신용평가, 로보어드바이저, 이상거래 탐지 등 금융 AI 활용이 빠르게 확산되고 있습니다."
        );
        sector.addCompany(new SectorRepresentativeCompanyEntity("KB금융", "105560", "KRX", "한국 최대 금융지주. KB국민은행, KB증권, KB손해보험 등을 보유하고 있습니다.", 1));
        sector.addCompany(new SectorRepresentativeCompanyEntity("신한지주", "055550", "KRX", "한국 2위 금융지주. 신한은행, 신한투자증권 등을 보유. 디지털 금융에 적극 투자하고 있습니다.", 2));
        sector.addCompany(new SectorRepresentativeCompanyEntity("JP모건 체이스", "JPM", "NYSE", "세계 최대 투자은행. 상업은행, 투자은행, 자산관리를 모두 아우르는 글로벌 금융 리더입니다.", 3));
        sector.addCompany(new SectorRepresentativeCompanyEntity("골드만삭스", "GS", "NYSE", "세계 최고 투자은행. M&A 자문, 트레이딩, 자산관리에서 강점을 보유하고 있습니다.", 4));
        sector.addCompany(new SectorRepresentativeCompanyEntity("하나금융지주", "086790", "KRX", "한국 3위 금융지주. 하나은행을 중심으로 글로벌 네트워크를 확대하고 있습니다.", 5));
        sectorContentRepo.save(sector);
    }

    private void seedEnergySector() {
        SectorContentEntity sector = new SectorContentEntity(
                null, "energy", "에너지", "Energy",
                "에너지 산업은 전력 생산, 송배전, 석유/가스, 신재생에너지를 포괄합니다. 탄소중립 목표에 따라 전통 에너지에서 청정에너지로의 전환이 가속화되고 있습니다.",
                "zap",
                "## 산업 구조\n\n" +
                        "에너지 산업은 **전통 에너지(화석연료)**, **원자력**, **신재생에너지**, **전력 유틸리티**, **에너지 인프라** 등으로 구분됩니다.\n\n" +
                        "### 화석연료 (Oil & Gas)\n" +
                        "석유, 천연가스의 탐사·시추·정제·판매를 수행합니다. 에너지 전환기에도 여전히 세계 에너지의 80% 이상을 차지합니다.\n\n" +
                        "### 원자력\n" +
                        "안정적인 베이스로드 전원으로 재평가받고 있습니다. SMR(소형모듈원전) 등 차세대 원전 기술이 주목받고 있습니다.\n\n" +
                        "### 신재생에너지\n" +
                        "태양광, 풍력, 수소 등 청정에너지원입니다. 정부 보조금과 탄소 규제로 빠르게 성장하고 있습니다.\n\n" +
                        "### 전력 유틸리티\n" +
                        "발전소를 운영하고 전력을 송배전하는 사업입니다. 규제 산업으로 안정적인 수익 구조를 가집니다.",
                "## 밸류체인\n\n" +
                        "```\n" +
                        "1차 에너지원(석유/가스/우라늄/태양광/풍력) → 에너지 변환(발전소/정유소) → 송배전/수송 → 최종 소비(가정/산업/수송)\n" +
                        "```\n\n" +
                        "### 1. 상류(Upstream)\n" +
                        "석유/가스 탐사, 시추, 생산. 높은 자본 투자와 가격 변동성이 특징입니다.\n\n" +
                        "### 2. 중류(Midstream)\n" +
                        "파이프라인, LNG 터미널, 저장 시설을 통한 에너지 수송/저장입니다.\n\n" +
                        "### 3. 하류(Downstream)\n" +
                        "정유, 석유화학, 주유소 운영 등 최종 소비자에게 에너지를 공급합니다.\n\n" +
                        "### 4. 발전/유틸리티\n" +
                        "다양한 에너지원으로 전기를 생산하고 소비자에게 공급합니다.",
                "## 산업 동향\n\n" +
                        "### 에너지 전환 가속\n" +
                        "2050 탄소중립 목표에 따라 태양광, 풍력 설치 용량이 매년 사상 최고치를 갱신하고 있습니다.\n\n" +
                        "### 원전 르네상스\n" +
                        "AI 데이터센터의 전력 수요 급증과 탄소중립 목표로 원전이 재평가받고 있습니다. 한국의 원전 수출(UAE 바라카 원전)도 성과를 내고 있습니다.\n\n" +
                        "### 수소 경제\n" +
                        "그린수소(신재생에너지 기반)를 중심으로 수소 경제 생태계 구축이 진행 중입니다. 수소 연료전지, 수전해 장비 등이 주목받고 있습니다.\n\n" +
                        "### ESS(에너지저장장치) 수요 급증\n" +
                        "신재생에너지의 간헐성을 보완하기 위한 대용량 배터리 ESS 수요가 폭발적으로 증가하고 있습니다."
        );
        sector.addCompany(new SectorRepresentativeCompanyEntity("한국전력", "015760", "KRX", "한국 전력 산업의 핵심 공기업. 발전, 송전, 배전, 판매를 총괄합니다.", 1));
        sector.addCompany(new SectorRepresentativeCompanyEntity("두산에너빌리티", "034020", "KRX", "원전/화력 발전설비 제조 전문 기업. 체코 원전 수출 등으로 원전 르네상스의 수혜가 기대됩니다.", 2));
        sector.addCompany(new SectorRepresentativeCompanyEntity("넥스트에라 에너지", "NEE", "NYSE", "미국 최대 신재생에너지 발전사. 풍력/태양광 발전 용량 세계 1위입니다.", 3));
        sector.addCompany(new SectorRepresentativeCompanyEntity("엑손모빌", "XOM", "NYSE", "세계 최대 석유/가스 기업 중 하나. 전통 에너지에서 탄소 포집, 수소 등 신사업으로 확장 중입니다.", 4));
        sector.addCompany(new SectorRepresentativeCompanyEntity("한화솔루션", "009830", "KRX", "태양광 셀/모듈 제조 및 발전 사업. 미국에 대규모 태양광 공장을 건설하고 있습니다.", 5));
        sectorContentRepo.save(sector);
    }
}
