package io.github.warnotte.warkanoid.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.warnotte.warkanoid.Main;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CrtSettingsOverlay {
    private final Main.CrtSettings settings;
    private final Stage stage;
    private final Skin skin;
    private boolean visible;

    public CrtSettingsOverlay(Main.CrtSettings settings, SpriteBatch sharedBatch) {
        this.settings = settings;
        this.stage = new Stage(new FitViewport(Main.GAME_WIDTH, Main.GAME_HEIGHT), sharedBatch);
        this.skin = createSkin();
        buildUi();
        setVisible(false);
    }

    private Skin createSkin() {
        Skin skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font, BitmapFont.class);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        skin.add("white", texture);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = skin.newDrawable("white", new Color(1f, 1f, 1f, 0.3f));
        sliderStyle.background.setMinHeight(4f);
        sliderStyle.knob = skin.newDrawable("white", new Color(1f, 0.7f, 0.2f, 0.9f));
        sliderStyle.knob.setMinHeight(16f);
        sliderStyle.knob.setMinWidth(12f);
        sliderStyle.knobOver = skin.newDrawable("white", new Color(1f, 0.9f, 0.4f, 0.9f));
        sliderStyle.knobOver.setMinHeight(18f);
        sliderStyle.knobOver.setMinWidth(14f);
        skin.add("default-horizontal", sliderStyle);

        return skin;
    }

    private void buildUi() {
        Table root = new Table();
        root.setFillParent(true);
        root.top().left().pad(12f);
        root.defaults().pad(6f).left();
        stage.addActor(root);

        root.add(new Label("CRT Settings (F10 to hide)", skin)).colspan(3).left();
        root.row();

        addSlider(root, "Curvature", 0f, 0.3f, 0.001f,
                () -> settings.curvature,
                value -> settings.curvature = value);
        addSlider(root, "Aberration Base", 0f, 0.02f, 0.0005f,
                () -> settings.aberrationBase,
                value -> settings.aberrationBase = value);
        addSlider(root, "Aberration Strength", 0f, 0.05f, 0.0005f,
                () -> settings.aberrationStrength,
                value -> settings.aberrationStrength = value);
        addSlider(root, "Scan Amplitude", 0f, 0.2f, 0.001f,
                () -> settings.scanAmplitude,
                value -> settings.scanAmplitude = value);
        addSlider(root, "Scan Frequency", 0.5f, 4f, 0.01f,
                () -> settings.scanFrequency,
                value -> settings.scanFrequency = value);
        addSlider(root, "Scan Speed", 0f, 8f, 0.05f,
                () -> settings.scanSpeed,
                value -> settings.scanSpeed = value);
        addSlider(root, "Vignette Scale", 4f, 30f, 0.1f,
                () -> settings.vignetteScale,
                value -> settings.vignetteScale = value);
        addSlider(root, "Vignette Power", 0.1f, 1.2f, 0.01f,
                () -> settings.vignettePower,
                value -> settings.vignettePower = value);
        addSlider(root, "Vignette Min", 0f, 1f, 0.01f,
                () -> settings.vignetteMin,
                value -> settings.vignetteMin = value);
        addSlider(root, "Vignette Max", 0.5f, 1.6f, 0.01f,
                () -> settings.vignetteMax,
                value -> settings.vignetteMax = value);
        addSlider(root, "Noise Amount", 0f, 0.1f, 0.001f,
                () -> settings.noiseAmount,
                value -> settings.noiseAmount = value);
        addSlider(root, "Noise Speed", 0f, 0.2f, 0.001f,
                () -> settings.noiseSpeed,
                value -> settings.noiseSpeed = value);
    }

    private void addSlider(Table table,
                           String labelText,
                           float min, float max, float step,
                           Supplier<Float> getter,
                           Consumer<Float> setter) {
        Label label = new Label(labelText, skin);
        final Label valueLabel = new Label(formatValue(getter.get()), skin);
        Slider slider = new Slider(min, max, step, false, skin, "default-horizontal");
        slider.setValue(getter.get());

        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                float value = slider.getValue();
                setter.accept(value);
                valueLabel.setText(formatValue(value));
            }
        });

        table.add(label).width(160f);
        table.add(slider).width(220f);
        table.add(valueLabel).width(70f);
        table.row();
    }

    private String formatValue(float value) {
        return String.format(Locale.US, "%.3f", value);
    }

    public void render(float delta) {
        if (!visible) {
            return;
        }
        stage.act(delta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    public void toggle() {
        setVisible(!visible);
    }

    public void setVisible(boolean show) {
        this.visible = show;
        stage.getRoot().setVisible(show);
        stage.getRoot().setTouchable(show ? Touchable.enabled : Touchable.disabled);
        if (show) {
            Gdx.input.setCursorCatched(false);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public Stage getStage() {
        return stage;
    }
}
