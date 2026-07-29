package com.tents.maintenance.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Touch-drawing pad for technician/customer signatures (replaces the HTML5 canvas signature pad). */
public class SignatureView extends View {

    private final Path path = new Path();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private boolean hasContent = false;
    private float lastX, lastY;

    public SignatureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.parseColor("#111827"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(5f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            Bitmap old = bitmap;
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmapCanvas = new Canvas(bitmap);
            if (old != null) {
                bitmapCanvas.drawBitmap(old, 0, 0, null);
                old.recycle();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                path.moveTo(x, y);
                lastX = x; lastY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
                lastX = x; lastY = y;
                hasContent = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                path.lineTo(x, y);
                if (bitmapCanvas != null) bitmapCanvas.drawPath(path, paint);
                path.reset();
                getParent().requestDisallowInterceptTouchEvent(false);
                invalidate();
                return true;
        }
        return false;
    }

    public boolean hasContent() {
        return hasContent;
    }

    public void clear() {
        hasContent = false;
        path.reset();
        if (bitmapCanvas != null && bitmap != null) {
            bitmap.eraseColor(Color.TRANSPARENT);
        }
        invalidate();
    }

    /** Returns a flattened white-background bitmap suitable for saving as JPEG/PNG. */
    public Bitmap exportBitmap() {
        if (bitmap == null) return null;
        Bitmap out = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.drawColor(Color.WHITE);
        c.drawBitmap(bitmap, 0, 0, null);
        return out;
    }

    public void loadBitmap(Bitmap src) {
        post(() -> {
            if (getWidth() <= 0 || getHeight() <= 0 || src == null) return;
            if (bitmapCanvas == null) return;
            android.graphics.Rect dst = new android.graphics.Rect(0, 0, getWidth(), getHeight());
            bitmapCanvas.drawBitmap(src, null, dst, null);
            hasContent = true;
            invalidate();
        });
    }
}
