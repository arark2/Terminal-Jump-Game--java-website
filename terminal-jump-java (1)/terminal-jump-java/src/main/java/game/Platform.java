package game;

/**
 * [추상화] 모든 발판의 공통 데이터와 기능을 정의하는 추상 부모 클래스.
 * 종류별 착지 반응과 색상은 자식 클래스가 구현한다.
 */
public abstract class Platform {
    // [캡슐화] 외부에서 좌표나 소멸 상태를 직접 변경하지 못하도록 보호한다.
    private final String text;
    private final double x, y, width;
    private boolean active = true;
    private double disappearAt = -1;

    protected Platform(String text, double x, double y, double width) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public final String getText() { return text; }
    public final double getX() { return x; }
    public final double getY() { return y; }
    public final double getWidth() { return width; }
    public final boolean isActive() { return active; }

    // [추상화] 구체적인 착지 반응과 색상은 발판 종류가 결정한다.
    public abstract void touched(double elapsed);
    public abstract String getColor();

    public boolean isFinish() { return false; }

    // [캡슐화] 자식도 내부 필드를 직접 변경하지 않고 이 기능을 통해 소멸을 예약한다.
    // 처음 착지한 시각을 유지하므로 재착지해도 소멸 시간이 연장되지 않는다.
    protected final void scheduleDisappear(double elapsed, double delay) {
        if (disappearAt < 0) disappearAt = elapsed + delay;
    }

    // [상속] 모든 자식 발판이 이 공통 소멸 처리 기능을 그대로 물려받는다.
    public final void update(double elapsed) {
        if (disappearAt >= 0 && elapsed >= disappearAt) active = false;
    }
}
