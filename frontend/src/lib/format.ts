export function formatPrice(price: number, currency: 'KRW' | 'USD' = 'KRW'): string {
  if (currency === 'USD') {
    return `$${price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
  return price.toLocaleString('ko-KR');
}

export function formatChange(change: number, currency: 'KRW' | 'USD' = 'KRW'): string {
  const sign = change > 0 ? '+' : '';
  if (currency === 'USD') {
    const absFormatted = Math.abs(change).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return change < 0 ? `-$${absFormatted}` : `${sign}$${absFormatted}`;
  }
  return `${sign}${change.toLocaleString('ko-KR')}`;
}

export function formatChangeRate(rate: number): string {
  const sign = rate > 0 ? '+' : '';
  return `${sign}${rate.toFixed(2)}%`;
}

export function getPriceColorClass(change: number): string {
  if (change > 0) return 'text-up';
  if (change < 0) return 'text-down';
  return 'text-flat';
}

export function getPriceArrow(change: number): string {
  if (change > 0) return '\u25B2';
  if (change < 0) return '\u25BC';
  return '-';
}

export function formatVolume(volume: number): string {
  if (volume >= 100000000) {
    return `${(volume / 100000000).toFixed(1)}억`;
  }
  if (volume >= 10000) {
    return `${Math.round(volume / 10000).toLocaleString('ko-KR')}만`;
  }
  return volume.toLocaleString('ko-KR');
}

export function formatTradingValue(value: number, currency: 'KRW' | 'USD' = 'KRW'): string {
  if (!value) return '-';

  if (currency === 'USD') {
    if (value >= 1_000_000_000) {
      return `$${(value / 1_000_000_000).toFixed(1)}B`;
    }
    if (value >= 1_000_000) {
      return `$${(value / 1_000_000).toFixed(1)}M`;
    }
    if (value >= 1_000) {
      return `$${(value / 1_000).toFixed(1)}K`;
    }
    return `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  if (value >= 1000000000000) {
    return `${(value / 1000000000000).toFixed(1)}조`;
  }
  if (value >= 100000000) {
    return `${Math.round(value / 100000000).toLocaleString('ko-KR')}억`;
  }
  if (value >= 10000) {
    return `${Math.round(value / 10000).toLocaleString('ko-KR')}만`;
  }
  return value.toLocaleString('ko-KR');
}

export function formatRelativeTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  const diffHour = Math.floor(diffMs / 3600000);
  const diffDay = Math.floor(diffMs / 86400000);

  if (diffMin < 1) return '방금 전';
  if (diffMin < 60) return `${diffMin}분 전`;
  if (diffHour < 24) return `${diffHour}시간 전`;
  if (diffDay < 7) return `${diffDay}일 전`;

  return date.toLocaleDateString('ko-KR', {
    month: 'long',
    day: 'numeric',
  });
}

export function getCurrencyFromMarket(market: string): 'KRW' | 'USD' {
  if (market === 'NASDAQ' || market === 'NYSE') return 'USD';
  return 'KRW';
}

export function getCurrencyLabel(currency: 'KRW' | 'USD'): string {
  return currency === 'USD' ? '$' : '원';
}

export function formatIndexValue(value: number): string {
  if (value >= 10000) {
    return value.toLocaleString('ko-KR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  }
  return value.toLocaleString('ko-KR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}
