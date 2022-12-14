package org.topnetwork.pintogether.utils.zxingV2;

/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.zxing.ResultPoint;

import org.topnetwork.pintogether.R;
import org.topnetwork.pintogether.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 20;

    /**
     * ??????
     */
    private Paint paint;

    /**
     * ????????????
     */
    private TextPaint textPaint;
    /**
     * ???????????????????????????
     */
    private int maskColor;
    /**
     * ????????????????????????
     */
    private int frameColor;
    /**
     * ???????????????
     */
    private int laserColor;
    /**
     * ?????????????????????
     */
    private int cornerColor;
    /**
     * ???????????????
     */
    private int resultPointColor;

    /**
     * ?????????????????????????????????
     */
    private float labelTextPadding;
    /**
     * ?????????????????????
     */
    private TextLocation labelTextLocation;
    /**
     * ????????????????????????
     */
    private String labelText;
    /**
     * ??????????????????????????????
     */
    private int labelTextColor;
    /**
     * ????????????????????????
     */
    private float labelTextSize;

    /**
     * ?????????????????????
     */
    public int scannerStart = 0;
    /**
     * ?????????????????????
     */
    public int scannerEnd = 0;
    /**
     * ?????????????????????
     */
    private boolean isShowResultPoint;

    /**
     * ?????????
     */
    private int screenWidth;
    /**
     * ?????????
     */
    private int screenHeight;
    /**
     * ????????????
     */
    private int frameWidth;
    /**
     * ????????????
     */
    private int frameHeight;
    /**
     * ?????????????????????
     */
    private LaserStyle laserStyle;

    /**
     * ????????????
     */
    private int gridColumn;
    /**
     * ????????????
     */
    private int gridHeight;

    /**
     * ?????????
     */
    private Rect frame;

    /**
     * ?????????????????????
     */
    private int cornerRectWidth;
    /**
     * ?????????????????????
     */
    private int cornerRectHeight;
    /**
     * ???????????????????????????
     */
    private int scannerLineMoveDistance;
    /**
     * ???????????????
     */
    private int scannerLineHeight;

    /**
     * ???????????????
     */
    private int frameLineWidth;

    /**
     * ?????????????????????????????? ??????15??????
     */
    private int scannerAnimationDelay;

    /**
     * ???????????????
     */
    private float frameRatio;


    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;

    public enum LaserStyle{
        NONE(0),LINE(1),GRID(2);
        private int mValue;
        LaserStyle(int value){
            mValue = value;
        }

        private static LaserStyle getFromInt(int value){

            for(LaserStyle style : LaserStyle.values()){
                if(style.mValue == value){
                    return style;
                }
            }

            return LaserStyle.LINE;
        }
    }

    public enum TextLocation {
        TOP(0),BOTTOM(1);

        private int mValue;

        TextLocation(int value){
            mValue = value;
        }

        private static TextLocation getFromInt(int value){

            for(TextLocation location : TextLocation.values()){
                if(location.mValue == value){
                    return location;
                }
            }

            return TextLocation.TOP;
        }


    }

    public ViewfinderView(Context context) {
        this(context,null);
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }


    private void init(Context context, AttributeSet attrs) {
        //??????????????????????????????
        @SuppressLint("CustomViewStyleable") TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderViewV2);
        maskColor = array.getColor(R.styleable.ViewfinderViewV2_maskColor, ContextCompat.getColor(context,R.color.viewfinder_mask));
        frameColor = array.getColor(R.styleable.ViewfinderViewV2_frameColor, ContextCompat.getColor(context,R.color.viewfinder_frame));
        cornerColor = array.getColor(R.styleable.ViewfinderViewV2_cornerColor, ContextCompat.getColor(context,R.color.viewfinder_corner));
        laserColor = array.getColor(R.styleable.ViewfinderViewV2_laserColor, ContextCompat.getColor(context,R.color.viewfinder_laser));
        resultPointColor = array.getColor(R.styleable.ViewfinderViewV2_resultPointColor, ContextCompat.getColor(context,R.color.viewfinder_result_point_color));

        labelText = array.getString(R.styleable.ViewfinderViewV2_labelText);
        labelTextColor = array.getColor(R.styleable.ViewfinderViewV2_labelTextColor, ContextCompat.getColor(context,R.color.viewfinder_text_color));
        labelTextSize = array.getDimension(R.styleable.ViewfinderViewV2_labelTextSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,14f,getResources().getDisplayMetrics()));
        labelTextPadding = array.getDimension(R.styleable.ViewfinderViewV2_labelTextPadding,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,24,getResources().getDisplayMetrics()));
        labelTextLocation = TextLocation.getFromInt(array.getInt(R.styleable.ViewfinderViewV2_labelTextLocation,0));

        isShowResultPoint = array.getBoolean(R.styleable.ViewfinderViewV2_showResultPoint,false);

        frameWidth = array.getDimensionPixelSize(R.styleable.ViewfinderViewV2_frameWidth,0);
        frameHeight = array.getDimensionPixelSize(R.styleable.ViewfinderViewV2_frameHeight,0);

        laserStyle = LaserStyle.getFromInt(array.getInt(R.styleable.ViewfinderViewV2_laserStyle, LaserStyle.LINE.mValue));
        gridColumn = array.getInt(R.styleable.ViewfinderViewV2_gridColumn,20);
        gridHeight = (int)array.getDimension(R.styleable.ViewfinderViewV2_gridHeight,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,40,getResources().getDisplayMetrics()));

        cornerRectWidth = (int)array.getDimension(R.styleable.ViewfinderViewV2_cornerRectWidth,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,4,getResources().getDisplayMetrics()));
        cornerRectHeight = (int)array.getDimension(R.styleable.ViewfinderViewV2_cornerRectHeight,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,16,getResources().getDisplayMetrics()));
        scannerLineMoveDistance = (int)array.getDimension(R.styleable.ViewfinderViewV2_scannerLineMoveDistance,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,2,getResources().getDisplayMetrics()));
        scannerLineHeight = (int)array.getDimension(R.styleable.ViewfinderViewV2_scannerLineHeight,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,5,getResources().getDisplayMetrics()));
        frameLineWidth = (int)array.getDimension(R.styleable.ViewfinderViewV2_frameLineWidth,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,0.6f,getResources().getDisplayMetrics()));
        scannerAnimationDelay = array.getInteger(R.styleable.ViewfinderViewV2_scannerAnimationDelay,15);
        frameRatio = array.getFloat(R.styleable.ViewfinderViewV2_frameRatio,0.625f);
        array.recycle();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;

        screenWidth = getDisplayMetrics().widthPixels;
        screenHeight = getDisplayMetrics().heightPixels;

        int size = (int)(Math.min(screenWidth,screenHeight) * frameRatio);

        if(frameWidth<=0 || frameWidth > screenWidth){
            frameWidth = size;
        }

        if(frameHeight<=0 || frameHeight > screenHeight){
            frameHeight = size;
        }

    }

    private DisplayMetrics getDisplayMetrics(){
        return getResources().getDisplayMetrics();
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    public void setLabelTextColor(@ColorInt int color) {
        this.labelTextColor = color;
    }

    public void setLabelTextColorResource(@ColorRes int id){
        this.labelTextColor = ContextCompat.getColor(getContext(),id);
    }

    public void setLabelTextSize(float textSize) {
        this.labelTextSize = textSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //?????????????????????????????????????????????????????????
        int leftOffset = (screenWidth - frameWidth) / 2 + getPaddingLeft() - getPaddingRight();
        int topOffset = (screenHeight - frameHeight) / 2 + getPaddingTop() - getPaddingBottom();
        frame = new Rect(leftOffset, topOffset, leftOffset + frameWidth, topOffset + frameHeight);
    }

    @Override
    public void onDraw(Canvas canvas) {

        if (frame == null) {
            return;
        }

        if(scannerStart == 0 || scannerEnd == 0) {
            scannerStart = frame.top;
            scannerEnd = frame.bottom - scannerLineHeight;
        }

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        drawExterior(canvas,frame,width,height);
        // Draw a red "laser scanner" line through the middle to show decoding is active
        drawLaserScanner(canvas,frame);
        // Draw a two pixel solid black border inside the framing rect
        drawFrame(canvas, frame);
        // ????????????
        drawCorner(canvas, frame);
        //??????????????????
        drawTextInfo(canvas, frame);
        //?????????????????????
        drawResultPoint(canvas,frame);
        // Request another update at the animation interval, but only repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(scannerAnimationDelay,
                frame.left - POINT_SIZE,
                frame.top - POINT_SIZE,
                frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE);
    }

    /**
     * ????????????
     * @param canvas
     * @param frame
     */
    private void drawTextInfo(Canvas canvas, Rect frame) {
        if(!TextUtils.isEmpty(labelText)){
            textPaint.setColor(labelTextColor);
            textPaint.setTextSize(labelTextSize);
            textPaint.setTextAlign(Paint.Align.CENTER);
            StaticLayout staticLayout = new StaticLayout(labelText,textPaint,canvas.getWidth(), Layout.Alignment.ALIGN_NORMAL,1.0f,0.0f,true);
            if(labelTextLocation == TextLocation.BOTTOM){
                canvas.translate(frame.left + frame.width() / 2,frame.bottom + labelTextPadding);
                staticLayout.draw(canvas);
            }else{
                canvas.translate(frame.left + frame.width() / 2,frame.top - labelTextPadding - staticLayout.getHeight());
                staticLayout.draw(canvas);
            }
        }

    }

    /**
     * ????????????
     * @param canvas
     * @param frame
     */
    private void drawCorner(Canvas canvas, Rect frame) {
        paint.setColor(cornerColor);
        //??????
        canvas.drawRect(frame.left, frame.top, frame.left + cornerRectWidth, frame.top + cornerRectHeight, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + cornerRectHeight, frame.top + cornerRectWidth, paint);
        //??????
        canvas.drawRect(frame.right - cornerRectWidth, frame.top, frame.right, frame.top + cornerRectHeight, paint);
        canvas.drawRect(frame.right - cornerRectHeight, frame.top, frame.right, frame.top + cornerRectWidth, paint);
        //??????
        canvas.drawRect(frame.left, frame.bottom - cornerRectWidth, frame.left + cornerRectHeight, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - cornerRectHeight, frame.left + cornerRectWidth, frame.bottom, paint);
        //??????
        canvas.drawRect(frame.right - cornerRectWidth, frame.bottom - cornerRectHeight, frame.right, frame.bottom, paint);
        canvas.drawRect(frame.right - cornerRectHeight, frame.bottom - cornerRectWidth, frame.right, frame.bottom, paint);
    }

    /**
     * ?????????????????????
     * @param canvas
     * @param frame
     */
    private void drawLaserScanner(Canvas canvas, Rect frame) {
        if(laserStyle!=null){
            paint.setColor(laserColor);
            switch (laserStyle){
                case LINE://???
                    drawLineScanner(canvas,frame);
                    break;
                case GRID://??????
                    drawGridScanner(canvas,frame);
                    break;
            }
            paint.setShader(null);
        }
    }

    /**
     * ?????????????????????
     * @param canvas
     * @param frame
     */
    private void drawLineScanner(Canvas canvas,Rect frame){
        //????????????
        LinearGradient linearGradient = new LinearGradient(
                frame.left, scannerStart,
                frame.left, scannerStart + scannerLineHeight,
                shadeColor(laserColor),
                laserColor,
                Shader.TileMode.MIRROR);

        paint.setShader(linearGradient);
        if(scannerStart <= scannerEnd) {
            //??????
            RectF rectF = new RectF(frame.left + 2 * scannerLineHeight, scannerStart, frame.right - 2 * scannerLineHeight, scannerStart + scannerLineHeight);
            canvas.drawOval(rectF, paint);
            scannerStart += scannerLineMoveDistance;
        } else {
            scannerStart = frame.top;
        }
    }

    /**
     * ?????????????????????
     * @param canvas
     * @param frame
     */
    private void drawGridScanner(Canvas canvas,Rect frame){
        int stroke = 2;
        paint.setStrokeWidth(stroke);
        //??????Y???????????????
        int startY = gridHeight > 0 && scannerStart - frame.top > gridHeight ? scannerStart - gridHeight : frame.top;

        LinearGradient linearGradient = new LinearGradient(frame.left + frame.width()/2, startY, frame.left + frame.width()/2, scannerStart, new int[]{shadeColor(laserColor), laserColor}, new float[]{0,1f}, LinearGradient.TileMode.CLAMP);
        //????????????????????????
        paint.setShader(linearGradient);

        float wUnit = frame.width() * 1.0f/ gridColumn;
        float hUnit = wUnit;
        //????????????????????????
        for (int i = 1; i < gridColumn; i++) {
            canvas.drawLine(frame.left + i * wUnit, startY,frame.left + i * wUnit, scannerStart,paint);
        }

        int height = gridHeight > 0 && scannerStart - frame.top > gridHeight ? gridHeight : scannerStart - frame.top;

        //????????????????????????
        for (int i = 0; i <= height/hUnit; i++) {
            canvas.drawLine(frame.left, scannerStart - i * hUnit,frame.right, scannerStart - i * hUnit,paint);
        }

        if(scannerStart<scannerEnd){
            scannerStart += scannerLineMoveDistance;
        } else {
            scannerStart = frame.top;
        }

    }

    /**
     * ??????????????????
     * @param color
     * @return
     */
    public int shadeColor(int color) {
        String hax = Integer.toHexString(color);
        String result = "01"+hax.substring(2);
        return Integer.valueOf(result, 16);
    }

    /**
     * ?????????????????????
     * @param canvas
     * @param frame
     */
    private void drawFrame(Canvas canvas, Rect frame) {
        paint.setColor(frameColor);
        canvas.drawRect(frame.left, frame.top, frame.right, frame.top + frameLineWidth, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + frameLineWidth, frame.bottom, paint);
        canvas.drawRect(frame.right - frameLineWidth, frame.top, frame.right, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - frameLineWidth, frame.right, frame.bottom, paint);
    }

    /**
     * ??????????????????
     * @param canvas
     * @param frame
     * @param width
     * @param height
     */
    private void drawExterior(Canvas canvas, Rect frame, int width, int height) {
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom, paint);
        canvas.drawRect(frame.right, frame.top, width, frame.bottom, paint);
        canvas.drawRect(0, frame.bottom, width, height, paint);
    }

    /**
     * ?????????????????????
     * @param canvas
     * @param frame
     */
    private void drawResultPoint(Canvas canvas,Rect frame){

        if(!isShowResultPoint){
            return;
        }

        List<ResultPoint> currentPossible = possibleResultPoints;
        List<ResultPoint> currentLast = lastPossibleResultPoints;

        if (currentPossible.isEmpty()) {
            lastPossibleResultPoints = null;
        } else {
            possibleResultPoints = new ArrayList<>(5);
            lastPossibleResultPoints = currentPossible;
            paint.setAlpha(CURRENT_POINT_OPACITY);
            paint.setColor(resultPointColor);
            synchronized (currentPossible) {
                float radius = POINT_SIZE / 2.0f;
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle( point.getX(),point.getY(), radius, paint);
                }
            }
        }
        if (currentLast != null) {
            paint.setAlpha(CURRENT_POINT_OPACITY / 2);
            paint.setColor(resultPointColor);
            synchronized (currentLast) {
                float radius = POINT_SIZE / 2.0f;
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle( point.getX(),point.getY(), radius, paint);
                }
            }
        }
    }

    public void drawViewfinder() {
        LogUtils.dTag("ViewfinderView","drawViewfinder");
        invalidate();
    }

    public boolean isShowResultPoint() {
        return isShowResultPoint;
    }

    public void setLaserStyle(LaserStyle laserStyle) {
        this.laserStyle = laserStyle;
    }

    /**
     * ?????????????????????
     * @param showResultPoint ?????????????????????
     */
    public void setShowResultPoint(boolean showResultPoint) {
        isShowResultPoint = showResultPoint;
    }


    public void addPossibleResultPoint(ResultPoint point) {
        if(isShowResultPoint){
            List<ResultPoint> points = possibleResultPoints;
            synchronized (points) {
                points.add(point);
                int size = points.size();
                if (size > MAX_RESULT_POINTS) {
                    // trim it
                    points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
                }
            }
        }

    }



}