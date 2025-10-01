package io.github.warnotte.warkanoid;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;

public class Ball {
    private Circle bounds;
    private Vector2 velocity;
    private float speed;
    private BallTrail trail;
    private float stickyOffsetX; // Offset from paddle center when stuck
    private final Color baseTrailColor = new Color(Color.WHITE);
    private final Color trailTint = new Color(Color.WHITE);
    private final Color ballColor = new Color(Color.WHITE);
    private static final float COLLISION_SEPARATION = 0.001f;

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

    public void renderShadow(ShapeRenderer shapeRenderer, float shadowOffsetX, float shadowOffsetY) {
        trail.renderShadow(shapeRenderer, shadowOffsetX, shadowOffsetY);
        shapeRenderer.setColor(0f, 0f, 0f, 0.4f);
        shapeRenderer.circle(bounds.x + shadowOffsetX, bounds.y + shadowOffsetY, bounds.radius);
    }

    public void render(ShapeRenderer shapeRenderer) {
        trail.render(shapeRenderer);
        shapeRenderer.setColor(ballColor);
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

    // OLD DISCRETE COLLISION - kept for comparison
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

    // NEW CONTINUOUS COLLISION DETECTION (Swept Circle-AABB)
    public int checkCollisionWithBrickSwept(Brick brick, float prevX, float prevY) {
        if (brick.isDestroyed()) {
            return 0;
        }

        // Note: Indestructible bricks still need collision response, just don't take damage

        // Expand brick by ball radius to treat ball as a point
        float expandedLeft = brick.getX() - bounds.radius;
        float expandedRight = brick.getX() + brick.getWidth() + bounds.radius;
        float expandedBottom = brick.getY() - bounds.radius;
        float expandedTop = brick.getY() + brick.getHeight() + bounds.radius;

        // Ray from previous position to current position
        float dx = bounds.x - prevX;
        float dy = bounds.y - prevY;

        // Check if ray intersects expanded AABB
        float tNear = Float.NEGATIVE_INFINITY;
        float tFar = Float.POSITIVE_INFINITY;

        // X axis slab
        if (Math.abs(dx) < 0.0001f) {
            // Ray parallel to X axis
            if (prevX < expandedLeft || prevX > expandedRight) {
                return 0; // No collision possible
            }
        } else {
            float t1 = (expandedLeft - prevX) / dx;
            float t2 = (expandedRight - prevX) / dx;
            if (t1 > t2) { float temp = t1; t1 = t2; t2 = temp; }
            tNear = Math.max(tNear, t1);
            tFar = Math.min(tFar, t2);
            if (tNear > tFar) return 0;
        }

        // Y axis slab
        if (Math.abs(dy) < 0.0001f) {
            // Ray parallel to Y axis
            if (prevY < expandedBottom || prevY > expandedTop) {
                return 0; // No collision possible
            }
        } else {
            float t1 = (expandedBottom - prevY) / dy;
            float t2 = (expandedTop - prevY) / dy;
            if (t1 > t2) { float temp = t1; t1 = t2; t2 = temp; }
            tNear = Math.max(tNear, t1);
            tFar = Math.min(tFar, t2);
            if (tNear > tFar) return 0;
        }

        // Check if collision happened during this frame (t in [0, 1])
        if (tNear > 1.0f || tFar < 0.0f) {
            return 0; // No collision this frame
        }

        // Ignore collisions that are too close to start (already overlapping/just separated)
        float minT = 0.001f; // Minimum t to consider a valid collision
        if (tNear < minT) {
            return 0; // Too close, ignore to prevent multiple hits
        }

        // Collision occurred!
        float collisionT = tNear;

        // Calculate collision point
        float collisionX = prevX + dx * collisionT;
        float collisionY = prevY + dy * collisionT;

        // Determine which face was hit based on collision point
        boolean hitVertical = false;
        boolean hitHorizontal = false;

        float epsilon = 0.1f;
        if (Math.abs(collisionX - expandedLeft) < epsilon || Math.abs(collisionX - expandedRight) < epsilon) {
            hitVertical = true;
        }
        if (Math.abs(collisionY - expandedBottom) < epsilon || Math.abs(collisionY - expandedTop) < epsilon) {
            hitHorizontal = true;
        }

        // Handle corner case - hit both faces
        if (hitVertical && hitHorizontal) {
            // Hit a corner - reverse both
            reverseX();
            reverseY();
        } else if (hitVertical) {
            reverseX();
        } else if (hitHorizontal) {
            reverseY();
        } else {
            // Default: use overlap method as fallback
            float brickCenterX = brick.getX() + brick.getWidth() / 2f;
            float brickCenterY = brick.getY() + brick.getHeight() / 2f;
            float deltaX = collisionX - brickCenterX;
            float deltaY = collisionY - brickCenterY;

            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                reverseX();
            } else {
                reverseY();
            }
        }

        // Position ball just before collision point by backing up slightly along velocity
        // This prevents the ball from overlapping with the brick after collision
        float backupT = Math.max(0.0f, collisionT - 0.01f); // Back up slightly from collision point
        float safeX = prevX + dx * backupT;
        float safeY = prevY + dy * backupT;

        bounds.setPosition(safeX, safeY);

        // Hit the brick
        boolean destroyed = brick.hit();
        return destroyed ? brick.getScore() : 0;
    }

    public int checkCollisionWithBrickRobust(Brick brick, float prevX, float prevY) {
        if (brick.isDestroyed()) {
            return 0;
        }

        CollisionResult result = sweepCircleAgainstBrick(brick, prevX, prevY);
        if (!result.hit) {
            if (!circleIntersectsBrick(bounds.x, bounds.y, bounds.radius, brick)) {
                return 0;
            }
            result = resolvePenetrationAgainstBrick(brick);
            if (!result.hit) {
                return 0;
            }
        }

        bounds.setPosition(result.posX, result.posY);

        float dot = velocity.x * result.normalX + velocity.y * result.normalY;
        if (dot < 0f) {
            velocity.x -= 2f * dot * result.normalX;
            velocity.y -= 2f * dot * result.normalY;
        }

        boolean destroyed = brick.hit();
        return destroyed ? brick.getScore() : 0;
    }

    private CollisionResult sweepCircleAgainstBrick(Brick brick, float prevX, float prevY) {
        float dx = bounds.x - prevX;
        float dy = bounds.y - prevY;

        if (Math.abs(dx) < 0.000001f && Math.abs(dy) < 0.000001f) {
            return CollisionResult.noHit();
        }

        float expandedLeft = brick.getX() - bounds.radius;
        float expandedRight = brick.getX() + brick.getWidth() + bounds.radius;
        float expandedBottom = brick.getY() - bounds.radius;
        float expandedTop = brick.getY() + brick.getHeight() + bounds.radius;

        float entryX;
        float exitX;
        if (Math.abs(dx) < 0.000001f) {
            if (prevX < expandedLeft || prevX > expandedRight) {
                return CollisionResult.noHit();
            }
            entryX = Float.NEGATIVE_INFINITY;
            exitX = Float.POSITIVE_INFINITY;
        } else {
            float invDx = 1f / dx;
            float t1 = (expandedLeft - prevX) * invDx;
            float t2 = (expandedRight - prevX) * invDx;
            entryX = Math.min(t1, t2);
            exitX = Math.max(t1, t2);
        }

        float entryY;
        float exitY;
        if (Math.abs(dy) < 0.000001f) {
            if (prevY < expandedBottom || prevY > expandedTop) {
                return CollisionResult.noHit();
            }
            entryY = Float.NEGATIVE_INFINITY;
            exitY = Float.POSITIVE_INFINITY;
        } else {
            float invDy = 1f / dy;
            float t1 = (expandedBottom - prevY) * invDy;
            float t2 = (expandedTop - prevY) * invDy;
            entryY = Math.min(t1, t2);
            exitY = Math.max(t1, t2);
        }

        float entryTime = Math.max(entryX, entryY);
        float exitTime = Math.min(exitX, exitY);

        if (entryTime > exitTime || exitTime < 0f || entryTime > 1f) {
            return CollisionResult.noHit();
        }

        float collisionT = MathUtils.clamp(entryTime, 0f, 1f);
        float contactX = prevX + dx * collisionT;
        float contactY = prevY + dy * collisionT;

        float normalX;
        float normalY;
        float axisEpsilon = 0.0001f;
        if (Math.abs(entryX - entryY) <= axisEpsilon) {
            float brickCenterX = brick.getX() + brick.getWidth() / 2f;
            float brickCenterY = brick.getY() + brick.getHeight() / 2f;
            float deltaX = contactX - brickCenterX;
            float deltaY = contactY - brickCenterY;
            float len = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            if (len > 0.0001f) {
                normalX = deltaX / len;
                normalY = deltaY / len;
            } else {
                normalX = 0f;
                normalY = 1f;
            }
        } else if (entryX > entryY) {
            normalX = dx > 0f ? -1f : 1f;
            normalY = 0f;
        } else {
            normalX = 0f;
            normalY = dy > 0f ? -1f : 1f;
        }

        float length = (float) Math.sqrt(normalX * normalX + normalY * normalY);
        if (length > 0f) {
            normalX /= length;
            normalY /= length;
        } else {
            normalX = 0f;
            normalY = 1f;
        }

        float resolvedX = contactX + normalX * COLLISION_SEPARATION;
        float resolvedY = contactY + normalY * COLLISION_SEPARATION;
        return new CollisionResult(true, resolvedX, resolvedY, normalX, normalY);
    }

    private CollisionResult resolvePenetrationAgainstBrick(Brick brick) {
        float cx = bounds.x;
        float cy = bounds.y;

        if (!circleIntersectsBrick(cx, cy, bounds.radius, brick)) {
            return CollisionResult.noHit();
        }

        OverlapInfo overlap = computeOverlapResolution(cx, cy, brick);
        float nx = overlap.normalX;
        float ny = overlap.normalY;
        float depth = overlap.depth;

        if (depth > 0f && (nx != 0f || ny != 0f)) {
            float length = (float) Math.sqrt(nx * nx + ny * ny);
            if (length > 0f) {
                nx /= length;
                ny /= length;
            } else {
                nx = 0f;
                ny = 1f;
            }
            float resolvedX = cx + nx * (depth + COLLISION_SEPARATION);
            float resolvedY = cy + ny * (depth + COLLISION_SEPARATION);
            return new CollisionResult(true, resolvedX, resolvedY, nx, ny);
        }

        float brickCenterX = brick.getX() + brick.getWidth() / 2f;
        float brickCenterY = brick.getY() + brick.getHeight() / 2f;
        float deltaX = cx - brickCenterX;
        float deltaY = cy - brickCenterY;
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            nx = Math.signum(deltaX);
            ny = 0f;
        } else {
            nx = 0f;
            ny = Math.signum(deltaY);
        }
        if (nx == 0f && ny == 0f) {
            nx = 0f;
            ny = 1f;
        }
        float resolvedXFallback = cx + nx * COLLISION_SEPARATION;
        float resolvedYFallback = cy + ny * COLLISION_SEPARATION;
        return new CollisionResult(true, resolvedXFallback, resolvedYFallback, nx, ny);
    }

    private static class CollisionResult {
        final boolean hit;
        final float posX;
        final float posY;
        final float normalX;
        final float normalY;

        CollisionResult(boolean hit, float posX, float posY, float normalX, float normalY) {
            this.hit = hit;
            this.posX = posX;
            this.posY = posY;
            this.normalX = normalX;
            this.normalY = normalY;
        }

        static CollisionResult noHit() {
            return new CollisionResult(false, 0f, 0f, 0f, 0f);
        }
    }

    private boolean circleIntersectsBrick(float cx, float cy, float radius, Brick brick) {
        float closestX = MathUtils.clamp(cx, brick.getX(), brick.getX() + brick.getWidth());
        float closestY = MathUtils.clamp(cy, brick.getY(), brick.getY() + brick.getHeight());
        float dx = cx - closestX;
        float dy = cy - closestY;
        return dx * dx + dy * dy <= radius * radius;
    }

    private OverlapInfo computeOverlapResolution(float cx, float cy, Brick brick) {
        float left = brick.getX();
        float right = left + brick.getWidth();
        float bottom = brick.getY();
        float top = bottom + brick.getHeight();
        float radius = bounds.radius;

        float overlapLeft = (cx + radius) - left;
        float overlapRight = right - (cx - radius);
        float overlapBottom = (cy + radius) - bottom;
        float overlapTop = top - (cy - radius);

        float depth = Float.POSITIVE_INFINITY;
        float normalX = 0f;
        float normalY = 0f;

        if (overlapLeft > 0f && overlapLeft < depth) {
            depth = overlapLeft;
            normalX = -1f;
            normalY = 0f;
        }
        if (overlapRight > 0f && overlapRight < depth) {
            depth = overlapRight;
            normalX = 1f;
            normalY = 0f;
        }
        if (overlapBottom > 0f && overlapBottom < depth) {
            depth = overlapBottom;
            normalX = 0f;
            normalY = -1f;
        }
        if (overlapTop > 0f && overlapTop < depth) {
            depth = overlapTop;
            normalX = 0f;
            normalY = 1f;
        }

        if (!Float.isFinite(depth) || depth == Float.POSITIVE_INFINITY) {
            return new OverlapInfo(0f, 0f, 0f);
        }

        return new OverlapInfo(normalX, normalY, depth);
    }

    private static class OverlapInfo {
        final float normalX;
        final float normalY;
        final float depth;

        OverlapInfo(float normalX, float normalY, float depth) {
            this.normalX = normalX;
            this.normalY = normalY;
            this.depth = depth;
        }
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

    public void updateTrailStyle(float comboFactor) {
        float speed = velocity.len();
        float speedNorm = MathUtils.clamp(speed / 450f, 0f, 1.5f);
        float comboNorm = MathUtils.clamp(comboFactor, 0f, 2f);

        float lengthFactor = 0.6f + speedNorm * 0.9f + comboNorm * 0.6f;
        float sizeFactor = 0.7f + speedNorm * 0.6f + comboNorm * 0.4f;

        trailTint.set(baseTrailColor);
        if (comboNorm > 0f) {
            trailTint.lerp(Color.ORANGE, MathUtils.clamp(comboNorm * 0.5f, 0f, 1f));
        }
        if (speedNorm > 0f) {
            trailTint.lerp(Color.RED, MathUtils.clamp(speedNorm * 0.35f, 0f, 1f));
        }

        trail.style(lengthFactor, sizeFactor, trailTint);
        ballColor.set(trailTint).lerp(Color.WHITE, 0.25f);
    }

    public void setTrailColor(Color color) {
        baseTrailColor.set(color);
        trail.setColor(color);
        ballColor.set(color);
    }

    public void setStickyOffset(float offsetX) {
        this.stickyOffsetX = offsetX;
    }

    public float getStickyOffsetX() {
        return stickyOffsetX;
    }
}


