package io.github.warnotte.warkanoid;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;
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
    private float lifetimeMultiplier;
    private float sizeMultiplier;
    private float spacingMultiplier;
    private final Color renderColor = new Color();
    private final Color shadowColor = new Color(0f, 0f, 0f, 1f);
    private float baseTrailLifetime;
    private float baseTrailSpacing;
    private int baseMaxTrailPoints;

    public BallTrail() {
        this.trailPoints = new ArrayList<>();
        this.trailLifetime = 0.4f; // Slightly shorter trail persistence
        this.trailSpacing = 30f; // Wider spacing for fewer stored points
        this.trailColor = new Color(Color.WHITE);
        this.maxTrailPoints = 3; // Cap number of dots to keep trail light
        this.baseTrailLifetime = trailLifetime;
        this.baseTrailSpacing = trailSpacing;
        this.baseMaxTrailPoints = maxTrailPoints;
        this.lifetimeMultiplier = 1f;
        this.sizeMultiplier = 1f;
        this.spacingMultiplier = 1f;
    }

    public void addPoint(float x, float y) {
        float spacing = baseTrailSpacing / MathUtils.clamp(spacingMultiplier, 0.5f, 2.5f);
        if (spacing < 0.5f) spacing = 0.5f;

        if (trailPoints.isEmpty() ||
            trailPoints.get(trailPoints.size() - 1).position.dst(x, y) >= spacing) {

            trailPoints.add(new TrailPoint(x, y));

            int maxPoints = Math.max(5, Math.round(baseMaxTrailPoints * MathUtils.clamp(lifetimeMultiplier, 0.4f, 3f)));
            while (trailPoints.size() > maxPoints) {
                trailPoints.remove(0);
            }
        }
    }

    public void update(float deltaTime) {
        float lifetime = Math.max(0.1f, baseTrailLifetime * MathUtils.clamp(lifetimeMultiplier, 0.2f, 3f));
        for (int i = trailPoints.size() - 1; i >= 0; i--) {
            TrailPoint point = trailPoints.get(i);
            point.life -= deltaTime / lifetime;
            point.alpha = point.life;

            if (point.life <= 0) {
                trailPoints.remove(i);
            }
        }
    }

    public void style(float lengthFactor, float sizeFactor, Color color) {
        this.lifetimeMultiplier = MathUtils.clamp(lengthFactor, 0.2f, 3f);
        this.sizeMultiplier = MathUtils.clamp(sizeFactor, 0.3f, 2.5f);
        this.spacingMultiplier = MathUtils.clamp(lengthFactor, 0.5f, 2.5f);
        if (color != null) {
            this.trailColor.set(color);
        }
    }

    public void renderShadow(ShapeRenderer shapeRenderer, float shadowOffsetX, float shadowOffsetY) {
        if (trailPoints.isEmpty()) return;

        for (int i = 0; i < trailPoints.size(); i++) {
            TrailPoint point = trailPoints.get(i);
            float progress = (float)i / Math.max(1, trailPoints.size() - 1);
            float size = (3f + progress * 5f) * MathUtils.clamp(sizeMultiplier, 0.4f, 2.5f);
            float alpha = MathUtils.clamp(point.alpha * 0.9f, 0f, 1f);

            shadowColor.a = alpha * 0.55f;
            shapeRenderer.setColor(shadowColor);
            shapeRenderer.circle(point.position.x + shadowOffsetX, point.position.y + shadowOffsetY, size * 1.05f);
        }
    }

    public void render(ShapeRenderer shapeRenderer) {
        if (trailPoints.isEmpty()) return;

        for (int i = 0; i < trailPoints.size(); i++) {
            TrailPoint point = trailPoints.get(i);
            float progress = (float)i / Math.max(1, trailPoints.size() - 1);
            float size = (3f + progress * 5f) * MathUtils.clamp(sizeMultiplier, 0.4f, 2.5f);
            float alpha = MathUtils.clamp(point.alpha * 0.9f, 0f, 1f);

            renderColor.set(trailColor);
            renderColor.a = alpha;
            shapeRenderer.setColor(renderColor);
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
