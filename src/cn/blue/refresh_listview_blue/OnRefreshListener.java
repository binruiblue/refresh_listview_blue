package cn.blue.refresh_listview_blue;

/**
 * @author 小奶牛
 * @date 2014-11-17 
 *
 * @Description 刷新监听器
 *
 */
public interface OnRefreshListener {
	
	/**
	 * 响应拉下刷新事件
	 */
	public void OnPullDownEvent();
	
	/**
	 * 响应加载更多
	 */
	public void OnLoadingMoreEvent();

}
