import { Stock, StockCandle } from '@/types';

export const MOCK_STOCKS: Stock[] = [
  {
    stockCode: '005930',
    stockName: '삼성전자',
    market: 'KRX',
    sector: '반도체',
    currentPrice: 74200,
    changeAmount: 1400,
    changeRate: 1.92,
    volume: 18523400,
    tradingValue: 1374636480000,
    marketCap: '442조 8천억',
    per: 14.8,
    pbr: 1.32,
    eps: 5012,
    dividendYield: 2.16,
    high52w: 88800,
    low52w: 64500,
  },
  {
    stockCode: '000660',
    stockName: 'SK하이닉스',
    market: 'KRX',
    sector: '반도체',
    currentPrice: 196500,
    changeAmount: -2500,
    changeRate: -1.26,
    volume: 4210800,
    tradingValue: 827422200000,
    marketCap: '143조 1천억',
    per: 22.4,
    pbr: 2.84,
    eps: 8772,
    dividendYield: 0.51,
    high52w: 238000,
    low52w: 127000,
  },
  {
    stockCode: '035420',
    stockName: 'NAVER',
    market: 'KRX',
    sector: 'IT서비스',
    currentPrice: 185500,
    changeAmount: 3000,
    changeRate: 1.64,
    volume: 612400,
    tradingValue: 113600200000,
    marketCap: '30조 4천억',
    per: 28.6,
    pbr: 1.87,
    eps: 6486,
    dividendYield: 0.27,
    high52w: 225000,
    low52w: 148200,
  },
  {
    stockCode: '035720',
    stockName: '카카오',
    market: 'KRX',
    sector: 'IT서비스',
    currentPrice: 42350,
    changeAmount: -650,
    changeRate: -1.51,
    volume: 3842100,
    tradingValue: 162712935000,
    marketCap: '18조 8천억',
    per: 45.2,
    pbr: 1.43,
    eps: 937,
    dividendYield: 0.0,
    high52w: 62000,
    low52w: 34100,
  },
  {
    stockCode: '373220',
    stockName: 'LG에너지솔루션',
    market: 'KRX',
    sector: '이차전지',
    currentPrice: 348000,
    changeAmount: 5000,
    changeRate: 1.46,
    volume: 421300,
    tradingValue: 146612400000,
    marketCap: '81조 5천억',
    per: 68.4,
    pbr: 4.12,
    eps: 5088,
    dividendYield: 0.0,
    high52w: 453000,
    low52w: 290000,
  },
  {
    stockCode: '005380',
    stockName: '현대차',
    market: 'KRX',
    sector: '자동차',
    currentPrice: 218500,
    changeAmount: 1500,
    changeRate: 0.69,
    volume: 728900,
    tradingValue: 159264650000,
    marketCap: '46조 7천억',
    per: 6.8,
    pbr: 0.76,
    eps: 32132,
    dividendYield: 2.74,
    high52w: 284000,
    low52w: 184000,
  },
  {
    stockCode: '006400',
    stockName: '삼성SDI',
    market: 'KRX',
    sector: '이차전지',
    currentPrice: 265000,
    changeAmount: -4000,
    changeRate: -1.49,
    volume: 312600,
    tradingValue: 82839000000,
    marketCap: '18조 2천억',
    per: 18.9,
    pbr: 1.08,
    eps: 14022,
    dividendYield: 0.75,
    high52w: 412000,
    low52w: 220000,
  },
  {
    stockCode: '000270',
    stockName: '기아',
    market: 'KRX',
    sector: '자동차',
    currentPrice: 92400,
    changeAmount: 800,
    changeRate: 0.87,
    volume: 1124500,
    tradingValue: 103903800000,
    marketCap: '37조 6천억',
    per: 5.2,
    pbr: 0.92,
    eps: 17769,
    dividendYield: 4.33,
    high52w: 129600,
    low52w: 78500,
  },
  {
    stockCode: '005490',
    stockName: 'POSCO홀딩스',
    market: 'KRX',
    sector: '철강',
    currentPrice: 321000,
    changeAmount: 0,
    changeRate: 0.0,
    volume: 215300,
    tradingValue: 69111300000,
    marketCap: '27조 9천억',
    per: 12.4,
    pbr: 0.56,
    eps: 25887,
    dividendYield: 3.12,
    high52w: 454000,
    low52w: 262000,
  },
  {
    stockCode: '068270',
    stockName: '셀트리온',
    market: 'KRX',
    sector: '바이오',
    currentPrice: 175800,
    changeAmount: 2800,
    changeRate: 1.62,
    volume: 892400,
    tradingValue: 156924120000,
    marketCap: '23조 5천억',
    per: 32.6,
    pbr: 3.41,
    eps: 5392,
    dividendYield: 0.57,
    high52w: 214500,
    low52w: 134000,
  },
];

function generateCandles(
  basePrice: number,
  count: number,
  startDate: Date
): StockCandle[] {
  const candles: StockCandle[] = [];
  let price = basePrice;

  for (let i = 0; i < count; i++) {
    const date = new Date(startDate);
    date.setDate(date.getDate() + i);

    if (date.getDay() === 0 || date.getDay() === 6) continue;

    const volatility = basePrice * 0.015;
    const change = (Math.random() - 0.48) * volatility;
    const open = price;
    const close = Math.round(price + change);
    const high = Math.round(Math.max(open, close) + Math.random() * volatility * 0.5);
    const low = Math.round(Math.min(open, close) - Math.random() * volatility * 0.5);
    const volume = Math.round(500000 + Math.random() * 2000000);

    candles.push({
      date: date.toISOString().split('T')[0],
      open,
      high,
      low,
      close,
      volume,
    });

    price = close;
  }

  return candles;
}

const ONE_YEAR_AGO = new Date('2024-02-01');

export const MOCK_CANDLES: Record<string, StockCandle[]> = {
  '005930': generateCandles(71000, 280, ONE_YEAR_AGO),
  '000660': generateCandles(188000, 280, ONE_YEAR_AGO),
  '035420': generateCandles(178000, 280, ONE_YEAR_AGO),
  '035720': generateCandles(46000, 280, ONE_YEAR_AGO),
  '373220': generateCandles(340000, 280, ONE_YEAR_AGO),
  '005380': generateCandles(210000, 280, ONE_YEAR_AGO),
  '006400': generateCandles(275000, 280, ONE_YEAR_AGO),
  '000270': generateCandles(88000, 280, ONE_YEAR_AGO),
  '005490': generateCandles(315000, 280, ONE_YEAR_AGO),
  '068270': generateCandles(168000, 280, ONE_YEAR_AGO),
};
