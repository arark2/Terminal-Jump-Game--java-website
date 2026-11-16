package game;

/** [상속] Platform의 위치 정보, 조회 기능, update()를 물려받는다. */
public final class NormalPlatform extends Platform {
    public NormalPlatform(String text, double x, double y, double width) {
        super(text, x, y, width);
    }

    // [다형성] 같은 touched() 호출이라도 실제 발판 종류의 구현이 실행된다.
    @Override
    public void touched(double elapsed) {
        // 일반 발판은 밟아도 사라지지 않는다.
    }

    @Override
    public String getColor() { return "#8bc9a5"; }
}
