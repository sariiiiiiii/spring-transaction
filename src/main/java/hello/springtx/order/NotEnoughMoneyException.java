package hello.springtx.order;

public class NotEnoughMoneyException extends Exception {

    /**
     * 결제 잔고가 부족하면 발생하는 비즈니스 예외
     * 이 예외가 발생할때는 롤백을 안하고 싶다
     * 롤백을 하지 않고 주문 데이터를 저장(커밋)을 할 것이다
     * 왜냐 ? 시스템적인 문제가 아니라 비즈니스 상황이 예외인 것이기 때문
     */

    public NotEnoughMoneyException(String message) {
        super(message);
    }

}
