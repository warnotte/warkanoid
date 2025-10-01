package io.github.warnotte.warkanoid;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;
import java.util.List;

public class Main extends ApplicationAdapter {
    private static final String CRT_VERTEX_SHADER = "attribute vec4 a_position;\n" +
            "attribute vec4 a_color;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "uniform mat4 u_projTrans;\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "void main() {\n" +
            "    v_color = a_color;\n" +
            "    v_texCoords = a_texCoord0;\n" +
            "    gl_Position = u_projTrans * a_position;\n" +
            "}\n";

    private static final String CRT_FRAGMENT_SHADER = "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "uniform sampler2D u_texture;\n" +
            "uniform float u_time;\n" +
            "uniform vec2 u_resolution;\n" +
            "void main() {\n" +
            "    vec2 uv = v_texCoords;\n" +
            "    vec2 centered = uv - 0.5;\n" +
            "    float dist = dot(centered, centered);\n" +
            "    float curvature = 0.12;\n" +
            "    vec2 curvedUV = centered * (1.0 + curvature * dist) + 0.5;\n" +
            "    curvedUV = clamp(curvedUV, 0.0, 1.0);\n" +
            "    float radius = length(centered);\n" +
            "    vec2 dir = radius > 0.0 ? centered / radius : vec2(0.0);\n" +
            "    float aberration = 0.003 + dist * 0.012;\n" +
            "    vec3 color;\n" +
            "    color.r = texture2D(u_texture, curvedUV + dir * aberration).r;\n" +
            "    color.g = texture2D(u_texture, curvedUV).g;\n" +
            "    color.b = texture2D(u_texture, curvedUV - dir * aberration).b;\n" +
            "    float scan = 0.04 * sin((curvedUV.y * u_resolution.y) * 1.5 + u_time * 3.0);\n" +
            "    color *= 1.0 - scan;\n" +
            "    float vignette = curvedUV.x * (1.0 - curvedUV.x) * curvedUV.y * (1.0 - curvedUV.y);\n" +
            "    vignette = pow(vignette * 16.0, 0.35);\n" +
            "    color *= mix(0.55, 1.05, clamp(vignette, 0.0, 1.0));\n" +
            "    float noise = fract(sin(dot(curvedUV + u_time * 0.05, vec2(12.9898, 78.233))) * 43758.5453);\n" +
            "    color += (noise - 0.5) * 0.02;\n" +
            "    color = clamp(color, 0.0, 1.0);\n" +
            "    gl_FragColor = vec4(color, 1.0) * v_color;\n" +
            "}\n";

    private static final String SHADOW_BLUR_VERTEX_SHADER = "attribute vec4 a_position;\n" +
            "attribute vec4 a_color;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "uniform mat4 u_projTrans;\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "void main() {\n" +
            "    v_color = a_color;\n" +
            "    v_texCoords = a_texCoord0;\n" +
            "    gl_Position = u_projTrans * a_position;\n" +
            "}\n";

    private static final String SHADOW_BLUR_FRAGMENT_SHADER = "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "uniform sampler2D u_texture;\n" +
            "uniform vec2 u_direction;\n" +
            "void main() {\n" +
            "    vec4 sum = vec4(0.0);\n" +
            "    sum += texture2D(u_texture, v_texCoords - 2.0 * u_direction) * 0.1216;\n" +
            "    sum += texture2D(u_texture, v_texCoords - 1.0 * u_direction) * 0.2333;\n" +
            "    sum += texture2D(u_texture, v_texCoords) * 0.2907;\n" +
            "    sum += texture2D(u_texture, v_texCoords + 1.0 * u_direction) * 0.2333;\n" +
            "    sum += texture2D(u_texture, v_texCoords + 2.0 * u_direction) * 0.1216;\n" +
            "    gl_FragColor = sum * v_color;\n" +
            "}\n";

    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 600;
    private static final float SHADOW_OFFSET_X = 30f;
    private static final float SHADOW_OFFSET_Y = -30f;

    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private SpriteBatch postProcessBatch;
    private FrameBuffer frameBuffer;
    private FrameBuffer shadowBuffer;
    private FrameBuffer shadowPingBuffer;
    private ShaderProgram crtShader;
    private ShaderProgram shadowBlurShader;
    private Matrix4 screenMatrix;
    private float shaderTime;
    private Texture pixelTexture;
    private GlyphLayout glyphLayout;
    private float screenShakeTime;
    private float screenShakeDuration;
    private float screenShakeIntensity;
    private int comboCount;
    private int maxCombo;
    private float comboTimer;
    private Sound startSound;
    private Sound paddleHitSound;
    private Sound brickHitSound;
    private Sound wallHitSound;
    private BitmapFont font;
    private OrthographicCamera camera;
    private Viewport viewport;
    private Paddle paddle;
    private List<Ball> balls;
    private List<Brick> bricks;
    private List<PowerUp> powerUps;
    private List<Particle> particles;
    private List<Laser> lasers;
    private float laserCooldown;
    private List<Ball> stickyBalls;
    private int score;
    private int lives;
    private boolean gameOver;
    private boolean gameWon;
    private boolean ballLaunched;
    private int currentLevel;
    private enum CollisionMode {
        DISCRETE("DISCRETE (OLD)"),
        CONTINUOUS("CONTINUOUS (CCD)"),
        ROBUST("ROBUST (HYBRID)");

        private final String label;

        CollisionMode(String label) {
            this.label = label;
        }

        public CollisionMode next() {
            CollisionMode[] modes = values();
            return modes[(ordinal() + 1) % modes.length];
        }

        public String getLabel() {
            return label;
        }
    }

    private static CollisionMode collisionMode = CollisionMode.CONTINUOUS; // Toggle with F7
    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();
        postProcessBatch = new SpriteBatch();
        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, GAME_WIDTH, GAME_HEIGHT, false);
        frameBuffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        screenMatrix = new Matrix4().setToOrtho2D(0f, 0f, GAME_WIDTH, GAME_HEIGHT);
        shaderTime = 0f;
        ShaderProgram.pedantic = false;
        crtShader = new ShaderProgram(CRT_VERTEX_SHADER, CRT_FRAGMENT_SHADER);
        if (!crtShader.isCompiled()) {
            Gdx.app.error("CRT", "Failed to compile CRT shader: " + crtShader.getLog());
            crtShader.dispose();
            crtShader = null;
        }
        startSound = Gdx.audio.newSound(Gdx.files.internal("sounds/arkanoid_start.mp3"));
        paddleHitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Arkanoid_SFX_2.wav"));
        brickHitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Arkanoid_SFX_3.wav"));
        wallHitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Arkanoid_SFX_4.wav"));
        font = new BitmapFont(); // Default font
        font.setColor(Color.WHITE);
        font.getData().setScale(1.5f); // Make text bigger

        Pixmap pixelMap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixelMap.setColor(Color.WHITE);
        pixelMap.fill();
        pixelTexture = new Texture(pixelMap);
        pixelMap.dispose();

        glyphLayout = new GlyphLayout();

        screenShakeTime = 0f;
        screenShakeDuration = 0f;
        screenShakeIntensity = 0f;
        comboCount = 0;
        maxCombo = 0;
        comboTimer = 0f;

        // Setup camera and viewport
        camera = new OrthographicCamera();
        camera.setToOrtho(false, GAME_WIDTH, GAME_HEIGHT); // false = Y-axis points up, origin at bottom-left
        viewport = new FitViewport(GAME_WIDTH, GAME_HEIGHT, camera);
        viewport.apply();

        if (frameBuffer == null) {
            frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, GAME_WIDTH, GAME_HEIGHT, false);
            frameBuffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        if (postProcessBatch == null) {
            postProcessBatch = new SpriteBatch();
        }

        // Create paddle at bottom center
        paddle = new Paddle(GAME_WIDTH / 2f - 50f, 30f, 100f, 15f);

        // Create initial ball on paddle
        balls = new ArrayList<>();
        Ball initialBall = new Ball(GAME_WIDTH / 2f, 30f + 15f + 8f, 8f); // paddle Y + paddle height + ball radius
        initialBall.setTrailColor(Color.CYAN);
        balls.add(initialBall);

        // Initialize power-ups list
        powerUps = new ArrayList<>();

        // Initialize particles list
        particles = new ArrayList<>();

        // Initialize lasers list
        lasers = new ArrayList<>();
        laserCooldown = 0f;

        // Initialize sticky balls
        stickyBalls = new ArrayList<>();

        // Initialize game state
        score = 0;
        lives = 3;
        gameOver = false;
        gameWon = false;
        ballLaunched = false;
        currentLevel = 1;

        // Create bricks for level 1
        loadLevel(currentLevel);
    }

    private void loadLevel(int level) {
        switch (level) {
            case 1:
                createLevel1();
                break;
            case 2:
                createLevel2();
                break;
            case 3:
                createLevel3();
                break;
            case 4:
                createLevel4();
                break;
            case 5:
                createLevel5();
                break;
            case 6:
                createLevel6CollisionTest();
                break;
            default:
                createLevel1();
                break;
        }
    }

    private void createLevel1() {
        bricks = new ArrayList<>();
        int rows = 6;
        int cols = 10;
        float brickWidth = (GAME_WIDTH - 100f) / cols;
        float brickHeight = 20f;
        float startX = 50f;
        float startY = GAME_HEIGHT - 100f;

        Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.PURPLE};
        int[] hitCounts = {1, 1, 2, 2, 3, 3}; // Bottom rows = 1 hit (easy), top rows = 3 hits (hard)

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float x = startX + col * brickWidth;
                float y = startY - row * (brickHeight + 2f);

                // Row 0 = top (hardest), Row 5 = bottom (easiest)
                // So we need to invert: bottom rows should have low hit counts
                int difficultyIndex = (rows - 1) - row; // row 0 -> index 5, row 5 -> index 0
                Color brickColor = colors[difficultyIndex % colors.length];
                int maxHits = hitCounts[difficultyIndex % hitCounts.length];

                // 10% chance for bomb brick (but not on easiest rows)
                boolean isBomb = difficultyIndex >= 2 && Math.random() < 0.1;
                Brick.Type brickType = isBomb ? Brick.Type.BOMB : Brick.Type.NORMAL;

                bricks.add(new Brick(x, y, brickWidth - 2f, brickHeight, brickColor, maxHits, brickType));
            }
        }
    }

    private void createLevel2() {
        bricks = new ArrayList<>();
        int rows = 8;
        int cols = 10;
        float brickWidth = (GAME_WIDTH - 100f) / cols;
        float brickHeight = 20f;
        float startX = 50f;
        float startY = GAME_HEIGHT - 100f;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float x = startX + col * brickWidth;
                float y = startY - row * (brickHeight + 2f);

                Brick.Type brickType = Brick.Type.NORMAL;
                Color brickColor;
                int maxHits = 1;

                // Create a pattern with indestructible bricks forming a cross
                if ((row == 3 || row == 4) && col >= 3 && col <= 6) {
                    // Horizontal bar of cross
                    brickType = Brick.Type.INDESTRUCTIBLE;
                    brickColor = Color.GRAY;
                } else if ((col == 4 || col == 5) && row >= 1 && row <= 6) {
                    // Vertical bar of cross
                    brickType = Brick.Type.INDESTRUCTIBLE;
                    brickColor = Color.GRAY;
                } else {
                    // Normal bricks in rainbow pattern
                    Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.PURPLE, Color.MAGENTA};
                    brickColor = colors[row % colors.length];
                    maxHits = (row < 2) ? 2 : 1;

                    // Add some bomb bricks
                    if (row >= 5 && Math.random() < 0.15) {
                        brickType = Brick.Type.BOMB;
                    }
                }

                bricks.add(new Brick(x, y, brickWidth - 2f, brickHeight, brickColor, maxHits, brickType));
            }
        }
    }

    private void createLevel3() {
        bricks = new ArrayList<>();
        int rows = 8;
        int cols = 10;
        float brickWidth = (GAME_WIDTH - 100f) / cols;
        float brickHeight = 20f;
        float startX = 50f;
        float startY = GAME_HEIGHT - 100f;

        // Alternating rows pattern - easier than checkerboard
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float x = startX + col * brickWidth;
                float y = startY - row * (brickHeight + 2f);

                // Every other row is indestructible
                boolean isIndestructible = row % 2 == 0;
                Color brickColor;
                Brick.Type brickType;
                int maxHits;

                if (isIndestructible) {
                    brickColor = Color.GRAY;
                    brickType = Brick.Type.INDESTRUCTIBLE;
                    maxHits = 1;
                } else {
                    Color[] colors = {Color.CYAN, Color.BLUE, Color.PURPLE, Color.MAGENTA};
                    brickColor = colors[row / 2 % colors.length];
                    maxHits = 1 + (row / 4);
                    brickType = (Math.random() < 0.1) ? Brick.Type.BOMB : Brick.Type.NORMAL;
                }

                bricks.add(new Brick(x, y, brickWidth - 2f, brickHeight, brickColor, maxHits, brickType));
            }
        }
    }

    private void createLevel4() {
        bricks = new ArrayList<>();
        int rows = 8;
        int cols = 10;
        float brickWidth = (GAME_WIDTH - 100f) / cols;
        float brickHeight = 20f;
        float startX = 50f;
        float startY = GAME_HEIGHT - 100f;

        // Frame pattern - indestructible border with opening at bottom
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float x = startX + col * brickWidth;
                float y = startY - row * (brickHeight + 2f);

                // Create opening at bottom center (columns 4 and 5)
                boolean isOpening = (row == rows - 1) && (col == 4 || col == 5);
                boolean isBorder = (row == 0 || row == rows - 1 || col == 0 || col == cols - 1) && !isOpening;

                Color brickColor;
                Brick.Type brickType;
                int maxHits;

                if (isBorder) {
                    brickColor = Color.GRAY;
                    brickType = Brick.Type.INDESTRUCTIBLE;
                    maxHits = 1;
                } else if (!isOpening) {
                    // Inside bricks - harder as you go up
                    Color[] colors = {Color.GREEN, Color.YELLOW, Color.ORANGE, Color.RED};
                    int colorIndex = Math.min((rows - 1 - row) / 2, colors.length - 1);
                    brickColor = colors[colorIndex];
                    maxHits = row < 3 ? 3 : (row < 5 ? 2 : 1);
                    brickType = (Math.random() < 0.08) ? Brick.Type.BOMB : Brick.Type.NORMAL;
                } else {
                    // Skip the opening - don't add a brick here
                    continue;
                }

                bricks.add(new Brick(x, y, brickWidth - 2f, brickHeight, brickColor, maxHits, brickType));
            }
        }
    }

    private void createLevel5() {
        bricks = new ArrayList<>();
        int rows = 8;
        int cols = 10;
        float brickWidth = (GAME_WIDTH - 100f) / cols;
        float brickHeight = 20f;
        float startX = 50f;
        float startY = GAME_HEIGHT - 100f;

        // Columns pattern - vertical stripes
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float x = startX + col * brickWidth;
                float y = startY - row * (brickHeight + 2f);

                // Every 3rd column is indestructible
                boolean isWall = col % 3 == 0;

                Color brickColor;
                Brick.Type brickType;
                int maxHits;

                if (isWall) {
                    brickColor = Color.GRAY;
                    brickType = Brick.Type.INDESTRUCTIBLE;
                    maxHits = 1;
                } else {
                    Color[] colors = {Color.PURPLE, Color.MAGENTA, Color.PINK, Color.ORANGE};
                    brickColor = colors[(row + col) % colors.length];
                    maxHits = 1 + (row / 3);
                    brickType = (Math.random() < 0.12) ? Brick.Type.BOMB : Brick.Type.NORMAL;
                }

                bricks.add(new Brick(x, y, brickWidth - 2f, brickHeight, brickColor, maxHits, brickType));
            }
        }
    }

    private void createLevel6CollisionTest() {
        bricks = new ArrayList<>();
        float brickWidth = 70f;
        float brickHeight = 20f;
        float spacing = 2f;

        // Test 1: Narrow vertical corridor (left side)
        for (int i = 0; i < 15; i++) {
            float y = GAME_HEIGHT - 100f - i * (brickHeight + spacing);
            bricks.add(new Brick(50f, y, brickWidth, brickHeight, Color.GRAY, 1, Brick.Type.INDESTRUCTIBLE));
            bricks.add(new Brick(50f + brickWidth + 50f, y, brickWidth, brickHeight, Color.GRAY, 1, Brick.Type.INDESTRUCTIBLE));
        }

        // Test 2: Spiral pattern (center-right)
        float centerX = 400f;
        float centerY = 400f;
        int spiralBricks = 20;
        for (int i = 0; i < spiralBricks; i++) {
            float angle = i * 0.8f;
            float radius = 30f + i * 8f;
            float x = centerX + (float)(Math.cos(angle) * radius);
            float y = centerY + (float)(Math.sin(angle) * radius);
            bricks.add(new Brick(x, y, 30f, brickHeight, Color.GRAY, 1, Brick.Type.INDESTRUCTIBLE));
        }

        // Test 3: Tight zigzag pattern (bottom)
        float zigzagStartX = 100f;
        float zigzagY = 200f;
        for (int i = 0; i < 8; i++) {
            float offsetY = (i % 2 == 0) ? 0f : 30f;
            bricks.add(new Brick(zigzagStartX + i * 60f, zigzagY + offsetY, 50f, brickHeight, Color.GRAY, 1, Brick.Type.INDESTRUCTIBLE));
        }

        // Test 4: Corners and tight angles (top-right)
        float cornerX = 600f;
        float cornerY = GAME_HEIGHT - 100f;
        // L-shape
        for (int i = 0; i < 5; i++) {
            bricks.add(new Brick(cornerX, cornerY - i * (brickHeight + spacing), 60f, brickHeight, Color.GRAY, 1, Brick.Type.INDESTRUCTIBLE));
            bricks.add(new Brick(cornerX + i * 62f, cornerY - 4 * (brickHeight + spacing), 60f, brickHeight, Color.GRAY, 1, Brick.Type.INDESTRUCTIBLE));
        }

        // Test 5: Very narrow gap (should be barely passable)
        float gapY = 300f;
        bricks.add(new Brick(250f, gapY, 80f, brickHeight, Color.GRAY, 1, Brick.Type.INDESTRUCTIBLE));
        bricks.add(new Brick(250f, gapY - (brickHeight + 20f), 80f, brickHeight, Color.GRAY, 1, Brick.Type.INDESTRUCTIBLE));

        // Test 6: Box trap (ball can get stuck)
        float boxX = 500f;
        float boxY = 250f;
        float boxSize = 80f;
        // Top, bottom, left, right
        bricks.add(new Brick(boxX, boxY + boxSize, boxSize, brickHeight, Color.GRAY, 1, Brick.Type.INDESTRUCTIBLE));
        bricks.add(new Brick(boxX, boxY - brickHeight, boxSize, brickHeight, Color.GRAY, 1, Brick.Type.INDESTRUCTIBLE));
        bricks.add(new Brick(boxX - brickHeight, boxY, brickHeight, boxSize, Color.GRAY, 1, Brick.Type.INDESTRUCTIBLE));
        bricks.add(new Brick(boxX + boxSize, boxY, brickHeight, boxSize, Color.GRAY, 1, Brick.Type.INDESTRUCTIBLE));

        // Add a few destructible bricks for testing scoring/winning
        bricks.add(new Brick(350f, 150f, 50f, brickHeight, Color.RED, 1, Brick.Type.NORMAL));
        bricks.add(new Brick(420f, 150f, 50f, brickHeight, Color.YELLOW, 1, Brick.Type.NORMAL));
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        update(deltaTime);

        camera.position.set(GAME_WIDTH / 2f, GAME_HEIGHT / 2f, 0f);
        if (screenShakeTime > 0f && screenShakeDuration > 0f) {
            float progress = screenShakeTime / screenShakeDuration;
            float currentIntensity = screenShakeIntensity * progress * progress;
            camera.position.add(MathUtils.random(-currentIntensity, currentIntensity),
                                MathUtils.random(-currentIntensity, currentIntensity), 0f);
        }
        camera.update();

        viewport.apply();

        if (frameBuffer == null) {
            frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, GAME_WIDTH, GAME_HEIGHT, false);
            frameBuffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        if (postProcessBatch == null) {
            postProcessBatch = new SpriteBatch();
        }

        ensureShadowResources();
        renderShadowLayer();

        frameBuffer.begin();
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);

        if (shadowBuffer != null) {
            Texture shadowTexture = shadowBuffer.getColorBufferTexture();
            shadowTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            spriteBatch.setProjectionMatrix(camera.combined);
            spriteBatch.setColor(1f, 1f, 1f, 1f);
            spriteBatch.enableBlending();
            spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            spriteBatch.begin();
            spriteBatch.draw(shadowTexture,
                    0f, 0f,
                    0f, 0f,
                    GAME_WIDTH, GAME_HEIGHT,
                    1f, 1f,
                    0f,
                    0, 0,
                    shadowTexture.getWidth(), shadowTexture.getHeight(),
                    false, true);
            spriteBatch.end();
            spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        drawShadowCasters(shapeRenderer);

        // Draw walls (white borders) without extra shadow
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(0, 0, 5f, GAME_HEIGHT);
        shapeRenderer.rect(GAME_WIDTH - 5f, 0, 5f, GAME_HEIGHT);
        shapeRenderer.rect(0, GAME_HEIGHT - 5f, GAME_WIDTH, 5f);

        paddle.render(shapeRenderer);

        for (Ball ball : balls) {
            ball.render(shapeRenderer);
        }

        for (Brick brick : bricks) {
            brick.render(shapeRenderer);
        }

        for (PowerUp powerUp : powerUps) {
            powerUp.render(shapeRenderer);
        }

        for (Particle particle : particles) {
            particle.render(shapeRenderer, 0f, 0f);
        }

        for (Laser laser : lasers) {
            laser.render(shapeRenderer);
        }

        shapeRenderer.setColor(Color.WHITE);
        for (int i = 0; i < lives; i++) {
            float x = GAME_WIDTH - 30f - (i * 20f);
            float y = 15f;
            shapeRenderer.circle(x, y, 6f);
        }

        shapeRenderer.end();

        // Draw text (score and game state)
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();

        renderHud();
        renderGameStateMessages();

        spriteBatch.end();
        frameBuffer.end();

        shaderTime += deltaTime;

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        Gdx.gl.glViewport(viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());

        Texture frameTexture = frameBuffer.getColorBufferTexture();
        frameTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        postProcessBatch.setProjectionMatrix(screenMatrix);
        if (crtShader != null) {
            postProcessBatch.setShader(crtShader);
        }

        postProcessBatch.begin();
        if (crtShader != null) {
            crtShader.setUniformf("u_time", shaderTime);
            crtShader.setUniformf("u_resolution", (float) frameBuffer.getWidth(), (float) frameBuffer.getHeight());
        }
        postProcessBatch.draw(frameTexture, 0f, 0f, GAME_WIDTH, GAME_HEIGHT, 0f, 0f, 1f, 1f);
        postProcessBatch.end();

        if (crtShader != null) {
            postProcessBatch.setShader(null);
        }
    }

    private void ensureShadowResources() {
        if (shadowBuffer == null) {
            shadowBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, GAME_WIDTH, GAME_HEIGHT, false);
            shadowBuffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        if (shadowPingBuffer == null) {
            shadowPingBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, GAME_WIDTH, GAME_HEIGHT, false);
            shadowPingBuffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        if (shadowBlurShader == null) {
            shadowBlurShader = new ShaderProgram(SHADOW_BLUR_VERTEX_SHADER, SHADOW_BLUR_FRAGMENT_SHADER);
            if (!shadowBlurShader.isCompiled()) {
                Gdx.app.error("ShadowBlur", "Failed to compile shadow blur shader: " + shadowBlurShader.getLog());
                shadowBlurShader.dispose();
                shadowBlurShader = null;
            }
        }
    }

    private void renderShadowLayer() {
        if (shadowBuffer == null) {
            return;
        }

        shadowBuffer.begin();
        ScreenUtils.clear(0f, 0f, 0f, 0f);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawShadowCasters(shapeRenderer);
        shapeRenderer.end();
        shadowBuffer.end();

        blurShadowBuffer();
    }

    private void blurShadowBuffer() {
        if (shadowBlurShader == null || shadowPingBuffer == null) {
            return;
        }

        Texture source = shadowBuffer.getColorBufferTexture();
        source.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        spriteBatch.setShader(shadowBlurShader);

        shadowPingBuffer.begin();
        ScreenUtils.clear(0f, 0f, 0f, 0f);
        spriteBatch.setProjectionMatrix(screenMatrix);
        spriteBatch.setColor(1f, 1f, 1f, 1f);
        spriteBatch.enableBlending();
        spriteBatch.begin();
        shadowBlurShader.setUniformf("u_direction", 1f / GAME_WIDTH, 0f);
        spriteBatch.draw(source,
                0f, 0f,
                0f, 0f,
                GAME_WIDTH, GAME_HEIGHT,
                1f, 1f,
                0f,
                0, 0,
                source.getWidth(), source.getHeight(),
                false, true);
        spriteBatch.end();
        shadowPingBuffer.end();

        Texture pingTexture = shadowPingBuffer.getColorBufferTexture();
        pingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        shadowBuffer.begin();
        ScreenUtils.clear(0f, 0f, 0f, 0f);
        spriteBatch.setProjectionMatrix(screenMatrix);
        spriteBatch.setColor(1f, 1f, 1f, 1f);
        spriteBatch.enableBlending();
        spriteBatch.begin();
        shadowBlurShader.setUniformf("u_direction", 0f, 1f / GAME_HEIGHT);
        spriteBatch.draw(pingTexture,
                0f, 0f,
                0f, 0f,
                GAME_WIDTH, GAME_HEIGHT,
                1f, 1f,
                0f,
                0, 0,
                pingTexture.getWidth(), pingTexture.getHeight(),
                false, true);
        spriteBatch.end();
        shadowBuffer.end();

        spriteBatch.setShader(null);
    }

    private void drawShadowCasters(ShapeRenderer shapeRenderer) {
        paddle.renderShadow(shapeRenderer, SHADOW_OFFSET_X, SHADOW_OFFSET_Y);
        for (Ball ball : balls) {
            ball.renderShadow(shapeRenderer, SHADOW_OFFSET_X, SHADOW_OFFSET_Y);
        }
        for (Brick brick : bricks) {
            brick.renderShadow(shapeRenderer, SHADOW_OFFSET_X, SHADOW_OFFSET_Y);
        }
        for (PowerUp powerUp : powerUps) {
            powerUp.renderShadow(shapeRenderer, SHADOW_OFFSET_X, SHADOW_OFFSET_Y);
        }
        for (Laser laser : lasers) {
            laser.renderShadow(shapeRenderer, SHADOW_OFFSET_X, SHADOW_OFFSET_Y);
        }
    }

    private void renderHud() {
        float panelX = 16f;
        float panelWidth = 240f;
        float panelHeight = 90f;
        float panelY = GAME_HEIGHT - panelHeight - 16f;

        if (pixelTexture != null) {
            spriteBatch.setColor(0f, 0f, 0f, 0.45f);
            spriteBatch.draw(pixelTexture, panelX, panelY, panelWidth, panelHeight);
            spriteBatch.setColor(0f, 0.8f, 1f, 0.25f);
            spriteBatch.draw(pixelTexture, panelX, panelY + panelHeight - 3f, panelWidth, 3f);
            spriteBatch.setColor(Color.WHITE);
        }

        drawTextWithShadow("Score: " + score, panelX + 16f, panelY + panelHeight - 18f);
        drawTextWithShadow("Lives: " + lives, panelX + 16f, panelY + panelHeight - 44f);
        drawTextWithShadow("Level: " + currentLevel, panelX + 16f, panelY + panelHeight - 70f);

        if (comboCount > 1) {
            drawTextWithShadow("Combo x" + comboCount, panelX + 16f, panelY + 22f);
        } else if (maxCombo > 1) {
            drawTextWithShadow("Max Combo: " + maxCombo, panelX + 16f, panelY + 22f);
        }

        drawTextWithShadow("Power-ups: 1-8 | Levels: F1-F6 | F7: Collision Mode", 16f, 36f);

        // Show collision mode
        drawTextWithShadow("Collision: " + collisionMode.getLabel(), GAME_WIDTH - 160f, 36f);
    }

    private void renderGameStateMessages() {
        if (gameOver) {
            drawTextCenteredWithShadow("GAME OVER", GAME_WIDTH / 2f, GAME_HEIGHT / 2f + 28f);
            drawTextCenteredWithShadow("Final Score: " + score, GAME_WIDTH / 2f, GAME_HEIGHT / 2f);
            drawTextCenteredWithShadow("Press SPACE to restart", GAME_WIDTH / 2f, GAME_HEIGHT / 2f - 32f);
        } else if (gameWon) {
            drawTextCenteredWithShadow("VICTORY!", GAME_WIDTH / 2f, GAME_HEIGHT / 2f + 28f);
            drawTextCenteredWithShadow("Final Score: " + score, GAME_WIDTH / 2f, GAME_HEIGHT / 2f);
            drawTextCenteredWithShadow("Press SPACE to restart", GAME_WIDTH / 2f, GAME_HEIGHT / 2f - 32f);
        } else if (!ballLaunched && !balls.isEmpty()) {
            drawTextCenteredWithShadow("Press SPACE to launch ball", GAME_WIDTH / 2f, GAME_HEIGHT / 2f);
        }
    }

    private void drawTextWithShadow(String text, float x, float y) {
        font.setColor(0f, 0f, 0f, 0.6f);
        font.draw(spriteBatch, text, x + 2f, y - 2f);
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, text, x, y);
    }

    private void drawTextCenteredWithShadow(String text, float centerX, float centerY) {
        if (glyphLayout == null) {
            glyphLayout = new GlyphLayout();
        }
        glyphLayout.setText(font, text);
        float x = centerX - glyphLayout.width / 2f;
        float y = centerY + glyphLayout.height / 2f;
        drawTextWithShadow(text, x, y);
    }

    private void triggerScreenShake(float duration, float intensity) {
        screenShakeTime = Math.max(screenShakeTime, duration);
        screenShakeDuration = Math.max(screenShakeDuration, duration);
        screenShakeIntensity = Math.max(screenShakeIntensity, intensity);
    }

    private void update(float deltaTime) {
        if (screenShakeTime > 0f) {
            screenShakeTime -= deltaTime;
            if (screenShakeTime <= 0f) {
                screenShakeTime = 0f;
                screenShakeDuration = 0f;
                screenShakeIntensity = 0f;
            }
        }

        if (comboCount > 0) {
            comboTimer -= deltaTime;
            if (comboTimer <= 0f) {
                comboCount = 0;
                comboTimer = 0f;
            }
        }

        if (gameOver || gameWon) {
            // Check for restart
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.justTouched()) {
                restartGame();
            }
            return;
        }


        // Update paddle movement (mouse has priority over keyboard)
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            paddle.update(deltaTime, GAME_WIDTH);
        } else {
            // Convert mouse coordinates to game world coordinates
            com.badlogic.gdx.math.Vector3 mousePos = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mousePos);
            paddle.updateWithMouse(deltaTime, GAME_WIDTH, mousePos.x);
        }

        // Check for level switch keys (F1-F6)
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            switchLevel(1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            switchLevel(2);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            switchLevel(3);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            switchLevel(4);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            switchLevel(5);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
            switchLevel(6);
        }

        // Toggle collision detection mode with F7
        if (Gdx.input.isKeyJustPressed(Input.Keys.F7)) {
            collisionMode = collisionMode.next();
            System.out.println("Collision mode: " + collisionMode.getLabel());
        }

        // Check for cheat keys (testing power-ups)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            applyPowerUp(PowerUp.Type.MULTI_BALL);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            applyPowerUp(PowerUp.Type.LARGE_PADDLE);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            applyPowerUp(PowerUp.Type.SMALL_PADDLE);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            applyPowerUp(PowerUp.Type.EXTRA_LIFE);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            applyPowerUp(PowerUp.Type.SPEED_UP);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
            applyPowerUp(PowerUp.Type.SPEED_DOWN);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) {
            applyPowerUp(PowerUp.Type.LASER);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8)) {
            applyPowerUp(PowerUp.Type.STICKY_PADDLE);
        }

        // Update laser cooldown
        if (laserCooldown > 0) {
            laserCooldown -= deltaTime;
        }

        float comboIntensity = comboCount > 0 ? MathUtils.clamp(comboCount / 6f, 0f, 2f) : 0f;

        // Handle SPACE key or mouse click based on paddle mode
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.justTouched()) {
            if (paddle.isLaser() && laserCooldown <= 0) {
                // Shoot twin lasers from paddle (like original Arkanoid)
                float laserY = paddle.getY() + paddle.getHeight();
                // Left laser
                lasers.add(new Laser(paddle.getX() + paddle.getWidth() * 0.25f, laserY));
                // Right laser
                lasers.add(new Laser(paddle.getX() + paddle.getWidth() * 0.75f, laserY));
                laserCooldown = 0.3f; // 300ms cooldown
            } else if (paddle.isSticky() && !stickyBalls.isEmpty()) {
                // Release sticky balls based on their position on paddle
                for (Ball stickyBall : stickyBalls) {
                    // Calculate ball's position relative to paddle center
                    float paddleCenter = paddle.getX() + paddle.getWidth() / 2f;
                    float ballOffsetFromCenter = stickyBall.getX() - paddleCenter;

                    // Launch angle based on position: -60° to +60° from vertical
                    float maxAngle = 60f; // degrees
                    float angleInDegrees = (ballOffsetFromCenter / (paddle.getWidth() / 2f)) * maxAngle;
                    float angleInRadians = (float) Math.toRadians(90f - angleInDegrees); // 90° = straight up

                    float speed = 300f;
                    stickyBall.setVelocity(
                        (float)(speed * Math.cos(angleInRadians)),
                        (float)(speed * Math.sin(angleInRadians))
                    );
                }
                stickyBalls.clear(); // Release all balls
            }
        }

        // Check for ball launch
        if (!ballLaunched && !balls.isEmpty()) {
            // Only follow paddle with the first ball if not launched
            Ball firstBall = balls.get(0);
            firstBall.followPaddle(paddle);
            firstBall.updateTrail(deltaTime);
            firstBall.updateTrailStyle(comboIntensity);
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.justTouched()) {
                firstBall.launch();
                if (startSound != null) {
                    startSound.play();
                }
                ballLaunched = true;
            }
            return; // Don't update ball physics until launched
        }

        // Update all balls movement with collision detection
        for (int i = balls.size() - 1; i >= 0; i--) {
            Ball ball = balls.get(i);

            if (stickyBalls.contains(ball)) {
                ball.followPaddle(paddle);
                ball.updateTrail(deltaTime);
                ball.updateTrailStyle(comboIntensity);
            } else {
                updateBallWithCollisions(ball, deltaTime);

                if (ball.isOutOfBounds(GAME_HEIGHT)) {
                    stickyBalls.remove(ball);
                    balls.remove(i);
                    continue;
                }

                ball.updateTrail(deltaTime);
                ball.updateTrailStyle(comboIntensity);
            }
        }

        // Check if all balls are lost (lost life)
        if (balls.isEmpty()) {
            comboCount = 0;
            comboTimer = 0f;
            lives--;
            if (lives <= 0) {
                gameOver = true;
            } else {
                // Reset with new ball on paddle
                Ball newBall = new Ball(paddle.getX() + paddle.getWidth() / 2f,
                                      paddle.getY() + paddle.getHeight() + 8f, 8f);
                newBall.setTrailColor(Color.CYAN);
                balls.add(newBall);
                ballLaunched = false;
            }
            return;
        }

        // Update power-ups
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp powerUp = powerUps.get(i);
            powerUp.update(deltaTime);

            // Check collision with paddle
            if (powerUp.checkCollisionWithPaddle(paddle)) {
                applyPowerUp(powerUp.getType());
                powerUps.remove(i);
                continue;
            }

            // Remove power-ups that are out of bounds
            if (powerUp.isOutOfBounds(GAME_HEIGHT)) {
                powerUps.remove(i);
            }
        }

        // Update particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            particle.update(deltaTime);
            if (particle.isDead()) {
                particles.remove(i);
            }
        }

        // Update lasers
        for (int i = lasers.size() - 1; i >= 0; i--) {
            Laser laser = lasers.get(i);
            laser.update(deltaTime);

            // Remove lasers that are out of bounds
            if (laser.isOutOfBounds(GAME_HEIGHT)) {
                lasers.remove(i);
                continue;
            }

            // Check collision with bricks
            boolean hit = false;
            for (Brick brick : bricks) {
                if (laser.checkCollisionWithBrick(brick)) {
                    if (brickHitSound != null) {
                        brickHitSound.play();
                    }

                    boolean destroyed = brick.hit();
                    if (destroyed) {
                        score += brick.getScore();
                        comboCount++;
                        comboTimer = 1.5f;
                        if (comboCount > maxCombo) {
                            maxCombo = comboCount;
                        }
                        triggerScreenShake(brick.isBomb() ? 0.45f : 0.15f, brick.isBomb() ? 14f : 5f);

                        createDestructionParticles(brick.getX() + brick.getWidth() / 2f,
                                                      brick.getY() + brick.getHeight() / 2f,
                                                      brick.getOriginalColor());

                        if (brick.isBomb()) {
                            explodeBrick(brick.getX() + brick.getWidth() / 2f,
                                       brick.getY() + brick.getHeight() / 2f);
                        }

                        if (Math.random() < 0.3) {
                            dropPowerUp(brick.getX() + brick.getWidth() / 2f, brick.getY());
                        }
                    }

                    hit = true;
                    break;
                }
            }
            if (hit) {
                lasers.remove(i);
            }
        }

        // Check win condition (all destructible bricks destroyed)
        boolean allDestroyed = true;
        for (Brick brick : bricks) {
            // Only count destructible bricks for win condition
            if (!brick.isIndestructible() && !brick.isDestroyed()) {
                allDestroyed = false;
                break;
            }
        }
        if (allDestroyed) {
            gameWon = true;
        }
    }

    private void restartGame() {
        // Reset game state
        score = 0;
        comboCount = 0;
        comboTimer = 0f;
        maxCombo = 0;
        screenShakeTime = 0f;
        screenShakeDuration = 0f;
        screenShakeIntensity = 0f;
        lives = 3;
        gameOver = false;
        gameWon = false;
        ballLaunched = false;

        // Reset balls - start with one ball on paddle
        balls.clear();
        Ball newBall = new Ball(paddle.getX() + paddle.getWidth() / 2f,
                               paddle.getY() + paddle.getHeight() + 8f, 8f);
        newBall.setTrailColor(Color.CYAN);
        balls.add(newBall);

        // Clear power-ups, particles and lasers
        powerUps.clear();
        particles.clear();
        lasers.clear();
        laserCooldown = 0f;
        stickyBalls = new ArrayList<>();

        // Reset paddle to normal mode
        paddle.setMode(Paddle.Mode.NORMAL);

        // Recreate bricks for current level
        loadLevel(currentLevel);
    }

    private void switchLevel(int newLevel) {
        currentLevel = newLevel;

        // Clear all game objects
        balls.clear();
        powerUps.clear();
        particles.clear();
        lasers.clear();
        stickyBalls.clear();

        // Reset paddle
        paddle.setX(GAME_WIDTH / 2f - 50f);
        paddle.setWidth(100f);
        paddle.setMode(Paddle.Mode.NORMAL);

        // Create new ball on paddle
        Ball newBall = new Ball(paddle.getX() + paddle.getWidth() / 2f,
                               paddle.getY() + paddle.getHeight() + 8f, 8f);
        newBall.setTrailColor(Color.CYAN);
        balls.add(newBall);

        ballLaunched = false;
        laserCooldown = 0f;

        // Load new level
        loadLevel(currentLevel);
    }

    private void updateBallWithCollisions(Ball ball, float deltaTime) {
        // Continuous collision detection - subdivide movement into small steps
        float stepSize = ball.getRadius(); // Move at most one radius per step
        float distance = ball.getVelocity().len() * deltaTime;
        int steps = Math.max(1, (int) Math.ceil(distance / stepSize));

        float stepX = (ball.getVelocity().x * deltaTime) / steps;
        float stepY = (ball.getVelocity().y * deltaTime) / steps;

        for (int i = 0; i < steps; i++) {
            // Store previous position for swept collision
            float prevX = ball.getX();
            float prevY = ball.getY();

            // Move ball one step
            ball.setPosition(ball.getX() + stepX, ball.getY() + stepY);

            // Check collision with walls
            if (ball.getX() - ball.getRadius() <= 0 || ball.getX() + ball.getRadius() >= GAME_WIDTH) {
                ball.reverseX();
                // Keep ball in bounds
                if (ball.getX() - ball.getRadius() < 0) ball.setPosition(ball.getRadius(), ball.getY());
                if (ball.getX() + ball.getRadius() > GAME_WIDTH) ball.setPosition(GAME_WIDTH - ball.getRadius(), ball.getY());
                if (wallHitSound != null) {
                    wallHitSound.play();
                }
                break; // Stop movement for this frame after collision
            }

            if (ball.getY() + ball.getRadius() >= GAME_HEIGHT) {
                ball.reverseY();
                ball.setPosition(ball.getX(), GAME_HEIGHT - ball.getRadius());
                if (wallHitSound != null) {
                    wallHitSound.play();
                }
                break; // Stop movement for this frame after collision
            }

            // Check collision with paddle
            if (ball.checkCollisionWithPaddle(paddle, paddle.isSticky())) {
                if (paddleHitSound != null) {
                    paddleHitSound.play();
                }
                if (paddle.isSticky() && ball.getVelocity().len() == 0) {
                    // Calculate and store offset from paddle center for following movement
                    float offsetX = ball.getX() - (paddle.getX() + paddle.getWidth() / 2f);
                    ball.setStickyOffset(offsetX);
                    stickyBalls.add(ball); // Ball is now stuck to paddle at its current position
                }
                break; // Stop movement for this frame after collision
            }

            // Check collision with bricks
            boolean brickHit = false;
            for (Brick brick : bricks) {
                int hitsBefore = brick.getHits();
                boolean wasDestroyed = brick.isDestroyed();

                // Choose collision method based on selected collision mode
                int points;
                switch (collisionMode) {
                    case DISCRETE:
                        points = ball.checkCollisionWithBrick(brick);
                        break;
                    case CONTINUOUS:
                        points = ball.checkCollisionWithBrickSwept(brick, prevX, prevY);
                        break;
                    case ROBUST:
                        points = ball.checkCollisionWithBrickRobust(brick, prevX, prevY);
                        break;
                    default:
                        points = ball.checkCollisionWithBrick(brick);
                        break;
                }

                int hitsAfter = brick.getHits();
                boolean isDestroyed = brick.isDestroyed();
                boolean collided = hitsAfter != hitsBefore || wasDestroyed != isDestroyed;

                if (collided) {
                    if (brickHitSound != null) {
                        brickHitSound.play();
                    }

                    if (points > 0) {
                    score += points;
                    comboCount++;
                    comboTimer = 1.5f;
                    if (comboCount > maxCombo) {
                        maxCombo = comboCount;
                    }

                        // Check if brick was destroyed
                        if (brick.isDestroyed()) {
                            triggerScreenShake(brick.isBomb() ? 0.5f : 0.2f, brick.isBomb() ? 18f : 6f);
                            // Create destruction particles
                            createDestructionParticles(brick.getX() + brick.getWidth() / 2f,
                                                      brick.getY() + brick.getHeight() / 2f,
                                                      brick.getOriginalColor());

                            // Check if it was a bomb brick
                            if (brick.isBomb()) {
                                explodeBrick(brick.getX() + brick.getWidth() / 2f,
                                           brick.getY() + brick.getHeight() / 2f);
                            }

                            // Maybe drop power-up
                            if (Math.random() < 0.3) { // 30% chance
                                dropPowerUp(brick.getX() + brick.getWidth() / 2f, brick.getY());
                            }
                        }
                    }

                    brickHit = true;
                    break; // Only one collision per step
                }
            }
            if (brickHit) {
                break; // Stop movement for this frame after collision
            }
        }
    }

    private void dropPowerUp(float x, float y) {
        // Random power-up type
        PowerUp.Type[] types = PowerUp.Type.values();
        PowerUp.Type randomType = types[(int) (Math.random() * types.length)];
        powerUps.add(new PowerUp(x - 10f, y, randomType)); // Center the power-up
    }

    private void applyPowerUp(PowerUp.Type type) {
        switch (type) {
            case MULTI_BALL:
                // Create 2 additional balls from the first existing ball
                if (!balls.isEmpty()) {
                    Ball firstBall = balls.get(0);
                    float speed = firstBall.getVelocity().len();
                    if (speed == 0) speed = 300f; // Default speed if ball is stationary

                    // Create balls with upward angles (45-135 degrees = upward directions)
                    float[] angles = {60f, 120f}; // Left-up and right-up

                    Color[] colors = {Color.YELLOW, Color.MAGENTA}; // Different colors for variety
                    for (int i = 0; i < 2; i++) {
                        Ball newBall = new Ball(firstBall.getX(), firstBall.getY(), firstBall.getRadius());
                        newBall.setTrailColor(colors[i]);
                        float angle = angles[i] + (float)(Math.random() * 20 - 10); // Add some randomness ±10°

                        newBall.setVelocity(
                            (float) (speed * Math.cos(Math.toRadians(angle))),
                            (float) (speed * Math.sin(Math.toRadians(angle)))
                        );
                        balls.add(newBall);
                    }
                }
                break;

            case LARGE_PADDLE:
                // Increase paddle width by 50%
                float oldWidth = paddle.getWidth();
                float newWidth = oldWidth * 1.5f;
                float newX = paddle.getX() - (newWidth - oldWidth) / 2f;
                // Keep paddle within bounds
                if (newX < 0) newX = 0;
                if (newX + newWidth > GAME_WIDTH) newX = GAME_WIDTH - newWidth;
                paddle.setX(newX);
                paddle.setWidth(newWidth);
                break;

            case SMALL_PADDLE:
                // Decrease paddle width by 30%
                float oldWidthSmall = paddle.getWidth();
                float newWidthSmall = oldWidthSmall * 0.7f;
                float newXSmall = paddle.getX() + (oldWidthSmall - newWidthSmall) / 2f;
                // Ensure minimum width
                if (newWidthSmall < 40f) newWidthSmall = 40f;
                paddle.setX(newXSmall);
                paddle.setWidth(newWidthSmall);
                break;

            case LASER:
                paddle.setMode(Paddle.Mode.LASER, 30f); // 30 seconds duration
                break;

            case STICKY_PADDLE:
                paddle.setMode(Paddle.Mode.STICKY, 30f); // 30 seconds duration
                break;

            case EXTRA_LIFE:
                lives++;
                break;

            case SPEED_UP:
                // Increase ball speed by 20%
                for (Ball ball : balls) {
                    ball.setVelocity(ball.getVelocity().x * 1.2f, ball.getVelocity().y * 1.2f);
                }
                break;

            case SPEED_DOWN:
                // Decrease ball speed by 20%
                for (Ball ball : balls) {
                    ball.setVelocity(ball.getVelocity().x * 0.8f, ball.getVelocity().y * 0.8f);
                }
                break;
        }
    }

    private void createDestructionParticles(float x, float y, Color brickColor) {
        // Create 8-12 particles with the brick's color
        int particleCount = 8 + (int)(Math.random() * 5);
        for (int i = 0; i < particleCount; i++) {
            particles.add(new Particle(x, y, brickColor));
        }
    }

    private void explodeBrick(float bombX, float bombY) {
        float explosionRadius = 80f; // Explosion radius

        triggerScreenShake(0.45f, 14f);

        // Create extra explosion particles
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(bombX, bombY, Color.ORANGE));
        }

        // Destroy nearby bricks (except indestructible ones)
        for (Brick brick : bricks) {
            if (!brick.isDestroyed() && !brick.isIndestructible()) {
                float brickCenterX = brick.getX() + brick.getWidth() / 2f;
                float brickCenterY = brick.getY() + brick.getHeight() / 2f;

                // Calculate distance from explosion center
                float distance = (float) Math.sqrt(
                    Math.pow(brickCenterX - bombX, 2) + Math.pow(brickCenterY - bombY, 2)
                );

                if (distance <= explosionRadius) {
                    // Destroy the brick and add score
                    score += brick.getScore();
                    brick.destroy();

                    // Create particles for destroyed brick
                    createDestructionParticles(brickCenterX, brickCenterY, brick.getOriginalColor());

                    // Chain reaction: if destroyed brick is also a bomb, explode it too
                    if (brick.isBomb()) {
                        explodeBrick(brickCenterX, brickCenterY);
                    }
                }
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
        if (postProcessBatch != null) {
            postProcessBatch.dispose();
        }
        if (frameBuffer != null) {
            frameBuffer.dispose();
        }
        if (shadowBuffer != null) {
            shadowBuffer.dispose();
        }
        if (shadowPingBuffer != null) {
            shadowPingBuffer.dispose();
        }
        if (crtShader != null) {
            crtShader.dispose();
        }
        if (shadowBlurShader != null) {
            shadowBlurShader.dispose();
        }
        font.dispose();
        if (pixelTexture != null) {
            pixelTexture.dispose();
        }
        if (startSound != null) {
            startSound.dispose();
        }
        if (paddleHitSound != null) {
            paddleHitSound.dispose();
        }
        if (brickHitSound != null) {
            brickHitSound.dispose();
        }
        if (wallHitSound != null) {
            wallHitSound.dispose();
        }
    }
}
