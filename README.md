可以往listView的头布局中加入多个不同的布局  
可以实现在多个布局的基础上下拉刷新和加载更多   
基本使用方法:   
mListView.addCustomHead(view); // view放置到listview的上面  
mListView.setEnablePullDown(true); // 开启下拉刷新  
mListView.setEnableLoadingMore(true); // 开启加载更多 
mListView.setOnRefreshListener(this); // 事件处理 
