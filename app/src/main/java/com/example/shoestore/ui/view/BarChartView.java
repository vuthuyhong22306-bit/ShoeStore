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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BarChartView extends View {
    private List<RevenueByDate> dataList = new ArrayList<>();
    private Paint barPaint;
    private Paint textPaint;
    private Paint axisPaint;
    private RectF barRect = new RectF();

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
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#FF6D00"));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(30f);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.LTGRAY);
        axisPaint.setStrokeWidth(3f);
    }

    public void setData(List<RevenueByDate> dataList) {
        this.dataList = dataList != null ? dataList : new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataList == null || dataList.isEmpty()) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Chưa có dữ liệu doanh thu", getWidth() / 2f, getHeight() / 2f, textPaint);
            return;
        }

        int width = getWidth();
        int height = getHeight();

        float paddingLeft = 40f;
        float paddingRight = 40f;
        float paddingTop = 60f;
        float paddingBottom = 70f;

        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;

        double maxVal = 0;
        for (RevenueByDate item : dataList) {
            if (item.getRevenue() > maxVal) {
                maxVal = item.getRevenue();
            }
        }
        if (maxVal == 0) maxVal = 1;

        int size = Math.min(dataList.size(), 7);
        float barWidth = (chartWidth / size) * 0.5f;
        float space = (chartWidth / size);

        canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, axisPaint);

        NumberFormat shortFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        shortFormat.setMaximumFractionDigits(0);

        for (int i = 0; i < size; i++) {
            RevenueByDate item = dataList.get(i);
            float left = paddingLeft + (i * space) + (space - barWidth) / 2f;
            float right = left + barWidth;

            float barHeightRatio = (float) (item.getRevenue() / maxVal);
            float top = height - paddingBottom - (barHeightRatio * chartHeight);

            if (top > height - paddingBottom - 10f) {
                top = height - paddingBottom - 10f;
            }

            barRect.set(left, top, right, height - paddingBottom);
            canvas.drawRoundRect(barRect, 12f, 12f, barPaint);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(26f);
            textPaint.setColor(Color.DKGRAY);
            String dateLabel = item.getDate();
            if (dateLabel != null && dateLabel.length() > 5 && dateLabel.contains("-")) {
                dateLabel = dateLabel.substring(5);
            }
            canvas.drawText(dateLabel != null ? dateLabel : "", left + barWidth / 2f, height - paddingBottom + 40f, textPaint);

            textPaint.setTextSize(22f);
            String revLabel = String.format(Locale.getDefault(), "%.1fM", item.getRevenue() / 1_000_000.0);
            canvas.drawText(revLabel, left + barWidth / 2f, Math.max(paddingTop - 10f, top - 15f), textPaint);
        }
    }
}
