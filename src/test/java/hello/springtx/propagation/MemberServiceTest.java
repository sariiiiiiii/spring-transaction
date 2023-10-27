package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.rmi.UnexpectedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired LogRepository logRepository;

    /**
     * MemberService    @Transactional:OFF
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON
     */
    @Test
    void outerTxOff_success() {
        //given
        String username = "outerTxOff_success";

        //when
        memberService.joinV1(username);

        //then : 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());

    }

    /**
     * MemberService    @Transactional:OFF
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON (Exception 발생)
     */
    @Test
    void outerTxOff_fail() {
        //given
        String username = "로그예외 outerTxOff_fail";

        //when
        assertThatThrownBy(() -> memberService.joinV1(username))
                        .isInstanceOf(RuntimeException.class);

        //then : log 데이터는 롤백된다
        assertTrue(memberRepository.find(username).isPresent()); // 멤버는 잘 저장됨
        assertTrue(logRepository.find(username).isEmpty()); // 로그는 단독적으로 롤백이 됨
    }

    /**
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:OFF
     * LogRepository    @Transactional:OFF
     */
    @Test
    void singleTx() {
        //given
        String username = "singleTx";

        //when
        memberService.joinV1(username);

        //then : 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());

    }

    /**
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON
     */
    @Test
    void outerTxOn_success() {
        //given
        String username = "outerTxOn_success";

        //when
        memberService.joinV1(username);

        //then : 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());

    }

    /**
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON (Exception 발생)
     * 회원과 회원 이력 로그를 처리하는 부분을 하나의 트랜잭션으로 묶은 덕분에 문제가 발생했을 때 회원과 회원 이력 로그가 모두 함께 롤백된다
     * 따라서 데이터 정합성에 문제가 발생하지 않는다
     */
    @Test
    void outerTxOn_fail() {

        //given
        String username = "로그예외 outerTxOn_fail";

        //when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        //then : 모든 데이터가 롤백된다
        assertTrue(memberRepository.find(username).isEmpty()); // 멤버는 잘 저장됨
        assertTrue(logRepository.find(username).isEmpty()); // 로그는 단독적으로 롤백이 됨
    }

    /**
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON (Exception 발생)
     */
    @Test
    void recoverException_fail() {

        /**
         * 내부 트랜잭션에서 rollbackOnly를 설정하기 때문에 결과적으로 정상 흐름 처리를 해서 외부 트랜잭션에서 커밋을 호출해도 물리 트랜잭션은 롤백된다
         * 그리고 UnexpectedRollbackException이 트랜잭션 동기호 매니저에 반환된다
         *
         * LogRepository에서 예외가 발생한다. 예외를 던지면 LogRepository의 트랜잭션 AOP가 해당 예외를 받는다
         * 신규 트랜잭션이 아니므로 물리 트랜잭션을 롤백하지는 않고, 트랜잭션 동기화 매니저에 rollbackOnly를 표시한다
         * 이후 트랜잭션 AOP는 전달 받은 예외를 밖으로 던진다
         * 예외가 MemberService에 던져지고, MemberService는 해당 예외를 복구한다. 그리고 정상적으로 리턴한다
         * 정상 흐름이 되었으므로, MemberService의 트랜잭션 AOP는 커밋을 호출한다
         * 커밋을 호출할 때 신규 트랜잭션임으로 실제 물리 트랜잭션을 커밋해야 한다. 이 때 rollbackOnly를 체크한다
         * rollbackOnly가 체크 되어 있으므로 물리 트랜잭션을 롤백한다
         * 트랜잭션 매니저는 UnexpectedRollbackException 예외를 던진다
         * 트랜잭션 AOPeh 전달받은 UnexpectedRollbackException을 클라이언트에 던진다
         *
         * - 정리 -
         * 논리 트랜잭션 중 하나라도 롤백되면 전체 트랜잭션은 롤백된다
         * 내부 트랜잭션이 롤백 되었는데, 외부 트랜잭션이 커밋되면 UnexpectedRollbackException 예외가 발생한다
         * rollbackOnly 상황에서 커밋이 발생하면 UnexpectedRollbackException 예외가 발생한다
         */

        //given
        String username = "로그예외 recoverException_fail";

        //when
        assertThatThrownBy(() -> memberService.joinV2(username))
                .isInstanceOf(UnexpectedException.class);

        //then : 모든 데이터가 롤백된다
        assertTrue(memberRepository.find(username).isEmpty()); // 롤백
        assertTrue(logRepository.find(username).isEmpty()); // 로그는 단독적으로 롤백이 됨
    }

    /**
     * MemberService    @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository    @Transactional:ON (REQUIRES_NEW) exception
     */
    @Test
    void recoverException_success() {

        /**
         * MemberRepository는 REQUIRES 옵션을 사용한다. 따라서 기존 트랜잭션에 참여한다
         * LogRepository의 트랜잭션 옵션에 REQUIRES_NEW를 사용했다
         * REQUIRES_NEW는 항상 새로운 트랜잭션을 만든다. 따라서 해당 트랜잭션 안에서는 DB 커넥션도 별도로 사용하게 된다
         *
         * REQUIRES_NEW를 사용하게 되면 물리 트랜잭션 자체가 완전히 분리되어 버린다
         * 그리고, REQUIRES_NEW는 신규 트랜잭션이므로, rollbackOnly 표시가 되지 않는다. 그냥 해당 트랜잭션이 물리 롤백되고 끝난다
         *
         * - 정리 -
         * 논리 트랜잭션은 하나라도 롤백되면 물리 트랜잭션은 롤백되어 버린다
         * 이 문제를 해결하려면 REQUIRES_NEW 를 사용해서 트랜잭션을 분리해야 한다
         * 참고로 예제를 단순화 하기 위해 MemberService가 MemberRepository, LogRepository만 호출하지만, 실제로는 더 많은
         * 레포지토리들을 호출하고 그 중에 LogRepository만 트랜잭션을 분리한다고 생각해보면 이해하는데 도움이 될 것이다
         *
         * - 주의 -
         * REQUIRES_NEW를 사용하면 하나의 HTTP 요청에 동시에 2개의 데이터베이스 커넥션으르 사용하게 된다.
         * 따라서 성능이 중요한 곳에서는 이런 부분을 주의해서 사용해야 한다
         * REQUIRES_NEW 를 사용하지 않고 문제를 해결할 수 있는 단순한 방법이 있다면, 그 방법을 선택하는 것이 더 좋다
         */

        //given
        String username = "로그예외 recoverException_success";

        //when
        memberService.joinV2(username);

        //then : @Transaction(REQUIRES_NEW) 를 적용함으로써 log만 롤백
        assertTrue(memberRepository.find(username).isPresent()); // 멤버는 잘 저장됨
        assertTrue(logRepository.find(username).isEmpty()); // 로그는 단독적으로 롤백이 됨
    }

}