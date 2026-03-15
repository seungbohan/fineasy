'use client';

import { use } from 'react';
import Link from 'next/link';
import { ArrowLeft, Clock } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { DifficultyBadge } from '@/components/shared/difficulty-badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useLearnArticle } from '@/hooks/use-learn';

function SimpleMarkdown({ content }: { content: string }) {
  const lines = content.split('\n');
  const elements: React.ReactNode[] = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    if (line.trim() === '') {
      i++;
      continue;
    }

    if (line.startsWith('# ')) {
      elements.push(
        <h1
          key={i}
          className="mt-6 mb-3 text-xl font-bold text-gray-900"
        >
          {line.slice(2)}
        </h1>
      );
      i++;
      continue;
    }
    if (line.startsWith('## ')) {
      elements.push(
        <h2
          key={i}
          className="mt-5 mb-2 text-lg font-semibold text-gray-900"
        >
          {line.slice(3)}
        </h2>
      );
      i++;
      continue;
    }
    if (line.startsWith('### ')) {
      elements.push(
        <h3
          key={i}
          className="mt-4 mb-2 text-base font-semibold text-gray-900"
        >
          {line.slice(4)}
        </h3>
      );
      i++;
      continue;
    }

    if (line.trim() === '---') {
      elements.push(
        <hr key={i} className="my-4 border-gray-200" />
      );
      i++;
      continue;
    }

    if (line.startsWith('> ')) {
      elements.push(
        <blockquote
          key={i}
          className="my-3 border-l-4 border-[#3182F6] bg-blue-50/50 py-2 pl-4 pr-3 text-sm text-gray-700 italic"
        >
          {renderInlineFormatting(line.slice(2))}
        </blockquote>
      );
      i++;
      continue;
    }

    if (line.startsWith('- ')) {
      const listItems: React.ReactNode[] = [];
      while (i < lines.length && lines[i].startsWith('- ')) {
        listItems.push(
          <li key={i} className="text-sm text-gray-700 leading-relaxed">
            {renderInlineFormatting(lines[i].slice(2))}
          </li>
        );
        i++;
      }
      elements.push(
        <ul
          key={`ul-${i}`}
          className="my-2 ml-4 list-disc space-y-1"
        >
          {listItems}
        </ul>
      );
      continue;
    }

    if (line.includes('|') && line.trim().startsWith('|')) {
      const tableRows: string[][] = [];
      while (i < lines.length && lines[i].includes('|')) {
        const cells = lines[i]
          .split('|')
          .filter((c) => c.trim() !== '')
          .map((c) => c.trim());
        if (!/^[-: ]+$/.test(cells.join(''))) {
          tableRows.push(cells);
        }
        i++;
      }

      if (tableRows.length > 0) {
        const [header, ...body] = tableRows;
        elements.push(
          <div key={`table-${i}`} className="my-3 overflow-x-auto">
            <table className="w-full text-sm border-collapse">
              <thead>
                <tr>
                  {header.map((cell, ci) => (
                    <th
                      key={ci}
                      className="border-b border-gray-200 bg-gray-50 px-3 py-2 text-left font-medium text-gray-700"
                    >
                      {cell}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {body.map((row, ri) => (
                  <tr key={ri}>
                    {row.map((cell, ci) => (
                      <td
                        key={ci}
                        className="border-b border-gray-100 px-3 py-2 text-gray-600"
                      >
                        {cell}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );
      }
      continue;
    }

    elements.push(
      <p
        key={i}
        className="my-2 text-sm leading-relaxed text-gray-700"
      >
        {renderInlineFormatting(line)}
      </p>
    );
    i++;
  }

  return <div>{elements}</div>;
}

function renderInlineFormatting(text: string): React.ReactNode {
  const parts = text.split(/(\*\*[^*]+\*\*)/g);
  return parts.map((part, idx) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return (
        <strong key={idx} className="font-semibold text-gray-900">
          {part.slice(2, -2)}
        </strong>
      );
    }
    return part;
  });
}

export default function LearnArticlePage({
  params,
}: {
  params: Promise<{ articleId: string }>;
}) {
  const { articleId } = use(params);
  const id = parseInt(articleId, 10);
  const { data: article, isLoading } = useLearnArticle(id);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-screen-lg p-4 space-y-4">
        <Skeleton className="h-6 w-32" />
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (!article) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <p className="text-lg font-medium text-gray-900">
          아티클을 찾을 수 없습니다
        </p>
        <Link href="/learn">
          <Button variant="outline">학습 센터로 돌아가기</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-screen-lg p-4 md:p-6">
      <Link
        href="/learn"
        className="mb-4 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 transition-colors"
      >
        <ArrowLeft className="h-4 w-4" />
        학습 센터
      </Link>

      <div className="mb-6">
        <h1 className="text-xl font-bold text-gray-900 mb-2">
          {article.title}
        </h1>
        <div className="flex items-center gap-3">
          <DifficultyBadge difficulty={article.difficulty} />
          <span className="flex items-center gap-1 text-xs text-gray-400">
            <Clock className="h-3 w-3" />
            {article.estimatedReadMinutes}분 읽기
          </span>
        </div>
      </div>

      <div className="rounded-xl bg-white p-5 md:p-8">
        <SimpleMarkdown content={article.content} />
      </div>
    </div>
  );
}
