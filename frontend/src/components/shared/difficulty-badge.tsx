import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

const DIFFICULTY_CONFIG = {
  BEGINNER: {
    label: '초급',
    className: 'bg-green-50 text-green-700 border-green-200',
  },
  INTERMEDIATE: {
    label: '중급',
    className: 'bg-yellow-50 text-yellow-700 border-yellow-200',
  },
  ADVANCED: {
    label: '고급',
    className: 'bg-red-50 text-red-700 border-red-200',
  },
};

export function DifficultyBadge({
  difficulty,
}: {
  difficulty: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
}) {
  const config = DIFFICULTY_CONFIG[difficulty];

  return (
    <Badge
      variant="outline"
      className={cn('text-[10px] px-1.5 py-0', config.className)}
    >
      {config.label}
    </Badge>
  );
}
