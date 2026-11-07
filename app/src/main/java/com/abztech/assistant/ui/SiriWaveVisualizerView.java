package com.abztech.assistant.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Custom hardware-accelerated animated visualizer that replicates Siri's
 * iconic colorful glowing waveforms and pulsing orb states.
 */
public class SiriWaveVisualizerView extends View {

    private AssistantState state = AssistantState.IDLE;
    private float targetAmplitude = 0.15f;
    private float currentAmplitude = 0.15f;
    private float phase = 0f;

    private final Paint wavePaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint3 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path path1 = new Path();
    private final Path path2 = new Path();
    private final Path path3 = new Path();

    // Siri gradient colors
    private final int COLOR_CYAN = Color.parseColor("#00F5D4");
    private final int COLOR_PURPLE = Color.parseColor("#7928CA");
    private final int COLOR_MAGENTA = Color.parseColor("#FF0080");
    private final int COLOR_BLUE = Color.parseColor("#0070F3");

    public SiriWaveVisualizerView(Context context) {
        super(context);
        init();
    }

    public SiriWaveVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SiriWaveVisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        wavePaint1.setStyle(Paint.Style.STROKE);
        wavePaint1.setStrokeWidth(6f);

        wavePaint2.setStyle(Paint.Style.STROKE);
        wavePaint2.setStrokeWidth(5f);

        wavePaint3.setStyle(Paint.Style.STROKE);
        wavePaint3.setStrokeWidth(4f);

        glowPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            Shader shader1 = new LinearGradient(0, 0, w, 0,
                    new int[]{COLOR_CYAN, COLOR_BLUE, COLOR_PURPLE}, null, Shader.TileMode.CLAMP);
            wavePaint1.setShader(shader1);

            Shader shader2 = new LinearGradient(0, 0, w, 0,
                    new int[]{COLOR_MAGENTA, COLOR_PURPLE, COLOR_CYAN}, null, Shader.TileMode.CLAMP);
            wavePaint2.setShader(shader2);

            Shader shader3 = new LinearGradient(0, 0, w, 0,
                    new int[]{COLOR_BLUE, COLOR_CYAN, COLOR_MAGENTA}, null, Shader.TileMode.CLAMP);
            wavePaint3.setShader(shader3);
        }
    }

    public void setState(AssistantState state) {
        this.state = state;
        switch (state) {
            case IDLE:
                targetAmplitude = 0.12f;
                break;
            case LISTENING:
                targetAmplitude = 0.45f;
                break;
            case THINKING:
                targetAmplitude = 0.35f;
                break;
            case SPEAKING:
                targetAmplitude = 0.65f;
                break;
            case ERROR:
                targetAmplitude = 0.1f;
                break;
        }
        invalidate();
    }

    /**
     * Updates amplitude based on speech RMS dB (range usually -2 to 10+).
     */
    public void setAudioLevel(float rmsDb) {
        if (state == AssistantState.LISTENING) {
            float normalized = Math.max(0.15f, Math.min(1.0f, (rmsDb + 2.0f) / 10.0f));
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

        float midY = height / 2.0f;

        // Smoothly interpolate amplitude towards target
        currentAmplitude += (targetAmplitude - currentAmplitude) * 0.15f;

        // Advance phase according to state speed
        float speed = (state == AssistantState.THINKING) ? 0.15f : (state == AssistantState.SPEAKING) ? 0.12f : 0.08f;
        phase += speed;

        // Center glow effect
        int glowAlpha = (int) (40 + (currentAmplitude * 60));
        glowPaint.setColor(COLOR_PURPLE);
        glowPaint.setAlpha(Math.min(255, Math.max(0, glowAlpha)));
        float glowRadius = (height * 0.35f) * (0.8f + currentAmplitude * 0.4f);
        canvas.drawCircle(width / 2.0f, midY, glowRadius, glowPaint);

        // Draw layered harmonic sine waves
        drawWave(canvas, path1, wavePaint1, midY, width, 1.0f, 1.0f, phase);
        drawWave(canvas, path2, wavePaint2, midY, width, 0.75f, 1.4f, -phase * 0.8f);
        drawWave(canvas, path3, wavePaint3, midY, width, 0.5f, 2.0f, phase * 1.2f);

        // Keep continuous animation running
        postInvalidateOnAnimation();
    }

    private void drawWave(Canvas canvas, Path path, Paint paint, float midY, int width,
                          float ampFactor, float freqFactor, float phaseShift) {
        path.reset();
        path.moveTo(0, midY);

        float maxWaveHeight = (getHeight() * 0.40f) * currentAmplitude * ampFactor;

        for (int x = 0; x <= width; x += 6) {
            // Envelope function (hanning-like window) to smoothly taper wave ends to 0 at edges
            float normalizedX = (float) x / width;
            float envelope = (float) Math.sin(normalizedX * Math.PI);

            // Sine wave formula: y = midY + envelope * maxAmp * sin(2*pi*f*x + phase)
            float y = midY + (envelope * maxWaveHeight *
                    (float) Math.sin((normalizedX * Math.PI * 2.5f * freqFactor) + phaseShift));
            path.lineTo(x, y);
        }

        canvas.drawPath(path, paint);
    }
}
