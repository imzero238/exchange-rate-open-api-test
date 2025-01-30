## 환율 Open API 설계

팀 프로젝트에서 실시간 환율 데이터가 필요해 다음과 같이 설계함

![](/img/exchange-rate-open-api-design-v250120.png)

- v250120
- 환율 데이터 필요 시 로컬 캐시 상태 확인 (현재 단일 서버 기준으로 구현)
- 로컬 캐시에 캐싱된 환율 데이터 시각이 현재 시각과 ns 이내 차이라면 캐싱 된 데이터 사용
- 그렇지 않다면 open api로 실시간 환율 데이터 요청 후 로컬 캐시 업데이트
<br>

### ReentrantLock 사용한 환율 데이터 업데이트 설계

- Open API 호출 제한한 이유
  - 이유1: 여러 요청이 동시에 Open API로 USD 환율을 요청해도 같은 데이터를 가져올 가능성 높음 
  - 이유2: Open API 다수 호출 시 호출 제한걸려 Forbidden 에러 응답받으면, 일정 시간 동안 실시간 환율 데이터 응답 못 받으므로 호출 제한 필요
<br>
  
- 화폐 단위로 ReentrantLock을 생성해 ReentrantLock을 잡은 스레드만이 환율 Open API 호출
- ReentrantLock 잡지 못한 스레드 while 문 돌면서 로컬 캐시 업데이트 상태 지속 확인 (while 문 내에서 여러 방식 사용, 하단에 모니터링 결과 추가)
  - spin lock
  - sleep
  - await
  - virtual thread

### 단계적 호출
- 호출 제한에 대비하기 위해 단계적 호출 설계
- 1차 호출: [naver open api](https://m.search.naver.com/p/csearch/content/qapirender.nhn?key=calculator&pkid=141&q=%ED%99%98%EC%9C%A8&where=m&u1=keb&u6=standardUnit&u7=0&u3=USD&u4=KRW&u8=down&u2=1)
- 2차 호출: [manana oepn api](https://api.manana.kr/exchange) & [구글 web scraping](https://www.google.com/finance/quote/USD-KRW) 동시 수행
- 1차 호출에서 이미 시간 소요되었으므로 2차 호출에서 동시 수행해 먼저 오는 응답으로 로컬 캐시 업데이트

### CompletableFuture 기반 Open API 호출
- 2차 호출에서 manana open api & 구글 web scraping 동시 수행
- 2차 호출에서 동시 수행해 먼저 오는 응답으로 로컬 캐시 업데이트하기 위해 CompletableFuture 사용해 비동기 처리

<br>

## CPU Usage & Requests per second 모니터링

ReentrantLock을 잡지 못한 스레드가 로컬 캐시의 업데이트를 기다리기 위해 while 문을 수행하는데,<br>
while 문 내부에서 여러 구현 방법으로 스레드 상태의 차이를 두었으며 CPU 사용률과 초당 처리율을 측정해봤다.

- spin lock
- sleep
- await
- virtual thread (예정)

### Spin lock

구현 설명: ReentrantLock을 잡지 못한 스레드는 sleep 없이 while 문을 계속 돌며 로컬 캐시 상태 확인

![](/img/cpu_usage_spin_lock.png)

- 시스템 CPU 사용률(mean): 0.204

![](/img/rps_spin_lock.png)

- RPS 최대: 120

<br>

### sleep(1000)

구현 설명: ReentrantLock을 잡지 못한 스레드는 sleep(1000) 간격으로 while 문을 계속 돌며 로컬 캐시 상태 확인

![](/img/cpu_usage_sleep_1000.png)

- 시스템 CPU 사용률(mean): 0.172
- spin lock 방식보다 CPU 사용률은 줄었으나, 그 차이가 크지 않음

![](/img/rps_sleep_1000.png)

- RPS 최대: 140
- spin lock 방식과의 RPS 차이도 크지 않음

<br>

### CV (await & signalAll)

구현 설명: ReentrantLock을 잡지 못한 스레드는 await 하다가, ReentrantLock을 잡은 스레드의 signal을 받고 일어나 로컬 캐시 상태 확인

![](/img/cpu_usage_cv.png)

- 시스템 CPU 사용률(mean): 0.176
- sleep(100) 방식과의 CPU 사용률 차이가 크지 않음

![](/img/rps_cv.png)

- RPS 최대: 200
- spin lock, sleep 방식보다 RPS 증가

<br>

### Virtual Thread

- 예정

<br>

### HTTP Request failed
![](/img/http_server_requests_count.png)

cpu 사용률과 RPS 측정을 위해 약 68,000개 요청을 보냈으며, 그 과정에서 spin lock & sleep 방식과 condition 방식에 차이 있었음

![](/img/http_req_failed_spin_lock_and_sleep.png)

- spin lock, sleep 방식에서는 요청 일부 실패

![](/img/http_req_failed_cv.png)

- Condition 방식에서는 모든 부하가 100%로 실패 없이 처리되었는데
- 관계가 있는지는 잘 모르겠음. 일단 기록...