package com.example.sunshow_02.videoplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * Created by sunshow-02 on 2017/5/22.
 */

public class CustomVideoView extends VideoView {

    int default_width = getResources().getDisplayMetrics().widthPixels;
    int default_height = getResources().getDisplayMetrics().heightPixels;
    public CustomVideoView(Context context) {
        super(context);
    }

    public CustomVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getDefaultSize(default_width,widthMeasureSpec);
        int height = getDefaultSize(default_height,heightMeasureSpec);
        setMeasuredDimension(width,height);
    }
}
