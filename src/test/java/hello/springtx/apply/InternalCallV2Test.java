package hello.springtx.apply;

import lombok.RequiredArgsConstructor;
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
public class InternalCallV2Test {

    @Autowired CallService callService;

    @Test
    void printProxy() {
        log.info("callService class={}", callService.getClass());
    }

    @Test
    void externalCallV2() {

        /**
         * 클라이언트인 테스트 코드는 callService.external()을 호출한다
         * callService는 실제 callService 객체 인스턴스이다
         * callService는 주입받은 internalService.internal()을 호출한다
         * internalService는 트랜잭션 프록시이다. internal() 메소드에 @Transactional이 붙어 있으므로 트랜잭션 프록시는 트랜잭션을 적용한다
         * 트랜잭션 적용 후 실제 internalService 객체 인스턴스의 internal()을 호출한다
         */

        callService.external();
    }

    @TestConfiguration
    static class InternalCallV1TetConfig {
        @Bean
        CallService callService() {
            return new CallService(internalService());
        }

        @Bean
        InternalService internalService() {
            return new InternalService();
        }

    }

    @Slf4j
    @RequiredArgsConstructor
    static class CallService {

        private final InternalService internalService;

        // 외부에서 호출하는 메소드 가정

        public void external() {
            log.info("call external");
            printTxInfo();
            internalService.internal(); // 내부메소드 호출이였던것을 외부 호출로 변경
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("txActive = {}", txActive);
        }

    }

    static class InternalService {

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
