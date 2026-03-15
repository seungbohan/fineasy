'use client';

import { useMemo } from 'react';
import type { SentimentTrendPoint } from '@/types';

/**
 * SVG-based sentiment trend line chart.
 * Displays sentiment score (0~1) over time with positive/negative area fills.
 * Neutral threshold at 0.5.
 */
export function SentimentTrendChart({
  points,
  width = 600,
  height = 200,
}: {
  points: SentimentTrendPoint[];
  width?: number;
  height?: number;
}) {
  const paddingTop = 20;
  const paddingBottom = 30;
  const paddingLeft = 36;
  const paddingRight = 12;
  const chartWidth = width - paddingLeft - paddingRight;
  const chartHeight = height - paddingTop - paddingBottom;

  const midY = paddingTop + chartHeight * 0.5;

  const pathData = useMemo(() => {
    if (points.length < 2) return null;

    const coords = points.map((p, i) => ({
      x: paddingLeft + (i / (points.length - 1)) * chartWidth,
      y: paddingTop + (1 - p.score) * chartHeight,
    }));

    /* Main line path */
    const linePath = coords
      .map((c, i) => `${i === 0 ? 'M' : 'L'} ${c.x} ${c.y}`)
      .join(' ');

    /* Positive area (above 0.5, below the line) - clipped to top half */
    const positiveArea =
      linePath +
      ` L ${coords[coords.length - 1].x} ${midY}` +
      ` L ${coords[0].x} ${midY} Z`;

    /* Negative area (below 0.5, above the line) - clipped to bottom half */
    const negativeArea =
      linePath +
      ` L ${coords[coords.length - 1].x} ${midY}` +
      ` L ${coords[0].x} ${midY} Z`;

    return { linePath, positiveArea, negativeArea, coords };
  }, [points, chartWidth, chartHeight, paddingLeft, paddingTop, midY]);

  if (!pathData || points.length < 2) {
    return (
      <div className="flex h-[200px] items-center justify-center text-sm text-gray-400">
        감성 데이터가 부족합니다
      </div>
    );
  }

  /* Date labels - show first, middle, last */
  const dateLabels = [
    { idx: 0, label: formatShortDate(points[0].date) },
    {
      idx: Math.floor(points.length / 2),
      label: formatShortDate(points[Math.floor(points.length / 2)].date),
    },
    {
      idx: points.length - 1,
      label: formatShortDate(points[points.length - 1].date),
    },
  ];

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className="w-full"
      preserveAspectRatio="xMidYMid meet"
      role="img"
      aria-label="뉴스 감성 트렌드 차트"
    >
      <defs>
        {/* Clip for positive region (top half) */}
        <clipPath id="clip-positive">
          <rect
            x={paddingLeft}
            y={paddingTop}
            width={chartWidth}
            height={chartHeight / 2}
          />
        </clipPath>
        {/* Clip for negative region (bottom half) */}
        <clipPath id="clip-negative">
          <rect
            x={paddingLeft}
            y={midY}
            width={chartWidth}
            height={chartHeight / 2}
          />
        </clipPath>
      </defs>

      {/* Grid lines */}
      <line
        x1={paddingLeft}
        y1={midY}
        x2={paddingLeft + chartWidth}
        y2={midY}
        stroke="#E5E7EB"
        strokeWidth={1}
        strokeDasharray="4 4"
      />
      <line
        x1={paddingLeft}
        y1={paddingTop}
        x2={paddingLeft + chartWidth}
        y2={paddingTop}
        stroke="#F3F4F6"
        strokeWidth={0.5}
      />
      <line
        x1={paddingLeft}
        y1={paddingTop + chartHeight}
        x2={paddingLeft + chartWidth}
        y2={paddingTop + chartHeight}
        stroke="#F3F4F6"
        strokeWidth={0.5}
      />

      {/* Y-axis labels */}
      <text
        x={paddingLeft - 6}
        y={paddingTop + 4}
        textAnchor="end"
        className="fill-gray-400 text-[10px]"
      >
        1.0
      </text>
      <text
        x={paddingLeft - 6}
        y={midY + 3}
        textAnchor="end"
        className="fill-gray-400 text-[10px]"
      >
        0.5
      </text>
      <text
        x={paddingLeft - 6}
        y={paddingTop + chartHeight + 4}
        textAnchor="end"
        className="fill-gray-400 text-[10px]"
      >
        0.0
      </text>

      {/* Positive area fill (red/positive) - clipped to top half */}
      <path
        d={pathData.positiveArea}
        fill="rgba(240, 68, 82, 0.08)"
        clipPath="url(#clip-positive)"
      />

      {/* Negative area fill (blue/negative) - clipped to bottom half */}
      <path
        d={pathData.negativeArea}
        fill="rgba(49, 130, 246, 0.08)"
        clipPath="url(#clip-negative)"
      />

      {/* Main line */}
      <path
        d={pathData.linePath}
        fill="none"
        stroke="#6B7280"
        strokeWidth={1.5}
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      {/* Data point dots (show only a few) */}
      {pathData.coords
        .filter(
          (_, i) =>
            i === 0 ||
            i === pathData.coords.length - 1 ||
            i % Math.max(1, Math.floor(pathData.coords.length / 8)) === 0
        )
        .map((c, i) => {
          const point = points[
            i === 0
              ? 0
              : i === pathData.coords.length - 1
                ? points.length - 1
                : i * Math.max(1, Math.floor(points.length / 8))
          ] ?? points[0];
          const dotColor = point.score >= 0.5 ? '#F04452' : '#3182F6';
          return (
            <circle
              key={i}
              cx={c.x}
              cy={c.y}
              r={2.5}
              fill="white"
              stroke={dotColor}
              strokeWidth={1.5}
            />
          );
        })}

      {/* X-axis date labels */}
      {dateLabels.map((dl) => (
        <text
          key={dl.idx}
          x={pathData.coords[dl.idx].x}
          y={height - 6}
          textAnchor="middle"
          className="fill-gray-400 text-[10px]"
        >
          {dl.label}
        </text>
      ))}

      {/* Legend */}
      <rect x={paddingLeft} y={height - 14} width={8} height={8} rx={2} fill="rgba(240, 68, 82, 0.3)" />
      <text x={paddingLeft + 12} y={height - 7} className="fill-gray-500 text-[9px]">
        긍정
      </text>
      <rect x={paddingLeft + 38} y={height - 14} width={8} height={8} rx={2} fill="rgba(49, 130, 246, 0.3)" />
      <text x={paddingLeft + 50} y={height - 7} className="fill-gray-500 text-[9px]">
        부정
      </text>
    </svg>
  );
}

function formatShortDate(dateStr: string): string {
  const d = new Date(dateStr);
  return `${d.getMonth() + 1}/${d.getDate()}`;
}
