export interface MarketIndex {
  code: string;
  name: string;
  currentValue: number;
  changeAmount: number;
  changeRate: number;
  sparklineData: number[];
}

export interface Stock {
  stockCode: string;
  stockName: string;
  market: 'KRX' | 'KOSDAQ' | 'NASDAQ' | 'NYSE';
  sector: string;
  currentPrice: number;
  changeAmount: number;
  changeRate: number;
  volume: number;
  tradingValue: number;
  marketCap: string;
  per: number;
  pbr: number;
  eps: number;
  dividendYield: number;
  high52w: number;
  low52w: number;
  currency?: 'KRW' | 'USD';
}

export interface StockCandle {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface StockChartData {
  candles: StockCandle[];
  indicators: Record<string, (number | null)[]>;
}

export interface NewsArticle {
  id: number;
  title: string;
  sourceName: string;
  publishedAt: string;
  sentiment: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  sentimentScore: number;
  originalUrl: string;
  stockCodes?: string[];
  taggedStocks?: TaggedStock[];
  isBreaking?: boolean;
}

export interface BokTerm {
  id: number;
  term: string;
  englishTerm: string;
  definition: string;
  category: string;
}

export interface BokTermExplanation {
  termId: number;
  term: string;
  simpleSummary: string;
  easyExplanation: string;
  example: string;
  keyPoints: string[];
  generatedAt: string;
}

export interface BokTermPage {
  content: BokTerm[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FinancialTerm {
  id: number;
  name: string;
  nameEn: string;
  category: string;
  difficulty: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  simpleDescription: string;
  detailedDescription: string;
  exampleSentence: string;
  relatedTerms: { id: number; name: string }[];
}

export interface AnalysisReport {
  stockCode: string;
  generatedAt: string;
  investmentOpinion?: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL' | 'BUY' | 'SELL' | 'HOLD';
  summary: string;
  description: string;
  keyPoints: string[];
  disclaimer: string;
  technicalSignals?: {
    macroImpact?: string;
    newsAnalysis?: string;
    globalEventImpact?: string;
    sentimentReason?: string;
  };
}

export interface Prediction {
  stockCode: string;
  period: '1D' | '1W';
  direction: 'UP' | 'DOWN' | 'SIDEWAYS';
  confidence: number;
  reasons: string[];
  disclaimer: string;
  generatedAt: string;
  valuation: 'UNDERVALUED' | 'FAIR' | 'OVERVALUED';
}

export interface LearnArticle {
  id: number;
  title: string;
  category: 'BASICS' | 'NEWS_READING' | 'CHART_ANALYSIS' | 'VALUE_ANALYSIS' | 'MACRO_ECONOMY';
  difficulty: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  estimatedReadMinutes: number;
  content: string;
  isCompleted?: boolean;
}

export interface MacroIndicator {
  id: number;
  indicatorCode: string;
  indicatorName: string;
  value: number;
  unit: string;
  recordDate: string;
  source: string;
  changeAmount?: number | null;
  changeRate?: number | null;
}

export interface DartFundamentals {
  stockCode: string;
  stockName: string;
  bsnsYear: string;
  revenue: number | null;
  operatingProfit: number | null;
  netIncome: number | null;
  totalAssets: number | null;
  totalLiabilities: number | null;
  totalEquity: number | null;
  operatingCashFlow: number | null;
  operatingMargin: number | null;
  revenueGrowthRate: number | null;
  debtRatio: number | null;
  roe: number | null;
  evaluationTags?: string[];
  summaryComment?: string;
}

export interface MultiYearFundamentals {
  stockCode: string;
  stockName: string;
  yearlyData: {
    bsnsYear: string;
    revenue: number | null;
    operatingProfit: number | null;
    netIncome: number | null;
    operatingMargin: number | null;
    roe: number | null;
    debtRatio: number | null;
    totalAssets?: number | null;
    totalLiabilities?: number | null;
    totalEquity?: number | null;
    operatingCashFlow?: number | null;
  }[];
}

export interface SectorComparison {
  stockCode: string;
  stockName: string;
  sector: string;
  currentPer: number | null;
  currentPbr: number | null;
  sectorAvgPer: number | null;
  sectorAvgPbr: number | null;
  perEvaluation: string;
  pbrEvaluation: string;
  peerCount: number;
}

export interface NewsAnalysisResponse {
  newsId: number;
  title: string;
  source: string;
  publishedAt: string;
  originalUrl: string;
  analysis: {
    summary: string;
    marketImpact: string;
    relatedStocks: string[];
    sentiment: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
    keyTakeaway: string;
  };
}

export type EventType = 'GEOPOLITICAL' | 'FISCAL' | 'INDUSTRY' | 'BLACK_SWAN';

export interface GlobalEvent {
  id: number;
  eventType: string;
  title: string;
  summary: string;
  sourceUrl: string;
  sourceName: string;
  riskLevel: string;
  publishedAt: string;
}

export interface GlobalEventResponse {
  events: GlobalEvent[];
  totalCount: number;
  page: number;
  size: number;
}

export interface RiskIndicator {
  code: string;
  name: string;
  value: number;
  unit: string;
  changeAmount: number | null;
  changeRate: number | null;
  riskLevel: string;
  description: string;
  recordDate: string;
}

export interface MarketRiskResponse {
  overallRiskLevel: string;
  overallRiskScore: number;
  riskComment: string;
  indicators: RiskIndicator[];
  yieldCurveStatus: string;
  assessedAt: string;
}

export interface CoinData {
  symbol: string;
  name: string;
  priceUsd: number;
  priceKrw: number;
  marketCapUsd: number;
  volume24hUsd: number;
  change24h: number | null;
  change7d: number | null;
  recordedAt: string;
}

export interface CryptoMarketResponse {
  coins: CoinData[];
  updatedAt: string;
}

/** DART domestic disclosure item */
export interface DomesticDisclosure {
  id: number;
  stockCode: string;
  title: string;
  submitter: string;
  filingDate: string;
  disclosureType: string;
  dartUrl: string;
}

/** SEC EDGAR overseas disclosure item */
export interface OverseasDisclosure {
  id: number;
  stockCode: string;
  title: string;
  filingType: string;
  filingDate: string;
  edgarUrl: string;
}

export interface WatchlistItem {
  stockCode: string;
  addedAt: string;
}

export interface User {
  id: number;
  email: string;
  nickname: string;
  createdAt: string;
}

/** Tagged stock reference in news articles */
export interface TaggedStock {
  stockCode: string;
  stockName: string;
}

/** New news count since a given timestamp */
export interface NewNewsCount {
  count: number;
  since: string;
}

/** AI-generated stock news summary */
export interface StockNewsSummary {
  stockCode: string;
  summary: string;
  generatedAt: string;
  articleCount: number;
}

/** Single data point for sentiment trend */
export interface SentimentTrendPoint {
  date: string;
  score: number;
  articleCount: number;
}

/** Sentiment trend response */
export interface SentimentTrendResponse {
  stockCode: string;
  points: SentimentTrendPoint[];
  averageScore: number;
}

/** Alert keyword for custom keyword management */
export interface AlertKeyword {
  id: number;
  keyword: string;
  createdAt: string;
}

/** Alert keywords response */
export interface AlertKeywordsResponse {
  keywords: AlertKeyword[];
  maxCount: number;
}
