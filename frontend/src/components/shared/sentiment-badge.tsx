import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

const SENTIMENT_CONFIG = {
  POSITIVE: {
    label: '긍정',
    className: 'bg-red-50 text-red-600 border-red-200',
  },
  NEGATIVE: {
    label: '부정',
    className: 'bg-blue-50 text-blue-600 border-blue-200',
  },
  NEUTRAL: {
    label: '중립',
    className: 'bg-gray-50 text-gray-600 border-gray-200',
  },
};

export function SentimentBadge({
  sentiment,
}: {
  sentiment: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
}) {
  const config = SENTIMENT_CONFIG[sentiment] ?? SENTIMENT_CONFIG.NEUTRAL;

  return (
    <Badge
      variant="outline"
      className={cn('text-[10px] px-1.5 py-0', config.className)}
    >
      {config.label}
    </Badge>
  );
}
