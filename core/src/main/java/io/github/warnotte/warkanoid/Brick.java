package io.github.warnotte.warkanoid;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class Brick {
    public enum Type {
        NORMAL,
        BOMB
    }

    private Rectangle bounds;
    private Color color;
    private Color originalColor;
    private boolean destroyed;
    private int hits;
    private int maxHits;
    private Type type;

    public Brick(float x, float y, float width, float height, Color color) {
        this(x, y, width, height, color, 1, Type.NORMAL);
    }

    public Brick(float x, float y, float width, float height, Color color, int maxHits) {
        this(x, y, width, height, color, maxHits, Type.NORMAL);
    }

    public Brick(float x, float y, float width, float height, Color color, int maxHits, Type type) {
        this.bounds = new Rectangle(x, y, width, height);
        this.originalColor = color.cpy();
        this.color = color.cpy();
        this.destroyed = false;
        this.hits = 0;
        this.maxHits = maxHits;
        this.type = type;
    }

    public void render(ShapeRenderer shapeRenderer) {
        if (!destroyed) {
            shapeRenderer.setColor(color);
            shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);

            // Draw bomb indicator (X mark)
            if (type == Type.BOMB) {
                shapeRenderer.setColor(Color.BLACK);
                float centerX = bounds.x + bounds.width / 2f;
                float centerY = bounds.y + bounds.height / 2f;
                float size = 3f;

                // Draw X with small rectangles
                shapeRenderer.rect(centerX - size, centerY - 1f, size * 2, 2f);
                shapeRenderer.rect(centerX - 1f, centerY - size, 2f, size * 2);
            }
        }
    }

    public boolean hit() {
        hits++;
        if (hits >= maxHits) {
            destroyed = true;
            return true; // Brick is destroyed
        } else {
            // Change color to show damage (make darker)
            float alpha = 1.0f - ((float)hits / maxHits) * 0.5f;
            color.set(originalColor.r * alpha, originalColor.g * alpha, originalColor.b * alpha, 1f);
            return false; // Brick still alive
        }
    }

    public void destroy() {
        destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
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

    public float getWidth() {
        return bounds.width;
    }

    public float getHeight() {
        return bounds.height;
    }

    public int getMaxHits() {
        return maxHits;
    }

    public int getHits() {
        return hits;
    }

    public int getScore() {
        // Score based on brick type
        switch (maxHits) {
            case 1: return 10;
            case 2: return 20;
            case 3: return 30;
            default: return 50;
        }
    }

    public Color getOriginalColor() {
        return originalColor;
    }

    public Type getType() {
        return type;
    }

    public boolean isBomb() {
        return type == Type.BOMB;
    }
}