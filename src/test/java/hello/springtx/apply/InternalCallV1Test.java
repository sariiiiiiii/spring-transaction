package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class InternalCallV1Test {

    /**
     * 외부에서 callService를 호출했을 떄 클래스단위에서는 @Transactional이 되어 있지 않고 callService 안에있는 internal() @Transactional이 되어 있는 메소드를 호출했을 때를 가정한 테스트
     */

    @Autowired CallService callService;

    @Test
    void printProxy() {
        log.info("callService class={}", callService.getClass());
    }

    @Test
    void internalCall() {

        /**
         * 클라이언트인 테스트 코드는 callService.internal()을 호출한다. 여기서 callService는 트랜잭션 프록시이다
         * callService의 트랜잭션 프록시가 호출된다
         * internal() 메소드에 @Transactional이 붙어 있으므로 트랜잭션 프록시는 트랜잭션을 적용한다
         * 트랜잭션 적용 후 실제 callService 객체 인스턴스의 internal()을 호출한다
         * 실제 callService가 처리를 완료하면 응답이 트랜잭션 프록시로 돌아오고, 트랜잭션 프록시는 트랜잭션을 완료한다
         */

        callService.internal();
    }

    @Test
    void externalCall() {

        /**
         * external()은 @Transactional 어노테이션이 없다. 따라서 트랜잭션 없이 시작한다. 그런데 내부에서 @Transactaionl이 있는 internal()을 호출하는 것을 확인할 수 있다
         * 이 경우 external()은 트랜잭션이 없지만 internal()에서는 트랜잭션이 적용되는 것 처럼 보인다
         *
         * 실행 로그를 보면 트랜잭션 관련 코드가 전혀 보이지 않는다. 프록시가 아닌 실제 callService에서 남긴 로그만 확인된다
         * 추가로 internal() 내부에서 호출한 tx active=false 로그를 통해 확실히 트랜잭션이 수행되지 않은 것을 확인할 수 있다
         * 우리의 기대와 다르게 internal()에서 트랜잭션이 전혀 적용되지 않았다. 왜 이런 문제가 발생하는 것일까?
         *
         * 클라이언트 테스트 코드는 callService.external()을 호출한다. 여기서 callService는 트랜잭션 프록시이다
         * callService의 트랜잭션 프록시가 호출된다
         * external() 메소드에는 @Transactional이 없다 따라서 트랜잭션 프록시는 트랜잭션을 적용하지 않는다
         * 트랜잭션 적용하지 않고, 실제 callService 객체 인스턴스의 external()을 호출한다
         * external()은 내부에서 internal() 메소드를 호출한다. 그런데 여기서 문제가 발생한다
         *
         * - 문제원인
         * 자바 언어세ㅓ 메소드 앞에 별도의 참조가 없으면 'this'라는 뜻으로 자기 자신의 인스턴스를 가리킨다
         * 결과적으로 자기 자신의 내부 메소드를 호출하는 this.internal()이 되는데, 여기서 'this'는 자기 자신을 가르키므로, 실제 대상 객체('target')의 인스턴스를 뜻한다
         * 결과적으로 이러한 내부 호출은 프록시를 거치지 않는다. 따라서 트랜잭션을 적용할 수 없다
         * 결과적으로 'target'에 있는 internal()을 직접 호출하게 된 것이다
         *
         * - 프록시 방식의 AOP 한계
         * @Transactinal를 사용하는 트랜잭션 AOP는 프록시를 사용한다. 프록시를 사용하면 메소드 내부 호출에 프록시를 적용할 수 없다
         *
         * - 그럼 어떻게 해결을 해야 할까?
         * 가장 단순한 방법은 내부 호출을 피하기 위해 internal() 메소드를 별도의 클래스로 분리하는 것이다
         */

        callService.external();
    }

    @TestConfiguration
    static class InternalCallV1TetConfig {
        @Bean
        CallService callService() {
            return new CallService();
        }
    }

    @Slf4j
    static class CallService {

        // 외부에서 호출하는 메소드 가정

        public void external() {
            log.info("call external");
            printTxInfo();
            internal();
        }

        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("txActive = {}", txActive);
        }

    }

}
