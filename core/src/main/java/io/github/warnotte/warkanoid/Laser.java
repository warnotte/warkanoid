package io.github.warnotte.warkanoid;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class Laser {
    private Rectangle bounds;
    private float speed;
    private boolean active;

    public Laser(float x, float y) {
        this.bounds = new Rectangle(x - 1f, y, 2f, 10f); // Thin laser beam
        this.speed = 400f; // Fast moving upward
        this.active = true;
    }

    public void update(float deltaTime) {
        if (active) {
            bounds.y += speed * deltaTime;
        }
    }

    public void renderShadow(ShapeRenderer shapeRenderer, float shadowOffsetX, float shadowOffsetY) {
        if (!active) return;
        shapeRenderer.setColor(0f, 0f, 0f, 0.4f);
        shapeRenderer.rect(bounds.x + shadowOffsetX, bounds.y + shadowOffsetY, bounds.width, bounds.height);
    }

    public void render(ShapeRenderer shapeRenderer) {
        if (!active) return;
        shapeRenderer.setColor(Color.CYAN);
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public boolean isOutOfBounds(float gameHeight) {
        return bounds.y > gameHeight;
    }

    public boolean checkCollisionWithBrick(Brick brick) {
        if (active && !brick.isDestroyed() && bounds.overlaps(brick.getBounds())) {
            active = false; // Laser disappears on impact
            return true;
        }
        return false;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }

    public float getX() {
        return bounds.x;
    }

    public float getY() {
        return bounds.y;
    }
}
