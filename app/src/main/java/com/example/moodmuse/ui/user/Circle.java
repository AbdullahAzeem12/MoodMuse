package com.example.moodmuse.ui.user;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.Random;

public class Circle {
    private View view;
    private float x, y, radius, velocityX, velocityY;
    private int color;
    private int containerWidth, containerHeight;

    public Circle(Context context, int containerWidth, int containerHeight) {
        this.containerWidth = containerWidth;
        this.containerHeight = containerHeight;
        this.radius = 20 + new Random().nextInt(30);
        this.x = radius + new Random().nextInt((int) (containerWidth - 2 * radius));
        this.y = radius + new Random().nextInt((int) (containerHeight - 2 * radius));
        this.velocityX = 2 + new Random().nextInt(5) * (new Random().nextBoolean() ? 1 : -1);
        this.velocityY = 2 + new Random().nextInt(5) * (new Random().nextBoolean() ? 1 : -1);
        this.color = Color.argb(100, new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256));

        this.view = new CircleView(context);
        updateViewPosition();
    }

    public void move() {
        x += velocityX;
        y += velocityY;

        if (x - radius < 0 || x + radius > containerWidth) {
            velocityX = -velocityX;
            x = Math.max(radius, Math.min(x, containerWidth - radius));
        }

        if (y - radius < 0 || y + radius > containerHeight) {
            velocityY = -velocityY;
            y = Math.max(radius, Math.min(y, containerHeight - radius));
        }

        updateViewPosition();
    }

    public void handleCollision(Circle other) {
        float dx = other.x - this.x;
        float dy = other.y - this.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance < this.radius + other.radius) {
            // Repulsion logic
            float angle = (float) Math.atan2(dy, dx);
            float thisSpeed = (float) Math.sqrt(this.velocityX * this.velocityX + this.velocityY * this.velocityY);
            float otherSpeed = (float) Math.sqrt(other.velocityX * other.velocityX + other.velocityY * other.velocityY);

            this.velocityX = -thisSpeed * (float) Math.cos(angle);
            this.velocityY = -thisSpeed * (float) Math.sin(angle);
            other.velocityX = otherSpeed * (float) Math.cos(angle);
            other.velocityY = otherSpeed * (float) Math.sin(angle);
        }
    }

    private void updateViewPosition() {
        view.setX(x - radius);
        view.setY(y - radius);
    }

    public View getView() {
        return view;
    }

    private class CircleView extends View {
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public CircleView(Context context) {
            super(context);
            paint.setColor(color);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawCircle(radius, radius, radius, paint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension((int) (2 * radius), (int) (2 * radius));
        }
    }
}
