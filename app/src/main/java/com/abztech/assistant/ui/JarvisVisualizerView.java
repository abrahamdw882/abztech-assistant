package com.abztech.assistant.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Futuristic Holographic Arc Reactor & Audio Reactive HUD Visualizer.
 * Inspired by Tony Stark's J.A.R.V.I.S. neural interface.
 */
public class JarvisVisualizerView extends View {

    private AssistantState state = AssistantState.IDLE;
    private float targetAmplitude = 0.15f;
    private float currentAmplitude = 0.15f;

    private float outerAngle = 0f;
    private float innerAngle = 0f;
    private float pulsePhase = 0f;

    // Paints
    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF outerBounds = new RectF();
    private final RectF innerBounds = new RectF();
    private final Path wavePath = new Path();

    // J.A.R.V.I.S. Holographic Color Palette
    private final int COLOR_ARC_CYAN = Color.parseColor("#00F0FF");
    private final int COLOR_ELECTRIC_BLUE = Color.parseColor("#0072FF");
    private final int COLOR_CORE_WHITE = Color.parseColor("#FFFFFF");
    private final int COLOR_AMBER_ORANGE = Color.parseColor("#FF9900");
    private final int COLOR_DEEP_GLOW = Color.parseColor("#003875");

    public JarvisVisualizerView(Context context) {
        super(context);
        init();
    }

    public JarvisVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public JarvisVisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        ringPaint1.setStyle(Paint.Style.STROKE);
        ringPaint1.setStrokeWidth(3.5f);
        ringPaint1.setColor(COLOR_ARC_CYAN);

        ringPaint2.setStyle(Paint.Style.STROKE);
        ringPaint2.setStrokeWidth(2.5f);
        ringPaint2.setColor(COLOR_ELECTRIC_BLUE);

        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(3f);
        wavePaint.setColor(COLOR_ARC_CYAN);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(2f);
        tickPaint.setColor(COLOR_ARC_CYAN);

        glowPaint.setStyle(Paint.Style.FILL);
        corePaint.setStyle(Paint.Style.FILL);
    }

    public void setState(AssistantState state) {
        this.state = state;
        switch (state) {
            case IDLE:
                targetAmplitude = 0.15f;
                ringPaint1.setColor(COLOR_ARC_CYAN);
                break;
            case LISTENING:
                targetAmplitude = 0.55f;
                ringPaint1.setColor(COLOR_ARC_CYAN);
                break;
            case THINKING:
                targetAmplitude = 0.40f;
                ringPaint1.setColor(COLOR_AMBER_ORANGE);
                break;
            case SPEAKING:
                targetAmplitude = 0.70f;
                ringPaint1.setColor(COLOR_ARC_CYAN);
                break;
            case ERROR:
                targetAmplitude = 0.12f;
                ringPaint1.setColor(COLOR_AMBER_ORANGE);
                break;
        }
        invalidate();
    }

    public void setAudioLevel(float rmsDb) {
        if (state == AssistantState.LISTENING) {
            float normalized = Math.max(0.18f, Math.min(1.0f, (rmsDb + 2.0f) / 10.0f));
            targetAmplitude = normalized;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float cx = width / 2.0f;
        float cy = height / 2.0f;
        float baseRadius = Math.min(width, height) * 0.36f;

        // Smooth amplitude interpolation
        currentAmplitude += (targetAmplitude - currentAmplitude) * 0.2f;

        // Rotation speed based on state
        float rotSpeed = (state == AssistantState.THINKING) ? 4.5f : (state == AssistantState.LISTENING) ? 2.5f : 0.8f;
        outerAngle = (outerAngle + rotSpeed) % 360f;
        innerAngle = (innerAngle - rotSpeed * 1.3f) % 360f;
        pulsePhase += 0.08f;

        // 1. Central Ambient Plasma Glow
        float glowRadius = baseRadius * (1.1f + (float) Math.sin(pulsePhase) * 0.1f + currentAmplitude * 0.3f);
        int activeGlowColor = (state == AssistantState.THINKING) ? COLOR_AMBER_ORANGE : COLOR_ARC_CYAN;
        glowPaint.setShader(new RadialGradient(cx, cy, glowRadius,
                new int[]{Color.argb(90, Color.red(activeGlowColor), Color.green(activeGlowColor), Color.blue(activeGlowColor)),
                        Color.argb(20, 0, 114, 255),
                        Color.TRANSPARENT},
                new float[]{0.0f, 0.6f, 1.0f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, glowRadius, glowPaint);

        // 2. Outer Segmented HUD Ring with Tick Marks
        float outerR = baseRadius * 0.95f;
        outerBounds.set(cx - outerR, cy - outerR, cx + outerR, cy + outerR);
        canvas.save();
        canvas.rotate(outerAngle, cx, cy);
        for (int i = 0; i < 4; i++) {
            canvas.drawArc(outerBounds, i * 90f + 10f, 65f, false, ringPaint1);
        }
        // Draw tick marks on outer ring
        for (int a = 0; a < 360; a += 30) {
            double rad = Math.toRadians(a);
            float x1 = (float) (cx + (outerR - 6) * Math.cos(rad));
            float y1 = (float) (cy + (outerR - 6) * Math.sin(rad));
            float x2 = (float) (cx + (outerR + 6) * Math.cos(rad));
            float y2 = (float) (cy + (outerR + 6) * Math.sin(rad));
            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }
        canvas.restore();

        // 3. Middle Concentric Ring (Counter-rotating)
        float midR = baseRadius * 0.72f;
        innerBounds.set(cx - midR, cy - midR, cx + midR, cy + midR);
        canvas.save();
        canvas.rotate(innerAngle, cx, cy);
        for (int i = 0; i < 3; i++) {
            canvas.drawArc(innerBounds, i * 120f + 15f, 80f, false, ringPaint2);
        }
        canvas.restore();

        // 4. Reactive Audio Spectrum Waves radiating across center
        drawAudioWave(canvas, cx, cy, width, baseRadius);

        // 5. Central Arc Reactor Core
        float coreRadius = baseRadius * 0.32f * (1.0f + currentAmplitude * 0.25f);
        corePaint.setColor(COLOR_CORE_WHITE);
        canvas.drawCircle(cx, cy, coreRadius * 0.55f, corePaint);

        ringPaint1.setStrokeWidth(4f);
        canvas.drawCircle(cx, cy, coreRadius, ringPaint1);

        // Keep continuous 60fps animation active
        postInvalidateOnAnimation();
    }

    private void drawAudioWave(Canvas canvas, float cx, float cy, int width, float baseRadius) {
        wavePath.reset();
        float waveWidth = width * 0.85f;
        float startX = (width - waveWidth) / 2.0f;
        float maxAmp = baseRadius * 0.55f * currentAmplitude;

        wavePath.moveTo(startX, cy);

        int points = 40;
        float step = waveWidth / points;
        for (int i = 0; i <= points; i++) {
            float x = startX + (i * step);
            float normalized = (float) i / points;
            // Hanning envelope window so wave tapers smoothly at both ends
            float envelope = (float) Math.sin(normalized * Math.PI);
            float freq = (state == AssistantState.SPEAKING) ? 3.0f : 2.0f;
            float y = cy + (envelope * maxAmp * (float) Math.sin((normalized * Math.PI * freq) + pulsePhase * 2f));
            wavePath.lineTo(x, y);
        }

        wavePaint.setColor((state == AssistantState.THINKING) ? COLOR_AMBER_ORANGE : COLOR_ARC_CYAN);
        wavePaint.setAlpha(Math.min(255, (int) (140 + currentAmplitude * 115)));
        canvas.drawPath(wavePath, wavePaint);
    }
}
