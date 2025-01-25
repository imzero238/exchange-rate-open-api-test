## 환율 Open API 설계

팀 프로젝트에서 실시간 환율 데이터갸 필요해 다음과 같이 설계함

![](/img/exchange-rate-open-api-design-v250120.png)

- v250120
- 환율 데이터 필요 시 로컬 캐시 상태 확인 (현재 단일 서버 기준으로 구현)
- 로컬 캐시에 캐싱된 환율 데이터 시각이 현재 시각과 ns 이내 차이라면 캐싱 된 데이터 사용
- 그렇지 않다면 open api로 실시간 환율 데이터 요청 후 로컬 캐시 업데이트
<br>

### ReentrantLock 사용한 환율 데이터 업데이트 설계

- 화폐 단위로 ReentrantLock을 생성해 ReentrantLock을 잡은 스레드만이 환율 Open API 호출 가능
- ReentrantLock으로 Open API 호출 제한한 이유
    - 이유1: 여러 요청이 동시에 Open API로 USD 환율을 요청해도 같은 데이터를 가져올 가능성 높음
    - 이유2: Open API 다수 호출 시 호출 제한걸려 Forbidden 에러 응답받으면, 일정 시간 동안 실시간 환율 데이터 응답 못 받으므로 호출 제한 필요
- ReentrantLock 잡은 스레드만 환율 Open API 호출해 로컬 캐시 업데이트
- ReentrantLock 잡지 못한 스레드는 로컬 캐시 바라보면서 ReentrantLock 잡은 스레드에 의해 로컬 캐시 업데이트 상태 지속 확인 (while 문)

### 단계적 호출
- 호출 제한에 대비하기 위해 단계적 호출 설계
- 1차 호출: [naver open api](https://m.search.naver.com/p/csearch/content/qapirender.nhn?key=calculator&pkid=141&q=%ED%99%98%EC%9C%A8&where=m&u1=keb&u6=standardUnit&u7=0&u3=USD&u4=KRW&u8=down&u2=1)
- 2차 호출: [manana oepn api](https://api.manana.kr/exchange) & [구글 web scraping](https://www.google.com/finance/quote/USD-KRW) 동시 수행
- 1차 호출에서 이미 시간 소요되었으므로 2차 호출에서 동시 수행해 먼저 오는 응답으로 로컬 캐시 업데이트

<br>

## 현재 문제점 🤔

### Spin Lock vs Sleep
- ReentrantLock 잡지 못한 스레드 -> while 문 돌면서 로컬 캐시 확인하는데
- sleep으로 상태 전환해 CPU 양보할 것 인가
- run 상태 유지하면서 CPU 선점할 것 인가
- 현재 채택한 방법: sleep 
- 이유: 외부 I/O 를 이용하고 있기 때문에 응답 지연도 고려해야 함 (현재 4s timeout 설정)

<br>

### ReentrantLock -> Virtual thread...?
- CPU 양보를 위해 상태 전환하면 사용자 모드 -> 커널 모드 전환 비용 발생
- 현재 외부 I/O 사용하고 있기 때문에 blocking I/O 발생
- 현재 계획: Virtual thread 사용해 유저 모드에서 blocking I/O 발생해도 커널 스레드가 block 상태로 전환하는 것을 방지 


