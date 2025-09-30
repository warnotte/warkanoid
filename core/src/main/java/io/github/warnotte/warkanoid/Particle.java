package io.github.warnotte.warkanoid;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class Particle {
    private Vector2 position;
    private Vector2 velocity;
    private Color color;
    private float size;
    private float life;
    private float maxLife;

    public Particle(float x, float y, Color color) {
        this.position = new Vector2(x, y);
        this.color = color.cpy();
        this.size = 2f + (float)(Math.random() * 3f); // Random size 2-5
        this.maxLife = 1f + (float)(Math.random() * 0.5f); // Life 1-1.5 seconds
        this.life = maxLife;

        // Random velocity in all directions
        float angle = (float)(Math.random() * 360);
        float speed = 50f + (float)(Math.random() * 100f); // Speed 50-150
        this.velocity = new Vector2(
            (float)(speed * Math.cos(Math.toRadians(angle))),
            (float)(speed * Math.sin(Math.toRadians(angle)))
        );
    }

    public void update(float deltaTime) {
        // Update position
        position.x += velocity.x * deltaTime;
        position.y += velocity.y * deltaTime;

        // Apply gravity
        velocity.y -= 200f * deltaTime;

        // Reduce life
        life -= deltaTime;

        // Fade out as life decreases
        float alpha = life / maxLife;
        color.a = alpha;

        // Shrink as life decreases
        size = size * alpha;
    }

    public void render(ShapeRenderer shapeRenderer, float shadowOffsetX, float shadowOffsetY) {
        if (life > 0) {
            shapeRenderer.setColor(color);
            shapeRenderer.circle(position.x, position.y, size);
        }
    }

    public boolean isDead() {
        return life <= 0;
    }

    public float getX() {
        return position.x;
    }

    public float getY() {
        return position.y;
    }
}
