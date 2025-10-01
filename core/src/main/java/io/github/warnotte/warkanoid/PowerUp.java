package io.github.warnotte.warkanoid;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class PowerUp {
    public enum Type {
        MULTI_BALL(Color.YELLOW),
        LARGE_PADDLE(Color.CYAN),
        SMALL_PADDLE(Color.MAGENTA),
        LASER(Color.WHITE),
        STICKY_PADDLE(Color.PINK),
        EXTRA_LIFE(Color.GREEN),
        SPEED_UP(Color.RED),
        SPEED_DOWN(Color.BLUE);

        public final Color color;

        Type(Color color) {
            this.color = color;
        }
    }

    private Rectangle bounds;
    private Type type;
    private float fallSpeed;
    private boolean collected;

    public PowerUp(float x, float y, Type type) {
        this.bounds = new Rectangle(x, y, 20f, 15f);
        this.type = type;
        this.fallSpeed = 100f; // pixels per second
        this.collected = false;
    }

    public void update(float deltaTime) {
        if (!collected) {
            bounds.y -= fallSpeed * deltaTime;
        }
    }

    public void render(ShapeRenderer shapeRenderer, RenderPass pass) {
        if (collected) {
            return;
        }

        boolean shadow = pass == RenderPass.SHADOW_MASK;

        if (shadow) {
            shapeRenderer.setColor(1f, 1f, 1f, 0.35f);
        } else {
            shapeRenderer.setColor(type.color);
        }
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);

        float borderX = bounds.x + 1f;
        float borderY = bounds.y + 1f;
        float borderW = bounds.width - 2f;
        float borderH = bounds.height - 2f;
        if (shadow) {
            shapeRenderer.setColor(1f, 1f, 1f, 0.3f);
        } else {
            shapeRenderer.setColor(Color.WHITE);
        }
        shapeRenderer.rect(borderX, borderY, borderW, borderH);

        float innerX = bounds.x + 2f;
        float innerY = bounds.y + 2f;
        float innerW = bounds.width - 4f;
        float innerH = bounds.height - 4f;
        if (shadow) {
            shapeRenderer.setColor(1f, 1f, 1f, 0.25f);
        } else {
            shapeRenderer.setColor(type.color);
        }
        shapeRenderer.rect(innerX, innerY, innerW, innerH);
    }

    public void render(ShapeRenderer shapeRenderer) {
        render(shapeRenderer, RenderPass.MAIN);
    }

    public boolean isOutOfBounds(float gameHeight) {
        return bounds.y + bounds.height < 0;
    }

    public boolean checkCollisionWithPaddle(Paddle paddle) {
        if (!collected && bounds.overlaps(paddle.getBounds())) {
            collected = true;
            return true;
        }
        return false;
    }

    public Type getType() {
        return type;
    }

    public boolean isCollected() {
        return collected;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public float getX() {
        return bounds.x;
    }

    public float getY() {
        return bounds.y;
    }
}
