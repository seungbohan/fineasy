'use client';

import React, { useMemo } from 'react';
import { TermPopover } from '@/components/shared/term-popover';
import { useTerms } from '@/hooks/use-terms';

/**
 * Automatically detects financial terms in text and wraps them
 * with TermPopover for inline definitions.
 *
 * @param text - The text to scan for financial terms
 */
export function TermHighlighter({ text }: { text: string }) {
  const { data: terms } = useTerms();

  const highlighted = useMemo(() => {
    if (!terms || terms.length === 0) return [text];

    /* Sort terms by name length (longest first) to avoid partial matches */
    const sortedTerms = [...terms].sort(
      (a, b) => b.name.length - a.name.length
    );

    /* Build a single regex pattern from all term names */
    const escapedNames = sortedTerms
      .map((t) => t.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
      .filter((n) => n.length >= 2); // skip single-character terms

    if (escapedNames.length === 0) return [text];

    const pattern = new RegExp(`(${escapedNames.join('|')})`, 'g');
    const parts = text.split(pattern);

    return parts.map((part, idx) => {
      const matchedTerm = sortedTerms.find((t) => t.name === part);
      if (matchedTerm) {
        return (
          <TermPopover key={`${part}-${idx}`} termName={matchedTerm.name}>
            {part}
          </TermPopover>
        );
      }
      return part;
    });
  }, [text, terms]);

  return <>{highlighted}</>;
}
