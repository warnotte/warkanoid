package io.github.warnotte.warkanoid;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;

public class Ball {
    private Circle bounds;
    private Vector2 velocity;
    private float speed;
    private BallTrail trail;
    private float stickyOffsetX; // Offset from paddle center when stuck

    public Ball(float x, float y, float radius) {
        this.bounds = new Circle(x, y, radius);
        this.velocity = new Vector2(0f, 0f); // stopped initially
        this.speed = 300f; // pixels per second
        this.trail = new BallTrail();
        this.stickyOffsetX = 0f;
    }

    public void update(float deltaTime, float gameWidth, float gameHeight) {
        // Continuous collision detection - subdivide movement into small steps
        float stepSize = bounds.radius; // Move at most one radius per step
        float distance = velocity.len() * deltaTime;
        int steps = Math.max(1, (int) Math.ceil(distance / stepSize));

        float stepX = (velocity.x * deltaTime) / steps;
        float stepY = (velocity.y * deltaTime) / steps;

        for (int i = 0; i < steps; i++) {
            bounds.x += stepX;
            bounds.y += stepY;

            // Add trail point only if ball is moving
            if (velocity.len() > 0) {
                trail.addPoint(bounds.x, bounds.y);
            }

            // Check collision with walls after each step
            if (bounds.x - bounds.radius <= 0 || bounds.x + bounds.radius >= gameWidth) {
                reverseX();
                // Keep ball in bounds
                if (bounds.x - bounds.radius < 0) bounds.x = bounds.radius;
                if (bounds.x + bounds.radius > gameWidth) bounds.x = gameWidth - bounds.radius;
                break; // Stop movement for this frame after collision
            }

            if (bounds.y + bounds.radius >= gameHeight) {
                reverseY();
                bounds.y = gameHeight - bounds.radius;
                break; // Stop movement for this frame after collision
            }
        }

        // Update trail
        trail.update(deltaTime);
    }

    public boolean isOutOfBounds(float gameHeight) {
        return bounds.y - bounds.radius < 0;
    }

    public void render(ShapeRenderer shapeRenderer) {
        // Render trail first (behind ball)
        trail.render(shapeRenderer);

        // Render ball
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(bounds.x, bounds.y, bounds.radius);
    }

    public Circle getBounds() {
        return bounds;
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public void setVelocity(float x, float y) {
        velocity.set(x, y);
    }

    public void reverseX() {
        velocity.x = -velocity.x;
    }

    public void reverseY() {
        velocity.y = -velocity.y;
    }

    public float getX() {
        return bounds.x;
    }

    public float getY() {
        return bounds.y;
    }

    public void setPosition(float x, float y) {
        bounds.setPosition(x, y);
    }

    public float getRadius() {
        return bounds.radius;
    }

    public boolean checkCollisionWithPaddle(Paddle paddle, boolean stickyMode) {
        if (Intersector.overlaps(bounds, paddle.getBounds())) {
            if (stickyMode && velocity.y < 0) { // Only stick if ball is going down
                // Stop the ball and position it on paddle - keep X position where it hit
                velocity.set(0, 0);
                bounds.y = paddle.getY() + paddle.getHeight() + bounds.radius;
                // Don't change X position - keep it where the ball hit
                return true; // Signal that ball is now stuck
            } else {
                // Normal collision response
                // Calculate hit position on paddle (0.0 = left edge, 1.0 = right edge)
                float hitPos = (bounds.x - paddle.getX()) / paddle.getWidth();
                hitPos = Math.max(0, Math.min(1, hitPos)); // Clamp between 0 and 1

                // Calculate new velocity based on hit position
                float angle = (hitPos - 0.5f) * 120f; // -60 to +60 degrees
                float speedMagnitude = (float) Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);

                velocity.x = (float) (speedMagnitude * Math.sin(Math.toRadians(angle)));
                velocity.y = Math.abs(speedMagnitude * (float) Math.cos(Math.toRadians(angle))); // Always up

                // Move ball above paddle to prevent sticking
                bounds.y = paddle.getY() + paddle.getHeight() + bounds.radius;
            }

            return true;
        }
        return false;
    }

    // Backward compatibility
    public boolean checkCollisionWithPaddle(Paddle paddle) {
        return checkCollisionWithPaddle(paddle, false);
    }

    public int checkCollisionWithBrick(Brick brick) {
        if (!brick.isDestroyed() && Intersector.overlaps(bounds, brick.getBounds())) {
            boolean destroyed = brick.hit();

            // Better collision response - determine which side was hit
            float brickCenterX = brick.getX() + brick.getWidth() / 2f;
            float brickCenterY = brick.getY() + brick.getHeight() / 2f;

            float deltaX = bounds.x - brickCenterX;
            float deltaY = bounds.y - brickCenterY;

            // Determine collision side based on which axis has smaller penetration
            float overlapX = (brick.getWidth() / 2f + bounds.radius) - Math.abs(deltaX);
            float overlapY = (brick.getHeight() / 2f + bounds.radius) - Math.abs(deltaY);

            if (overlapX < overlapY) {
                // Hit from left or right
                reverseX();
            } else {
                // Hit from top or bottom
                reverseY();
            }

            return destroyed ? brick.getScore() : 0;
        }
        return 0;
    }

    public void launch() {
        velocity.set(200f, 200f);
    }

    public boolean isMoving() {
        return velocity.len() > 0;
    }

    public void followPaddle(Paddle paddle) {
        bounds.x = paddle.getX() + paddle.getWidth() / 2f + stickyOffsetX;
        bounds.y = paddle.getY() + paddle.getHeight() + bounds.radius;
    }

    public void clearTrail() {
        trail.clear();
    }

    public void setTrailColor(Color color) {
        trail.setColor(color);
    }

    public void setStickyOffset(float offsetX) {
        this.stickyOffsetX = offsetX;
    }

    public float getStickyOffsetX() {
        return stickyOffsetX;
    }
}