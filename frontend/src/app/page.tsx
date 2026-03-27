import HomeClient from './home-client';

export default function HomePage() {
  return (
    <div className="mx-auto max-w-screen-xl space-y-6 p-4 pb-8 md:p-6 md:pb-10">
      <h1 className="sr-only">FinEasy - 주식 초보를 위한 AI 종목 분석과 금융 정보</h1>
      <section aria-label="서비스 소개">
        <p className="text-sm text-gray-600 leading-relaxed">
          FinEasy는 주식 초보자를 위한 무료 금융 정보 플랫폼입니다.
          AI 종목 분석, 실시간 시세, 금융 용어 사전(한국은행 700선),
          거시경제 지표를 한곳에서 확인하세요.
        </p>
      </section>
      <HomeClient />
    </div>
  );
}
