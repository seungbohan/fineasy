/**
 * @file Static sector analysis data
 * @description Contains structured content for each sector: industry overview, value chain,
 *   trends, and representative companies with optional stock codes for linking.
 */

export interface SectorCompany {
  name: string;
  stockCode: string | null;
  description: string;
  role: string;
}

export interface SectorDetail {
  slug: string;
  name: string;
  overview: string;
  structure: string[];
  valueChain: { step: string; description: string }[];
  trends: { title: string; content: string }[];
  companies: SectorCompany[];
}

export const SECTOR_DATA: Record<string, SectorDetail> = {
  semiconductor: {
    slug: 'semiconductor',
    name: '반도체',
    overview:
      '반도체는 전자 기기의 핵심 부품으로, 메모리(DRAM/NAND)와 비메모리(CPU, GPU, AP)로 나뉩니다. TSMC가 파운드리 시장의 60%를 장악하고, 엔비디아가 AI GPU 시장을 독점하며, 삼성전자·SK하이닉스가 메모리 시장을 양분합니다. AI/HBM 수요 폭증으로 반도체는 IT 산업의 핵심 인프라로 자리잡았습니다.',
    structure: [
      '메모리 반도체: DRAM, NAND Flash, HBM — 데이터 저장 (삼성전자, SK하이닉스, 마이크론)',
      '비메모리(로직): CPU(인텔, AMD), GPU(엔비디아), AP(퀄컴, 애플) — 연산/제어',
      '파운드리: 설계 없이 위탁 생산 (TSMC 60%, 삼성 12%, GlobalFoundries)',
      '팹리스: 설계만 수행, 생산은 파운드리에 위탁 (엔비디아, 퀄컴, AMD, 브로드컴)',
      '반도체 장비: 노광(ASML), 식각(Lam Research), 증착(Applied Materials), 검사(KLA)',
      '반도체 소재: 웨이퍼(신에쓰, SUMCO), 포토레지스트(도쿄오카), 가스(에어리퀴드)',
    ],
    valueChain: [
      { step: '설계 (Design)', description: 'ARM이 아키텍처 라이선스, 엔비디아·퀄컴·AMD가 칩 설계, Synopsys·Cadence가 EDA 툴 제공' },
      { step: '웨이퍼 제조', description: '신에쓰화학(일본), SUMCO, SK실트론이 고순도 실리콘 웨이퍼 생산' },
      { step: '전공정 (Fab)', description: 'ASML의 EUV 노광장비로 회로 패턴 형성, TSMC·삼성이 3nm급 양산' },
      { step: '후공정 (OSAT)', description: 'ASE, Amkor가 패키징·테스트 수행, HBM용 TSV/CoWoS 공정이 핵심' },
      { step: '완제품 납품', description: '애플, 테슬라, AWS, 마이크로소프트 등 최종 고객에 납품' },
    ],
    trends: [
      {
        title: 'AI/HBM 수요 폭증',
        content:
          '엔비디아 H100/B200 GPU에 필수적인 HBM(고대역폭 메모리) 수요가 급증하며 SK하이닉스가 최대 수혜. 마이크론도 HBM3E 양산에 합류했고, HBM4 개발 경쟁이 가속화되고 있습니다.',
      },
      {
        title: '미중 반도체 분쟁 & 공급망 재편',
        content:
          '미국의 대중 수출 규제(CHIPS Act)로 TSMC·삼성이 미국 애리조나에, 인텔이 오하이오에 신규 팹을 건설 중입니다. 글로벌 공급망이 미국·유럽·일본 중심으로 재편되고 있습니다.',
      },
      {
        title: '첨단 패키징 & 칩렛',
        content:
          'TSMC의 CoWoS, 인텔의 Foveros 등 2.5D/3D 패키징과 칩렛 아키텍처가 무어의 법칙 한계를 돌파하는 핵심 기술로 부상했습니다.',
      },
    ],
    companies: [
      { name: 'TSMC', stockCode: 'TSM', description: '글로벌 파운드리 1위 (시장점유율 60%), 3nm 양산', role: '파운드리' },
      { name: '엔비디아', stockCode: 'NVDA', description: 'AI GPU 시장 독점 (H100/B200), 시가총액 세계 1위급', role: 'GPU/AI' },
      { name: '삼성전자', stockCode: '005930', description: 'DRAM/NAND 세계 1위, 파운드리 2위', role: '종합 반도체' },
      { name: 'SK하이닉스', stockCode: '000660', description: 'HBM 세계 1위, DRAM 2위', role: '메모리/HBM' },
      { name: 'ASML', stockCode: 'ASML', description: 'EUV 노광장비 독점, 반도체 장비 최강자', role: '장비' },
      { name: 'AMD', stockCode: 'AMD', description: 'CPU/GPU 설계, 엔비디아 대항마 MI300', role: 'CPU/GPU' },
      { name: '브로드컴', stockCode: 'AVGO', description: '네트워크칩, 커스텀 AI칩(구글 TPU)', role: '팹리스' },
      { name: '마이크론', stockCode: 'MU', description: '미국 유일 메모리 기업, HBM3E 양산', role: '메모리' },
      { name: '인텔', stockCode: 'INTC', description: 'x86 CPU, 파운드리 사업 재도전', role: 'CPU/파운드리' },
    ],
  },

  defense: {
    slug: 'defense',
    name: '방산',
    overview:
      '글로벌 방산 산업은 러시아-우크라이나 전쟁, 중동 긴장, 대만해협 리스크로 각국 국방비가 급증하며 호황을 맞고 있습니다. 미국의 록히드마틴·레이시온이 세계 시장을 주도하고, 한국은 K-9 자주포·K-2 전차·FA-50 등으로 폴란드·사우디 대형 수출 계약을 체결하며 글로벌 방산 수출국으로 부상했습니다.',
    structure: [
      '항공: 전투기(F-35, KF-21), 훈련기, 무인기(UAV), 헬기',
      '지상무기: 전차(K-2, 에이브럼스), 자주포(K-9), 장갑차',
      '해양: 항공모함, 잠수함, 구축함, 호위함',
      '유도무기: 미사일(THAAD, 천궁, 패트리어트), 방공 시스템',
      '우주/사이버: 군사위성, 사이버전, 우주감시체계',
      '전자전/C4I: 레이더, 통신, 지휘통제 시스템',
    ],
    valueChain: [
      { step: '연구개발', description: 'DARPA(미국), ADD(한국) 등 국방연구기관 + 민간 업체 공동 개발' },
      { step: '시제품 & 시험', description: '프로토타입 제작 및 시험 평가, 수년 소요' },
      { step: '양산', description: '록히드마틴, 한화에어로 등 방산 프라임 업체가 대량 생산' },
      { step: '내수 납품', description: '자국 군에 우선 배치' },
      { step: '수출 & 기술이전', description: 'FMS(미국), DAPA(한국) 등을 통해 동맹국에 수출' },
    ],
    trends: [
      {
        title: 'NATO 국방비 증액 & 재무장',
        content:
          'NATO 회원국들이 GDP 대비 국방비 2% → 3% 목표를 상향하며, 유럽 방산 예산이 급증하고 있습니다. 독일은 1,000억 유로 특별기금을 편성했습니다.',
      },
      {
        title: '무인화/자율무기 시대',
        content:
          '우크라이나 전쟁에서 드론의 효과가 입증되며 무인 전투체계(UAV, 무인수상정, 자율로봇)에 대한 투자가 전 세계적으로 확대되고 있습니다.',
      },
      {
        title: '한국 방산 수출 급성장',
        content:
          '폴란드 K-2/K-9/FA-50(200억 달러), 사우디·UAE 등 중동 수출로 한국이 글로벌 방산 수출 4위권에 진입했습니다.',
      },
    ],
    companies: [
      { name: '록히드마틴', stockCode: 'LMT', description: 'F-35, THAAD, 세계 방산 매출 1위', role: '항공/미사일' },
      { name: 'RTX(레이시온)', stockCode: 'RTX', description: '패트리어트, 프랫&휘트니 엔진', role: '유도무기/엔진' },
      { name: '노스롭그루먼', stockCode: 'NOC', description: 'B-21 폭격기, 군사위성, 사이버전', role: '항공/우주' },
      { name: '한화에어로스페이스', stockCode: '012450', description: 'K-9 자주포, 항공엔진, 천궁', role: '항공/지상' },
      { name: 'BAE Systems', stockCode: null, description: '유럽 최대 방산기업 (영국), 전투함·장갑차', role: '해양/지상' },
      { name: '현대로템', stockCode: '064350', description: 'K-2 전차, 폴란드 대형 수출', role: '지상무기' },
      { name: 'LIG넥스원', stockCode: '079550', description: '천궁 미사일, 유도무기 전문', role: '유도무기' },
      { name: '한국항공우주', stockCode: '047810', description: 'KF-21, FA-50, 수리온', role: '항공' },
    ],
  },

  battery: {
    slug: 'battery',
    name: '이차전지',
    overview:
      '이차전지는 전기차(EV)와 에너지저장장치(ESS)의 핵심입니다. 중국 CATL이 글로벌 배터리 시장 37%를 장악하고, BYD가 급성장 중이며, 한국 3사(LG에너지솔루션·삼성SDI·SK온)와 일본 파나소닉이 뒤를 잇고 있습니다. 전고체 배터리와 LFP/NCM 기술 경쟁이 치열합니다.',
    structure: [
      '배터리 셀: 양극/음극/전해질/분리막 조합 단위 전지',
      '배터리 팩: 셀을 모듈/팩으로 조립, BMS(배터리 관리 시스템) 포함',
      '양극재: NCM(니켈·코발트·망간), LFP(리튬인산철) — 배터리 성능 결정',
      '음극재: 흑연, 실리콘 — 충전 속도/수명 관련',
      '전해질: 액체/고체 — 이온 이동 매개체',
      '분리막: 양극과 음극 사이 절연 필름 (SK아이이테크, 아사히카세이)',
    ],
    valueChain: [
      { step: '원자재 채굴', description: '호주(리튬), 인도네시아(니켈), 콩고(코발트) 등에서 채굴. Albemarle, SQM이 리튬 주요 공급' },
      { step: '소재 제조', description: '양극재(유미코어, 에코프로비엠), 음극재(히타치화학), 전해질, 분리막 제조' },
      { step: '셀 제조', description: 'CATL, LG에너지솔루션, 삼성SDI, BYD, 파나소닉이 셀 생산' },
      { step: '팩 조립', description: '셀을 BMS와 함께 모듈/팩으로 조립하여 완성차 업체에 납품' },
      { step: 'EV/ESS 장착', description: '테슬라, 현대, BYD, 폭스바겐 등 완성차에 장착 또는 ESS 납품' },
    ],
    trends: [
      {
        title: '전고체 배터리 경쟁',
        content:
          '삼성SDI, 토요타, QuantumScape 등이 2027-2028년 전고체 배터리 양산을 목표로 경쟁 중입니다. 에너지밀도 2배, 충전시간 절반이 장점입니다.',
      },
      {
        title: 'LFP vs NCM 경쟁',
        content:
          'CATL·BYD의 LFP 배터리가 저가형 EV에 급속 확산(테슬라도 채용). 고급형은 하이니켈 NCM이 주류를 유지하며 양극 기술 경쟁이 치열합니다.',
      },
      {
        title: 'IRA & 공급망 현지화',
        content:
          '미국 IRA법에 따라 CATL을 제외한 한국·일본 배터리사가 미국 현지 공장을 대거 건설 중입니다. 유럽 배터리법도 공급망 현지화를 요구합니다.',
      },
    ],
    companies: [
      { name: 'CATL', stockCode: null, description: '글로벌 배터리 시장점유율 1위(37%), 중국', role: '셀 제조' },
      { name: 'LG에너지솔루션', stockCode: '373220', description: '글로벌 2위, 테슬라·GM 납품', role: '셀 제조' },
      { name: 'BYD', stockCode: null, description: '중국 EV/배터리 일체형, 블레이드 배터리', role: '셀/EV' },
      { name: '삼성SDI', stockCode: '006400', description: '각형 배터리, 전고체 기술 선도', role: '셀 제조' },
      { name: '파나소닉', stockCode: null, description: '테슬라 초기 파트너, 원통형 4680 양산', role: '셀 제조' },
      { name: 'Albemarle', stockCode: 'ALB', description: '세계 최대 리튬 생산 기업(미국)', role: '원자재' },
      { name: '에코프로비엠', stockCode: '247540', description: '하이니켈 양극재 글로벌 선도', role: '양극재' },
      { name: '포스코퓨처엠', stockCode: '003670', description: '양극재/음극재 통합 소재', role: '소재' },
    ],
  },

  bio: {
    slug: 'bio',
    name: '바이오',
    overview:
      '글로벌 바이오 산업은 GLP-1 비만치료제(노보노디스크·일라이릴리), 면역항암제(머크·BMS), CDMO(삼성바이오로직스·론자), 바이오시밀러(셀트리온) 등이 핵심입니다. AI 신약 개발과 ADC(항체약물접합체) 기술이 차세대 성장 동력으로 부상 중입니다.',
    structure: [
      '빅파마(Big Pharma): 글로벌 대형 제약사 — 신약 개발·마케팅·유통 (화이자, 로슈, 노보노디스크)',
      '바이오텍: 혁신 신약(First-in-class) R&D 중심 기업',
      '바이오시밀러: 오리지널 바이오의약품 복제약 (셀트리온, 삼도즈)',
      'CDMO: 위탁개발생산 — 바이오의약품 생산 대행 (삼성바이오로직스, 론자, WuXi)',
      '진단/의료기기: 체외진단(IVD), 의료영상, 수술로봇',
      '디지털 헬스: AI 신약 발굴, 원격의료, 디지털 치료제',
    ],
    valueChain: [
      { step: '타겟 발굴', description: '질병 관련 단백질/유전자 타겟 발굴 (AI 활용 가속화)' },
      { step: '후보물질 도출', description: '수천 개 화합물 중 후보물질 선별, ADC 등 신기술 적용' },
      { step: '전임상', description: '동물 실험으로 안전성/유효성 확인 (1~2년)' },
      { step: '임상시험 (1~3상)', description: '사람 대상 검증, 전체 개발비의 60-70% 소요 (5~10년)' },
      { step: 'FDA/EMA 허가 & 출시', description: '규제기관 승인 후 전 세계 상업화' },
    ],
    trends: [
      {
        title: 'GLP-1 비만치료제 초대형 시장',
        content:
          '노보노디스크(위고비)와 일라이릴리(마운자로/젭바운드)가 주도하는 GLP-1 비만치료제 시장이 2030년 1,000억 달러를 전망합니다. 관련 CDMO 수요도 폭증 중입니다.',
      },
      {
        title: 'AI 신약 개발 가속화',
        content:
          '알파폴드(DeepMind) 이후 AI 기반 약물 설계가 혁신을 이끌고 있습니다. Recursion, Insilico Medicine 등 AI 바이오텍이 임상 단계에 진입했습니다.',
      },
      {
        title: 'ADC(항체약물접합체) & 세포치료제',
        content:
          '다이이치산쿄의 엔허투 성공 이후 ADC 기술이전이 수조 원대로 커졌습니다. CAR-T 등 세포치료제도 혈액암을 넘어 고형암으로 확장 중입니다.',
      },
    ],
    companies: [
      { name: '노보노디스크', stockCode: 'NVO', description: 'GLP-1(위고비/오젬픽) 개발, 유럽 시총 1위', role: '비만/당뇨' },
      { name: '일라이릴리', stockCode: 'LLY', description: '마운자로/젭바운드, 미국 시총 Top 10', role: '비만/당뇨' },
      { name: '로슈', stockCode: null, description: '항암제·진단 글로벌 1위 (스위스)', role: '항암/진단' },
      { name: '삼성바이오로직스', stockCode: '207940', description: 'CDMO 글로벌 1위, 생산능력 60만L', role: 'CDMO' },
      { name: '셀트리온', stockCode: '068270', description: '바이오시밀러 글로벌 선두 (램시마)', role: '바이오시밀러' },
      { name: '머크(MSD)', stockCode: 'MRK', description: '키트루다(면역항암제) 세계 매출 1위 약물', role: '항암' },
      { name: '화이자', stockCode: 'PFE', description: 'mRNA 백신, ADC, 유전자치료제', role: '종합제약' },
      { name: '다이이치산쿄', stockCode: null, description: 'ADC(엔허투) 글로벌 1위, 아스트라제네카 협업', role: 'ADC' },
    ],
  },

  finance: {
    slug: 'finance',
    name: '금융',
    overview:
      '글로벌 금융 섹터는 미국 대형 은행(JP모건, 골드만삭스)이 투자은행·자산운용 시장을 주도하고, 비자·마스터카드가 결제 인프라를 장악합니다. 한국 4대 금융지주(KB, 신한, 하나, 우리)는 높은 배당과 밸류업 정책으로 주목받고 있으며, 핀테크 혁신이 전통 금융을 빠르게 변화시키고 있습니다.',
    structure: [
      '투자은행(IB): M&A 자문, IPO 주관, 트레이딩 (골드만삭스, 모건스탠리)',
      '상업은행: 예금, 대출, 외환 (JP모건, 뱅크오브아메리카, KB금융)',
      '보험: 생명/손해보험, 재보험 (버크셔해서웨이, 알리안츠)',
      '결제/핀테크: 카드 네트워크, 디지털 결제 (비자, 마스터카드, 페이팔)',
      '자산운용: ETF, 펀드, 대체투자 (블랙록, 뱅가드, 피델리티)',
      '거래소/인프라: 증권거래소, 데이터 (CME, ICE, LSEG)',
    ],
    valueChain: [
      { step: '수신(예금/자금조달)', description: '고객 예금, 채권 발행, 기관 자금으로 자금 확보' },
      { step: '여신(대출/투자)', description: '기업/개인 대출, 채권·주식 투자로 수익 창출' },
      { step: '수수료 사업', description: 'IB, 자산운용, 카드, 보험 등 비이자 수익' },
      { step: '리스크 관리', description: '신용·시장·운영 리스크 관리, 규제 준수' },
      { step: '주주환원', description: '배당, 자사주 매입/소각으로 주주가치 제고' },
    ],
    trends: [
      {
        title: '고금리 수혜 & 금리 인하 전환',
        content:
          '미국 고금리로 JP모건 등 대형은행이 사상 최대 이자이익을 기록했고, 금리 인하 전환 시 대출 수요 회복과 채권 평가이익이 기대됩니다.',
      },
      {
        title: '한국 밸류업 프로그램',
        content:
          '정부 밸류업 정책으로 한국 금융지주들이 배당성향 확대, 자사주 소각 등 주주환원을 대폭 강화하고 있습니다. PBR 1배 미만 해소가 목표입니다.',
      },
      {
        title: 'AI & 디지털 전환',
        content:
          'JP모건은 AI 리서치에 연 150억 달러 투자, 블랙록은 Aladdin AI로 자산운용 혁신. 로보어드바이저, 디지털 뱅킹이 전통 금융을 재편 중입니다.',
      },
    ],
    companies: [
      { name: 'JP모건', stockCode: 'JPM', description: '세계 최대 은행, IB/상업은행/자산운용', role: '종합금융' },
      { name: '골드만삭스', stockCode: 'GS', description: '글로벌 IB 1위, 트레이딩·자문', role: '투자은행' },
      { name: '비자', stockCode: 'V', description: '글로벌 결제 네트워크 1위, 수수료 비즈니스', role: '결제' },
      { name: '블랙록', stockCode: 'BLK', description: '세계 최대 자산운용사 (AUM 10조 달러+)', role: '자산운용' },
      { name: '버크셔해서웨이', stockCode: 'BRK-B', description: '워런 버핏의 보험·투자 지주회사', role: '보험/투자' },
      { name: 'KB금융', stockCode: '105560', description: '한국 시총 1위 금융지주, 배당 선도', role: '금융지주' },
      { name: '신한지주', stockCode: '055550', description: '한국 대표 글로벌 금융지주', role: '금융지주' },
      { name: 'HSBC', stockCode: null, description: '아시아 강점 글로벌 은행(영국)', role: '글로벌 은행' },
    ],
  },

  energy: {
    slug: 'energy',
    name: '에너지',
    overview:
      '글로벌 에너지 섹터는 석유·가스 메이저(엑손모빌, 셰브런, 사우디 아람코)가 여전히 핵심이면서, 신재생에너지(태양광, 풍력, 수소)와 원자력 르네상스가 병행되고 있습니다. AI 데이터센터 전력 수요 급증으로 원전·SMR에 대한 관심이 폭증하며 에너지 전환이 가속화되고 있습니다.',
    structure: [
      '석유 & 가스: 탐사·생산(E&P), 정제, 유통 — 엑손모빌, 셰브런, 아람코',
      '원자력: 대형 원전 + SMR(소형모듈원전) — 넥스트에라, EDF, 두산에너빌리티',
      '태양광: 모듈·인버터·설치 — First Solar, 한화솔루션, LONGi',
      '풍력: 해상/육상 풍력 터빈 — 베스타스, 지멘스가메사, Ørsted',
      '수소: 그린/블루 수소 생산, 연료전지 — 플러그파워, 블룸에너지',
      '전력 유틸리티: 발전·송전·배전 — 넥스트에라, 에넬, 한국전력',
    ],
    valueChain: [
      { step: '자원 확보', description: '중동(아람코), 미국(셰일), 호주(LNG) 등에서 원유/가스 생산' },
      { step: '정제/변환', description: '정유소(엑손모빌, SK), LNG 터미널, 수소 전해조' },
      { step: '발전', description: '화력, 원자력, 태양광, 풍력 등 다양한 에너지원으로 전력 생산' },
      { step: '송전/배전', description: '고압 송전망 → 변전소 → 가정/산업 배전' },
      { step: '최종 소비', description: '산업, 가정, 수송(EV 충전), 데이터센터 등' },
    ],
    trends: [
      {
        title: 'AI 데이터센터 전력 수요 폭증',
        content:
          '마이크로소프트·구글·아마존의 AI 데이터센터가 막대한 전력을 필요로 하며, 원전 인접 부지 확보 경쟁이 벌어지고 있습니다. 넥스트에라에너지, Constellation Energy 등이 수혜입니다.',
      },
      {
        title: '원전 르네상스 & SMR',
        content:
          '탄소중립 + AI 전력 수요로 원자력이 재조명됩니다. NuScale, 테라파워(빌 게이츠) 등 SMR 기업과 두산에너빌리티가 주목받고 있습니다.',
      },
      {
        title: '에너지 전환 가속화',
        content:
          'IRA법에 따라 미국 내 태양광/풍력/수소 투자가 급증하고, 유럽은 REPowerEU로 러시아 의존도를 탈피하며 신재생 확대 중입니다.',
      },
    ],
    companies: [
      { name: '엑손모빌', stockCode: 'XOM', description: '세계 최대 석유 메이저, 탄소포집 투자', role: '석유/가스' },
      { name: '넥스트에라에너지', stockCode: 'NEE', description: '세계 최대 풍력/태양광 발전사(미국)', role: '신재생/유틸리티' },
      { name: '사우디 아람코', stockCode: null, description: '세계 최대 석유회사, 일 생산 1,200만 배럴', role: '석유' },
      { name: '셰브런', stockCode: 'CVX', description: '미국 2위 석유 메이저, 수소 투자 확대', role: '석유/가스' },
      { name: '두산에너빌리티', stockCode: '034020', description: '원전 기자재·가스터빈·SMR 핵심', role: '원전/발전장비' },
      { name: 'First Solar', stockCode: 'FSLR', description: '미국 1위 태양광 모듈, IRA 최대 수혜', role: '태양광' },
      { name: '한국전력', stockCode: '015760', description: '국내 전력 독점, 원전 재가동 수혜', role: '유틸리티' },
      { name: 'Ørsted', stockCode: null, description: '세계 최대 해상풍력 개발사(덴마크)', role: '해상풍력' },
    ],
  },
};

/** Get all valid sector slugs for static params */
export function getAllSectorSlugs(): string[] {
  return Object.keys(SECTOR_DATA);
}
