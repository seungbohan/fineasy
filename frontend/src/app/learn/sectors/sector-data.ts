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
      '반도체는 전자 기기의 핵심 부품으로, 메모리(DRAM/NAND)와 비메모리(AP, CPU, GPU)로 나뉩니다. 한국은 메모리 반도체 분야에서 세계 1위를 차지하고 있으며, AI/HBM 수요 확대로 새로운 성장 국면에 진입했습니다.',
    structure: [
      '메모리 반도체: DRAM, NAND Flash — 데이터 저장',
      '비메모리 반도체: AP, CPU, GPU, ASIC — 연산/제어',
      '파운드리: 설계 없이 위탁 생산만 수행 (TSMC, 삼성)',
      '팹리스: 설계만 하고 생산은 파운드리에 위탁 (퀄컴, AMD)',
      '반도체 장비: 노광, 식각, 증착 등 제조 공정 장비',
      '반도체 소재: 웨이퍼, 포토레지스트, 가스 등',
    ],
    valueChain: [
      { step: '설계 (Design)', description: 'ARM, 퀄컴, 미디어텍 등이 회로 설계' },
      { step: '웨이퍼 제조', description: '고순도 실리콘 웨이퍼 생산 (SK실트론)' },
      { step: '전공정 (Fab)', description: '노광-식각-증착-세정 등 패턴 형성' },
      { step: '후공정 (OSAT)', description: '패키징, 테스트, HBM TSV 공정' },
      { step: '완제품', description: '모듈 조립 후 고객사(서버, 모바일 등)에 납품' },
    ],
    trends: [
      {
        title: 'AI/HBM 수요 폭증',
        content:
          'ChatGPT 이후 AI 학습/추론용 고대역폭 메모리(HBM) 수요가 급증하며 SK하이닉스가 최대 수혜를 받고 있습니다. HBM4 개발 경쟁도 가속화되고 있습니다.',
      },
      {
        title: '미중 반도체 분쟁',
        content:
          '미국의 대중 수출 규제로 글로벌 공급망이 재편되고 있습니다. 한국 기업들은 미국 내 투자를 확대하며 대응하고 있습니다.',
      },
      {
        title: '첨단 패키징 기술',
        content:
          '2.5D/3D 패키징, 칩렛 아키텍처 등 후공정의 중요성이 커지며 관련 장비/소재 기업에 주목해야 합니다.',
      },
    ],
    companies: [
      { name: '삼성전자', stockCode: '005930', description: 'DRAM/NAND 세계 1위, 파운드리', role: '종합 반도체' },
      { name: 'SK하이닉스', stockCode: '000660', description: 'HBM 세계 1위, DRAM/NAND', role: '메모리' },
      { name: 'DB하이텍', stockCode: '000990', description: '8인치 파운드리 전문', role: '파운드리' },
      { name: '리노공업', stockCode: '058470', description: '반도체 테스트 소켓', role: '후공정 장비' },
      { name: 'HPSP', stockCode: '403870', description: '고압 어닐링 장비', role: '전공정 장비' },
      { name: '한미반도체', stockCode: '042700', description: 'TC 본더 글로벌 1위', role: '후공정 장비' },
      { name: 'SK실트론', stockCode: null, description: '300mm 웨이퍼', role: '소재' },
      { name: '원익IPS', stockCode: '240810', description: 'CVD/ALD 증착 장비', role: '전공정 장비' },
    ],
  },

  defense: {
    slug: 'defense',
    name: '방산',
    overview:
      '한국 방산 산업은 K-9 자주포, K-2 전차, FA-50 경공격기, 천궁 미사일 등으로 글로벌 수출 강국으로 부상했습니다. 폴란드, 사우디, UAE 등 대형 수출 계약이 이어지며 수출 주도 성장 구조로 전환 중입니다.',
    structure: [
      '지상무기: 전차, 자주포, 장갑차, 탄약',
      '항공: 전투기, 훈련기, 무인기(UAV)',
      '해양: 잠수함, 호위함, 상륙함',
      '유도무기: 미사일, 방공 시스템',
      '전자전/C4I: 레이더, 통신, 지휘통제',
    ],
    valueChain: [
      { step: '연구개발', description: 'ADD(국방과학연구소), 민간 업체 공동 개발' },
      { step: '시제품 생산', description: '프로토타입 제작 및 시험 평가' },
      { step: '양산', description: '방산 업체가 대량 생산' },
      { step: '내수 납품', description: '국군에 우선 납품' },
      { step: '수출', description: 'DAPA(방위사업청) 승인 후 해외 수출' },
    ],
    trends: [
      {
        title: '폴란드 대형 수출 계약',
        content:
          'K-2 전차, K-9 자주포, FA-50, 천무 등 총 200억 달러 규모의 계약이 진행 중입니다. 현대로템, 한화에어로스페이스가 핵심 수혜주입니다.',
      },
      {
        title: '무인화/자율무기 시대',
        content:
          '드론, 무인수상정, AI 기반 자율전투 시스템 등 미래전 대비 투자가 확대되고 있습니다.',
      },
      {
        title: '글로벌 국방비 증가',
        content:
          'NATO 국가들의 GDP 대비 국방비 2% 목표, 중동/동남아 무장 강화 추세로 글로벌 방산 시장이 확대되고 있습니다.',
      },
    ],
    companies: [
      { name: '한화에어로스페이스', stockCode: '012450', description: '항공엔진, K-9, 천궁', role: '항공/지상' },
      { name: '현대로템', stockCode: '064350', description: 'K-2 전차, 장갑차', role: '지상무기' },
      { name: 'LIG넥스원', stockCode: '079550', description: '유도무기, 방공 시스템', role: '유도무기' },
      { name: '한국항공우주', stockCode: '047810', description: 'KF-21, FA-50, 수리온', role: '항공' },
      { name: '한화시스템', stockCode: '272210', description: '레이더, 전자전, C4I', role: '전자전' },
      { name: '풍산', stockCode: '103140', description: '탄약/방산 소재', role: '탄약' },
    ],
  },

  battery: {
    slug: 'battery',
    name: '이차전지',
    overview:
      '이차전지(2차전지)는 충전과 방전을 반복할 수 있는 배터리로, 전기차(EV)와 에너지저장장치(ESS)의 핵심입니다. 한국은 LG에너지솔루션, 삼성SDI, SK온 등 글로벌 Top 3 배터리 기업을 보유하고 있습니다.',
    structure: [
      '배터리 셀: 양극/음극/전해질/분리막을 조합한 단위 전지',
      '배터리 팩: 셀을 모듈/팩으로 조립, BMS 포함',
      '양극재: NCM, LFP, 하이니켈 — 배터리 용량/수명 결정',
      '음극재: 흑연, 실리콘 — 충전 속도/수명 관련',
      '전해질: 액체/고체 — 이온 이동 매개',
      '분리막: 양극과 음극을 분리하는 필름',
    ],
    valueChain: [
      { step: '원자재', description: '리튬, 니켈, 코발트, 망간 등 광물 채굴/정련' },
      { step: '소재', description: '양극재, 음극재, 전해질, 분리막 제조' },
      { step: '셀 제조', description: '전극 코팅→조립→화성 공정으로 셀 생산' },
      { step: '모듈/팩 조립', description: '셀을 BMS와 함께 모듈/팩으로 조립' },
      { step: '완성차 장착', description: 'EV OEM에 납품 (현대, 테슬라, GM 등)' },
    ],
    trends: [
      {
        title: '전고체 배터리 경쟁',
        content:
          '차세대 배터리로 주목받는 전고체 배터리의 상용화 경쟁이 본격화되고 있습니다. 삼성SDI, 토요타 등이 2027-2028년 양산을 목표로 하고 있습니다.',
      },
      {
        title: 'LFP vs NCM 경쟁',
        content:
          '저가형 LFP(리튬인산철) 배터리가 보급형 EV에 확산되고 있으며, 고급형은 하이니켈 NCM이 주류를 유지합니다.',
      },
      {
        title: '북미 현지화 투자',
        content:
          'IRA(인플레이션 감축법)에 따라 한국 배터리 3사 모두 미국 현지 공장을 건설/증설 중입니다.',
      },
    ],
    companies: [
      { name: 'LG에너지솔루션', stockCode: '373220', description: '글로벌 배터리 Top 2', role: '셀 제조' },
      { name: '삼성SDI', stockCode: '006400', description: '각형 배터리, 전고체 선도', role: '셀 제조' },
      { name: 'SK이노베이션', stockCode: '096770', description: 'SK온 모회사', role: '셀 제조' },
      { name: '에코프로비엠', stockCode: '247540', description: '하이니켈 양극재', role: '양극재' },
      { name: '포스코퓨처엠', stockCode: '003670', description: '양극재/음극재', role: '소재' },
      { name: '엘앤에프', stockCode: '066970', description: 'NCMA 양극재', role: '양극재' },
      { name: 'SK아이이테크놀로지', stockCode: '361610', description: '분리막 전문', role: '분리막' },
    ],
  },

  bio: {
    slug: 'bio',
    name: '바이오',
    overview:
      '바이오 산업은 신약 개발, 바이오시밀러, CDMO(위탁 개발 생산), 진단 기기 등을 포함합니다. 한국은 삼성바이오로직스의 CDMO, 셀트리온의 바이오시밀러 분야에서 글로벌 경쟁력을 보유하고 있습니다.',
    structure: [
      '신약 개발: 혁신 신약(First-in-class) 연구 개발',
      '바이오시밀러: 오리지널 바이오의약품의 복제약',
      'CDMO: 위탁개발생산(Contract Development & Manufacturing)',
      '진단: 체외진단(IVD), 동반진단',
      '디지털 헬스케어: AI 신약 발굴, 원격의료',
    ],
    valueChain: [
      { step: '타겟 발굴', description: '질병 관련 단백질/유전자 타겟 발굴' },
      { step: '후보물질 도출', description: '수천 개 화합물 중 후보물질 선별' },
      { step: '전임상', description: '동물 실험으로 안전성/유효성 확인' },
      { step: '임상시험 (1~3상)', description: '사람 대상 안전성/유효성 검증' },
      { step: '허가/출시', description: 'FDA/EMA 승인 후 상업화' },
    ],
    trends: [
      {
        title: 'GLP-1 비만치료제 열풍',
        content:
          '위고비, 마운자로 등 GLP-1 계열 비만치료제가 초대형 시장을 형성하며 관련 CDMO 수요가 폭증하고 있습니다.',
      },
      {
        title: 'AI 신약 개발',
        content:
          '알파폴드 이후 AI 기반 약물 설계/후보물질 발굴이 가속화되고 있습니다. 임상 성공률을 높이는 핵심 기술로 부상 중입니다.',
      },
      {
        title: 'ADC(항체약물접합체) 시대',
        content:
          '항체와 약물을 결합한 ADC가 차세대 항암제로 주목받으며, 기술이전(L/O) 규모가 수조 원대로 커지고 있습니다.',
      },
    ],
    companies: [
      { name: '삼성바이오로직스', stockCode: '207940', description: 'CDMO 글로벌 1위', role: 'CDMO' },
      { name: '셀트리온', stockCode: '068270', description: '바이오시밀러 글로벌 선두', role: '바이오시밀러' },
      { name: '유한양행', stockCode: '000100', description: '렉라자 등 신약 개발', role: '신약' },
      { name: '알테오젠', stockCode: '196170', description: 'SC 제형 플랫폼 기술', role: '플랫폼' },
      { name: '리가켐바이오', stockCode: '141080', description: 'ADC 기술 전문', role: 'ADC' },
      { name: 'HLB', stockCode: '028300', description: '항암 신약 개발', role: '신약' },
    ],
  },

  finance: {
    slug: 'finance',
    name: '금융',
    overview:
      '금융 섹터는 은행, 보험, 증권, 카드 등 금융 서비스 전반을 포함합니다. 한국 4대 금융지주(KB, 신한, 하나, 우리)가 핵심이며, 높은 배당 수익률과 주주환원 정책으로 가치투자 관점에서 주목받고 있습니다.',
    structure: [
      '은행: 예금, 대출, 외환 등 전통 금융 서비스',
      '보험: 생명보험, 손해보험 — 보험료 수입 및 투자',
      '증권: 위탁매매, IB(투자은행), 자기매매(트레이딩)',
      '카드: 신용/체크카드, 할부금융',
      '핀테크: 디지털 뱅킹, 간편결제, P2P',
    ],
    valueChain: [
      { step: '수신 (예금)', description: '고객 예금을 유치하여 자금 조달' },
      { step: '여신 (대출)', description: '기업/개인에 대출하여 이자 수익 창출' },
      { step: '투자', description: '채권, 주식, 부동산 등에 자산 운용' },
      { step: '수수료 사업', description: '카드, 보험, IB 등 비이자 수익' },
      { step: '주주환원', description: '배당, 자사주 매입/소각' },
    ],
    trends: [
      {
        title: '밸류업 프로그램',
        content:
          '정부의 기업 밸류업 프로그램으로 금융지주들이 주주환원을 대폭 강화하고 있습니다. 배당성향 확대, 자사주 소각 등이 주요 내용입니다.',
      },
      {
        title: '금리 인하 사이클',
        content:
          '글로벌 금리 인하 추세로 NIM(순이자마진) 축소 우려가 있지만, 대출 수요 회복 기대도 공존합니다.',
      },
      {
        title: '디지털 전환',
        content:
          '비대면 금융 확산, AI 챗봇, 마이데이터 사업 등 전통 금융의 디지털화가 가속되고 있습니다.',
      },
    ],
    companies: [
      { name: 'KB금융', stockCode: '105560', description: '시가총액 1위 금융지주', role: '금융지주' },
      { name: '신한지주', stockCode: '055550', description: '글로벌 금융지주', role: '금융지주' },
      { name: '하나금융지주', stockCode: '086790', description: 'NIM 최상위 금융지주', role: '금융지주' },
      { name: '우리금융지주', stockCode: '316140', description: '기업금융 강점', role: '금융지주' },
      { name: '삼성생명', stockCode: '032830', description: '생명보험 1위', role: '보험' },
      { name: '키움증권', stockCode: '039490', description: '온라인 증권 1위', role: '증권' },
    ],
  },

  energy: {
    slug: 'energy',
    name: '에너지',
    overview:
      '에너지 섹터는 정유, 가스, 발전, 신재생에너지를 포함합니다. 한국은 세계 5위 정유 능력을 보유하고 있으며, 수소/태양광/풍력 등 신재생에너지 투자도 확대하고 있습니다.',
    structure: [
      '정유: 원유를 정제하여 휘발유, 경유, 나프타 등 생산',
      '가스: LNG 수입/유통, 도시가스',
      '발전: 화력, 원자력, 신재생 발전',
      '신재생: 태양광, 풍력, 수소 등',
      '석유화학: 나프타를 원료로 플라스틱, 합성섬유 생산',
    ],
    valueChain: [
      { step: '원유/가스 도입', description: '중동, 미국 등에서 원유/LNG 수입' },
      { step: '정제/가공', description: '정유소에서 원유를 정제하여 석유 제품 생산' },
      { step: '유통', description: '주유소, 석유화학 공장 등에 유통' },
      { step: '발전', description: 'LNG, 석탄, 원자력 등으로 전력 생산' },
      { step: '최종 소비', description: '산업용, 가정용, 수송용 에너지 소비' },
    ],
    trends: [
      {
        title: '정유 마진 사이클',
        content:
          '글로벌 정유 설비 투자 감소로 중장기 정유 마진 개선이 예상됩니다. 중국 독립 정유사 구조조정도 긍정적 요인입니다.',
      },
      {
        title: '수소 경제 추진',
        content:
          '정부 수소경제 로드맵에 따라 수소 생산/유통/충전 인프라 투자가 확대되고 있습니다.',
      },
      {
        title: '원전 르네상스',
        content:
          'AI 데이터센터 전력 수요 급증으로 원자력 발전이 재조명받고 있으며, SMR(소형모듈원전) 기술 개발도 활발합니다.',
      },
    ],
    companies: [
      { name: 'SK이노베이션', stockCode: '096770', description: '정유/배터리/석유화학', role: '정유' },
      { name: 'S-Oil', stockCode: '010950', description: '아람코 합작 정유사', role: '정유' },
      { name: '한국전력', stockCode: '015760', description: '국내 유일 전력 판매', role: '발전/송전' },
      { name: '한국가스공사', stockCode: '036460', description: 'LNG 독점 수입/유통', role: '가스' },
      { name: '두산에너빌리티', stockCode: '034020', description: '원전/가스터빈/풍력', role: '발전 장비' },
    ],
  },
};

/** Get all valid sector slugs for static params */
export function getAllSectorSlugs(): string[] {
  return Object.keys(SECTOR_DATA);
}
