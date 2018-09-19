package com.zlm.libs.widget;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.zlm.libs.register.RegisterHelper;

/**
 * @Description: 右滑动关闭界面或者下拉关闭界面；这里使用ValueAnimator来实现，当然也可以使用ViewDragHelper来实现
 * @Param:
 * @Return:
 * @Author: zhangliangming
 * @Date: 2018/02/09
 * @Throws:
 */
public class SwipeBackLayout extends LinearLayout {
    /**
     * 全部
     */
    public static final int ALL = -1;

    /**
     * 左到右
     */
    public static final int LEFT_TO_RIGHT = 0;

    /**
     * 上到下
     */
    public static final int TOP_TO_BOTTOM = 1;
    /**
     * 全部_左到右
     */
    private final int ALL_AND_LEFT_TO_RIGHT = 2;
    /**
     * 全部_上到下
     */
    private final int ALL_AND_TOP_TO_BOTTOM = 3;
    /**
     * 状态打开
     */
    private final int OPEN = 0;
    /**
     * 状态关闭
     */
    private final int CLOSE = 1;
    /**
     * 状态移动
     */
    private final int MOVE = 2;
    /**
     * 界面状态
     */
    private int mDragStatus = CLOSE;

    /**
     * 拖动类型
     */
    private int mDragType = LEFT_TO_RIGHT;

    /**
     * 判断view是点击还是移动的距离
     */
    private int mTouchSlop;

    /**
     * 拦截的X轴和Y最后的位置
     */
    private float mLastInterceptX = 0, mLastInterceptY = 0;

    /**
     * 记录手势速度
     */
    private VelocityTracker mVelocityTracker;
    private int mMaximumVelocity;
    private int mMinimumVelocity;

    /**
     * 右滑动关闭界面或者下拉关闭界面view
     */
    private LinearLayout mSwipeBackLayout;

    /**
     * 记录当前内容view的x轴位置，方便设置contentView的位置
     */
    private int mSwipeBackLayoutCurX = 0, mSwipeBackLayoutCurY = 0;

    /**
     * 动画时间
     */
    private int mDuration = 250;
    /**
     * xy轴移动动画
     */
    private ValueAnimator mValueAnimator;

    /**
     * 事件回调
     */
    private SwipeBackLayoutListener mSwipeBackLayoutListener;

    /**
     * 阴影画笔
     */
    private Paint mFadePaint;
    /**
     * 最小的透明度
     */
    private int mMinAlpha = 200;

    /**
     * 是否绘画阴影
     */
    private boolean isPaintFade = true;
    /**
     *
     */
    private LayoutInflater mLayoutInflater;
    /**
     * LinearLayout布局
     */
    public static final int CONTENTVIEWTYPE_LINEARLAYOUT = 0;
    /**
     * RelativeLayout布局
     */
    public static final int CONTENTVIEWTYPE_RELATIVELAYOUT = 1;

    /**
     * 不拦截水平视图
     */
    private List<View> mIgnoreHorizontalViews;

    /**
     * 不拦截垂直视图
     */
    private List<View> mIgnoreVerticalViews;

    /**
     * 不处理视图，该集合中的view所在的区域，将不做任何操作
     */
    private List<View> mIgnoreViews;

    public SwipeBackLayout(Context context) {
        super(context);
        init(context);
    }

    public SwipeBackLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 初始化
     *
     * @param context
     */
    private void init(Context context) {

        RegisterHelper.verify();

        mLayoutInflater = LayoutInflater.from(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        // 获取屏幕的高度和宽度
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int screensWidth = display.getWidth();
        int screensHeight = display.getHeight();
        mSwipeBackLayoutCurX = screensWidth;
        mSwipeBackLayoutCurY = screensHeight;
        //
        setOrientation(LinearLayout.VERTICAL);
        setBackgroundColor(Color.TRANSPARENT);

        // 初始化阴影画笔
        mFadePaint = new Paint();
        mFadePaint.setAntiAlias(true);

        // 初始化手势速度监听
        final ViewConfiguration configuration = ViewConfiguration
                .get(getContext());
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        // 加载完成后回调
        getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);

                        open();

                    }
                });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 0 && getChildCount() < 2) {
            mSwipeBackLayout = (LinearLayout) getChildAt(0);
        } else {
            mSwipeBackLayout = new LinearLayout(getContext());
            mSwipeBackLayout.setOrientation(LinearLayout.VERTICAL);
            mSwipeBackLayout.setBackgroundColor(Color.WHITE);
            removeAllViews();
            addView(mSwipeBackLayout, LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mSwipeBackLayout != null) {
            invalidateSwipeBackLayout(mSwipeBackLayoutCurX,
                    mSwipeBackLayoutCurY);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mSwipeBackLayout == null)
            super.onInterceptTouchEvent(event);

        boolean intercepted = false;
        float curX = event.getX();
        float curY = event.getY();

        int actionId = event.getAction();
        switch (actionId) {

            case MotionEvent.ACTION_DOWN:
                mLastInterceptX = curX;
                mLastInterceptY = curY;
                break;

            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (mLastInterceptX - curX);
                int deltaY = (int) (mLastInterceptY - curY);

                if ((mDragType == LEFT_TO_RIGHT || mDragType == ALL)
                        && (Math.abs(deltaX) > mTouchSlop
                        && Math.abs(deltaY) < mTouchSlop
                        && !isInIgnoreHorizontalView(event) && !isInIgnoreView(event))) {
                    // 左右移动事件
                    if (mDragType == ALL) {
                        setDragType(ALL_AND_LEFT_TO_RIGHT);
                    }
                    intercepted = true;
                } else if ((mDragType == TOP_TO_BOTTOM || mDragType == ALL)
                        && (Math.abs(deltaX) < mTouchSlop
                        && Math.abs(deltaY) > mTouchSlop
                        && !isInIgnoreVerticalView(event) && !isInIgnoreView(event))) {
                    // 上下移动事件
                    if (mDragType == ALL) {
                        setDragType(ALL_AND_TOP_TO_BOTTOM);
                    }
                    intercepted = true;
                }
                break;

            default:
                break;
        }

        return intercepted;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mSwipeBackLayout == null)
            super.onTouchEvent(event);

        obtainVelocityTracker(event);

        float curX = event.getX();
        float curY = event.getY();

        int actionId = event.getAction();
        switch (actionId) {

            case MotionEvent.ACTION_DOWN:
                mLastInterceptX = curX;
                mLastInterceptY = curY;
                break;

            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (mLastInterceptX - curX);
                int deltaY = (int) (mLastInterceptY - curY);

                if ((mDragType == LEFT_TO_RIGHT
                        || mDragType == ALL_AND_LEFT_TO_RIGHT || mDragType == ALL)
                        && (mDragStatus == MOVE || (Math.abs(deltaX) > mTouchSlop
                        && Math.abs(deltaY) < mTouchSlop
                        && !isInIgnoreHorizontalView(event) && !isInIgnoreView(event)))) {
                    if (mDragType == ALL) {
                        setDragType(ALL_AND_LEFT_TO_RIGHT);
                    }
                    // 左右移动事件
                    if ((mSwipeBackLayoutCurX - deltaX >= 0)
                            && (mDragType == LEFT_TO_RIGHT || mDragType == ALL_AND_LEFT_TO_RIGHT)) {
                        // 防止用户左滑动时，超出屏幕大小
                        invalidateSwipeBackLayout(mSwipeBackLayoutCurX - deltaX,
                                mSwipeBackLayoutCurY);
                    }
                    setDragStatus(MOVE);
                } else if ((mDragType == TOP_TO_BOTTOM
                        || mDragType == ALL_AND_TOP_TO_BOTTOM || mDragType == ALL)
                        && (mDragStatus == MOVE || (Math.abs(deltaX) < mTouchSlop
                        && Math.abs(deltaY) > mTouchSlop
                        && !isInIgnoreVerticalView(event) && !isInIgnoreView(event)))) {
                    if (mDragType == ALL) {
                        setDragType(ALL_AND_TOP_TO_BOTTOM);
                    }
                    // 上下移动事件
                    if ((mSwipeBackLayoutCurY - deltaY >= 0)
                            && (mDragType == TOP_TO_BOTTOM || mDragType == ALL_AND_TOP_TO_BOTTOM)) {
                        // 防止用户上滑动时，超出屏幕大小
                        invalidateSwipeBackLayout(mSwipeBackLayoutCurX,
                                mSwipeBackLayoutCurY - deltaY);
                    }

                    setDragStatus(MOVE);

                }
                break;

            default:

                //更新xy的位置值
                mSwipeBackLayoutCurX = mSwipeBackLayout.getLeft();
                mSwipeBackLayoutCurY = mSwipeBackLayout.getTop();

                // 处理up，cancel事件
                // 处理手势速度监听事件
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int xVelocity = (int) velocityTracker.getXVelocity();
                int yVelocity = (int) velocityTracker.getYVelocity();

                int oldDragStatus = mDragStatus;
                setDragStatus(OPEN);
                if (mDragType == LEFT_TO_RIGHT
                        || mDragType == ALL_AND_LEFT_TO_RIGHT) {

                    if (((Math.abs(xVelocity) > mMinimumVelocity && xVelocity > 0) && oldDragStatus == MOVE)
                            || mSwipeBackLayout.getLeft() > getWidth() / 2) {

                        // 在右半边或者达到一定的速度
                        setDragStatus(CLOSE);
                        translationXYAnimator(mSwipeBackLayout.getLeft(),
                                getWidth(), mDuration);

                    } else if (mSwipeBackLayout.getLeft() < getWidth() / 2) {
                        // 在左半边
                        translationXYAnimator(mSwipeBackLayout.getLeft(), 0,
                                mDuration);
                    }

                } else {

                    if (((Math.abs(yVelocity) > mMinimumVelocity && yVelocity > 0) && oldDragStatus == MOVE)
                            || mSwipeBackLayout.getTop() > getHeight() / 2) {

                        // 在下半边或者达到一定的速度
                        setDragStatus(CLOSE);
                        translationXYAnimator(mSwipeBackLayout.getTop(),
                                getHeight(), mDuration);

                    } else if (mSwipeBackLayout.getTop() < getHeight() / 2) {
                        // 在上半边
                        translationXYAnimator(mSwipeBackLayout.getTop(), 0,
                                mDuration);
                    }
                }
                //
                releaseVelocityTracker();
                break;
        }

        return true;
    }

    /**
     * 是否在水平不处理视图中
     *
     * @param event
     * @return
     */
    private boolean isInIgnoreHorizontalView(MotionEvent event) {
        return isInView(mIgnoreHorizontalViews, event);
    }

    /**
     * 是否在垂直不处理视图中
     *
     * @param event
     * @return
     */
    private boolean isInIgnoreVerticalView(MotionEvent event) {
        return isInView(mIgnoreVerticalViews, event);
    }

    /**
     * 是否在不处理视图中
     *
     * @param event
     * @return
     */
    private boolean isInIgnoreView(MotionEvent event) {
        return isInView(mIgnoreViews, event);
    }

    /**
     * 是否在view里面
     *
     * @param views
     * @param event
     * @return
     */
    private boolean isInView(List<View> views, MotionEvent event) {
        if (views == null || views.size() == 0)
            return false;
        for (int i = 0; i < views.size(); i++) {
            View view = views.get(i);
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            int left = location[0];
            int top = location[1];
            int right = left + view.getWidth();
            int bottom = top + view.getHeight();
            Rect rect = new Rect(left, top, right, bottom);
            if (rect.contains((int) event.getRawX(), (int) event.getRawY())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mSwipeBackLayout != null
                && isPaintFade
                && (mSwipeBackLayout.getLeft() > 0 || mSwipeBackLayout.getTop() > 0)) {
            if (mDragType == LEFT_TO_RIGHT
                    || mDragType == ALL_AND_LEFT_TO_RIGHT) {

                float percent = mSwipeBackLayout.getLeft() * 1.0f / getWidth();
                int alpha = mMinAlpha - (int) (mMinAlpha * percent);
                mFadePaint.setColor(Color.argb(Math.max(alpha, 0), 0, 0, 0));
                canvas.drawRect(0, 0, mSwipeBackLayout.getLeft(),
                        mSwipeBackLayout.getHeight(), mFadePaint);

            } else {
                float percent = mSwipeBackLayout.getTop() * 1.0f / getHeight();
                int alpha = mMinAlpha - (int) (mMinAlpha * percent);
                mFadePaint.setColor(Color.argb(Math.max(alpha, 0), 0, 0, 0));
                canvas.drawRect(0, 0, mSwipeBackLayout.getWidth(),
                        mSwipeBackLayout.getTop(), mFadePaint);
            }

        }
    }

    /**
     * @param event
     */
    private void obtainVelocityTracker(MotionEvent event) {

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        mVelocityTracker.addMovement(event);

    }

    /**
     * 释放
     */
    private void releaseVelocityTracker() {

        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;

        }

    }

    /**
     * 打开界面
     */
    public void open() {
        if (mDragStatus == OPEN) {
            return;
        }
        setDragStatus(OPEN);
        if (mSwipeBackLayout != null) {
            // 执行打开动画
            if (mDragType == LEFT_TO_RIGHT
                    || mDragType == ALL_AND_LEFT_TO_RIGHT) {
                mSwipeBackLayoutCurY = 0;
                translationXYAnimator(mSwipeBackLayout.getLeft(), 0, mDuration);

            } else {
                mSwipeBackLayoutCurX = 0;
                translationXYAnimator(mSwipeBackLayout.getTop(), 0, mDuration);

            }
        }
    }

    /**
     * 关闭界面
     */
    public void closeView() {
        if (mDragStatus == CLOSE) {
            return;
        }
        setDragStatus(CLOSE);
        if (mSwipeBackLayout != null) {
            // 执行关闭动画
            if (mDragType == LEFT_TO_RIGHT
                    || mDragType == ALL_AND_LEFT_TO_RIGHT) {

                translationXYAnimator(mSwipeBackLayout.getLeft(), getWidth(),
                        mDuration);

            } else {

                translationXYAnimator(mSwipeBackLayout.getTop(), getHeight(),
                        mDuration);

            }
        }
    }

    /**
     * x轴移动动画
     *
     * @param from
     * @param to
     * @param duration 动画时间
     */
    private void translationXYAnimator(int from, int to, int duration) {
        if (mValueAnimator != null && mValueAnimator.isRunning()) {
            mValueAnimator.cancel();
            mValueAnimator = null;
        }
        mValueAnimator = ValueAnimator.ofInt(from, to);
        mValueAnimator.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Number number = (Number) animation.getAnimatedValue();

                if (mDragType == LEFT_TO_RIGHT
                        || mDragType == ALL_AND_LEFT_TO_RIGHT) {
                    mSwipeBackLayoutCurX = number.intValue();
                } else {
                    mSwipeBackLayoutCurY = number.intValue();
                }
                //
                invalidateSwipeBackLayout(mSwipeBackLayoutCurX,
                        mSwipeBackLayoutCurY);

            }
        });
        mValueAnimator.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {

                // 还原拖动类型
                if (mDragType == ALL_AND_TOP_TO_BOTTOM
                        || mDragType == ALL_AND_LEFT_TO_RIGHT) {
                    setDragType(ALL);
                }

                if (mSwipeBackLayoutListener != null && mDragStatus == CLOSE) {
                    mSwipeBackLayoutListener.finishActivity();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        mValueAnimator.setInterpolator(new LinearInterpolator());
        mValueAnimator.setDuration(duration);
        mValueAnimator.start();
    }

    /**
     * 刷新SwipeBackLayout界面的位置
     *
     * @param contentViewCurX
     * @param contentViewCurY
     */
    private void invalidateSwipeBackLayout(int contentViewCurX,
                                             int contentViewCurY) {
        if (mDragType == LEFT_TO_RIGHT || mDragType == ALL_AND_LEFT_TO_RIGHT) {
            mSwipeBackLayout
                    .layout(contentViewCurX, 0, contentViewCurX
                                    + mSwipeBackLayout.getWidth(),
                            mSwipeBackLayout.getHeight());
        } else {
            mSwipeBackLayout.layout(0, contentViewCurY,
                    mSwipeBackLayout.getWidth(), contentViewCurY
                            + mSwipeBackLayout.getHeight());
        }
        invalidate();
    }

    /**
     * 设置拖动的状态
     *
     * @param dragStatus
     */
    private void setDragStatus(int dragStatus) {
        this.mDragStatus = dragStatus;
    }

    // /////////////////////////////////////////////////

    /**
     * 添加不拦截垂直view
     *
     * @param ignoreView
     */
    public void addIgnoreVerticalView(View ignoreView) {
        if (mIgnoreVerticalViews == null) {
            mIgnoreVerticalViews = new ArrayList<View>();
        }
        if (!mIgnoreVerticalViews.contains(ignoreView)) {
            mIgnoreVerticalViews.add(ignoreView);
        }
    }

    /**
     * 添加不拦截水平view
     *
     * @param ignoreView
     */
    public void addIgnoreHorizontalView(View ignoreView) {
        if (mIgnoreHorizontalViews == null) {
            mIgnoreHorizontalViews = new ArrayList<View>();
        }
        if (!mIgnoreHorizontalViews.contains(ignoreView)) {
            mIgnoreHorizontalViews.add(ignoreView);
        }
    }

    /**
     * 添加不处理view
     *
     * @param ignoreView
     */
    public void addIgnoreView(View ignoreView) {
        if (mIgnoreViews == null) {
            mIgnoreViews = new ArrayList<View>();
        }
        if (!mIgnoreViews.contains(ignoreView)) {
            mIgnoreViews.add(ignoreView);
        }
    }

    public void setIgnoreViews(List<View> ignoreViews) {
        this.mIgnoreViews = ignoreViews;
    }

    public void setIgnoreHorizontalViews(List<View> ignoreHorizontalViews) {
        this.mIgnoreHorizontalViews = ignoreHorizontalViews;
    }

    public void setIgnoreVerticalViews(List<View> ignoreVerticalViews) {
        this.mIgnoreVerticalViews = ignoreVerticalViews;
    }

    /**
     * 添加内容view
     *
     * @param resourceId 布局文件id(默认是：CONTENTVIEWTYPE_LINEARLAYOUT)
     */
    public void setContentView(int resourceId) {
        View contentView = mLayoutInflater.inflate(resourceId, null);
        setContentView(contentView, CONTENTVIEWTYPE_LINEARLAYOUT);
    }

    /**
     * 添加内容view
     *
     * @param resourceId
     * @param contentViewType 内容view类型(CONTENTVIEWTYPE_LINEARLAYOUT /
     *                        CONTENTVIEWTYPE_RELATIVELAYOUT)
     */
    public void setContentView(int resourceId, int contentViewType) {
        View contentView = mLayoutInflater.inflate(resourceId, null);
        setContentView(contentView, contentViewType);
    }

    /**
     * 添加内容view
     *
     * @param contentView     内容view
     * @param contentViewType 内容view类型(CONTENTVIEWTYPE_LINEARLAYOUT /
     *                        CONTENTVIEWTYPE_RELATIVELAYOUT)
     */
    public void setContentView(View contentView, int contentViewType) {
        if (contentViewType == CONTENTVIEWTYPE_LINEARLAYOUT) {
            LayoutParams layoutParams = new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT);
            mSwipeBackLayout.addView(contentView, layoutParams);
        } else if (contentViewType == CONTENTVIEWTYPE_RELATIVELAYOUT) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            mSwipeBackLayout.addView(contentView, layoutParams);
        }
    }

    public void setDragType(int dragType) {
        this.mDragType = dragType;
    }

    public LinearLayout getSwipeBackLayout() {
        return mSwipeBackLayout;
    }

    public void setMinAlpha(int minAlpha) {
        this.mMinAlpha = minAlpha;
    }

    public void setPaintFade(boolean isPaintFade) {
        this.isPaintFade = isPaintFade;
    }

    public void setSwipeBackLayoutListener(
            SwipeBackLayoutListener swipeBackLayoutListener) {
        this.mSwipeBackLayoutListener = swipeBackLayoutListener;
    }

    public interface SwipeBackLayoutListener {
        void finishActivity();
    }
}
