'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { FinancialTerm } from '@/types';
import { apiClient } from '@/lib/api-client';

export function TermPopover({
  termName,
  children,
}: {
  termName: string;
  children?: React.ReactNode;
}) {
  const { data: terms } = useQuery<FinancialTerm[]>({
    queryKey: ['terms', 'search', termName],
    queryFn: () =>
      apiClient.get<FinancialTerm[]>(
        `/terms/search?q=${encodeURIComponent(termName)}`
      ),
    staleTime: 30 * 60 * 1000,
  });

  const term = terms?.find(
    (t) => t.name === termName || t.nameEn === termName
  );

  if (!term) {
    return <span>{children || termName}</span>;
  }

  return (
    <Popover>
      <PopoverTrigger asChild>
        <button
          className="border-b border-dotted border-gray-400 text-inherit hover:border-gray-600 transition-colors cursor-help"
          aria-label={`${termName} 용어 설명 보기`}
        >
          {children || termName}
        </button>
      </PopoverTrigger>
      <PopoverContent className="w-72 p-4" align="start">
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <span className="font-semibold text-sm">{term.name}</span>
            <span className="text-xs text-gray-400">{term.nameEn}</span>
          </div>
          <p className="text-sm text-gray-600 leading-relaxed">
            {term.simpleDescription}
          </p>
          <Link
            href={`/dictionary/${term.id}`}
            className="inline-block text-xs text-[#3182F6] hover:underline"
          >
            자세히 보기 &rarr;
          </Link>
        </div>
      </PopoverContent>
    </Popover>
  );
}
