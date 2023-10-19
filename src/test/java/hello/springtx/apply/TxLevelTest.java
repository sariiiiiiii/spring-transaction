package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
public class TxLevelTest {

    @Autowired
    LevelService levelService;

    @TestConfiguration
    static class TxLevelConfig {
        @Bean
        LevelService levelService() {
            return new LevelService();
        }
    }

    @Test
    void orderTest() {
        levelService.write();
        levelService.read();
    }

    @Slf4j
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션이 만들어짐
    static class LevelService {

        @Transactional
        public void write() {
            log.info("call write");
            printTxInfo();
        }

        public void read() {
            log.info("call read");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("txActive = {}", txActive);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly(); // transaction readOnly 확인
            log.info("ReadOnly = {}", readOnly);
        }

    }


}
