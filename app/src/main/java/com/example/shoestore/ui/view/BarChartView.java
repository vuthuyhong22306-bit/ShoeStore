package com.example.shoestore.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.shoestore.model.RevenueByDate;

import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {
    private List<RevenueByDate> dataList = new ArrayList<>();
    private Paint highlightBarPaint;
    private Paint normalBarPaint;
    private Paint textPaint;
    private RectF barRect = new RectF();

    private static final String[] WEEKDAY_LABELS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        highlightBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightBarPaint.setColor(Color.parseColor("#D0343C"));

        normalBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        normalBarPaint.setColor(Color.parseColor("#F0B8BC"));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#6B6B6B"));
        textPaint.setTextSize(32f);
    }

    public void setData(List<RevenueByDate> dataList) {
        this.dataList = dataList != null ? dataList : new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        if (dataList == null || dataList.isEmpty()) {
            // Draw 7 empty/default placeholder bars (T2..CN) matching screenshot layout
            drawPlaceholderBars(canvas, width, height);
            return;
        }

        float paddingLeft = 40f;
        float paddingRight = 40f;
        float paddingTop = 30f;
        float paddingBottom = 60f;

        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;

        double maxVal = 0;
        int maxIndex = 0;
        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.get(i).getRevenue() > maxVal) {
                maxVal = dataList.get(i).getRevenue();
                maxIndex = i;
            }
        }
        if (maxVal == 0) maxVal = 1;

        int count = Math.min(dataList.size(), 7);
        float barWidth = (chartWidth / count) * 0.55f;
        float space = (chartWidth / count);

        for (int i = 0; i < count; i++) {
            RevenueByDate item = dataList.get(i);
            float left = paddingLeft + (i * space) + (space - barWidth) / 2f;
            float right = left + barWidth;

            float ratio = (float) (item.getRevenue() / maxVal);
            if (ratio < 0.15f && item.getRevenue() > 0) ratio = 0.15f;

            float top = height - paddingBottom - (ratio * chartHeight);
            if (top > height - paddingBottom - 15f) {
                top = height - paddingBottom - 15f;
            }

            barRect.set(left, top, right, height - paddingBottom);
            Paint p = (i == maxIndex && item.getRevenue() > 0) ? highlightBarPaint : normalBarPaint;
            canvas.drawRoundRect(barRect, 10f, 10f, p);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(30f);
            String label = (i < WEEKDAY_LABELS.length) ? WEEKDAY_LABELS[i] : item.getDate();
            canvas.drawText(label != null ? label : "", left + barWidth / 2f, height - paddingBottom + 45f, textPaint);
        }
    }

    private void drawPlaceholderBars(Canvas canvas, int width, int height) {
        float paddingLeft = 40f;
        float paddingRight = 40f;
        float paddingTop = 30f;
        float paddingBottom = 60f;

        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;

        float[] sampleHeights = {0.4f, 0.6f, 0.35f, 0.7f, 0.5f, 0.95f, 0.55f}; // T7 is highest (matching photo)

        float barWidth = (chartWidth / 7) * 0.55f;
        float space = (chartWidth / 7);

        for (int i = 0; i < 7; i++) {
            float left = paddingLeft + (i * space) + (space - barWidth) / 2f;
            float right = left + barWidth;

            float top = height - paddingBottom - (sampleHeights[i] * chartHeight);

            barRect.set(left, top, right, height - paddingBottom);
            Paint p = (i == 5) ? highlightBarPaint : normalBarPaint; // T7 highlighted
            canvas.drawRoundRect(barRect, 10f, 10f, p);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(30f);
            canvas.drawText(WEEKDAY_LABELS[i], left + barWidth / 2f, height - paddingBottom + 45f, textPaint);
        }
    }
}