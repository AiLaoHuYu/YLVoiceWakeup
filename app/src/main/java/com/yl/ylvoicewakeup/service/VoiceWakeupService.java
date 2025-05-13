package com.yl.ylvoicewakeup.service;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.service.credentials.CreateEntry;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;

import com.yl.creteEntity.crete.roomCrete.entity.creteEntity;
import com.yl.creteEntity.crete.roomCrete.entity.roomDIal;
import com.yl.ylvoicewakeup.R;
import com.yl.ylvoicewakeup.creteVoice.CreateLogotype;
import com.yl.ylvoicewakeup.creteVoice.QueryFeatureList;
import com.yl.ylvoicewakeup.creteVoice.SearchFeature;
import com.yl.ylvoicewakeup.creteVoice.SearchOneFeature;
import com.yl.ylvoicewakeup.utils.AnimatorUtil;
import com.yl.ylvoicewakeup.utils.CommonUtil;
import com.yl.ylvoicewakeup.utils.CreteUtlis;
import com.yl.ylvoicewakeup.utils.JsonParser;
import com.yl.ylvoicewakeup.utils.PcmUtils;
import com.yl.ylvoicewakeup.utils.SystemPropertiesReflection;
import com.yl.ylvoicewakeup.utils.TenSecondsOfAudio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class VoiceWakeupService extends Service {

    private int curThresh = 1450;
    // 语音唤醒对象
    private VoiceWakeuper mIvw;
    private SpeechRecognizer mIat;// 语音听写对象
    private SpeechSynthesizer mTts;
    private String keep_alive = "1";
    private String ivwNetMode = "0";
    private final String APP_ID = "c7af7320";
    private String mEngineType = SpeechConstant.TYPE_CLOUD;// 引擎类型
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams wmParams;
    private View mFloatingLayout;
    private ImageView voiceImg;
    private TextView voiceText;
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
    private boolean mttsFig = false;
    private roomDIal roomDIal;
    public TenSecondsOfAudio tenSecondsOfAudio;
    public PcmUtils pcmUtils;
    public ServiceConnection mConnection;


    @Override
    public void onCreate() {
        super.onCreate();
        //  开放平台注册的APPID
        SpeechUtility.createUtility(getApplication(), SpeechConstant.APPID + "=c7af7320");
        // 初始化唤醒对象
        mIvw = VoiceWakeuper.createWakeuper(this, null);
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
        mTts = SpeechSynthesizer.createSynthesizer(this, mInitListener);
        mSharedPreferences = getSharedPreferences("ASR", Activity.MODE_PRIVATE);
        animatorUtil = new AnimatorUtil();
        mHandler = new Handler(Looper.getMainLooper());
        mConnection  = new ServiceConnection() {
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
        Log.d(TAG, "onCreate: "+mConnection);
        initWindow();
    }

    private void initWindow() {
        //初始话声纹识别必须参数
        CreateAPP_ID = "27b3a946";
        CreateAPISecret = "MGNhOTM2Yjg3MmVhMTFjYzhhODQzMTYw";
        CreateAPIKey = "06224092793087296b1f47c96e0133bc";
        CreateRequestUrl = "https://api.xf-yun.com/v1/private/s782b4996";
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wmParams = new WindowManager.LayoutParams();
        wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        wmParams.format = PixelFormat.TRANSLUCENT;
        wmParams.gravity = Gravity.START | Gravity.TOP;
        wmParams.x = 100;
        wmParams.y = 50;
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initXunFei();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private WakeuperListener mWakeuperListener = new WakeuperListener() {

        @Override
        public void onResult(WakeuperResult result) {
            if (!"1".equalsIgnoreCase(keep_alive)) {

            }
            try {
                String text = result.getResultString();
                JSONObject object;
                object = new JSONObject(text);
                StringBuffer buffer = new StringBuffer();
                buffer.append("【RAW】 " + text);
                buffer.append("\n");
                buffer.append("【操作类型】" + object.optString("sst"));
                buffer.append("\n");
                buffer.append("【唤醒词id】" + object.optString("id"));
                buffer.append("\n");
                buffer.append("【得分】" + object.optString("score"));
                buffer.append("\n");
                buffer.append("【前端点】" + object.optString("bos"));
                buffer.append("\n");
                buffer.append("【尾端点】" + object.optString("eos"));
                resultString = buffer.toString();
            } catch (JSONException e) {
                resultString = "结果解析出错";
                e.printStackTrace();
            }
            Log.e(TAG, "onResult: " + resultString);
            if ("com.yl.deepseekxunfei".equals(CommonUtil.getForegroundActivity(getApplicationContext()))) {
                sendBroadCast(ACTION);
            } else {
                mttsFig = false;
                mTts.startSpeaking("我在，请问有什么吩咐", mSynthesizerListener);
                LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
                mFloatingLayout = inflater.inflate(R.layout.voice_layout, null);
                mFloatingLayout.setBackgroundResource(R.drawable.voice_bg);
                voiceImg = mFloatingLayout.findViewById(R.id.voice_img);
                voiceText = mFloatingLayout.findViewById(R.id.voice_text);
                mWindowManager.addView(mFloatingLayout, wmParams);
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

        @Override
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));

        }

        @Override
        public void onBeginOfSpeech() {
        }

        @Override
        public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {
            switch (eventType) {
                // EVENT_RECORD_DATA 事件仅在 NOTIFY_RECORD_DATA 参数值为 真 时返回
                case SpeechEvent.EVENT_RECORD_DATA:
                    final byte[] audio = obj.getByteArray(SpeechEvent.KEY_EVENT_RECORD_DATA);
                    Log.i(TAG, "ivw audio length: " + audio.length);
                    break;
            }
        }

        @Override
        public void onVolumeChanged(int volume) {

        }
    };

    private void showTip(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * 释放连接
     */
    @Override
    public void onDestroy() {
        if (mIvw != null) {
            mIvw.cancel();
            mIvw.destroy();
        }
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
        mIvw = VoiceWakeuper.getWakeuper();
        if (mIvw != null) {
            // 清空参数
            mIvw.setParameter(SpeechConstant.PARAMS, null);
            // 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
            mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + curThresh);
            // 设置唤醒模式
            mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
            // 设置持续进行唤醒
            mIvw.setParameter(SpeechConstant.KEEP_ALIVE, keep_alive);
            // 设置闭环优化网络模式
            mIvw.setParameter(SpeechConstant.IVW_NET_MODE, ivwNetMode);
            // 设置唤醒资源路径
            mIvw.setParameter(SpeechConstant.IVW_RES_PATH, getResource());
            // 设置唤醒录音保存路径，保存最近一分钟的音频
            String ivwPath = getExternalFilesDir("msc").getAbsolutePath() + "/ivw.wav";
            Log.d("IVW_PATH", "唤醒音频保存路径: " + ivwPath);
            mIvw.setParameter(SpeechConstant.IVW_AUDIO_PATH, ivwPath);
            mIvw.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
            // 如有需要，设置 NOTIFY_RECORD_DATA 以实时通过 onEvent 返回录音音频流字节
            //mIvw.setParameter( SpeechConstant.NOTIFY_RECORD_DATA, "1" );
            // 启动唤醒
            /*	mIvw.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");*/

            mIvw.startListening(mWakeuperListener);
        } else {
            showTip("唤醒未初始化");
        }
        String deepseekVoiceSpeed = SystemPropertiesReflection.get("deepseek_voice_speed", "50");
        String deepseekVoicespeaker = SystemPropertiesReflection.get("deepseek_voice_speaker", "aisjiuxu");
        if (deepseekVoicespeaker.equals("许久")) {
            deepseekVoicespeaker = "aisjiuxu";
        } else if (deepseekVoicespeaker.equals("小萍")) {
            deepseekVoicespeaker = "aisxping";
        } else if (deepseekVoicespeaker.equals("小婧")) {
            deepseekVoicespeaker = "aisjinger";
        } else if (deepseekVoicespeaker.equals("许小宝")) {
            deepseekVoicespeaker = "aisbabyxu";
        } else if (deepseekVoicespeaker.equals("小燕")) {
            deepseekVoicespeaker = "xiaoyan";
        }

        String deepseekFontSize = SystemPropertiesReflection.get("deepseek_font_size", "20dp");
        String deepseekFontColor = SystemPropertiesReflection.get("deepseek_font_color", "黑色");
        String deepseekBackgroundColor = SystemPropertiesReflection.get("deepseek_background_color", "白色");

        mTts.setParameter(SpeechConstant.PARAMS, null);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端
        mTts.setParameter(SpeechConstant.VOICE_NAME, deepseekVoicespeaker);//设置发音人
        mTts.setParameter(SpeechConstant.SPEED, deepseekVoiceSpeed);//设置语速
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, "50");//设置音高
        mTts.setParameter(SpeechConstant.VOLUME, "100");//设置音量，范围0~100
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
    }

    private final SynthesizerListener mSynthesizerListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
            animatorUtil.startJumpAnimation(voiceImg);
        }

        @Override
        public void onBufferProgress(int i, int i1, int i2, String s) {

        }

        @Override
        public void onSpeakPaused() {

        }

        @Override
        public void onSpeakResumed() {

        }

        @Override
        public void onSpeakProgress(int i, int i1, int i2) {

        }

        @Override
        public void onCompleted(SpeechError speechError) {
            animatorUtil.stopJumpAnimation(voiceImg);
            if (isHideView) {
                mHandler.postDelayed(() -> {
                    if (mWindowManager != null) {
                        if (mFloatingLayout != null) {
                            mWindowManager.removeView(mFloatingLayout);
                        }
                    }
                }, 1000);
                isHideView = false;
            } else {
                //开始识别，并设置监听器
                mIat.startListening(mRecogListener);
            }
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

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
                    Intent intent = new Intent();
                    SystemPropertiesReflection.set("persist.sys.yl.text", result);
                    intent.setComponent(new ComponentName("com.yl.deepseekxunfei", "com.yl.deepseekxunfei.MainActivity"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    if (mWindowManager != null) {
                        if (mFloatingLayout != null) {
                            mWindowManager.removeView(mFloatingLayout);
                        }
                    }
                } else {
                    if (!mttsFig) {
                        voiceText.setText("未识别到文字，请稍后尝试");
                        mTts.startSpeaking("未识别到文字，请稍后尝试", mSynthesizerListener);
                        isHideView = true;
                    } else {
                        // 延迟3秒后移除悬浮窗
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mFloatingLayout != null && mFloatingLayout.isAttachedToWindow()) {
                                    mWindowManager.removeView(mFloatingLayout);
                                }
                            }
                        }, 3000);
                        isHideView = true;
                    }
                }
            }
        }

        @Override
        public void onError(SpeechError speechError) {
            mTts.stopSpeaking();
            mTts.startSpeaking(speechError.getErrorDescription(), mSynthesizerListener);
            voiceText.setText(speechError.getErrorDescription());
            isHideView = true;
            Log.e(TAG, "onError: " + speechError.getErrorDescription());
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    public void sendBroadCast(String action) {
        Intent broadcastIntent = new Intent(action);
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

    private String getResource() {
        final String resPath = ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + APP_ID + ".jet");
        Log.d(TAG, "resPath: " + resPath);
        return resPath;
    }

    //判断是否已注册声纹
    public boolean createFig() {
        if (createLogotype.getGroupId() != null) {
            Log.d(TAG, "roomDial: "+createLogotype.getGroupId().size());
            Log.d(TAG, "roomDial: "+createLogotype.getGroupId());
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
}
