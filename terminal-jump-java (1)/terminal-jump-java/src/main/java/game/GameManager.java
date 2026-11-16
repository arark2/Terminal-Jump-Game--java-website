package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;

/** 감독: 시작/종료, 화면, 카메라, 발판 생성, 진행도, 키보드 입력을 통합한다. */
public class GameManager {
    private static final double W = 760, H = 520, FINISH_HEIGHT = 3600;
    private static final String[] WORDS = {
        "[INFO] SYSTEM_READY", "[WARN] MEMORY_LEAK", "return 0;", "process.exit(1);",
        "[ERROR] NULL_POINTER", "[DEBUG] CONNECTION_OK", "if (status == 200)",
        "[INFO] CACHE_UPDATED", "[WARN] TIMEOUT", "throw new Error();",
        "[TRACE] 0x0000F4", "[OK] BUILD_SUCCESS", "int main()", "404 NOT_FOUND",
        "[DEBUG] RETRY", "null", "EOF", "false", "0xFF", "ERR"
    };
    private static final Random RNG = new Random();
    private static final List<Platform> platforms = new ArrayList<>();
    private static Player player;
    private static double cameraY, highest, elapsed, lastFrame, lastSafeY;
    private static boolean finished, lost, finishCreated;
    private static Platform lastSafe;

    @JSFunctor interface FrameCallback extends org.teavm.jso.JSObject { void call(double time); }
    @JSFunctor interface KeyCallback extends org.teavm.jso.JSObject { void call(String key, boolean down); }

    @JSBody(params = {"callback"}, script = "requestAnimationFrame(callback);")
    private static native void requestFrame(FrameCallback callback);
    @JSBody(params = {"callback"}, script = "window.addEventListener('keydown', e => { if(['ArrowLeft','ArrowRight','ArrowUp',' ','a','d','w','r'].includes(e.key)) e.preventDefault(); callback(e.key, true); }); window.addEventListener('keyup', e => callback(e.key, false));")
    private static native void registerKeys(KeyCallback callback);
    @JSBody(script = "const c=document.getElementById('game'); return c.getContext('2d');")
    private static native org.teavm.jso.JSObject context();
    @JSBody(params={"ctx","x","y","w","h","color"}, script="ctx.fillStyle=color; ctx.fillRect(x,y,w,h);")
    private static native void rect(org.teavm.jso.JSObject ctx,double x,double y,double w,double h,String color);
    @JSBody(params={"ctx","text","x","y","color","size"}, script="ctx.font=size+'px monospace';ctx.fillStyle=color;ctx.fillText(text,x,y);")
    private static native void text(org.teavm.jso.JSObject ctx,String text,double x,double y,String color,int size);
    @JSBody(params={"text"}, script="const c=document.getElementById('game').getContext('2d'); c.font='15px monospace';return c.measureText(text).width;")
    private static native double textWidth(String text);
    @JSBody(params={"ctx","x","y"}, script="const img=document.getElementById('playerImage');if(img.complete && img.naturalWidth){ctx.drawImage(img,x,y,18,24);}else{ctx.fillStyle='#f6fff8';ctx.beginPath();ctx.moveTo(x,y);ctx.lineTo(x,y+21);ctx.lineTo(x+5,y+16);ctx.lineTo(x+10,y+23);ctx.lineTo(x+14,y+21);ctx.lineTo(x+9,y+14);ctx.lineTo(x+17,y+14);ctx.closePath();ctx.fill();}")
    private static native void cursor(org.teavm.jso.JSObject ctx,double x,double y);

    public static void main(String[] args) {
        reset();
        registerKeys((key, down) -> {
            if (key.equals("ArrowLeft") || key.equals("a")) player.setLeft(down);
            if (key.equals("ArrowRight") || key.equals("d")) player.setRight(down);
            if (down && (key.equals("ArrowUp") || key.equals("w") || key.equals(" "))) player.jump();
            if (down && key.equals("r") && (finished || lost)) reset();
        });
        requestFrame(GameManager::frame);
    }

    private static void reset() {
        platforms.clear(); player = new Player();
        elapsed = 0; lastFrame = -1; cameraY = -H + 100; highest = 0;
        finished = false; lost = false; finishCreated = false;
        // 시작 바닥은 확실히 착지 가능하며, 커서의 발이 바닥에 닿도록 배치한다.
        lastSafe = new NormalPlatform("[BOOT] PRESS SPACE TO JUMP", 130, 40, textWidth("[BOOT] PRESS SPACE TO JUMP"));
        platforms.add(lastSafe); player.spawnOn(lastSafe, 220);
        lastSafeY = lastSafe.getY();
        // 화면 위쪽까지 미리 안전 경로를 생성한다.
        generateUntil(lastSafeY - 900);
    }

    private static void frame(double millis) {
        double now = millis / 1000.0;
        double dt = lastFrame < 0 ? 0 : Math.min(.033, Math.max(0, now - lastFrame));
        lastFrame = now;
        if (!finished && !lost) update(dt);
        draw();
        requestFrame(GameManager::frame);
    }

    private static void update(double dt) {
        elapsed += dt;
        double previousBottom = player.getY() + player.getHeight();
        player.update(dt, W - 34);
        // 내려올 때만 윗면 충돌 판정. 한 프레임에 여러 발판과 닿으면 가장 위의 발판을 선택.
        Platform landing = null;
        for (Platform p : platforms) {
            p.update(elapsed);
            if (player.canLand(p, previousBottom) && (landing == null || p.getY() < landing.getY())) landing = p;
        }
        if (landing != null) {
            player.land(landing);
            // [다형성] Platform 변수에 들어 있는 실제 자식의 touched()를 호출한다.
            landing.touched(elapsed);
            if (landing.isFinish()) finished = true;
        }
        // 플레이어가 올라간 만큼 화면이 따라간다. 내려갈 때는 카메라가 되돌아가지 않는다.
        cameraY = Math.min(cameraY, player.getY() - H * .42);
        highest = Math.max(highest, lastSafeStartHeight() - player.getY());
        generateUntil(cameraY - 240);
        // 너무 아래로 떨어지면 실패. 화면 아래쪽에 남은 오래된 발판은 메모리에서 제거.
        if (player.getY() > cameraY + H + 90) lost = true;
        platforms.removeIf(p -> p.getY() > cameraY + H + 700 && p != lastSafe);
    }

    private static double lastSafeStartHeight() { return 40 - player.getHeight(); }

    /** 안전 경로를 먼저 만든다. 간격은 최대 점프 높이보다 작고, 옆으로 이동 가능한 범위 안에서 선택한다. */
    private static void generateUntil(double targetY) {
        int guard = 0;
        while (lastSafeY > targetY && !finishCreated && guard++ < 300) {
            double progress = Math.min(1, Math.max(0, (40 - lastSafeY) / FINISH_HEIGHT));
            // 점점 간격이 넓어지되 점프 최대 높이(약 110px)보다 여유 있게 낮춘다.
            double gap = 45 + progress * 35 + RNG.nextDouble() * 12;
            double nextY = lastSafeY - gap;
            if (40 - nextY >= FINISH_HEIGHT) {
                // 끝에 도달하면 마지막 안전 발판에서 갈 수 있는 EXIT 발판을 만든다.
                nextY = lastSafeY - 60;
                String label = "[EXIT] SYSTEM_COMPLETE";
                double width = textWidth(label);
                double x = clamp(lastSafe.getX() + (lastSafe.getWidth() - width) / 2, 8, W - 40 - width);
                Platform finish = new FinishPlatform(label, x, nextY, width);
                platforms.add(finish); finishCreated = true; break;
            }
            String label = pickWord(progress);
            double width = textWidth(label);
            // 점프 시간에 따른 수평 이동 가능 범위를 보수적으로 제한(약 90px 이내).
            double center = lastSafe.getX() + lastSafe.getWidth() / 2;
            double nextCenter = center + (RNG.nextDouble() * 2 - 1) * 85;
            double x = clamp(nextCenter - width / 2, 8, W - 40 - width);
            // 안전 경로 발판은 절대로 사라지지 않도록 NORMAL로 고정한다.
            Platform safe = new NormalPlatform(label, x, nextY, width);
            platforms.add(safe);
            // 추가 발판은 선택적 경로이므로 사라지는 발판을 넣을 수 있다.
            if (RNG.nextDouble() < .55) addOptionalPlatform(safe, progress);
            lastSafe = safe; lastSafeY = nextY;
        }
    }

    private static void addOptionalPlatform(Platform safe, double progress) {
        String label = pickWord(progress);
        double width = textWidth(label);
        double x = clamp(safe.getX() + (RNG.nextBoolean() ? 150 : -150), 8, W - 40 - width);
        double y = safe.getY() + 15 + RNG.nextDouble() * 20;
        // 발판끼리 겹치지 않도록 사각형 영역을 검사한다.
        for (Platform p : platforms) {
            if (Math.abs(p.getY() - y) < 24 && x < p.getX() + p.getWidth() + 12 && x + width + 12 > p.getX()) return;
        }
        double roll = RNG.nextDouble();
        Platform optional;
        if (roll < .2 + progress * .35) {
            optional = new BreakablePlatform(label, x, y, width);
        } else if (roll < .2 + progress * .35 + progress * .2) {
            optional = new TimedPlatform(label, x, y, width);
        } else {
            optional = new NormalPlatform(label, x, y, width);
        }
        platforms.add(optional);
    }

    private static String pickWord(double progress) {
        // 난이도가 높을수록 짧은 단어의 선택 확률이 증가한다.
        if (RNG.nextDouble() < progress * .75) return WORDS[14 + RNG.nextInt(WORDS.length - 14)];
        return WORDS[RNG.nextInt(14)];
    }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private static void draw() {
        org.teavm.jso.JSObject ctx = context();
        rect(ctx, 0, 0, W, H, "#101820");
        // 터미널처럼 보이는 장식용 로그 (충돌 판정 없음).
        text(ctx, "> terminal_jump.exe --running", 14, 25, "#476d58", 13);
        for (Platform p : platforms) {
            if (!p.isActive()) continue;
            double sy = p.getY() - cameraY;
            if (sy < -30 || sy > H + 30) continue;
            // [다형성] 종류 검사 없이 실제 자식 클래스의 색상을 사용한다.
            String color = p.getColor();
            text(ctx, p.getText(), p.getX(), sy - 4, color, 15);
            // 글자 폭과 동일한 보이지 않는 윗면이 충돌 영역이다.
        }
        cursor(ctx, player.getX(), player.getY() - cameraY);
        // 진행도는 현재 최고 높이 / 목표 높이. 오른쪽 막대가 위로 차오른다.
        rect(ctx, W - 22, 44, 8, H - 84, "#2c4135");
        double ratio = clamp(highest / FINISH_HEIGHT, 0, 1);
        double barHeight = (H - 84) * ratio;
        rect(ctx, W - 22, H - 40 - barHeight, 8, barHeight, "#8bc9a5");
        text(ctx, "HEIGHT " + (int)highest + " / " + (int)FINISH_HEIGHT, 14, H - 18, "#b4ddc2", 13);
        if (finished || lost) {
            rect(ctx, 90, 175, W - 180, 150, "#1b3026");
            text(ctx, finished ? "SYSTEM COMPLETE - CLEAR!" : "PROCESS TERMINATED - GAME OVER", 130, 230, finished ? "#d4f7ad" : "#f0a0a0", 21);
            text(ctx, "Press R to restart", 230, 273, "#b4ddc2", 16);
        }
    }
}
