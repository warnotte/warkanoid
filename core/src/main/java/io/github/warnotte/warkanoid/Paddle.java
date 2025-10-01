package io.github.warnotte.warkanoid;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;


public class Paddle {
    public enum Mode {
        NORMAL,
        STICKY,
        LASER
    }

    // Mode colors - centralized for consistency
    public static final Color COLOR_NORMAL = Color.WHITE;
    public static final Color COLOR_STICKY = Color.GREEN;
    public static final Color COLOR_LASER = Color.RED;

    private Rectangle bounds;
    private float speed;
    private Mode mode;
    private float modeTimer; // Timer for automatic mode reset
    private float initialModeTimer; // Store initial duration for progress calculation

    public Paddle(float x, float y, float width, float height) {
        this.bounds = new Rectangle(x, y, width, height);
        this.speed = 300f; // pixels per second
        this.mode = Mode.NORMAL;
        this.modeTimer = 0f;
        this.initialModeTimer = 0f;
    }

    public void update(float deltaTime, float gameWidth) {
        // Update mode timer
        if (mode != Mode.NORMAL && modeTimer > 0) {
            modeTimer -= deltaTime;
            if (modeTimer <= 0) {
                mode = Mode.NORMAL;
            }
        }

        // Movement with arrow keys
        if (com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT)) {
            bounds.x -= speed * deltaTime;
        }
        if (com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT)) {
            bounds.x += speed * deltaTime;
        }

        // Keep paddle within screen bounds
        if (bounds.x < 0) bounds.x = 0;
        if (bounds.x + bounds.width > gameWidth) bounds.x = gameWidth - bounds.width;
    }

    public void updateWithMouse(float deltaTime, float gameWidth, float mouseX) {
        // Update mode timer
        if (mode != Mode.NORMAL && modeTimer > 0) {
            modeTimer -= deltaTime;
            if (modeTimer <= 0) {
                mode = Mode.NORMAL;
            }
        }

        // Center paddle on mouse X position
        bounds.x = mouseX - bounds.width / 2f;

        // Keep paddle within screen bounds
        if (bounds.x < 0) bounds.x = 0;
        if (bounds.x + bounds.width > gameWidth) bounds.x = gameWidth - bounds.width;
    }

    public void render(ShapeRenderer shapeRenderer, RenderPass pass) {
        boolean shadow = pass == RenderPass.SHADOW_MASK;

        if (shadow) {
            shapeRenderer.setColor(1f, 1f, 1f, 0.35f);
        } else {
            shapeRenderer.setColor(getModeColor());
        }
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);

        if (mode == Mode.LASER) {
            float barrelWidth = 3f;
            float barrelHeight = 8f;
            float leftX = bounds.x + bounds.width * 0.25f - barrelWidth / 2f;
            float rightX = bounds.x + bounds.width * 0.75f - barrelWidth / 2f;
            float barrelY = bounds.y + bounds.height;
            if (shadow) {
                shapeRenderer.setColor(1f, 1f, 1f, 0.3f);
            } else {
                shapeRenderer.setColor(Color.CYAN);
            }
            shapeRenderer.rect(leftX, barrelY, barrelWidth, barrelHeight);
            shapeRenderer.rect(rightX, barrelY, barrelWidth, barrelHeight);
        } else if (mode == Mode.STICKY) {
            float dotSize = 2f;
            float dotY = bounds.y + bounds.height - 3f;
            if (shadow) {
                shapeRenderer.setColor(1f, 1f, 1f, 0.3f);
            } else {
                shapeRenderer.setColor(Color.YELLOW);
            }
            for (int i = 0; i < 5; i++) {
                float x = bounds.x + (bounds.width / 6f) * (i + 1) - dotSize / 2f;
                shapeRenderer.rect(x, dotY, dotSize, dotSize);
            }
        }

        if (mode != Mode.NORMAL && modeTimer > 0) {
            float barHeight = 3f;
            float barY = bounds.y - barHeight - 2f;
            float barWidth = bounds.width;
            float progress = getInitialModeTimer() > 0f ? modeTimer / getInitialModeTimer() : 1f;
            float progressWidth = shadow ? barWidth : barWidth * Math.min(progress, 1f);
            if (shadow) {
                shapeRenderer.setColor(1f, 1f, 1f, 0.4f);
            } else {
                shapeRenderer.setColor(getModeColor());
            }
            shapeRenderer.rect(bounds.x, barY, progressWidth, barHeight);
        }
    }

    public void render(ShapeRenderer shapeRenderer) {
        render(shapeRenderer, RenderPass.MAIN);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void setX(float x) {
        bounds.x = x;
    }

    public float getX() {
        return bounds.x;
    }

    public float getY() {
        return bounds.y;
    }

    public float getWidth() {
        return bounds.width;
    }

    public float getHeight() {
        return bounds.height;
    }

    public float getSpeed() {
        return speed;
    }

    public void setWidth(float width) {
        bounds.width = width;
    }

    // Mode management methods
    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode newMode, float duration) {
        this.mode = newMode;
        this.modeTimer = duration;
        this.initialModeTimer = duration;
    }

    public void setMode(Mode newMode) {
        this.mode = newMode;
        this.modeTimer = 0f; // Permanent until changed
        this.initialModeTimer = 0f;
    }

    public boolean isSticky() {
        return mode == Mode.STICKY;
    }

    public boolean isLaser() {
        return mode == Mode.LASER;
    }

    public float getModeTimer() {
        return modeTimer;
    }

    private float getInitialModeTimer() {
        return initialModeTimer;
    }

    private Color getModeColor() {
        switch (mode) {
            case STICKY:
                return COLOR_STICKY;
            case LASER:
                return COLOR_LASER;
            case NORMAL:
            default:
                return COLOR_NORMAL;
        }
    }
}
