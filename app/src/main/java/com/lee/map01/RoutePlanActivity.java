package com.lee.map01;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.BikingRoutePlanOption;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRoutePlanOption;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.navisdk.adapter.BNCommonSettingParam;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.BaiduNaviManagerFactory;
import com.baidu.navisdk.adapter.IBNRoutePlanManager;
import com.baidu.navisdk.adapter.IBNTTSManager;
import com.baidu.navisdk.adapter.IBaiduNaviManager;
import com.lee.map01.adapter.RouteAdapter;
import com.lee.map01.adapter.RouteDetailsAdapter;
import com.lee.map01.adapter.RouteLineAdapter;
import com.lee.map01.overlay.MyBikingRouteOverlay;
import com.lee.map01.overlay.MyDrivingRouteOverlay;
import com.lee.map01.overlay.MyTransitRouteOverlay;
import com.lee.map01.overlay.MyWalkingRouteOverlay;
import com.lee.map01.overlayutil.BikingRouteOverlay;
import com.lee.map01.overlayutil.DrivingRouteOverlay;
import com.lee.map01.overlayutil.OverlayManager;
import com.lee.map01.overlayutil.TransitRouteOverlay;
import com.lee.map01.overlayutil.WalkingRouteOverlay;
import com.lee.map01.utils.MyRecycleView;
import com.lee.map01.utils.NormalUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @类名: ${type_name}
 * @功能描述:
 * @作者: ${user}
 * @时间: ${date}
 * @最后修改者:
 * @最后修改内容:
 */
public class RoutePlanActivity extends AppCompatActivity implements BaiduMap.OnMapClickListener,
        OnGetRoutePlanResultListener {


    @BindView(R.id.mass)
    Button mass;
    @BindView(R.id.drive)
    Button drive;
    @BindView(R.id.transit)
    Button transit;
    @BindView(R.id.walk)
    Button walk;
    @BindView(R.id.bike)
    Button bike;
    @BindView(R.id.map)
    MapView map;
    @BindView(R.id.recycleview)
    MyRecycleView recycleview;
    @BindView(R.id.details)
    Button details;
    @BindView(R.id.btn_dh)
    TextView btn_dh;


    // 浏览路线节点相关
    RouteLine route = null;
    OverlayManager routeOverlay = null;

    // 地图相关，使用继承MapView的MyRouteMapView目的是重写touch事件实现泡泡处理
    // 如果不处理touch事件，则无需继承，直接使用MapView即可
    MapView mMapView = null;    // 地图View
    BaiduMap mBaidumap = null;
    // 搜索相关
    RoutePlanSearch mSearch = null;    // 搜索模块，也可去掉地图模块独立使用

    WalkingRouteResult nowResultwalk = null;
    BikingRouteResult nowResultbike = null;
    TransitRouteResult nowResultransit = null;
    DrivingRouteResult nowResultdrive = null;
    MassTransitRouteResult nowResultmass = null;

    int nowSearchType = -1; // 当前进行的检索，供判断浏览节点时结果使用。

    private RouteAdapter adapter;
    PlanNode stNode;
    PlanNode enNode;
    private ArrayList<String> strings;
    private String flag;

    static final String ROUTE_PLAN_NODE = "routePlanNode";
    private static final String APP_FOLDER_NAME = "BNSDKSimpleDemo";
    private String mSDCardPath = null;
    private boolean hasInitSuccess = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_plan);
        ButterKnife.bind(this);
        strings = new ArrayList<>();
        flag = getIntent().getExtras().getString("flag");
        // 初始化地图
        mMapView = (MapView) findViewById(R.id.map);
        mBaidumap = mMapView.getMap();

        // 地图点击事件处理
        mBaidumap.setOnMapClickListener(this);
        // 初始化搜索模块，注册事件监听
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(this);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recycleview.setLayoutManager(manager);
        adapter = new RouteAdapter(this);

        stNode = PlanNode.withLocation(new LatLng(Common.Location_Latitude, Common.Location_Longitude));
        enNode = PlanNode.withLocation(new LatLng(Common.Sreach_Latitude, Common.Sreach_Longitude));
        mSearch.drivingSearch((new DrivingRoutePlanOption()).from(stNode).to(enNode));
        nowSearchType = 1;
        if (initDirs()) {
            initNavi();
        }

    }
    /**
     * 发起路线规划搜索示例
     *
     * @param v
     */
    public void searchButtonProcess(View v) {
        // 重置浏览节点的路线数据
        route = null;
        mBaidumap.clear();
        // 处理搜索按钮响应
        // 设置起终点信息，对于tranist search 来说，城市名无意义
        //        PlanNode stNode = PlanNode.withCityNameAndPlaceName(Common.Location_City, startNodeStr);
        //        PlanNode enNode = PlanNode.withCityNameAndPlaceName(Common.Sreach_City, endNodeStr);

        if (v.getId() == R.id.mass) {
            mSearch.masstransitSearch(new MassTransitRoutePlanOption().from(stNode).to(enNode));
            nowSearchType = 0;
        } else if (v.getId() == R.id.drive) {
            mSearch.drivingSearch((new DrivingRoutePlanOption()).from(stNode).to(enNode));
            nowSearchType = 1;
        } else if (v.getId() == R.id.transit) {
            mSearch.transitSearch((new TransitRoutePlanOption()).from(stNode).city(Common.Location_City).to(enNode));
            nowSearchType = 2;
        } else if (v.getId() == R.id.walk) {
            mSearch.walkingSearch((new WalkingRoutePlanOption()).from(stNode).to(enNode));
            nowSearchType = 3;
        } else if (v.getId() == R.id.bike) {
            mSearch.bikingSearch((new BikingRoutePlanOption()).from(stNode).to(enNode));
            nowSearchType = 4;
        } else if (v.getId() == R.id.details){
            Intent intent = new Intent(RoutePlanActivity.this, RouteDetailsActivity.class);
            intent.putStringArrayListExtra("STRING", strings);
            startActivity(intent);
        } else if (v.getId() == R.id.btn_dh){
            if (BaiduNaviManager.isNaviInited()) {
                routeplanToNavi(BNRoutePlanNode.CoordinateType.BD09LL);
            }
        }
    }

    private void routeplanToNavi(int coType) {
        if (!hasInitSuccess) {
            Toast.makeText(this, "还未初始化!", Toast.LENGTH_SHORT).show();
        }
        final BNRoutePlanNode sNode = new BNRoutePlanNode(stNode.getLocation().longitude, stNode.getLocation().latitude,
                Common.Location_Address,
                Common.Location_End, coType);
        BNRoutePlanNode eNode = new BNRoutePlanNode(enNode.getLocation().longitude, enNode.getLocation().latitude,
                Common.Location_Address,
                Common.Location_End, coType);
        List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
        list.add(sNode);
        list.add(eNode);


        BaiduNaviManagerFactory.getRoutePlanManager().routeplanToNavi(
                list,
                IBNRoutePlanManager.RoutePlanPreference.ROUTE_PLAN_PREFERENCE_DEFAULT,
                null,
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_START:
                                Toast.makeText(RoutePlanActivity.this, "算路开始", Toast.LENGTH_SHORT)
                                        .show();
                                break;
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_SUCCESS:
                                Toast.makeText(RoutePlanActivity.this, "算路成功", Toast.LENGTH_SHORT)
                                        .show();
                                break;
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_FAILED:
                                Toast.makeText(RoutePlanActivity.this, "算路失败", Toast.LENGTH_SHORT)
                                        .show();
                                break;
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_TO_NAVI:
                                Toast.makeText(RoutePlanActivity.this, "算路成功准备进入导航", Toast.LENGTH_SHORT)
                                        .show();
                                Intent intent = new Intent(RoutePlanActivity.this,
                                        DemoGuideActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable(ROUTE_PLAN_NODE, sNode);
                                intent.putExtras(bundle);
                                startActivity(intent);
                                break;
                            default:
                                // nothing
                                break;
                        }
                    }
                });

    }

    private void initNavi() {
        try {
            BaiduNaviManagerFactory.getBaiduNaviManager().init(this,
                    mSDCardPath, APP_FOLDER_NAME, new IBaiduNaviManager.INaviInitListener() {

                        @Override
                        public void onAuthResult(int status, String msg) {
                            String result;
                            if (0 == status) {
                                result = "key校验成功!";
                            } else {
                                result = "key校验失败, " + msg;
                            }
                            Toast.makeText(RoutePlanActivity.this, result, Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void initStart() {
                            Toast.makeText(RoutePlanActivity.this, "百度导航引擎初始化开始", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void initSuccess() {
                            Toast.makeText(RoutePlanActivity.this, "百度导航引擎初始化成功", Toast.LENGTH_SHORT).show();
                            hasInitSuccess = true;
                            // 初始化tts
                            initTTS();
                        }

                        @Override
                        public void initFailed() {
                            Toast.makeText(RoutePlanActivity.this, "百度导航引擎初始化失败", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void initTTS() {
        // 使用内置TTS
        BaiduNaviManagerFactory.getTTSManager().initTTS(getApplicationContext(), getSdcardDir(), APP_FOLDER_NAME, NormalUtils.getTTSAppID());


        // 不使用内置TTS
        //         BaiduNaviManagerFactory.getTTSManager().initTTS(mTTSCallback);

        // 注册同步内置tts状态回调
        BaiduNaviManagerFactory.getTTSManager().setOnTTSStateChangedListener(new IBNTTSManager.IOnTTSPlayStateChangedListener() {
            @Override
            public void onPlayStart() {
                Log.e("BNSDKDemo", "ttsCallback.onPlayStart");
            }

            @Override
            public void onPlayEnd(String speechId) {
                Log.e("BNSDKDemo", "ttsCallback.onPlayEnd");
            }

            @Override
            public void onPlayError(int code, String message) {
                Log.e("BNSDKDemo", "ttsCallback.onPlayError");
            }
        });

        // 注册内置tts 异步状态消息
        BaiduNaviManagerFactory.getTTSManager().setOnTTSStateChangedHandler(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.e("BNSDKDemo", "ttsHandler.msg.what=" + msg.what);
            }
        });

    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    private boolean initDirs() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            mSDCardPath = Environment.getExternalStorageDirectory().toString();
        } else {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        mBaidumap.hideInfoWindow();
    }

    @Override
    public boolean onMapPoiClick(MapPoi mapPoi) {
        return false;
    }

    @Override
    public void onGetWalkingRouteResult(final WalkingRouteResult result) {

        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            recycleview.setVisibility(View.GONE);
            Toast.makeText(RoutePlanActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            recycleview.setVisibility(View.GONE);
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            AlertDialog.Builder builder = new AlertDialog.Builder(RoutePlanActivity.this);
            builder.setTitle("提示");
            builder.setMessage("检索地址有歧义，请重新设置。\n可通过getSuggestAddrInfo()接口获得建议查询信息");
            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {

            if (result.getRouteLines().size() >= 1) {
                try {
                    // 直接显示
                    recycleview.setVisibility(View.GONE);
                    route = result.getRouteLines().get(0);
                    mBaidumap.clear();
                    WalkingRouteOverlay overlay = new MyWalkingRouteOverlay(mBaidumap);
                    mBaidumap.setOnMarkerClickListener(overlay);
                    routeOverlay = overlay;
                    overlay.setData(result.getRouteLines().get(0));
                    overlay.addToMap();
                    overlay.zoomToSpan();
                    strings.clear();
                    for (int i = 0; i < result.getRouteLines().get(0).getAllStep().size(); i++) {
                        strings.add(result.getRouteLines().get(0).getAllStep().get(i).getInstructions());
                    }
                    if (result.getRouteLines().size() > 1) {
                        nowResultwalk = result;
                        recycleview.setVisibility(View.VISIBLE);
                        adapter.setDatas(result.getRouteLines(), RouteAdapter.Type.WALKING_ROUTE);
                        recycleview.setAdapter(adapter);
                        adapter.setItemClickListener(new RouteAdapter.MyItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {

                                route = nowResultwalk.getRouteLines().get(position);
                                mBaidumap.clear();
                                WalkingRouteOverlay overlay = new MyWalkingRouteOverlay(mBaidumap);
                                mBaidumap.setOnMarkerClickListener(overlay);
                                routeOverlay = overlay;
                                overlay.setData(nowResultwalk.getRouteLines().get(position));
                                overlay.addToMap();
                                overlay.zoomToSpan();

                                strings.clear();
                                for (int i = 0; i < nowResultwalk.getRouteLines().get(position).getAllStep().size(); i++) {

                                    strings.add(nowResultwalk.getRouteLines().get(position).getAllStep().get(i)
                                            .getInstructions());
                                }
                            }
                        });
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            } else {
                Log.d("route result", "结果数<0");
                return;
            }

        }

    }

    @Override
    public void onGetTransitRouteResult(TransitRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            recycleview.setVisibility(View.GONE);
            Toast.makeText(RoutePlanActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            recycleview.setVisibility(View.GONE);
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {

            if (result.getRouteLines().size() >= 1) {
                try {
                    // 直接显示
                    recycleview.setVisibility(View.GONE);
                    route = result.getRouteLines().get(0);
                    mBaidumap.clear();
                    TransitRouteOverlay overlay = new MyTransitRouteOverlay(mBaidumap);
                    mBaidumap.setOnMarkerClickListener(overlay);
                    routeOverlay = overlay;
                    overlay.setData(result.getRouteLines().get(0));
                    overlay.addToMap();
                    overlay.zoomToSpan();
                    strings.clear();
                    for (int i = 0; i < result.getRouteLines().get(0).getAllStep().size(); i++) {
                        strings.add(result.getRouteLines().get(0).getAllStep().get(i).getInstructions());
                    }
                    if (result.getRouteLines().size() > 1) {
                        recycleview.setVisibility(View.VISIBLE);
                        nowResultransit = result;
                        adapter.setDatas(result.getRouteLines(), RouteAdapter.Type.TRANSIT_ROUTE);
                        recycleview.setAdapter(adapter);
                        adapter.setItemClickListener(new RouteAdapter.MyItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                route = nowResultransit.getRouteLines().get(position);
                                mBaidumap.clear();
                                TransitRouteOverlay overlay = new MyTransitRouteOverlay(mBaidumap);
                                mBaidumap.setOnMarkerClickListener(overlay);
                                routeOverlay = overlay;
                                overlay.setData(nowResultransit.getRouteLines().get(position));
                                overlay.addToMap();
                                overlay.zoomToSpan();
                                strings.clear();
                                for (int i = 0; i < nowResultransit.getRouteLines().get(position).getAllStep().size(); i++) {
                                    strings.add(nowResultransit.getRouteLines().get(position).getAllStep().get(i).getInstructions());
                                }
                            }
                        });
                        mMapView.onResume();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            } else {
                Log.d("route result", "结果数<0");
                return;
            }


        }
    }

    @Override
    public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

    }

    @Override
    public void onGetDrivingRouteResult(DrivingRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            recycleview.setVisibility(View.GONE);
            Toast.makeText(RoutePlanActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            recycleview.setVisibility(View.GONE);
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {

            if (result.getRouteLines().size() >= 1) {
                try {
                    recycleview.setVisibility(View.GONE);
                    route = result.getRouteLines().get(0);
                    mBaidumap.clear();
                    DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mBaidumap);

                    routeOverlay = overlay;
                    mBaidumap.setOnMarkerClickListener(overlay);
                    overlay.setData(result.getRouteLines().get(0));
                    overlay.addToMap();
                    overlay.zoomToSpan();
                    strings.clear();
                    for (int i = 0; i < result.getRouteLines().get(0).getAllStep().size(); i++) {
                        strings.add(result.getRouteLines().get(0).getAllStep().get(i).getInstructions());
                    }
                    if (result.getRouteLines().size() > 1) {
                        recycleview.setVisibility(View.VISIBLE);
                        nowResultdrive = result;
                        adapter.setDatas(result.getRouteLines(), RouteAdapter.Type.DRIVING_ROUTE);
                        recycleview.setAdapter(adapter);
                        adapter.setItemClickListener(new RouteAdapter.MyItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                route = nowResultdrive.getRouteLines().get(position);
                                mBaidumap.clear();
                                DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mBaidumap);
                                mBaidumap.setOnMarkerClickListener(overlay);
                                routeOverlay = overlay;
                                overlay.setData(nowResultdrive.getRouteLines().get(position));
                                overlay.addToMap();
                                overlay.zoomToSpan();

                                mMapView.refreshDrawableState();
                                strings.clear();
                                for (int i = 0; i < nowResultdrive.getRouteLines().get(position).getAllStep().size(); i++) {
                                    strings.add(nowResultdrive.getRouteLines().get(position).getAllStep().get(i).getInstructions());
                                }

                            }
                        });
                        mMapView.onResume();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            } else {
                Log.d("route result", "结果数<0");
                return;
            }

        }
    }

    @Override
    public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

    }

    @Override
    public void onGetBikingRouteResult(BikingRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            recycleview.setVisibility(View.GONE);
            Toast.makeText(RoutePlanActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            recycleview.setVisibility(View.GONE);
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            AlertDialog.Builder builder = new AlertDialog.Builder(RoutePlanActivity.this);
            builder.setTitle("提示");
            builder.setMessage("检索地址有歧义，请重新设置。\n可通过getSuggestAddrInfo()接口获得建议查询信息");
            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {

            if (result.getRouteLines().size() >= 1) {
                try{
                    recycleview.setVisibility(View.GONE);
                    route = result.getRouteLines().get(0);
                    mBaidumap.clear();
                    BikingRouteOverlay overlay = new MyBikingRouteOverlay(mBaidumap);
                    routeOverlay = overlay;
                    mBaidumap.setOnMarkerClickListener(overlay);
                    overlay.setData(result.getRouteLines().get(0));
                    overlay.addToMap();
                    overlay.zoomToSpan();
                    strings.clear();
                    for (int i = 0; i < result.getRouteLines().get(0).getAllStep().size(); i++) {
                        strings.add(result.getRouteLines().get(0).getAllStep().get(i).getInstructions());
                    }
                    if (result.getRouteLines().size() > 1) {
                        recycleview.setVisibility(View.VISIBLE);
                        nowResultbike = result;
                        adapter.setDatas(result.getRouteLines(), RouteAdapter.Type.BIKING_ROUTE);
                        recycleview.setAdapter(adapter);
                        adapter.setItemClickListener(new RouteAdapter.MyItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                route = nowResultbike.getRouteLines().get(position);
                                mBaidumap.clear();
                                BikingRouteOverlay overlay = new MyBikingRouteOverlay(mBaidumap);
                                mBaidumap.setOnMarkerClickListener(overlay);
                                routeOverlay = overlay;
                                overlay.setData(nowResultbike.getRouteLines().get(position));
                                overlay.addToMap();
                                overlay.zoomToSpan();
                                strings.clear();
                                for (int i = 0; i < nowResultbike.getRouteLines().get(position).getAllStep().size(); i++) {
                                    strings.add(nowResultbike.getRouteLines().get(position).getAllStep().get(i).getInstructions());
                                }
                            }
                        });
                        mMapView.onResume();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            } else {
                Log.d("route result", "结果数<0");
                return;
            }

        }
    }


    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (mSearch != null) {
            mSearch.destroy();
        }
        mMapView.onDestroy();
        super.onDestroy();
    }




}
//jhfghfh