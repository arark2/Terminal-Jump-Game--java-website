package game;

/** 플레이어: 커서의 위치, 중력, 점프, 발판 착지 판정을 담당한다. */
public class Player {
    private double x = 210, y = 0, vy = 0;
    private final double width = 18, height = 24;
    private boolean grounded = false;
    private boolean left = false, right = false;
    public static final double SPEED = 230, GRAVITY = 1050, JUMP_SPEED = 480;

    // [캡슐화] 감독은 필드를 직접 바꾸지 않고 공개된 기능으로 조작한다.
    public double getX() { return x; }
    public double getY() { return y; }
    public double getHeight() { return height; }
    public void setLeft(boolean pressed) { left = pressed; }
    public void setRight(boolean pressed) { right = pressed; }

    public void spawnOn(Platform platform, double startX) {
        x = startX;
        land(platform);
    }

    /** 매 프레임 이동한다. dt는 초 단위이며 긴 프레임은 감독이 제한한다. */
    public void update(double dt, double worldWidth) {
        grounded = false; // 이번 프레임의 착지 판정에서 다시 설정한다
        double axis = (right ? 1 : 0) - (left ? 1 : 0);
        x += axis * SPEED * dt;
        x = Math.max(0, Math.min(worldWidth - width, x));
        vy += GRAVITY * dt; // 아래쪽이 양수인 좌표계: 중력으로 낙하속도 증가
        y += vy * dt;
    }

    /** 땅에 서 있을 때만 점프한다. */
    public void jump() {
        if (grounded) { vy = -JUMP_SPEED; grounded = false; }
    }

    /** 내려오며 발판 윗면을 통과하고 가로로 겹치면 착지한다. */
    public boolean canLand(Platform p, double previousBottom) {
        double footLeft = x + 4, footRight = x + width - 4;
        return p.isActive() && vy >= 0 && previousBottom <= p.getY() + 3
                && y + height >= p.getY() && footRight > p.getX() + 3 && footLeft < p.getX() + p.getWidth() - 3;
    }

    public void land(Platform p) {
        y = p.getY() - height;
        vy = 0;
        grounded = true;
    }
}
