// 경기 데이터의 영문 팀명을 화면 표기로 바꾼다.
// flag는 flag-icons의 국가 코드, code는 티커에 쓰는 세 글자 코드다.
type Team = { ko: string; code: string; flag: string };

const TEAMS: Record<string, Team> = {
  Algeria: { ko: "알제리", code: "ALG", flag: "dz" },
  Argentina: { ko: "아르헨티나", code: "ARG", flag: "ar" },
  Australia: { ko: "호주", code: "AUS", flag: "au" },
  Austria: { ko: "오스트리아", code: "AUT", flag: "at" },
  Belgium: { ko: "벨기에", code: "BEL", flag: "be" },
  "Bosnia & Herzegovina": { ko: "보스니아", code: "BIH", flag: "ba" },
  Brazil: { ko: "브라질", code: "BRA", flag: "br" },
  Canada: { ko: "캐나다", code: "CAN", flag: "ca" },
  "Cape Verde": { ko: "카보베르데", code: "CPV", flag: "cv" },
  Colombia: { ko: "콜롬비아", code: "COL", flag: "co" },
  Croatia: { ko: "크로아티아", code: "CRO", flag: "hr" },
  Curaçao: { ko: "퀴라소", code: "CUW", flag: "cw" },
  "Czech Republic": { ko: "체코", code: "CZE", flag: "cz" },
  "DR Congo": { ko: "콩고민주공화국", code: "COD", flag: "cd" },
  Ecuador: { ko: "에콰도르", code: "ECU", flag: "ec" },
  Egypt: { ko: "이집트", code: "EGY", flag: "eg" },
  England: { ko: "잉글랜드", code: "ENG", flag: "gb-eng" },
  France: { ko: "프랑스", code: "FRA", flag: "fr" },
  Germany: { ko: "독일", code: "GER", flag: "de" },
  Ghana: { ko: "가나", code: "GHA", flag: "gh" },
  Haiti: { ko: "아이티", code: "HAI", flag: "ht" },
  Iran: { ko: "이란", code: "IRN", flag: "ir" },
  Iraq: { ko: "이라크", code: "IRQ", flag: "iq" },
  "Ivory Coast": { ko: "코트디부아르", code: "CIV", flag: "ci" },
  Japan: { ko: "일본", code: "JPN", flag: "jp" },
  Jordan: { ko: "요르단", code: "JOR", flag: "jo" },
  Mexico: { ko: "멕시코", code: "MEX", flag: "mx" },
  Morocco: { ko: "모로코", code: "MAR", flag: "ma" },
  Netherlands: { ko: "네덜란드", code: "NED", flag: "nl" },
  "New Zealand": { ko: "뉴질랜드", code: "NZL", flag: "nz" },
  Norway: { ko: "노르웨이", code: "NOR", flag: "no" },
  Panama: { ko: "파나마", code: "PAN", flag: "pa" },
  Paraguay: { ko: "파라과이", code: "PAR", flag: "py" },
  Portugal: { ko: "포르투갈", code: "POR", flag: "pt" },
  Qatar: { ko: "카타르", code: "QAT", flag: "qa" },
  "Saudi Arabia": { ko: "사우디아라비아", code: "KSA", flag: "sa" },
  Scotland: { ko: "스코틀랜드", code: "SCO", flag: "gb-sct" },
  Senegal: { ko: "세네갈", code: "SEN", flag: "sn" },
  "South Africa": { ko: "남아공", code: "RSA", flag: "za" },
  "South Korea": { ko: "대한민국", code: "KOR", flag: "kr" },
  Spain: { ko: "스페인", code: "ESP", flag: "es" },
  Sweden: { ko: "스웨덴", code: "SWE", flag: "se" },
  Switzerland: { ko: "스위스", code: "SUI", flag: "ch" },
  Tunisia: { ko: "튀니지", code: "TUN", flag: "tn" },
  Turkey: { ko: "튀르키예", code: "TUR", flag: "tr" },
  USA: { ko: "미국", code: "USA", flag: "us" },
  Uruguay: { ko: "우루과이", code: "URU", flag: "uy" },
  Uzbekistan: { ko: "우즈베키스탄", code: "UZB", flag: "uz" },
};

export type TeamView = { name: string; code: string; flag: string | null };

// 표에 없는 팀은 원래 이름을 그대로 쓰고 국기는 생략한다.
export function teamOf(name: string): TeamView {
  const team = TEAMS[name];
  if (!team) {
    return { name, code: name.slice(0, 3).toUpperCase(), flag: null };
  }
  return { name: team.ko, code: team.code, flag: team.flag };
}

const ROUNDS: Record<string, string> = {
  "Round of 32": "32강",
  "Round of 16": "16강",
  "Quarter-final": "8강",
  "Semi-final": "4강",
  "Match for third place": "3·4위전",
  Final: "결승",
};

// 예: "월드컵 16강", 조별리그는 "월드컵 A조"
export function stageLabel(round: string, groupName: string | null): string {
  const stage = ROUNDS[round] ?? (groupName ? `${groupName.replace("Group ", "")}조` : "조별리그");
  return `월드컵 ${stage}`;
}
