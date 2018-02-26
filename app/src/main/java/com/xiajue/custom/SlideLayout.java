package com.xiajue.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Scroller;

/**
 * xiaJue 2018/1/27创建
 */
public class SlideLayout extends ViewGroup {
    private static final int OUT_TYPE_ALL = 329;//左右都可以越界
    private static final int OUT_TYPE_LEFT = 688;//左边可以越界
    private static final int OUT_TYPE_CANT = 679;//都不能越界
    private int mOutType;//允许越界的模式
    /// 判定为拖动的最小移动像素数
    private int mTouchSlop;
    //滚动类
    private Scroller mScroller;
    //手势检测
    private final GestureDetector mDetector;
    //是否正在快速滑动
    boolean isFull;
    //坐标位置
    private MPoint mLastPoint;
    private MPoint mNewPoint;

    public SlideLayout(Context context) {
        this(context, null);
    }

    public SlideLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getValue(context, attrs, defStyleAttr);
        //创建手势
        mDetector = new GestureDetector(context, mListener);
        setClickable(true);
        mLastPoint = new MPoint();
        mNewPoint = new MPoint();
        // 创建Scroller的实例
        mScroller = new Scroller(context, getAnimationInterpolator());
        ViewConfiguration configuration = ViewConfiguration.get(context);
        // 获取TouchSlop值
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        //mTouchSlop = 5;//这会在手机用AIDE编译。找不到ViewConfigurationCompat
    }

    private void getValue(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable
                .SlideLayout, 0, defStyleAttr);
        mOutType = typedArray.getInt(R.styleable.SlideLayout_outType, OUT_TYPE_ALL);
    }

    protected Interpolator getAnimationInterpolator() {
        return new DecelerateInterpolator();
    }

    public void setAnimationInterpolator(Interpolator interpolator) {
        mScroller = new Scroller(getContext(), interpolator);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //测量所有子View
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width, height;
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (getChildCount() == 0) {
            width = 0;
        } else {
            View childAt = getChildAt(0);
            MarginLayoutParams layoutParams = (MarginLayoutParams) childAt.getLayoutParams();
            width = childAt.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin;
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (getChildCount() == 0) {
            height = 0;
        } else {
            View childAt = getChildAt(0);
            MarginLayoutParams layoutParams = (MarginLayoutParams) childAt.getLayoutParams();
            height = getChildAt(0).getMeasuredHeight() + layoutParams.topMargin + layoutParams
                    .bottomMargin;
        }
        setMeasuredDimension(width, height);
    }

    private int rightWidth;//右侧菜单的总长度

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            //布局
            int left_location = 0;
            int childCount = getChildCount();
            Log.e("xj", "count=" + childCount);
            for (int i = 0; i < childCount; i++) {
                View childAt = getChildAt(i);
                if (childAt.getVisibility() != GONE) {
                    MarginLayoutParams layoutParams = (MarginLayoutParams) childAt
                            .getLayoutParams();
                    if (i <= 0) {
                        //定位主要显示的view
                        childAt.layout(left_location + layoutParams.leftMargin, layoutParams
                                        .topMargin,
                                left_location + childAt.getMeasuredWidth() - layoutParams
                                        .rightMargin, childAt.getMeasuredHeight() - layoutParams
                                        .bottomMargin);
                        left_location += childAt.getMeasuredWidth();
                        Log.e("xj", "0  childWidth=" + childAt.getMeasuredWidth());
                    } else {
                        //侧滑菜单在右侧
                        childAt.layout(left_location, getPaddingTop(), left_location + childAt
                                .getMeasuredWidth(), getPaddingTop() + childAt.getMeasuredHeight());
                        left_location += childAt.getMeasuredWidth();
                        rightWidth += childAt.getMeasuredWidth();
                        Log.e("xj", "childWidth=" + childAt.getMeasuredWidth());
                    }
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //将触摸事件交给detector
        mDetector.onTouchEvent(event);
        //处理弹起事件
        if (event.getAction() == MotionEvent.ACTION_UP) {
            recovery();
        }
        return super.onTouchEvent(event);
    }

    private void recovery() {
        if (!isFull) {
            //如果不是快速滑动,将scroll移动回去
            if (getScrollX() > rightWidth / 2) {
                mScroller.startScroll(getScrollX(), 0, -getScrollX() + rightWidth, 0);
            } else {
                mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0);
            }
            invalidate();
            requestDisallowInterceptTouchEvent(false);
        }
    }

    private GestureDetector.OnGestureListener mListener;

    {
        /**
         * 手势处理
         */
        mListener = new GestureDetector
                .SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                //保存最初的位置
                mLastPoint.setPoint(e);
                return super.onDown(e);
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float
                    distanceY) {
                isFull = false;
                Log.e("xj", "scroll " + getScrollX());
                //实时更新位置
                mNewPoint.setPoint(e2);
                if (Math.abs(mLastPoint.x - mNewPoint.x) > Math.abs(mLastPoint.y - mNewPoint.y) &&
                        Math.abs(mLastPoint.x - mNewPoint.x) > mTouchSlop) {
                    //判断是否横向滑动，一定要用最初的位置和当前的位置来判断。
                    int nextSX = (int) (getScrollX() + distanceX);
                    if (mOutType == OUT_TYPE_CANT && (nextSX >= rightWidth || nextSX <= 0)) {
                        //不能越界
                        return true;
                    }
                    if (mOutType == OUT_TYPE_LEFT && nextSX <= 0) {
                        //右边不能越界
                        return true;
                    }
                    scrollBy((int) distanceX, 0);
                    //请求父类不要处理事件
                    requestDisallowInterceptTouchEvent(true);
                }
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float
                    velocityY) {
                Log.e("xj", "fling");
                mLastPoint.setPoint(e1);
                mNewPoint.setPoint(e2);
                if (Math.abs(mLastPoint.x - mNewPoint.x) > Math.abs(mLastPoint.y - mNewPoint.y) &&
                        Math.abs(mLastPoint.x - mNewPoint.x) > mTouchSlop) {
                    //横屏操作
                    isFull = true;
                    if (mLastPoint.x > mNewPoint.x) {
                        //左边--打开
                        Log.e("xj", "fling left...");
                        open();
                    } else {
                        //右边--还原
                        Log.e("xj", "fling right");
                        restore();
                    }
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        };
    }

    private void open() {
        mScroller.startScroll(getScrollX(), 0, -getScrollX() + rightWidth, 0);
        invalidate();
    }

    private void restore() {
        mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0);
        invalidate();
    }


    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        //如果要支持子控件margin一定要重写这个方法
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    public void computeScroll() {
        // 滚动
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        }
    }

    /**
     * 简单的存放xy坐标的类
     */
    class MPoint {
        public int x;
        public int y;

        public void setPoint(MotionEvent event) {
            x = (int) event.getX();
            y = (int) event.getY();
        }
    }
}