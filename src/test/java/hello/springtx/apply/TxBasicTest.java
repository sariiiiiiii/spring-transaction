package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest // AOP가 동작해야 되니까 어노테이션 추가
public class TxBasicTest {

    @Autowired
    BasicService basicService;

    @Test
    void proxyCheck() {

        /**
         * AOP 적용 여부 확인
         */

        log.info("aop class = {}", basicService.getClass());
        assertThat(AopUtils.isAopProxy(basicService)).isTrue();

    }

    @Test
    void txTest() {
        basicService.tx();
        basicService.nonTx();
    }

    @TestConfiguration
    static class TxApplyBasicConfig {

        @Bean
        BasicService basicService() {
            return new BasicService();
        }

    }

    @Slf4j
    @Transactional
    static class BasicService {

//        @Transactional
        public void tx() {

            /**
             * txActive = true
             * @Transaction 어노테이션이 붙은 메소드는 현재 이 메소드 안에 트랜잭션이 수행이 된다는 것을 알 수 있음
             */

            log.info("call tx");
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("txActive = {}", txActive);
        }

        public void nonTx() {

            /**
             * txAcive = false
             * @Transaction이 없는 메소드는 현재 이 메소드에서 트랜잭션이 수행이 안되고 있다는 것을 볼 수 있음
             */

            log.info("call nonTx");
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive(); // false
            log.info("txActive = {}", txActive);
        }

    }

}
