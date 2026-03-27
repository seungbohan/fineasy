import Link from 'next/link';

export function Footer() {
  return (
    <footer className="hidden md:block border-t border-gray-100 bg-gray-50/50">
      <div className="mx-auto max-w-screen-xl px-4 py-8">
        <div className="flex flex-col gap-4 md:flex-row md:justify-between">
          <div>
            <p className="text-sm font-bold text-gray-700">FinEasy</p>
            <p className="mt-1 text-xs text-gray-500">
              주식 초보를 위한 AI 금융 정보 플랫폼
            </p>
          </div>
          <nav aria-label="하단 링크">
            <ul className="flex gap-4 text-xs text-gray-500">
              <li><Link href="/dictionary">금융 용어 사전</Link></li>
              <li><Link href="/learn">투자 학습</Link></li>
              <li><Link href="/stocks">종목 분석</Link></li>
            </ul>
          </nav>
        </div>
        <p className="mt-6 text-[11px] text-gray-400">
          FinEasy에서 제공하는 정보는 투자 권유가 아니며, 투자 판단의 책임은 이용자에게 있습니다.
        </p>
        <p className="mt-1 text-[11px] text-gray-400">
          &copy; 2026 FinEasy. All rights reserved.
        </p>
      </div>
    </footer>
  );
}
