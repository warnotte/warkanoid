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

    private Rectangle bounds;
    private float speed;
    private Mode mode;
    private float modeTimer; // Timer for automatic mode reset

    public Paddle(float x, float y, float width, float height) {
        this.bounds = new Rectangle(x, y, width, height);
        this.speed = 300f; // pixels per second
        this.mode = Mode.NORMAL;
        this.modeTimer = 0f;
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

    public void render(ShapeRenderer shapeRenderer) {
        // Set paddle color based on mode
        switch (mode) {
            case NORMAL:
                shapeRenderer.setColor(Color.WHITE);
                break;
            case STICKY:
                shapeRenderer.setColor(Color.GREEN);
                break;
            case LASER:
                shapeRenderer.setColor(Color.RED);
                break;
        }

        // Draw main paddle
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);

        // Add mode-specific visual indicators
        if (mode == Mode.LASER) {
            // Draw laser cannon barrels
            shapeRenderer.setColor(Color.CYAN);
            float barrelWidth = 3f;
            float barrelHeight = 8f;
            // Left barrel
            shapeRenderer.rect(bounds.x + bounds.width * 0.25f - barrelWidth/2,
                             bounds.y + bounds.height, barrelWidth, barrelHeight);
            // Right barrel
            shapeRenderer.rect(bounds.x + bounds.width * 0.75f - barrelWidth/2,
                             bounds.y + bounds.height, barrelWidth, barrelHeight);
        } else if (mode == Mode.STICKY) {
            // Draw sticky indicator dots
            shapeRenderer.setColor(Color.YELLOW);
            float dotSize = 2f;
            for (int i = 0; i < 5; i++) {
                float x = bounds.x + (bounds.width / 6f) * (i + 1) - dotSize/2;
                shapeRenderer.rect(x, bounds.y + bounds.height - 3f, dotSize, dotSize);
            }
        }
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
    }

    public void setMode(Mode newMode) {
        this.mode = newMode;
        this.modeTimer = 0f; // Permanent until changed
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
}