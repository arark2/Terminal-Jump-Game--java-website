package game;

/** 외부 테스트 라이브러리 없이 실행하는 게임 로직 검사. */
public class OopBehaviorCheck {
    private static int count;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        count++;
    }
    public static void main(String[] args) {
        Platform normal = new NormalPlatform("normal", 0, 100, 300);
        Platform broken = new BreakablePlatform("broken", 0, 100, 300);
        Platform timed = new TimedPlatform("timed", 0, 100, 300);
        Platform finish = new FinishPlatform("finish", 0, 100, 300);
        // 동일한 부모 타입으로 네 종류에 같은 명령을 내린다.
        Platform[] all = {normal, broken, timed, finish};
        for (Platform platform : all) platform.touched(10);
        broken.touched(10.2);
        timed.touched(10.2);
        broken.update(10.349);
        check(broken.isActive(), "0.35초 전에는 유지");
        broken.update(10.35);
        check(!broken.isActive(), "0.35초 후 소멸, 재착지로 연장되지 않음");
        timed.update(10.799);
        check(timed.isActive(), "0.8초 전에는 유지");
        timed.update(10.8);
        check(!timed.isActive(), "0.8초 후 소멸, 재착지로 연장되지 않음");
        normal.update(100);
        finish.update(100);
        check(normal.isActive() && finish.isActive(), "일반/도착 발판 유지");
        check(finish.isFinish() && !normal.isFinish(), "도착 판정");
        check(broken.getColor().equals("#edb86e"), "발판별 색상");

        Player player = new Player();
        player.spawnOn(normal, 30);
        check(player.getY() == 76, "발판 위 생성");
        player.jump();
        player.update(0.02, 726);
        check(player.getY() < 76, "점프하면 상승");
        check(!player.canLand(normal, 100), "상승 중 착지 금지");
        // 공중 추가 점프가 속도를 초기화하지 않는지 별도 플레이어와 비교.
        Player reference = new Player();
        reference.spawnOn(normal, 30);
        reference.jump();
        reference.update(0.02, 726);
        player.jump();
        player.update(0.02, 726);
        reference.update(0.02, 726);
        check(player.getY() == reference.getY(), "공중 점프 금지");

        boolean landed = false;
        for (int i = 0; i < 200; i++) {
            double previousBottom = player.getY() + player.getHeight();
            player.update(0.01, 726);
            if (player.canLand(normal, previousBottom)) {
                player.land(normal);
                landed = true;
                break;
            }
        }
        check(landed && player.getY() == 76, "낙하 후 착지");
        check(!player.canLand(broken, 100), "소멸 발판 착지 금지");
        player.setRight(true);
        player.update(10, 726);
        check(player.getX() == 708, "오른쪽 경계 제한");
        player.setRight(false);
        player.setLeft(true);
        player.update(10, 726);
        check(player.getX() == 0, "왼쪽 경계 제한");
        System.out.println("PASS: " + count + " checks");
    }
}
