'use client';

import { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import Link from 'next/link';
import { Search, X, BookOpen, GraduationCap, ChevronRight } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { DifficultyBadge } from '@/components/shared/difficulty-badge';
import { useTerms, useTermCategories } from '@/hooks/use-terms';
import {
  useBokTermsAll,
  useFilteredBokTerms,
  CHOSUNG_LIST,
  getChosung,
  getBokCategories,
} from '@/hooks/use-bok-terms';
import { cn } from '@/lib/utils';
import type { BokTerm, FinancialTerm } from '@/types';

const ITEMS_PER_PAGE = 30;

type TabType = 'bok' | 'basic';

function HighlightText({
  text,
  keyword,
  className,
}: {
  text: string;
  keyword: string;
  className?: string;
}) {
  if (!keyword || !keyword.trim()) {
    return <span className={className}>{text}</span>;
  }

  const kw = keyword.trim();
  const regex = new RegExp(`(${kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
  const parts = text.split(regex);

  return (
    <span className={className}>
      {parts.map((part, i) =>
        i % 2 === 1 ? (
          <mark
            key={i}
            className="bg-yellow-200 text-gray-900 rounded-sm px-0.5"
          >
            {part}
          </mark>
        ) : (
          <span key={i}>{part}</span>
        )
      )}
    </span>
  );
}

export default function DictionaryPage() {

  const [activeTab, setActiveTab] = useState<TabType>('bok');
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedChosung, setSelectedChosung] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [basicCategory, setBasicCategory] = useState('all');
  const [visibleCount, setVisibleCount] = useState(ITEMS_PER_PAGE);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const listRef = useRef<HTMLDivElement>(null);



  const { data: allBokTerms, isLoading: isBokLoading } = useBokTermsAll();

  const { data: categories } = useTermCategories();
  const { data: basicTerms, isLoading: isBasicLoading } = useTerms({
    category: basicCategory === 'all' ? undefined : basicCategory,
    search: activeTab === 'basic' ? debouncedSearch || undefined : undefined,
  });

  const bokCategories = useMemo(
    () => (allBokTerms ? getBokCategories(allBokTerms) : []),
    [allBokTerms]
  );

  const { filtered: filteredBokTerms, grouped: bokGrouped, chosungCounts } =
    useFilteredBokTerms(allBokTerms, {
      chosung: selectedChosung || undefined,
      keyword: debouncedSearch || undefined,
      category: selectedCategory === 'all' ? undefined : selectedCategory,
    });

  const bokGroupKeys = useMemo(() => {
    return Object.keys(bokGrouped).sort((a, b) => a.localeCompare(b, 'ko'));
  }, [bokGrouped]);

  const visibleBokTerms = useMemo(() => {
    return filteredBokTerms.slice(0, visibleCount);
  }, [filteredBokTerms, visibleCount]);

  const hasMore = visibleCount < filteredBokTerms.length;

  const basicGrouped = useMemo(() => {
    if (!basicTerms) return {};
    return basicTerms.reduce<Record<string, FinancialTerm[]>>((acc, term) => {
      const initial = getChosung(term.name[0]);
      if (!acc[initial]) acc[initial] = [];
      acc[initial].push(term);
      return acc;
    }, {});
  }, [basicTerms]);

  const basicGroupKeys = useMemo(() => {
    return Object.keys(basicGrouped).sort((a, b) => a.localeCompare(b, 'ko'));
  }, [basicGrouped]);


  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(search);
      setVisibleCount(ITEMS_PER_PAGE);
    }, 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [search]);


  useEffect(() => {
    setSearch('');
    setDebouncedSearch('');
    setSelectedChosung(null);
    setSelectedCategory('all');
    setBasicCategory('all');
    setVisibleCount(ITEMS_PER_PAGE);
  }, [activeTab]);


  useEffect(() => {
    setVisibleCount(ITEMS_PER_PAGE);
  }, [selectedChosung, selectedCategory, debouncedSearch]);


  const handleChosungClick = useCallback(
    (ch: string) => {
      if (selectedChosung === ch) {
        setSelectedChosung(null);
      } else {
        setSelectedChosung(ch);
      }

      listRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    },
    [selectedChosung]
  );


  const clearAllFilters = useCallback(() => {
    setSearch('');
    setDebouncedSearch('');
    setSelectedChosung(null);
    setSelectedCategory('all');
    setVisibleCount(ITEMS_PER_PAGE);
  }, []);

  const isFiltering =
    !!debouncedSearch || !!selectedChosung || selectedCategory !== 'all';


  return (
    <div className="mx-auto max-w-screen-xl px-4 py-4 md:px-6 md:py-6">

      <div className="mb-5">
        <h1 className="text-xl font-bold text-gray-900 md:text-2xl">
          금융 용어 사전
        </h1>
        <p className="mt-1 text-sm text-gray-500">
          경제금융 용어를 쉽고 빠르게 찾아보세요
        </p>
      </div>

      <div className="mb-4 flex rounded-xl bg-white p-1 shadow-sm">
        <TabButton
          active={activeTab === 'bok'}
          onClick={() => setActiveTab('bok')}
          icon={<BookOpen className="h-4 w-4" />}
          label="경제금융용어"
          count={allBokTerms?.length}
        />
        <TabButton
          active={activeTab === 'basic'}
          onClick={() => setActiveTab('basic')}
          icon={<GraduationCap className="h-4 w-4" />}
          label="기초 용어"
          count={basicTerms?.length}
        />
      </div>

      <div className="relative mb-4">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <Input
          type="text"
          placeholder={
            activeTab === 'bok'
              ? '경제금융용어 검색 (한글, 영문, 내용)'
              : '기초 용어 검색'
          }
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="h-11 rounded-xl border-gray-200 bg-white pl-10 pr-10 text-sm shadow-sm placeholder:text-gray-400 focus:border-blue-300 focus:ring-blue-200"
          aria-label="용어 검색"
        />
        {search && (
          <button
            onClick={() => {
              setSearch('');
              setDebouncedSearch('');
            }}
            className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-0.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
            aria-label="검색어 삭제"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      {activeTab === 'bok' && (
        <>

          <ChosungNav
            selected={selectedChosung}
            onSelect={handleChosungClick}
            counts={chosungCounts}
          />

          {bokCategories.length > 1 && (
            <div className="mb-4 flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
              <FilterChip
                label="전체"
                active={selectedCategory === 'all'}
                onClick={() => setSelectedCategory('all')}
              />
              {bokCategories.map((cat) => (
                <FilterChip
                  key={cat}
                  label={cat}
                  active={selectedCategory === cat}
                  onClick={() => setSelectedCategory(cat)}
                />
              ))}
            </div>
          )}

          {!isBokLoading && (
            <div className="mb-3 flex items-center justify-between">
              <p className="text-xs text-gray-500">
                {isFiltering ? (
                  <>
                    <span className="font-semibold text-gray-700">
                      {filteredBokTerms.length}
                    </span>
                    개 결과
                    {selectedChosung && (
                      <span className="ml-1.5 text-gray-400">
                        ('{selectedChosung}' 초성)
                      </span>
                    )}
                  </>
                ) : (
                  <>
                    전체{' '}
                    <span className="font-semibold text-gray-700">
                      {allBokTerms?.length || 0}
                    </span>
                    개 용어
                  </>
                )}
              </p>
              {isFiltering && (
                <button
                  onClick={clearAllFilters}
                  className="text-xs text-blue-500 hover:text-blue-700"
                >
                  필터 초기화
                </button>
              )}
            </div>
          )}

          <div ref={listRef}>
            {isBokLoading ? (
              <LoadingSkeleton count={8} />
            ) : filteredBokTerms.length > 0 ? (
              <>

                {selectedChosung || debouncedSearch ? (
                  <div className="space-y-2">
                    {visibleBokTerms.map((term) => (
                      <BokTermCard
                        key={term.id}
                        term={term}
                        keyword={debouncedSearch}
                      />
                    ))}
                  </div>
                ) : (

                  <div className="space-y-5">
                    {bokGroupKeys.map((group) => (
                      <BokTermGroup
                        key={group}
                        groupKey={group}
                        terms={bokGrouped[group]}
                        keyword={debouncedSearch}
                      />
                    ))}
                  </div>
                )}

                {hasMore && (selectedChosung || debouncedSearch) && (
                  <div className="mt-4 flex justify-center">
                    <Button
                      variant="outline"
                      size="sm"
                      className="w-full max-w-xs rounded-xl text-sm text-gray-600"
                      onClick={() =>
                        setVisibleCount((c) => c + ITEMS_PER_PAGE)
                      }
                    >
                      더 보기 ({filteredBokTerms.length - visibleCount}개 남음)
                    </Button>
                  </div>
                )}
              </>
            ) : (
              <EmptyState
                search={search}
                isFiltering={isFiltering}
                onReset={clearAllFilters}
              />
            )}
          </div>
        </>
      )}

      {activeTab === 'basic' && (
        <>

          <div className="mb-4 flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
            <FilterChip
              label="전체"
              active={basicCategory === 'all'}
              onClick={() => setBasicCategory('all')}
            />
            {categories?.map((cat) => (
              <FilterChip
                key={cat.id}
                label={cat.name}
                active={basicCategory === String(cat.id)}
                onClick={() => setBasicCategory(String(cat.id))}
              />
            ))}
          </div>

          {!isBasicLoading && basicTerms && (
            <p className="mb-3 text-xs text-gray-500">
              전체{' '}
              <span className="font-semibold text-gray-700">
                {basicTerms.length}
              </span>
              개 용어
            </p>
          )}

          {isBasicLoading ? (
            <LoadingSkeleton count={6} />
          ) : basicTerms && basicTerms.length > 0 ? (
            <div className="space-y-5">
              {basicGroupKeys.map((initial) => (
                <div key={initial}>
                  <div className="mb-2 flex items-center gap-2">
                    <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-50 text-xs font-bold text-blue-600">
                      {initial}
                    </span>
                    <div className="h-px flex-1 bg-gray-100" />
                  </div>
                  <div className="overflow-hidden rounded-xl bg-white shadow-sm">
                    {basicGrouped[initial]?.map((term, idx) => (
                      <Link
                        key={term.id}
                        href={`/dictionary/${term.id}`}
                        className={cn(
                          'flex items-center gap-3 px-4 py-3.5 transition-colors hover:bg-gray-50 active:bg-gray-100',
                          idx > 0 && 'border-t border-gray-50'
                        )}
                      >
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center gap-2">
                            <HighlightText
                              text={term.name}
                              keyword={debouncedSearch}
                              className="text-sm font-semibold text-gray-900"
                            />
                            <DifficultyBadge difficulty={term.difficulty} />
                          </div>
                          <p className="mt-1 text-xs leading-relaxed text-gray-500 line-clamp-1">
                            {term.simpleDescription}
                          </p>
                        </div>
                        <ChevronRight className="h-4 w-4 shrink-0 text-gray-300" />
                      </Link>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              search={search}
              isFiltering={!!debouncedSearch || basicCategory !== 'all'}
              onReset={() => {
                setSearch('');
                setDebouncedSearch('');
                setBasicCategory('all');
              }}
            />
          )}
        </>
      )}
    </div>
  );
}

function TabButton({
  active,
  onClick,
  icon,
  label,
  count,
}: {
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
  count?: number;
}) {
  return (
    <button
      className={cn(
        'flex flex-1 items-center justify-center gap-1.5 rounded-lg py-2.5 text-sm font-medium transition-all',
        active
          ? 'bg-gray-900 text-white shadow-sm'
          : 'text-gray-500 hover:text-gray-700'
      )}
      onClick={onClick}
    >
      {icon}
      <span>{label}</span>
      {count !== undefined && (
        <span
          className={cn(
            'ml-0.5 text-[10px]',
            active ? 'text-gray-300' : 'text-gray-400'
          )}
        >
          {count}
        </span>
      )}
    </button>
  );
}

function ChosungNav({
  selected,
  onSelect,
  counts,
}: {
  selected: string | null;
  onSelect: (ch: string) => void;
  counts: Record<string, number>;
}) {
  return (
    <div className="mb-4 overflow-x-auto scrollbar-hide">
      <div className="flex gap-1 pb-1">
        {CHOSUNG_LIST.map((ch) => {
          const count = counts[ch] || 0;
          const isActive = selected === ch;
          const hasTerms = count > 0;
          return (
            <button
              key={ch}
              onClick={() => hasTerms && onSelect(ch)}
              disabled={!hasTerms}
              className={cn(
                'flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-sm font-semibold transition-all',
                isActive
                  ? 'bg-gray-900 text-white shadow-sm'
                  : hasTerms
                    ? 'bg-white text-gray-700 shadow-sm hover:bg-gray-50 active:bg-gray-100'
                    : 'bg-transparent text-gray-300 cursor-default'
              )}
              aria-label={`초성 ${ch} (${count}개)`}
              title={`${ch} - ${count}개`}
            >
              {ch}
            </button>
          );
        })}

        <button
          onClick={() => {

            const hasEng = Object.keys(counts).some((k) => /[A-Z]/.test(k));
            if (hasEng) onSelect('ENG');
          }}
          className={cn(
            'flex h-9 shrink-0 items-center justify-center rounded-lg px-2.5 text-xs font-semibold transition-all',
            selected === 'ENG'
              ? 'bg-gray-900 text-white shadow-sm'
              : 'bg-white text-gray-700 shadow-sm hover:bg-gray-50 active:bg-gray-100'
          )}
          aria-label="영문 용어"
          title="영문 시작 용어"
        >
          A-Z
        </button>
      </div>
    </div>
  );
}

function FilterChip({
  label,
  active,
  onClick,
}: {
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      className={cn(
        'shrink-0 rounded-full px-3 py-1.5 text-xs font-medium transition-all',
        active
          ? 'bg-gray-900 text-white'
          : 'bg-white text-gray-600 shadow-sm hover:bg-gray-50'
      )}
      onClick={onClick}
    >
      {label}
    </button>
  );
}

function BokTermGroup({
  groupKey,
  terms,
  keyword,
}: {
  groupKey: string;
  terms: BokTerm[];
  keyword: string;
}) {
  return (
    <div>

      <div className="mb-2 flex items-center gap-2">
        <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-50 text-xs font-bold text-blue-600">
          {groupKey}
        </span>
        <span className="text-xs text-gray-400">{terms.length}개</span>
        <div className="h-px flex-1 bg-gray-100" />
      </div>

      <div className="space-y-2">
        {terms.map((term) => (
          <BokTermCard key={term.id} term={term} keyword={keyword} />
        ))}
      </div>
    </div>
  );
}

function BokTermCard({
  term,
  keyword,
}: {
  term: BokTerm;
  keyword: string;
}) {
  return (
    <Link href={`/dictionary/bok/${term.id}`} className="block">
      <div className="rounded-xl bg-white p-4 shadow-sm transition-all hover:shadow-md active:bg-gray-50">

        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <HighlightText
                text={term.term}
                keyword={keyword}
                className="text-sm font-semibold text-gray-900"
              />
              {term.englishTerm && (
                <span className="shrink-0 text-xs text-gray-400">
                  {term.englishTerm}
                </span>
              )}
            </div>
          </div>
          <ChevronRight className="mt-0.5 h-4 w-4 shrink-0 text-gray-300" />
        </div>

        <p className="mt-1.5 text-xs leading-relaxed text-gray-500 line-clamp-2">
          {keyword ? (
            <HighlightText text={term.definition} keyword={keyword} />
          ) : (
            term.definition
          )}
        </p>

        {term.category && term.category !== '경제금융일반' && (
          <div className="mt-2">
            <Badge
              variant="outline"
              className="border-gray-200 bg-gray-50 px-2 py-0.5 text-[10px] font-normal text-gray-500"
            >
              {term.category}
            </Badge>
          </div>
        )}
      </div>
    </Link>
  );
}

function LoadingSkeleton({ count = 6 }: { count?: number }) {
  return (
    <div className="space-y-2">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="rounded-xl bg-white p-4 shadow-sm">
          <div className="flex items-center gap-2 mb-2">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-3 w-16" />
          </div>
          <Skeleton className="h-3 w-full mb-1" />
          <Skeleton className="h-3 w-3/4" />
          <Skeleton className="mt-2 h-4 w-16 rounded-full" />
        </div>
      ))}
    </div>
  );
}

function EmptyState({
  search,
  isFiltering,
  onReset,
}: {
  search: string;
  isFiltering: boolean;
  onReset: () => void;
}) {
  return (
    <div className="flex flex-col items-center gap-4 rounded-xl bg-white py-16 shadow-sm">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gray-100">
        <Search className="h-5 w-5 text-gray-400" />
      </div>
      <div className="text-center">
        <p className="text-sm font-medium text-gray-700">
          {search ? `"${search}"에 대한 검색 결과가 없습니다` : '결과가 없습니다'}
        </p>
        <p className="mt-1 text-xs text-gray-400">
          다른 검색어나 필터를 시도해보세요
        </p>
      </div>
      {isFiltering && (
        <Button
          variant="outline"
          size="sm"
          className="rounded-full text-xs"
          onClick={onReset}
        >
          필터 초기화
        </Button>
      )}
    </div>
  );
}
