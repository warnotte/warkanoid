package io.github.warnotte.warkanoid;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

public class BallTrail {
    private static class TrailPoint {
        public Vector2 position;
        public float alpha;
        public float life;

        public TrailPoint(float x, float y) {
            this.position = new Vector2(x, y);
            this.alpha = 1.0f;
            this.life = 1.0f;
        }
    }

    private List<TrailPoint> trailPoints;
    private float trailLifetime;
    private float trailSpacing;
    private Color trailColor;
    private int maxTrailPoints;

    public BallTrail() {
        this.trailPoints = new ArrayList<>();
        this.trailLifetime = 0.8f; // Trail lasts longer
        this.trailSpacing = 3f; // Closer trail points for smoother effect
        this.trailColor = new Color(Color.WHITE);
        this.maxTrailPoints = 30; // More trail points for smoother trail
    }

    public void addPoint(float x, float y) {
        // Only add if far enough from last point
        if (trailPoints.isEmpty() ||
            trailPoints.get(trailPoints.size() - 1).position.dst(x, y) >= trailSpacing) {

            trailPoints.add(new TrailPoint(x, y));

            // Remove excess points
            while (trailPoints.size() > maxTrailPoints) {
                trailPoints.remove(0);
            }
        }
    }

    public void update(float deltaTime) {
        // Update trail points life and alpha
        for (int i = trailPoints.size() - 1; i >= 0; i--) {
            TrailPoint point = trailPoints.get(i);
            point.life -= deltaTime / trailLifetime;
            point.alpha = point.life;

            if (point.life <= 0) {
                trailPoints.remove(i);
            }
        }
    }

    public void render(ShapeRenderer shapeRenderer) {
        if (trailPoints.isEmpty()) return;

        // Draw trail as circles with decreasing size and alpha
        for (int i = 0; i < trailPoints.size(); i++) {
            TrailPoint point = trailPoints.get(i);

            // Calculate size based on position in trail (older = smaller)
            float progress = (float)i / Math.max(1, trailPoints.size() - 1);
            float size = 3f + progress * 5f; // 3-8 pixels, larger for newer points

            // Set color with fading alpha (more opaque)
            trailColor.a = point.alpha * 0.9f; // Less transparent
            shapeRenderer.setColor(trailColor);

            shapeRenderer.circle(point.position.x, point.position.y, size);
        }
    }

    public void clear() {
        trailPoints.clear();
    }

    public void setColor(Color color) {
        this.trailColor.set(color);
    }
}