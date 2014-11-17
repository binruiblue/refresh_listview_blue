package cn.blue.refresh_listview_blue;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * @author 小奶牛
 * @date 2014-11-17 
 *
 * @Description 自定义下拉刷新的listView
 *
 */
public class RefreshListView extends ListView implements OnScrollListener {

	private LinearLayout llRoot; //根节点的线性布局
	private LinearLayout llPullDown; // 下拉刷新
	private ImageView ivArr; // 箭头
	private ProgressBar mProgressBar; // 进度条
	private TextView tvState; // 文本状态
	private TextView tvDate; // 更新时间
	private View mCustonHeadView; // 传递过来的轮播图
	
	private int downY = -1; // 当前手指按下的位置
	private int llPullDownHeight;  // 拉下刷新控件的高度
	private int mFirstVisiblePosition = -1; // 当前第一条可见条目 默认是-1
	
	private final int PULL_DOWN = 0;  // 下拉刷新
	private final int RELEASE_REFRESH = 1; // 释放刷新
	private final int REFRESHING = 2; // 正在刷新
	private int currentState = PULL_DOWN; // 当前的状态
	
	private RotateAnimation downAnimation;  // 下拉的动画
	private RotateAnimation upAnimation;   // 上去的动画
	
	private OnRefreshListener listener; // 刷新相关的监听器
	
	private View footView;  // 脚布局  加载更多
	private int footViewHeight; //脚布局的高度
	private boolean isLoadingMore = false; // 标记 当前是否正在加载更多  
	
	private boolean isEnablePullDown = false; //是否需要开启下拉刷新
	private boolean isEnableLoadingMore = false; // 是否开启加载更多
	
	/**
	 * 是否需要开启下拉刷新
	 */
	public void setEnablePullDown(boolean isEnablePullDown){
		this.isEnablePullDown = isEnablePullDown;
	}
	/**
	 * 是否需要开启加载更多
	 */
	public void setEnableLoadingMore(boolean isEnableLoadingMore){
		this.isEnableLoadingMore = isEnableLoadingMore;
	}

	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initHead();
		initFoot();
		setOnScrollListener(this);
	}

	public RefreshListView(Context context) {
		super(context);
		initHead();
		initFoot();
		setOnScrollListener(this);
	}

	/**
	 * 初始化脚布局
	 */
	private void initFoot() {
		footView = View.inflate(getContext(), R.layout.refresh_list_foot, null);
		footView.measure(0, 0);
		footViewHeight = footView.getMeasuredHeight();
		
		footView.setPadding(0, -footViewHeight, 0, 0);
		this.addFooterView(footView);
	}

	/**
	 * 初始化头数据
	 */
	private void initHead() {
		View pullDownHeadView = View.inflate(getContext(), R.layout.refresh_listview, null);
		llRoot = (LinearLayout) pullDownHeadView.findViewById(R.id.ll_refresh_listview_root);
		llPullDown = (LinearLayout) pullDownHeadView.findViewById(R.id.ll_refresh_listview_pull_down);
		ivArr = (ImageView) pullDownHeadView.findViewById(R.id.iv_refresh_listview_arrow);
		mProgressBar = (ProgressBar) pullDownHeadView.findViewById(R.id.pb_refresh_listview);
		tvState = (TextView) pullDownHeadView.findViewById(R.id.tv_refresh_listview_state);
		tvDate = (TextView) pullDownHeadView.findViewById(R.id.tv_refresh_listview_date);
		
		tvDate.setText("最后更新时间 : " + getCurrentTime());
		//将下拉刷新头进行隐藏
		llPullDown.measure(0, 0);
		llPullDownHeight = llPullDown.getMeasuredHeight();
		
		llPullDown.setPadding(0, -llPullDownHeight, 0, 0);
		
		this.addHeaderView(pullDownHeadView);
		
		initAnimation();
	}
	
	/**
	 * 初始化动画
	 */
	private void initAnimation() {
		downAnimation = new RotateAnimation(-180, -360, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		downAnimation.setDuration(500);
		downAnimation.setFillAfter(true);
		
		upAnimation = new RotateAnimation(0, -180, 
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF, 0.5f);
		upAnimation.setDuration(500);
		upAnimation.setFillAfter(true);
	}

	/**
	 * 添加到自定义的listView头
	 */
	public void addCustomHead(View v){
		mCustonHeadView = v;
		llRoot.addView(v);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			downY = (int) ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			// 判断是否需要拉下刷新
			if(!isEnablePullDown){
				break;
			}
			
			if(downY == -1){
				downY = (int) ev.getY();
			}
			//当前状态为正在刷新则不可进行操作
			if(currentState == REFRESHING){
				break;
			}
			
			//判断listView在屏幕中的位置和轮播图当前在屏幕的位置是否一致 一致则证明已经完全显示
			if(mCustonHeadView != null){
				//获取当前listView第一条在屏幕中的Y轴的位置
				int[] location = new int[2];
				this.getLocationOnScreen(location);
				int mListViewLocationOnScreenY = location[1];  // listView在屏幕中的y轴的值
				
				mCustonHeadView.getLocationOnScreen(location);
				int mCustonHeadViewLocationOnScreenY = location[1]; // 轮播图在屏幕中的Y轴的
				
				if(mCustonHeadViewLocationOnScreenY < mListViewLocationOnScreenY){
					//当前的轮播图没有完全显示  不可以进行下拉刷新的操作
					break;
				}
			}
			
			//计算拉出的距离
			int moveY = (int) ev.getY();
			int paddingTop = -llPullDownHeight + (moveY - downY);
			
			//进行判断  必须是当前拉出的距离要大于原来的高度 也就是向下拉的 还有就是判断当前是否为第一条
			if(paddingTop > -llPullDownHeight 
					&& mFirstVisiblePosition == 0){
				//判断当前的状态
				if(paddingTop > 0 && currentState == PULL_DOWN){
					// 释放刷新
					currentState = RELEASE_REFRESH;
					refreshPullDownHeadView();
				}else if (paddingTop < 0 && currentState == RELEASE_REFRESH){
					// 下拉刷新
					currentState = PULL_DOWN;
					refreshPullDownHeadView();
				}
				
				llPullDown.setPadding(0, paddingTop, 0, 0);
				return true;
			}
			
			break;
		case MotionEvent.ACTION_UP:
			//复位
			downY = -1;
			
			if(currentState == RELEASE_REFRESH){
				//正在刷新
				currentState = REFRESHING;
				refreshPullDownHeadView();
				
				llPullDown.setPadding(0, 0, 0, 0);
				
				// 设置监听器回调
				if(listener != null){
					listener.OnPullDownEvent();
				}
				
			}else if (currentState == PULL_DOWN){
				//还原原来的样子
				llPullDown.setPadding(0, -llPullDownHeight, 0, 0);
			}
			
			break;
		}
		return super.onTouchEvent(ev);
	}
	
	/**
	 * 刷新当前的状态
	 */
	public void refreshPullDownHeadView(){
		switch (currentState) {
		case PULL_DOWN:  // 下拉状态
			ivArr.startAnimation(downAnimation);
			tvState.setText("下拉刷新");
			break;
		case RELEASE_REFRESH: // 释放更新状态
			ivArr.startAnimation(upAnimation);
			tvState.setText("释放刷新");
			
			break;
		case REFRESHING: // 正在刷新
			ivArr.clearAnimation(); // 清空动画
			ivArr.setVisibility(View.INVISIBLE);
			mProgressBar.setVisibility(View.VISIBLE);
			tvState.setText("正在刷新..");
			
			break;
		}
	}
	

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// 判断是否需要加载更多
		if(!isEnableLoadingMore){
			return;
		}
		
		if(scrollState == SCROLL_STATE_IDLE 
				|| scrollState == SCROLL_STATE_FLING){
			//判断是否为最后的位置
			if(getLastVisiblePosition() == (getCount() - 1) ){
				isLoadingMore = true;
				
				footView.setPadding(0, 0, 0, 0);
				this.setSelection(getCount());
				
				// 设置监听器
				if(listener != null){
					listener.OnLoadingMoreEvent();
				}
			}
		}
		
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// 记得第一个条目的位置
		mFirstVisiblePosition = firstVisibleItem;
	}
	
	public void setOnRefreshListener(OnRefreshListener listener){
		this.listener = listener;
	}

	/**
	 * 数据处理完毕后调用此方法关闭刷新界面
	 */
	public void refreshFinish() {
		if(currentState == REFRESHING){ //刷新数据处理完毕
			currentState = PULL_DOWN;
			mProgressBar.setVisibility(View.INVISIBLE);
			ivArr.setVisibility(View.VISIBLE);
			tvState.setText("正在刷新");
			tvDate.setText("最后更新时间 : " + getCurrentTime());
			
			llPullDown.setPadding(0, -llPullDownHeight, 0, 0);
		}else if (isLoadingMore){
			isLoadingMore = false;
			
			footView.setPadding(0, -footViewHeight, 0, 0);
		}
	}

	/**
	 * 获取当前事件
	 * @return 格式:2014-11-17 18:55:00
	 */
	private String getCurrentTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(new Date());
	}

}
