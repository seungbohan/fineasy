import { ImageResponse } from 'next/og';

export const runtime = 'edge';
export const alt = 'FinEasy - 초보자를 위한 금융 정보 플랫폼';
export const size = { width: 1200, height: 630 };
export const contentType = 'image/png';

export default function Image() {
  return new ImageResponse(
    (
      <div
        style={{
          background: 'linear-gradient(135deg, #1e3a5f 0%, #0f1f3c 100%)',
          width: '100%',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          fontFamily: 'sans-serif',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
            marginBottom: '24px',
          }}
        >
          <div
            style={{
              width: '64px',
              height: '64px',
              borderRadius: '16px',
              background: '#3182F6',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '32px',
              fontWeight: '800',
              color: 'white',
            }}
          >
            FE
          </div>
          <div style={{ fontSize: '48px', fontWeight: '800', color: 'white' }}>
            FinEasy
          </div>
        </div>
        <div
          style={{
            fontSize: '28px',
            fontWeight: '600',
            color: '#94b8db',
            textAlign: 'center',
            maxWidth: '800px',
          }}
        >
          초보자를 위한 금융 정보 플랫폼
        </div>
        <div
          style={{
            display: 'flex',
            gap: '32px',
            marginTop: '48px',
          }}
        >
          {['AI 종목 분석', '실시간 시세', '금융 용어 사전', '투자 학습'].map(
            (text) => (
              <div
                key={text}
                style={{
                  background: 'rgba(49, 130, 246, 0.2)',
                  border: '1px solid rgba(49, 130, 246, 0.4)',
                  borderRadius: '12px',
                  padding: '12px 24px',
                  fontSize: '18px',
                  fontWeight: '600',
                  color: '#7bb3f0',
                }}
              >
                {text}
              </div>
            )
          )}
        </div>
      </div>
    ),
    { ...size }
  );
}
