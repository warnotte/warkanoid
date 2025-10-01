package io.github.warnotte.warkanoid;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class Brick {
    public enum Type {
        NORMAL,
        BOMB,
        INDESTRUCTIBLE
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

    public void renderShadow(ShapeRenderer shapeRenderer, float shadowOffsetX, float shadowOffsetY) {
        if (destroyed) return;

        shapeRenderer.setColor(0f, 0f, 0f, 0.35f);
        shapeRenderer.rect(bounds.x + shadowOffsetX, bounds.y + shadowOffsetY, bounds.width, bounds.height);

        if (type == Type.BOMB) {
            float centerX = bounds.x + bounds.width / 2f;
            float centerY = bounds.y + bounds.height / 2f;
            float size = 3f;
            shapeRenderer.setColor(0f, 0f, 0f, 0.45f);
            shapeRenderer.rect(centerX - size + shadowOffsetX, centerY - 1f + shadowOffsetY, size * 2f, 2f);
            shapeRenderer.rect(centerX - 1f + shadowOffsetX, centerY - size + shadowOffsetY, 2f, size * 2f);
        }
    }

    public void render(ShapeRenderer shapeRenderer) {
        if (destroyed) return;

        shapeRenderer.setColor(color);
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);

        if (type == Type.BOMB) {
            float centerX = bounds.x + bounds.width / 2f;
            float centerY = bounds.y + bounds.height / 2f;
            float size = 3f;
            shapeRenderer.setColor(Color.BLACK);
            shapeRenderer.rect(centerX - size, centerY - 1f, size * 2f, 2f);
            shapeRenderer.rect(centerX - 1f, centerY - size, 2f, size * 2f);
        } else if (type == Type.INDESTRUCTIBLE) {
            shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
            float stripeWidth = 2f;
            for (float offset = -bounds.height; offset < bounds.width; offset += 6f) {
                float x1 = bounds.x + offset;
                float y1 = bounds.y + bounds.height;
                float x2 = bounds.x + offset + bounds.height;
                float y2 = bounds.y;

                if (x1 < bounds.x) { y1 -= (bounds.x - x1); x1 = bounds.x; }
                if (x2 > bounds.x + bounds.width) { y2 += (x2 - bounds.x - bounds.width); x2 = bounds.x + bounds.width; }

                if (x2 > x1) {
                    shapeRenderer.rectLine(x1, y1, x2, y2, stripeWidth);
                }
            }
        }
    }

    public boolean hit() {
        // Indestructible bricks cannot be destroyed
        if (type == Type.INDESTRUCTIBLE) {
            return false;
        }

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

    public boolean isIndestructible() {
        return type == Type.INDESTRUCTIBLE;
    }
}
