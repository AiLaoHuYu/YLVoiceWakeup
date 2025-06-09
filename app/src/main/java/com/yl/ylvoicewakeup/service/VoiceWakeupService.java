package com.yl.ylvoicewakeup.service;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.BaseLibrary;
import com.iflytek.aikit.core.CoreListener;
import com.iflytek.aikit.core.ErrType;
import com.iflytek.aikit.core.LogLvl;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

import com.yl.creteEntity.crete.roomCrete.entity.roomDIal;
import com.yl.ylvoicewakeup.R;
import com.yl.ylvoicewakeup.creteVoice.CreateLogotype;
import com.yl.ylvoicewakeup.creteVoice.QueryFeatureList;
import com.yl.ylvoicewakeup.creteVoice.SearchFeature;
import com.yl.ylvoicewakeup.creteVoice.SearchOneFeature;
import com.yl.ylvoicewakeup.model.AsrModel;
import com.yl.ylvoicewakeup.utils.AnimatorUtil;
import com.yl.ylvoicewakeup.utils.AudioTrackManager;
import com.yl.ylvoicewakeup.utils.CommonUtil;
import com.yl.ylvoicewakeup.utils.CreteUtlis;
import com.yl.ylvoicewakeup.utils.FileUtils;
import com.yl.ylvoicewakeup.utils.JsonParser;
import com.yl.ylvoicewakeup.utils.PcmUtils;
import com.yl.ylvoicewakeup.utils.SystemPropertiesReflection;
import com.yl.ylvoicewakeup.utils.TenSecondsOfAudio;
import com.yl.ylvoicewakeup.utils.ToastUtil;
import com.yl.ylvoicewakeup.utils.XunFeiAsr;
import com.yl.ylvoicewakeup.utils.XunFeiXTTS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class VoiceWakeupService extends Service {

    private SpeechRecognizer mIat;// 语音听写对象
    private String mEngineType = SpeechConstant.TYPE_CLOUD;// 引擎类型
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams wmParams;
    private View mFloatingLayout;
    private ImageView voiceImg;
    private TextView voiceText;
    private View floatingView;
    //用来判断是否要隐藏窗口
    private boolean isHideView = false;
    private String resultType = "json";//结果内容数据格式
    private String language = "zh_cn";//识别语言
    private String TAG = VoiceWakeupService.class.getSimpleName();
    private AnimatorUtil animatorUtil;
    private Handler mHandler;
    // 唤醒结果内容
    private String resultString;
    private final static String ACTION = "com.yl.voice.wakeup";
    private ContentValues mIatResults = new ContentValues();
    private SharedPreferences mSharedPreferences;//缓存
    //初始话科大讯飞声纹识别
    private String CreateAPP_ID;
    private String CreateAPISecret;
    private String CreateAPIKey;
    private String CreateRequestUrl;
    //声纹识别所需文件保存路径
    private String creteFlies;
    //对比声纹文件
    private String contrastFies;
    //全局声纹识别创建文件工具类
    private CreteUtlis creteUtlis = new CreteUtlis();
    //声纹识别标识参数设置
    private CreateLogotype createLogotype = new CreateLogotype();
    final Map<String, String>[] SearchOneFeatureList = new Map[]{new HashMap<>()}; //1:1服务结果
    final Map<String, String>[] result = new Map[]{new HashMap<>()};//1:N服务结果
    final Map<String, String>[] group = new Map[]{new HashMap<>()};//创建特征库服务结果
    final Map<String, String>[] querySelect = new Map[]{new HashMap<>()};//查询结果
    List<String> groupIdList = new ArrayList<>();//分组标识
    List<String> groupNameList = new ArrayList<>();//声纹分组名称
    List<String> groupInfoLsit = new ArrayList<>();//分组描述信息
    List<String> featureIdList = new ArrayList<>();//特征唯一标识
    List<String> featureInfoList = new ArrayList<>();//特征描述
    public boolean fig = true;//返回判断是否注册声纹结果
    private roomDIal roomDIal;
    public TenSecondsOfAudio tenSecondsOfAudio;
    public PcmUtils pcmUtils;
    public ServiceConnection mConnection;
    private String APPID;
    private String APIKEY;
    private String APISECRET;
    private String WORK_DIR;
    private String RES_DIR;
    private int authResult = -1;
    private XunFeiAsr xunFeiAsr;
    private XunFeiXTTS xunFeiXTTS;
    private final String[] selectionText = {"第一个", "第二个", "第三个", "第四个", "第五个", "第六个", "最后一个", "暂停"};
    private AudioManager audioManager;
    private boolean isMusicPlaying = false;
    //用来区分开始命令字识别还是在线识别
    private boolean isOnlineRecognize = false;
    //是否授权成功
    private boolean isAuth = false;

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        registerBroadcast();
        //  开放平台注册的APPID
        SpeechUtility.createUtility(getApplication(), SpeechConstant.APPID + "=c7af7320");
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
        mSharedPreferences = getSharedPreferences("ASR", Activity.MODE_PRIVATE);
        animatorUtil = new AnimatorUtil();
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                roomDIal = com.yl.creteEntity.crete.roomCrete.entity.roomDIal.Stub.asInterface(service);
                Log.d(TAG, "onServiceConnected: 已连接");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                roomDIal = null;
                Log.d(TAG, "onServiceConnected: 断开连接");
            }
        };
        Intent intent = new Intent();
        //Android现在对隐式意图管理严格，除了要setAction包名也需要设置，这里包名是服务端app的包名
        intent.setAction("com.example.testapp.aidl");
        intent.setPackage("com.yl.deepseekxunfei");
        //绑定服务
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        tenSecondsOfAudio = new TenSecondsOfAudio(VoiceWakeupService.this);
        tenSecondsOfAudio.startRecording();
        pcmUtils = new PcmUtils();
        mHandler = new Handler(Looper.getMainLooper());
        initWindow();
        initXunFei();
        initInfo();
        initSDK();
        xunFeiAsr = new XunFeiAsr(getApplication(), resultCallBack);
        xunFeiXTTS = new XunFeiXTTS();
        judgeRecognizeAndStart();
        startMonitoring();
    }

    private void initInfo() {
        APPID = getResources().getString(R.string.appId);
        APIKEY = getResources().getString(R.string.apiKey);
        APISECRET = getResources().getString(R.string.apiSecret);
        WORK_DIR = getResources().getString(R.string.workDir);
        File file = new File(WORK_DIR);
        if (!file.exists()) {
            file.mkdir();
        }
        RES_DIR = WORK_DIR + "ivw";
        File file1 = new File(RES_DIR);
        if (!file1.exists()) {
            file1.mkdir();
        }
        FileUtils.copyAssetsToSDCard(this, "resource", WORK_DIR);
    }

    private XunFeiAsr.ResultCallBack resultCallBack = new XunFeiAsr.ResultCallBack() {
        @Override
        public String onResultAction(String result) {
            if (isOnlineRecognize) {
                mHandler.removeCallbacks(exitWindowRunnable);
                return "true;200";
            }
            Log.e(TAG, "onResultAction: " + result);
            xunFeiAsr.endRecognize();
            mHandler.removeCallbacks(exitWindowRunnable);
            StringBuilder isNeedToStart = new StringBuilder("true;");
            if ("com.yl.deepseekxunfei".equals(CommonUtil.getForegroundActivity(getApplicationContext()))) {
                if (Arrays.asList(selectionText).contains(result)) {
                    sendBroadCast("com.yl.voice.commit.text", "text", result);
                }
            } else {
                if (result.equals("你好小天")) {
                    wakeupAction();
                    isNeedToStart.append("100");
                } else if (result.equals("回到桌面") || result.equals("回到首页")
                        || result.equals("打开桌面") || result.equals("打开首页")) {
                    goToHome();
                    isNeedToStart.append("200");
                } else if (result.equals("打开导航") || result.equals("打开地图") || result.equals("回到地图") || result.equals("回到导航")) {
                    openAmap();
                    isNeedToStart.append("200");
                } else if (result.equals("打开酷我音乐") || result.equals("回到酷我音乐")) {
                    openKuwo();
                    isNeedToStart.append("200");
                } else if (result.equals("上一首")) {
                    inputKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                    isNeedToStart.append("200");
                } else if (result.equals("下一首")) {
                    inputKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
                    isNeedToStart.append("200");
                } else if (result.equals("暂停播放")) {
                    if (isMusicPlaying) {
                        inputKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    }
                    isNeedToStart.append("200");
                } else if (result.equals("继续播放")) {
                    if (!isMusicPlaying) {
                        inputKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    }
                    isNeedToStart.append("200");
                }
            }
            return isNeedToStart.toString();
        }

        @Override
        public void onResultText(String text, boolean isEnd) {
            if (isOnlineRecognize) {
                mHandler.removeCallbacks(exitWindowRunnable);
                return;
            }
            mHandler.removeCallbacks(exitWindowRunnable);
            mHandler.postDelayed(exitWindowRunnable, 7000);
            if (mWindowManager != null) {
                if (voiceImg != null) {
                    if (voiceImg.isAttachedToWindow()) {
                        mHandler.post(() -> {
                            voiceText.setText(text);
                            if (isEnd) {
                                if (!text.equals("你好小天")) {
                                    AudioTrackManager.getInstance().stopPlay();
                                    xunFeiXTTS.startTTS("好的");
                                    mHandler.postDelayed(() -> {
                                        mWindowManager.removeView(floatingView);
                                    }, 500);
                                }
                            }
                        });
                    }
                }
            }
        }
    };

    public void openAmap() {
        // 检查高德地图是否安装
        PackageManager packageManager = getPackageManager();
        try {
            packageManager.getPackageInfo("com.autonavi.amapauto", 0);

            // 构建高德地图的URI
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.autonavi.amapauto", "com.autonavi.amapauto.MainMapActivity"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // 启动高德地图应用
            startActivity(intent);
        } catch (PackageManager.NameNotFoundException e) {
            // 未安装高德地图，提示用户安装
            e.printStackTrace();
        }
    }

    public void openKuwo() {
        // 检查高德地图是否安装
        PackageManager packageManager = getPackageManager();
        try {
            packageManager.getPackageInfo("cn.kuwo.kwmusiccar", 0);

            // 构建高德地图的URI
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("cn.kuwo.kwmusiccar", "cn.kuwo.kwmusiccar.ui.MainActivity"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // 启动高德地图应用
            startActivity(intent);
        } catch (PackageManager.NameNotFoundException e) {
            // 未安装高德地图，提示用户安装
            e.printStackTrace();
        }
    }

    private void goToHome() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.setComponent(new ComponentName("com.yl.yldesktop", "com.yl.yldesktop.activity.MainActivity"));
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
    }

    private void initSDK() {
        AiHelper.getInst().setLogInfo(LogLvl.VERBOSE, 1, "/sdcard/iflytek/aikit/aeeLog.txt");
        AiHelper.getInst().setLogMode(2); //可将日志打印到文件中，发送到工单
        AudioTrackManager.getInstance().setSpeakCallBack(speakCallBack);
        //设定初始化参数
        //能力id列表 唤醒：e867a88f2  合成e2e44feff  命令词e75f07b62  合成轻量版ece9d3c90
        BaseLibrary.Params params = BaseLibrary.Params.builder()
                .appId(APPID)  //您的应用ID，可从控制台查看
                .apiKey(APIKEY) //您的APIKEY，可从控制台查看
                .apiSecret(APISECRET) //您的APISECRET，可从控制台查看
                .workDir(WORK_DIR) //SDK的工作目录，需要确保有读写权限。一般用于存放离线能力资源，日志存放目录等使用。
                .ability("e75f07b62;e2e44feff") //初始化时使用几个能力就填几个能力的id，以;分开，如：使用唤醒+合成填入"e867a88f2;e2e44feff"
                .build();
        //初始化SDK
        new Thread(new Runnable() {
            @Override
            public void run() {
                AiHelper.getInst().initEntry(getApplicationContext(), params);
            }
        }).start();
        AiHelper.getInst().registerListener(coreListener);// 注册SDK 初始化状态监听
    }

    private AudioTrackManager.SpeakCallBack speakCallBack = new AudioTrackManager.SpeakCallBack() {
        @Override
        public void onSpeakEnd() {
            mHandler.post(() -> {
                animatorUtil.stopJumpAnimation(voiceImg);
            });
            if (isHideView) {
                mHandler.postDelayed(() -> {
                    if (mWindowManager != null) {
                        if (mFloatingLayout != null && floatingView.isAttachedToWindow()) {
                            mWindowManager.removeView(floatingView);
                        }
                    }
                }, 1000);
                isHideView = false;
            } else {
                if (isNetSystemUsable()) {
                    //开始识别，并设置监听器
                    mIat.startListening(mRecogListener);
                }
            }
        }

        @Override
        public void onSpeakStart() {
            animatorUtil.startJumpAnimation(voiceImg);
        }
    };

    //授权结果回调
    private CoreListener coreListener = new CoreListener() {
        @Override
        public void onAuthStateChange(final ErrType type, final int code) {
            Log.i(TAG, "core listener code:" + code);
            switch (type) {
                case AUTH:
                    authResult = code;
                    if (code == 0) {
                        Log.e(TAG, "SDK授权成功");
                        xunFeiAsr.initAsr();
                        xunFeiXTTS.init(getApplicationContext());
                        mHandler.postDelayed(() -> {
                            xunFeiAsr.startRecognize();
                        }, 2000);
                        isAuth = true;
                    } else {
                        Log.e(TAG, "SDK授权失败，授权码为:" + authResult);
                        initSDK();
                        isAuth = false;
                    }
                    break;
                case HTTP:
                    Toast.makeText(getBaseContext(), "SDK状态：HTTP认证结果" + code,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(getBaseContext(), "SDK状态：其他错误" + code,
                            Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void registerBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.yl.start.voice");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                toggleWakeup();
            }
        }, intentFilter);
    }

    private void toggleWakeup() {
        //如果语音悬浮窗已经拉起，则remove它，若无，则拉起语音悬浮窗
        if (mFloatingLayout != null && mWindowManager != null) {
            if (!floatingView.isAttachedToWindow()) {
                wakeupAction();
            }
//            else {
//                mWindowManager.removeView(floatingView);
//            }
        }
    }

    public void wakeupAction() {
        if ("com.yl.deepseekxunfei".equals(CommonUtil.getForegroundActivity(getApplicationContext()))) {
            sendBroadCast(ACTION);
        } else {
            if (!isAuth) {
                ToastUtil.showMessage(this, "请先联网激活语音", Toast.LENGTH_LONG);
                return;
            }
            xunFeiXTTS.stopTTS();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    voiceText.setText("你好");
                    xunFeiXTTS.startTTS("你好");
                    if (!floatingView.isAttachedToWindow()) {
                        showFloatingWindow();
                    }
                }
            });
//                Log.d(TAG, "onCreate: "+mConnection);
//                if (roomDIal != null) {
//                    try {
//                        List<creteEntity> creteEntityList = roomDIal.listRoom();
//                        Log.d(TAG, "roomDIal: " + creteEntityList.toString());
//                        for (creteEntity c : creteEntityList) {
//                            groupIdList.add(c.getGroupId());
//                            groupNameList.add(c.getGroupName());
//                            groupInfoLsit.add(c.getGroupInfo());
//                            featureIdList.add(c.getFeatureId());
//                            featureInfoList.add(c.getFeatureInfo());
//
//                            createLogotype.setGroupId(groupIdList);
//                            createLogotype.setGroupName(groupNameList);
//                            createLogotype.setGroupInfo(groupInfoLsit);
//                            createLogotype.setFeatureId(featureIdList);
//                            createLogotype.setFeatureInfo(featureInfoList);
//                        }
//                        //判断是否有声纹信息
//                        if (seleteCrete()) {
//                            //对比声纹文件new WeakReference<>(this)
//                            byte[] last10Seconds = tenSecondsOfAudio.getLast5Seconds();
//                            // 处理音频数据（如保存为WAV或上传服务器）
//                            String flies = pcmUtils.savePcmToFile(last10Seconds, VoiceWakeupService.this.getExternalFilesDir("pcm"), "duibi.pcm");
//                            if (flies != null) {
//                                contrastFies = flies;
//                            }
//                            if (!createFig()) {
//                                mttsFig = false;
//                                mTts.startSpeaking("我在，请问有什么吩咐", mSynthesizerListener);
//                                LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
//                                mFloatingLayout = inflater.inflate(R.layout.voice_layout, null);
//                                mFloatingLayout.setBackgroundResource(R.drawable.voice_bg);
//                                voiceImg = mFloatingLayout.findViewById(R.id.voice_img);
//                                voiceText = mFloatingLayout.findViewById(R.id.voice_text);
//                                mWindowManager.addView(mFloatingLayout, wmParams);
//                            } else {
//                                mttsFig = true;
//                                mTts.startSpeaking("声纹验证失败，请重试！", mSynthesizerListener);
//                                LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
//                                mFloatingLayout = inflater.inflate(R.layout.voice_layout, null);
//                                mFloatingLayout.setBackgroundResource(R.drawable.voice_bg);
//                                voiceImg = mFloatingLayout.findViewById(R.id.voice_img);
//                                voiceText = mFloatingLayout.findViewById(R.id.voice_text);
//                                voiceText.setText("声纹验证失败，请重试");
//                                mWindowManager.addView(mFloatingLayout, wmParams);
//                                isHideView = true;
//                            }
//                        } else {
//                            mttsFig = true;
//                            mTts.startSpeaking("暂无声纹信息，请注册", mSynthesizerListener);
//                            LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
//                            mFloatingLayout = inflater.inflate(R.layout.voice_layout, null);
//                            mFloatingLayout.setBackgroundResource(R.drawable.voice_bg);
//                            voiceImg = mFloatingLayout.findViewById(R.id.voice_img);
//                            voiceText = mFloatingLayout.findViewById(R.id.voice_text);
//                            voiceText.setText("暂无声纹信息，请注册");
//                            mWindowManager.addView(mFloatingLayout, wmParams);
//                            isHideView = true;
//                        }
//                    } catch (RemoteException e) {
//                        throw new RuntimeException(e);
//                    }
//                } else {
//                    Log.d(TAG, "roomDIal: " + roomDIal);
//                }
        }
    }

    Runnable exitWindowRunnable = new Runnable() {
        @Override
        public void run() {
//            if (!isNetSystemUsable()){
            if (mFloatingLayout != null && floatingView.isAttachedToWindow()) {
                voiceText.setText("未识别到您说话，请稍后再试");
                xunFeiXTTS.startTTS("未识别到您说话，请稍后再试");
                mWindowManager.removeView(floatingView);
            }
//            } else {
//                isHideView = true;
//            }
        }
    };

    private void initWindow() {
        //初始话声纹识别必须参数
        CreateAPP_ID = "27b3a946";
        CreateAPISecret = "MGNhOTM2Yjg3MmVhMTFjYzhhODQzMTYw";
        CreateAPIKey = "06224092793087296b1f47c96e0133bc";
        CreateRequestUrl = "https://api.xf-yun.com/v1/private/s782b4996";
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wmParams = new WindowManager.LayoutParams();
        wmParams.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        // 设置全屏标志
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        wmParams.format = PixelFormat.TRANSLUCENT;
        wmParams.gravity = Gravity.START | Gravity.TOP;
        wmParams.x = 120;
        wmParams.y = 50;
        wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        // 创建全屏透明背景
        floatingView = new FrameLayout(this);
        floatingView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        floatingView.setBackgroundColor(0x00000000); // 完全透明
        floatingView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // 检查点击是否在内容视图外部
                if (!isPointInsideView(event.getRawX(), event.getRawY(), mFloatingLayout)) {
                    closeFloatingWindow();
                    return true;
                }
            }
            return false;
        });
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        mFloatingLayout = inflater.inflate(R.layout.voice_layout, null);
        mFloatingLayout.setLayoutParams(new ViewGroup.LayoutParams(250, 100));
        mFloatingLayout.setBackgroundResource(R.drawable.voice_bg);
        voiceImg = mFloatingLayout.findViewById(R.id.voice_img);
        voiceText = mFloatingLayout.findViewById(R.id.voice_text);
    }

    // 显示悬浮窗
    private void showFloatingWindow() {
        try {
            ((FrameLayout) floatingView).removeView(mFloatingLayout);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 将内容视图添加到全屏背景
        ((FrameLayout) floatingView).addView(mFloatingLayout);

        // 设置内容视图位置（居中）
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mFloatingLayout.getLayoutParams();
        mFloatingLayout.setLayoutParams(params);

        // 添加窗口到WindowManager
        mWindowManager.addView(floatingView, wmParams);
    }

    // 关闭悬浮窗
    private void closeFloatingWindow() {
        if (mWindowManager != null && floatingView != null) {
            if (floatingView.isAttachedToWindow()) {
                mWindowManager.removeView(floatingView);
            }
        }
        if (xunFeiXTTS != null) {
            xunFeiXTTS.stopTTS();
        }
        if (mIat != null) {
            if (mIat.isListening()) {
                mIat.stopListening();
                mIat.cancel();
            }
        }
    }

    // 检查点是否在视图内
    private boolean isPointInsideView(float x, float y, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        int viewX = location[0];
        int viewY = location[1];

        return (x >= viewX && x <= (viewX + view.getWidth()) &&
                (y >= viewY && y <= (viewY + view.getHeight())));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showTip(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * 释放连接
     */
    @Override
    public void onDestroy() {
        unbindService(mConnection);
        super.onDestroy();
    }

    private void initXunFei() {
        //初始化识别无UI识别对象
        //使用SpeechRecognizer对象，可根据回调消息自定义界面；
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, resultType);

        if (language.equals("zh_cn")) {
            String lag = mSharedPreferences.getString("iat_language_preference", "mandarin");
            Log.e(TAG, "language:" + language);// 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);
        } else {
            mIat.setParameter(SpeechConstant.LANGUAGE, language);
        }
        Log.e(TAG, "last language:" + mIat.getParameter(SpeechConstant.LANGUAGE));

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "2000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "2000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));
    }

    private final RecognizerListener mRecogListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {

        }

        @Override
        public void onBeginOfSpeech() {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean isLast) {
            String result = printResult(recognizerResult);
            Log.e(TAG, "onResult: " + isLast + ":: result: " + result);
            if (isLast) {
                if (!TextUtils.isEmpty(result) && !"。".equals(result)) {
                    voiceText.setText(result);
                    Intent intent = new Intent();
                    SystemPropertiesReflection.set("persist.sys.yl.text", result);
                    intent.setComponent(new ComponentName("com.yl.deepseekxunfei", "com.yl.deepseekxunfei.MainActivity"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    if (mWindowManager != null) {
                        if (mFloatingLayout != null) {
                            if (floatingView.isAttachedToWindow()) {
                                mWindowManager.removeView(floatingView);
                            }
                        }
                    }
                } else {
                    //如果窗口拉起来了才会进行播报和文字显示
                    if (mFloatingLayout != null && mFloatingLayout.isAttachedToWindow()) {
                        voiceText.setText("未识别到文字，请稍后尝试");
                        xunFeiXTTS.startTTS("未识别到文字，请稍后尝试");
                        isHideView = true;
                    }
                }
                isOnlineRecognize = false;
            } else {
                if (!TextUtils.isEmpty(result) && !"。".equals(result)) {
                    if (mFloatingLayout != null && mFloatingLayout.isAttachedToWindow()) {
                        voiceText.setText(result);
                    }
                }
            }
        }

        @Override
        public void onError(SpeechError speechError) {
            xunFeiXTTS.stopTTS();
            xunFeiXTTS.startTTS(speechError.getErrorDescription());
            voiceText.setText(speechError.getErrorDescription());
            isHideView = true;
            Log.e(TAG, "onError: " + speechError.getErrorDescription());
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    public void sendBroadCast(String action, String... extra) {
        Intent broadcastIntent = new Intent(action);
        if (extra.length % 2 == 0) {
            for (int i = 0; i < extra.length; i = i + 2) {
                broadcastIntent.putExtra(extra[i], extra[i + 1]);
            }
        }
        sendBroadcast(broadcastIntent);
    }

    // 读取动态修正返回结果示例代码
    private String printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        String pgs = null;
        String rg = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
            pgs = resultJson.optString("pgs");
            rg = resultJson.optString("rg");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //如果pgs是rpl就在已有的结果中删除掉要覆盖的sn部分
        if (pgs.equals("rpl")) {
            String[] strings = rg.replace("[", "").replace("]", "").split(",");
            int begin = Integer.parseInt(strings[0]);
            int end = Integer.parseInt(strings[1]);
            for (int i = begin; i <= end; i++) {
                mIatResults.remove(i + "");
            }
        }

        mIatResults.put(sn, text);
        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        Log.e(TAG, "printResult: " + resultBuffer.toString());
        return resultBuffer.toString();
    }

    /**
     * 初始化监听器。
     */
    private final InitListener mInitListener = code -> {

        if (code != ErrorCode.SUCCESS) {
            showTip("初始化失败，错误码：" + code + ",请联系开发人员解决方案");
        }
    };

    //判断是否已注册声纹
    public boolean createFig() {
        if (createLogotype.getGroupId() != null) {
            Log.d(TAG, "roomDial: " + createLogotype.getGroupId().size());
            Log.d(TAG, "roomDial: " + createLogotype.getGroupId());
            if (createLogotype.getGroupId().size() > 1) {
                for (int j = 0; j < createLogotype.getGroupId().size(); j++) {
                    result[0] = SearchFeature.doSearchFeature(CreateRequestUrl, CreateAPP_ID, CreateAPISecret, CreateAPIKey, contrastFies, createLogotype, createLogotype.getGroupId().get(j));//1:N比对
                    if (result[0] != null) {
                        if (result[0].get("score") != null) {
                            if (Double.parseDouble(Objects.requireNonNull(result[0].get("score"))) >= 0.35) {
                                fig = false;
                                break;
                            }
                        } else {
                            showTip("音频数据无效，请稍后再试");
                        }
                    } else {
                        showTip("没有匹配的声纹信息，请注册1：N");
                        fig = true;
                    }
                }
            } else if (createLogotype.getGroupId().size() == 1) {
                for (int j = 0; j < createLogotype.getGroupId().size(); j++) {
                    for (int k = 0; k < createLogotype.getFeatureId().size(); k++) {
                        SearchOneFeatureList[0] = SearchOneFeature.doSearchOneFeature(CreateRequestUrl, CreateAPP_ID, CreateAPISecret, CreateAPIKey, contrastFies, createLogotype, createLogotype.getGroupId().get(j), createLogotype.getFeatureId().get(k));//1:1
                        if (SearchOneFeatureList[0] != null && SearchOneFeatureList[0].get("score") != null) {
                            if (Double.parseDouble(Objects.requireNonNull(SearchOneFeatureList[0].get("score"))) >= 0.35) {
                                fig = false;
                            }
                        } else {
                            showTip("没有匹配的声纹信息，请注册1:1");
                            fig = true;
                        }
                    }
                }
            }
        } else {
            showTip("请先注册声纹信息");
            fig = true;
        }
        createLogotype.clear();
        return fig;
    }


    /**
     * 判断当前网络是否可用(6.0以上版本)
     * 实时
     *
     * @return
     */
    private boolean isNetSystemUsable() {
        boolean isNetUsable = false;
        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NetworkCapabilities networkCapabilities =
                    manager.getNetworkCapabilities(manager.getActiveNetwork());
            if (networkCapabilities != null) {
                isNetUsable = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
        }
        Log.e(TAG, "isNetSystemUsable: " + isNetUsable);
        return isNetUsable;
    }

    public boolean seleteCrete() {
        // 检查groupId是否存在
        if (createLogotype.getGroupId() != null && !createLogotype.getGroupId().isEmpty()) {
            // 用于存储所有结果的列表
            final List<JSONObject> allResults = new ArrayList<>();
            // 记录groupId总数和已完成的查询数
            final int totalGroups = createLogotype.getGroupId().size();
            final AtomicInteger completedGroups = new AtomicInteger(0);

            // 遍历所有groupId进行查询
            for (int j = 0; j < totalGroups; j++) {
                final String groupId = createLogotype.getGroupId().get(j);
                QueryFeatureList.doQueryFeatureList(CreateRequestUrl, CreateAPP_ID, CreateAPISecret, CreateAPIKey, createLogotype, groupId,
                        new QueryFeatureList.NetCall() {
                            @Override
                            public void OnSuccess(String success) {
                                try {
                                    JSONArray jsonArray = new JSONArray(success);
                                    // 将当前groupId的结果添加到总结果列表
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject item = jsonArray.getJSONObject(i);
                                        item.put("groupId", groupId); // 保存groupId到结果中
                                        allResults.add(item);
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "解析JSON失败", e);
                                }
                            }

                            @Override
                            public void OnError() {
                                // 即使部分查询失败，也继续处理已有的结果
                                if (completedGroups.incrementAndGet() == totalGroups) {

                                }
                            }
                        });
                Log.d(TAG, "seleteCrete: " + createLogotype);
            }
            return true;
        } else {
            Log.d(TAG, "seleteCrete: 暂无声纹信息");
            showTip("暂无声纹信息！");
            return false;
        }
    }


    private void judgeRecognizeAndStart() {
        mHandler.postDelayed(RecognizeRunnable, 1000);
    }

    private final int NO_SPEED_TIME_THREESHOLDER = 5;
    private int countTime = NO_SPEED_TIME_THREESHOLDER;

    Runnable RecognizeRunnable = new Runnable() {
        @Override
        public void run() {
            AsrModel lastAsrModel = xunFeiAsr.getLastAsrModel();
            if (lastAsrModel != null) {
                if (lastAsrModel.isEnd()) {
                    countTime = NO_SPEED_TIME_THREESHOLDER;
                } else {
                    if (countTime <= 0) {
                        xunFeiAsr.end("true;1000");
                        countTime = NO_SPEED_TIME_THREESHOLDER;
                    } else {
                        countTime--;
                    }
                }
            }
//            Log.e(TAG, "run: " + xunFeiAsr.isIsRecognize());
            mHandler.postDelayed(this, 1000);
        }
    };

    public static void inputKeyEvent(int key) {
        try {
            String keyCommand = "input keyevent = " + key;
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(keyCommand);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startMonitoring() {
        audioManager.registerAudioDeviceCallback(new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                checkMusicState();
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                checkMusicState();
            }
        }, mHandler);

        // 定期检查音乐状态
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkMusicState();
                mHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void checkMusicState() {
        boolean newState = audioManager.isMusicActive();
        if (newState != isMusicPlaying) {
            isMusicPlaying = newState;
        }
    }

}
