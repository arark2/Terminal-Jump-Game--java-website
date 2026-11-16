package game;

/** [상속] Platform의 위치 정보, 조회 기능, update()를 물려받는다. */
public final class TimedPlatform extends Platform {
    public TimedPlatform(String text, double x, double y, double width) {
        super(text, x, y, width);
    }

    // [다형성] 같은 touched() 호출이라도 실제 발판 종류의 구현이 실행된다.
    @Override
    public void touched(double elapsed) {
        scheduleDisappear(elapsed, 0.8);
    }

    @Override
    public String getColor() { return "#e88a8a"; }
}
