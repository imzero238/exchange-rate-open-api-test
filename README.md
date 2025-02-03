## 환율 Open API 설계

팀 프로젝트에서 실시간 환율 데이터가 필요해 다음과 같이 설계

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
- **ReentrantLock 잡지 못한 스레드 sleep 상태 유지하다가, ReentrantLock 잡은 스레드의 시그널 받고 일어나** 업데이트된 값 가지고 탈출

### 단계적 호출
- 호출 제한에 대비하기 위해 단계적 호출 설계
- 1차 호출: [naver open api](https://m.search.naver.com/p/csearch/content/qapirender.nhn?key=calculator&pkid=141&q=%ED%99%98%EC%9C%A8&where=m&u1=keb&u6=standardUnit&u7=0&u3=USD&u4=KRW&u8=down&u2=1)
- 2차 호출: [manana oepn api](https://api.manana.kr/exchange) & [구글 web scraping](https://www.google.com/finance/quote/USD-KRW) 동시 수행
- 1차 호출에서 이미 시간 소요되었으므로 2차 호출에서 동시 수행해 먼저 오는 응답으로 로컬 캐시 업데이트

### CompletableFuture 기반 Open API 호출
- 2차 호출에서 manana open api & 구글 web scraping 동시 수행
- 2차 호출에서 동시 수행해 먼저 오는 응답으로 로컬 캐시 업데이트하기 위해 CompletableFuture 사용해 비동기 처리
- 1차 호출은 동기로 변경할 예정...

<br>

## sleep & wake up 방법

- 생산자: ReentrantLock을 잡은 생산자 스레드, Open API 호출해 환율 데이터 업데이트
- 소비자: ReentrantLock 잡지 못한 소비자 스레드, 생산자 스레드의 작업을 기다리며 생산자 스레드가 업데이트한 값 읽고 모든 로직 탈출

아래 코드는 전체 코드의 일부분입니다.

### 방법1: condition await & signalAll (add-cv 브랜치)

```java
// 시작 메서드
public BigDecimal getLatestExchangeRate(Currency fromCurrency, Currency toCurrency) {

	ReentrantLock lock = currencyLocks.computeIfAbsent(fromCurrency, k -> new ReentrantLock());
	Condition condition = currencyConditions.computeIfAbsent(fromCurrency, k -> lock.newCondition());
	
	if (lock.tryLock(TRY_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {  // 생산자
		return fetchPrimaryExchangeRate(fromCurrency, toCurrency, lock, conditoin);
	} else {  // 소비자
		return monitorExchangeRateUpdate(fromCurrency, toCurrency, lock, conditoin);
	}
}

// ReentrantLock을 잡은 생산자 스레드
private BigDecimal fetchPrimaryExchangeRate(Currency fromCurrency, Currency toCurrency, ReentrantLock lock, Condition condition) {
	try {
		// 환율 Open API 호출
	} finally {
		condition.signalAll();
		lock.unlock();
	}
}

/*
    ReentrantLock 못 잡은 소비자 스레드
    생산자 스레드의 작업을 기다리며 업데이트된 값 읽고 모든 로직 탈출
    Open API 호출하지 않음
 */
private BigDecimal monitorExchangeRateUpdate(Currency fromCurrency, Currency toCurrency, ReentrantLock lock, Condition condition) throws InterruptedException {
	lock.lock();
	try {
		while (!isAvailableExchangeRate(fromCurrency, toCurrency)) {
			if(!condition.await(AWAIT_TIMEOUT, TimeUnit.MILLISECONDS)) ;
		}
	} finally {
		lock.unlock();
	}
}
```
https://github.com/imzero238/exchange-rate-open-api-test/blob/feat/add-cv/src/main/java/com/nayoung/exchangerateopenapitest/domain/exchangerate/ExchangeRateService.java

- 구현 의도대로 1개의 스레드만 Open API를 호출하지만
- 생산자, 소비자 모두 같은 락을 획득하려고 하기 떄문에, 소비자는 await 지점에서 대기하는 것이 아니라 lock.lock() 지점에서 대기
- 즉, 이론으로 배웠던 생산자, 소비자 문제(누가 먼저 공유 자원에 접근하는지 파악 불가)와 다르게 생산자가 무조건 lock을 먼저 획득한다는 차이
- 이 코드에선 await가 필요하지 않을 수 있다고 판단해 방법 2로 변경했습니다!
<br>

- [소비자 await 코드 라인](https://github.com/imzero238/exchange-rate-open-api-test/blob/feat/add-cv/src/main/java/com/nayoung/exchangerateopenapitest/domain/exchangerate/ExchangeRateService.java#L142) 위 링크와 같습니다.
- [생산자 signalAll 코드 라인](https://github.com/imzero238/exchange-rate-open-api-test/blob/feat/add-cv/src/main/java/com/nayoung/exchangerateopenapitest/domain/exchangerate/ExchangeRateService.java#L90) 위 링크와 같습니다.

<br>

### 방법2: lock 없이 condition await (add-synchronized-condition 브랜치)

```java
// ReentrantLock 못 잡은 소비자 스레드
private BigDecimal monitorExchangeRateUpdate(Currency fromCurrency, Currency toCurrency, ReentrantLock lock, Condition condition) throws InterruptedException {
	// lock.lock();
	try {
		while (!isAvailableExchangeRate(fromCurrency, toCurrency)) {
			synchronized (condition) {
				condition.wait(CONDITION_WAIT_TIMEOUT);
			}
		}
	} finally {
		lock.unlock();
	}
}
```
https://github.com/imzero238/exchange-rate-open-api-test/blob/feat/add-synchronized-condition/src/main/java/com/nayoung/exchangerateopenapitest/domain/exchangerate/ExchangeRateService.java#L142

lock 없이 condition wait을 호출
- 소비자가 wake up 하자마다 ReentrantLock 잡을 필요 없음
- ReentrantLock 잡은 생산자 스레드가 업데이트한 값만 읽고 탈출하면 됨

synchronized 블록
- lock 없이 await 호출해서 IllegalMonitorStateException 발생 -> synchronized 블록 추가
- synchronized 블록 들어와 바로 wait 호출하므로 여러 소비자 스레드 synchronized 블록 내부에서 대기 (동기화 안 됨)
- 모든 소비자 스레드가 함께 대기하다가 같이 깨어나도 되니, 동기화 필요 없음

고민
- 하지만 synchronized 키워드를 사용하고 있는데 동기화하지 않는다....? 
- synchronized + condition(lock 없이) 조합...? (condition은 lock과 함께 사용하는 것으로 알고 있습니다.)
- 이 코드는 기술의 특성을 잘 살리지 못한 것 같아서 방법 3으로 변경했습니다!

<br>

### 방법3: condition 제거, CompletableFuture get & complete 사용 (add-future-complete-get 브랜치)

```java
private static final Map<Currency, ReentrantLock> currencyLocks = new ConcurrentHashMap<>();
private static final Map<Currency, CompletableFuture<BigDecimal>> currencyFutures = new ConcurrentHashMap<>();

public BigDecimal getLatestExchangeRate(Currency fromCurrency, Currency toCurrency) {

	ReentrantLock lock = currencyLocks.computeIfAbsent(fromCurrency, k -> new ReentrantLock());
	CompletableFuture<BigDecimal> future = currencyFutures.computeIfAbsent(fromCurrency, k -> new CompletableFuture<>());
  
	if (lock.tryLock(TRY_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
		return fetchPrimaryExchangeRate(fromCurrency, toCurrency, lock, future);
	} else {
		return monitorExchangeRateUpdate(fromCurrency, toCurrency, lock, future);
	}
}

// ReentrantLock을 잡은 생산자 스레드
private BigDecimal fetchPrimaryExchangeRate(Currency fromCurrency, Currency toCurrency, ReentrantLock lock, CompletableFuture<BigDecimal> future) {
	try {
		// 환율 Open API 호출
                future.complete(latestExchangeRate);
	} finally {
		lock.unlock();
	}
}

// ReentrantLock 못 잡은 소비자 스레드, 생산자 스레드의 작업을 기다리며 업데이트된 값 읽고 모든 로직 탈출
private BigDecimal monitorExchangeRateUpdate(Currency fromCurrency, Currency toCurrency, ReentrantLock lock, CompletableFuture<BigDecimal> future) throws InterruptedException {
	return future.orTimeout(FUTURE_TIMEOUT, TimeUnit.MILLISECONDS)
            .thenApply(result -> {
				log.info("업데이트된 환율({}, {}) 사용", fromCurrency, result);
				return result;
			})
            // 이후 코드 생략
}
```
https://github.com/imzero238/exchange-rate-open-api-test/blob/feat/add-future-complete-get/src/main/java/com/nayoung/exchangerateopenapitest/domain/exchangerate/ExchangeRateService.java#L136

condition 제거하고
- 소비자는 CompletableFuture.get 에서 대기 (timeout 설정으로 orTimeout 작성)
- 생산자는 CompletableFuture.complete 호출하는 방법으로 변경했습니다.

<br>

방법 1 ~ 3 모두 구현 의도대로 1개의 스레드만 Open API를 호출하고
- 나머지 스레드는 모두 대기하다가 로컬 캐시 or 생산자로 값을 전달받아 Open API 호출 없이 로직 탈출합니다.
- 하지만 대기하는 스레드가 어느 지점에서 대기하는가.. 기술의 특성은 잘 살리고 있는가...를 고민하다 보니 해당 과정까지 오게 되었습니다.
- 방법 1 ~ 3 과정까지 진행하면서 놓친 부분이 있는지, 최종 코드인 방법 3에서 잘못된 점은 없는지 피드백 받고 싶습니다! 