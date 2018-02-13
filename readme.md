# 简介 #
该SwipeBackLayout主要实现了从左向右滑动或者从上向下滑动关闭界面功能（目前只实现是这两种功能）。
# 截图 #

![](https://i.imgur.com/s08E8h0.png)

# Gradle #
1.root build.gradle

	`allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}`
	
2.app build.gradle

`dependencies {
	         compile 'com.github.zhangliangming:SwipeBackLayout:v2.0'
	}`

# 第一种调用用法 #
1. styles.xml（activity的主题）

![](https://i.imgur.com/RDul7jX.png)

2. activity.xml

    <com.zlm.libs.widget.SwipeBackLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/swipebacklayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent" >
    </com.zlm.libs.widget.SwipeBackLayout>

3. test_com.xml

内容布局，普通布局即可

4. java调用
- 初始化

        mSwipeBackLayout = (SwipeBackLayout) findViewById(R.id.swipebacklayout);
    		mSwipeBackLayout.setDragType(SwipeBackLayout.ALL);
    		mSwipeBackLayout
    				.setSwipeBackLayoutListener(new SwipeBackLayoutListener() {
    
    					@Override
    					public void finishActivity() {
    						finish();
    						overridePendingTransition(0, 0);
    					}
    				});
    
    		mSwipeBackLayout.setContentView(R.layout.test_com);
    
    		// 左右视图
    		HorizontalScrollView hsv = (HorizontalScrollView) mSwipeBackLayout
    				.findViewById(R.id.horizontalScrollView);
    		mSwipeBackLayout.addIgnoreHorizontalView(hsv);
    
    		// 上下视图
    		ScrollView sv = (ScrollView) mSwipeBackLayout
    				.findViewById(R.id.scrollView);
    		mSwipeBackLayout.addIgnoreVerticalView(sv);


- activity返回事

    @Override
    	public void onBackPressed() {
    		mSwipeBackLayout.closeView();
    	}
# 第二种调用用法 #

用法具体和第一种差不多
![](https://i.imgur.com/yv6GrwG.png)

![](https://i.imgur.com/vvtw2Zj.png)

# API #
- addIgnoreVerticalView：添加竖直方向不拦截的view
- addIgnoreHorizontalView：添加水平方向不拦截的view
- setContentView：默认是添加LinearLayout布局的view，contentViewType有两种：CONTENTVIEWTYPE_LINEARLAYOUT（LinearLayout类型） / CONTENTVIEWTYPE_RELATIVELAYOUT（RelativeLayout类型）
- setDragType：设置右移关闭（LEFT_TO_RIGHT）/下拉关闭（TOP_TO_BOTTOM）/全部
- setPaintFade：设置是否绘画阴影
- setMinAlpha：设置绘画阴影时的，最小透明度
- SwipeBackLayoutListener：需要在view关闭时，关闭activity时使用
# 日志 #
## v1.0 ##
1. 实现从左向右滑动或者从上向下滑动关闭界面功能
2. 修复同一方向view的滑动冲突的问题

# 捐赠 #
如果该项目对您有所帮助，欢迎您的赞赏

- 微信

![](https://i.imgur.com/e3hERHh.png)

- 支付宝

![](https://i.imgur.com/29AcEPA.png)