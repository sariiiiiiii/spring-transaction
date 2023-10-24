package hello.springtx.exception;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class RollbackTest {

    @Autowired RollbackService service;

    @Test
    void runtimeException() {

        /**
         * properties 파일에 DEBUG 모드로 설정 한 후 실행시킨 후 로그를 보면
         * RuntimeException은 Initiating transaction rollback 인 것을 볼 수 있다
         */

        Assertions.assertThatThrownBy(() -> service.runtimeException())
                        .isInstanceOf(RuntimeException.class);
    }

    @Test
    void checkedException() {

        /**
         * properties 파일에 DEBUG 모드로 설정 한 후 실행시킨 후 로그를 보면
         * checkedException은 Initiating transaction commit 인 것을 볼 수 있다
         */

        Assertions.assertThatThrownBy(() -> service.checkedException())
                .isInstanceOf(MyException.class);
    }

    @Test
    void rollbackForException() {

        /**
         * properties 파일에 DEBUG 모드로 설정 한 후 실행시킨 후 로그를 보면
         * checkedException이 발생하지만 @Transactional(rollbackFor = MyException.class) 설정을 한 매소드는
         * Initiating transaction rollback 인 것을 볼 수 있다
         */

        Assertions.assertThatThrownBy(() -> service.rollbackForException())
                .isInstanceOf(MyException.class);
    }

    @TestConfiguration
    static class RollBackTestConfig {

        @Bean
        RollbackService rollbackService() {
            return new RollbackService();
        }

    }


    @Slf4j
    static class RollbackService {

        //런타임 예외 발생: 롤백
        @Transactional
        public void runtimeException() {
            log.info("call runtimeException");
            throw new RuntimeException();
        }

        //체크 예외 발생: 커밋
        public void checkedException() throws MyException {
            log.info("call checkedException");
            throw new MyException();
        }

        //체크 예외 rollbackFor 지정 : 강제 롤백
        @Transactional(rollbackFor = MyException.class)
        public void rollbackForException() throws MyException {
            log.info("call rollbackForException");
            throw new MyException();
        }

    }

    static class MyException extends Exception {
    }


}
