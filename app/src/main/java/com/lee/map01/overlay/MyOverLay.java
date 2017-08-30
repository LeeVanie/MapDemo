package com.lee.map01.overlay;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.Overlay;
import com.lee.map01.overlayutil.PoiOverlay;

/**
 * @类名: ${type_name}
 * @功能描述:
 * @作者: ${user}
 * @时间: ${date}
 * @最后修改者:
 * @最后修改内容:
 */
public class MyOverLay extends PoiOverlay {
    
    /**
     * 构造函数
     *
     * @param baiduMap 该 PoiOverlay 引用的 BaiduMap 对象
     */
    public MyOverLay(BaiduMap baiduMap) {
        super(baiduMap);
    }

    @Override
    public boolean onPoiClick(int i) {
        super.onPoiClick(i);
        return true;
    }
}
//jhfghfh