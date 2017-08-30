package com.lee.map01;

import android.app.Application;

import com.baidu.mapapi.SDKInitializer;

/**
 * @类名: ${type_name}
 * @功能描述:
 * @作者: ${user}
 * @时间: ${date}
 * @最后修改者:
 * @最后修改内容:
 */
public class MyApplication extends Application {
    
    public static MyApplication application;
    
    @Override
    public void onCreate() {
        super.onCreate();
        application = this;

        //在使用SDK各组件之前初始化context信息，传入ApplicationContext  
        //注意该方法要再setContentView方法之前实现  
        SDKInitializer.initialize(getApplicationContext());   
    }
}
//jhfghfh