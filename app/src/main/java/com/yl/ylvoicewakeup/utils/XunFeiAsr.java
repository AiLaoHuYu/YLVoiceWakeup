package com.yl.ylvoicewakeup.utils;

import android.Manifest;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.iflytek.aikit.core.AiAudio;
import com.iflytek.aikit.core.AiHandle;
import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.AiListener;
import com.iflytek.aikit.core.AiRequest;
import com.iflytek.aikit.core.AiResponse;
import com.iflytek.aikit.core.AiStatus;
import com.yl.ylvoicewakeup.model.AsrModel;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class XunFeiAsr {

    private final String TAG = "XunFeiAsr";
    private static final String ABILITYID = "e75f07b62";
    private AtomicBoolean isEnd = new AtomicBoolean(true);
    private AtomicBoolean isStartRecord = new AtomicBoolean(false);
    private String fsaPath;
    private AudioRecord audioRecord = null;
    //录音缓冲区大小
    private final int BUFFER_SIZE = 1280;
    //录音是否在进行
    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private boolean isLoadData = false;
    private AiHandle aiHandle;
    private int languageType = 0;
    private static final int START = 0x0001;
    private static final int WRITE_BY_RECORDING = 0x0002;
    private static final int END = 0x0004;
    private boolean engineInit = false;
    private Handler mHandler;
    private ResultCallBack resultCallBack;
    private List<AsrModel> asrModels = new ArrayList<>();
    int index = 0;

    public XunFeiAsr(Context context, ResultCallBack resultCallBack) {
        this.resultCallBack = resultCallBack;
    }

    public void initAsr() {
        AiHelper.getInst().registerListener(ABILITYID, edListener);
        initEngine();
        mThread.start();
    }

    /**
     * 引擎初始化
     */
    private void initEngine() {
        int ret = 0;
        if (!engineInit) {
            AiRequest.Builder engineBuilder = AiRequest.builder();
            engineBuilder.param("decNetType", "fsa");
            engineBuilder.param("punishCoefficient", 0.0);
            engineBuilder.param("wfst_addType", languageType); // 0中文，1英文
            ret = AiHelper.getInst().engineInit(ABILITYID, engineBuilder.build());
            if (ret != 0) {
                Log.i(TAG, "engineInit 失败!" + ret);
                return;
            }
            Log.i(TAG, "ESR engineInit  成功：" + ret);
            engineInit = true;
        }
    }

    public void startRecognize() {
        isStartRecord.set(false);
        Message msg = new Message();
        msg.what = START;
        msg.arg1 = WRITE_BY_RECORDING;
        mHandler.sendMessage(msg);
    }

    public void endRecognize() {
        Log.e(TAG, "endRecognize");
        Message msg = new Message();
        msg.what = WRITE_BY_RECORDING;
        msg.obj = AiStatus.END;
        mHandler.sendMessage(msg);
    }

    /**
     * ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓SDK处理逻辑↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
     **/
    private Thread mThread = new Thread(new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler(Looper.myLooper()) {

                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case START:
                            /**
                             * 开始会话
                             */
                            Log.d(TAG, "START");
                            int ret = start();
                            if (ret == 0) {
                                int type = msg.arg1;
                                if (type == WRITE_BY_RECORDING) {
                                    createAudioRecord();//创建录音器
                                    isRecording.set(true);
                                    audioRecord.startRecording();//录音器启动录音
                                }
                                mHandler.removeCallbacksAndMessages(null);//清空消息队列
                                Message write_msg = new Message();
                                write_msg.what = type;
                                write_msg.obj = AiStatus.BEGIN;
                                mHandler.sendMessage(write_msg);//调用write方法送音频数据给引擎
                            }
                            break;
                        case WRITE_BY_RECORDING:
                            /**
                             * 写入数据-录音方式
                             */
                            AiStatus status = (AiStatus) msg.obj;
                            byte data[] = new byte[BUFFER_SIZE];
                            int read = audioRecord.read(data, 0, BUFFER_SIZE);
                            if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                                //处理录音数据
                                write(data, status);//送尾帧
                            }
                            if (AiStatus.END == status) {
                                if (audioRecord != null) {
                                    audioRecord.stop();
                                    isRecording.set(false);
                                }
                            } else {
                                if (isRecording.get()) {
                                    Message write_msg = new Message();
                                    write_msg.what = WRITE_BY_RECORDING;
                                    write_msg.obj = AiStatus.CONTINUE;
                                    mHandler.sendMessage(write_msg);//调用write方法送音频数据给引擎
                                }
                            }
                            break;
                        case END:
                            /**
                             * 结束会话
                             */
                            Log.d(TAG, "END");
                            String isNeedToStart = (String) msg.obj;
                            end(isNeedToStart);
                            break;
                    }
                }
            };
            Looper.loop();
        }
    });

    /**
     * 创建录音器
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void createAudioRecord() {
        if (isRecording.get()) {
            return;
        }
        if (audioRecord == null) {
            Log.d(TAG, "createAudioRecord");
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    BUFFER_SIZE);
        }
    }

    public interface ResultCallBack {
        String onResultAction(String result);

        void onResultText(String text, boolean isEnd);
    }

    /**
     * 开始会话
     */
    private int start() {
        unLoadData();
        if (!isLoadData) {//加载个性化资源至缓存。个性化资源如果没有变动的话，仅需加载一次，不用每次start都去加载。如果变动了则需要先卸载(调用unLoadData)，再重新加载
            /*****
             * FSA:fsa命令词文件
             */
            fsaPath = "/sdcard/iflytek/esr/fsa/cn_fsa.txt";
            AiRequest.Builder customBuilder = AiRequest.builder();
            customBuilder.customText("FSA", fsaPath, index);
            int ret = AiHelper.getInst().loadData(ABILITYID, customBuilder.build());
            if (ret != 0) {
                Log.d(TAG, "open esr loadData 失败：" + ret);
                return ret;
            }
            isLoadData = true;
        }
        int ret = 0;
        int[] indexs = {index};
        ret = AiHelper.getInst().specifyDataSet(ABILITYID, "FSA", indexs);//从缓存中把个性化资源设置到引擎中
        if (ret != 0) {
            Log.d(TAG, "open esr specifyDataSet 失败：" + ret);
            return ret;
        }
        Log.d(TAG, "open esr specifyDataSet  success：" + ret);
        AsrModel asrModel = new AsrModel();
        asrModel.setEnd(false);
        asrModels.add(asrModel);
        AiRequest.Builder paramBuilder = AiRequest.builder();
        paramBuilder.param("languageType", languageType);//0:中文, 1:英文
        paramBuilder.param("vadEndGap", 60);//子句分割时间间隔，中文建议60，英文建议75
        paramBuilder.param("vadOn", true);//vad开关
        paramBuilder.param("beamThreshold", 20);//解码控制beam的阈值，中文建议20，英文建议25
        paramBuilder.param("hisGramThreshold", 3000);//解码Gram阈值，建议值3000
        paramBuilder.param("vadLinkOn", false);//vad子句连接开关
        paramBuilder.param("vadSpeechEnd", 80);//vad后端点
        paramBuilder.param("vadResponsetime", 1000);//vad前端点
        paramBuilder.param("postprocOn", false);//后处理开关
        isEnd.set(false);
        aiHandle = AiHelper.getInst().start(ABILITYID, paramBuilder.build(), null);
        if (aiHandle.getCode() != 0) {
            Log.d(TAG, "open esr start失败：" + aiHandle.getCode());
            return aiHandle.getCode();
        }
        return 0;
    }

    /**
     * 卸载资源
     */
    private void unLoadData() {
        int ret = 0;
        if (isLoadData) {
            ret = AiHelper.getInst().unLoadData(ABILITYID, "FSA", index);
            if (ret != 0) {
                Log.d(TAG, "esr unloadData  失败: " + ret);
            } else {
                Log.d(TAG, "esr unloadData  成功");
                isLoadData = false;
            }
        }
    }

    /**
     * 写入数据
     */
    private void write(byte[] part, AiStatus status) {
        if (isEnd.get()) {
            return;
        }
        AiRequest.Builder dataBuilder = AiRequest.builder();
        int ret = 0;
        /**
         * 送入音频需要标识音频的状态，第一帧为起始帧，status要传AiStatus.BEGIN,最后一帧为结束帧，status要传AiStatus.END,其他为中间帧，status要传AiStatus.CONTINUE
         * 音频要求16bit，16K，单声道的pcm音频。
         * 建议每次发送音频间隔40ms，每次发送音频字节数为一帧音频大小的整数倍。
         */
        AiAudio aiAudio = AiAudio.get("audio").data(part).status(status).valid();
        dataBuilder.payload(aiAudio);

        ret = AiHelper.getInst().write(dataBuilder.build(), aiHandle);
        if (ret == 0) {
            ret = AiHelper.getInst().read(ABILITYID, aiHandle);
            if (ret != 0) {
                Log.d(TAG, "read失败：" + ret + " ");
            }
        } else {
            Log.d(TAG, "write失败：" + ret + " ");
        }
    }

    /**
     * 结束会话
     */
    public void end(String isNeedToStart) {
        if (!isEnd.get()) {
            int ret = AiHelper.getInst().end(aiHandle);
            if (ret == 0) {
                isEnd.set(true);
                aiHandle = null;
                Log.d(TAG, "识别完成，end： " + ret);
            } else {
                isEnd.set(false);
                Log.d(TAG, "识别失败，end： " + ret);
            }
        }
        String[] split = isNeedToStart.split(";");
        if ("true".equals(split[0])) {
            mHandler.postDelayed(this::startRecognize, Long.parseLong(split[1]));
        }
    }

    /**
     * 能力监听回调
     */
    private AiListener edListener = new AiListener() {
        @Override
        public void onResult(int handleID, List<AiResponse> outputData, Object usrContext) {
            String isNeedToStartRecognize = "true;200";
            if (null != outputData && outputData.size() > 0) {
                Log.i(TAG, "onResult:handleID:" + handleID + ":" + outputData.size() + "," +
                        "usrContext:" + usrContext);
                AsrModel asrModel = getLastAsrModel();
                asrModel.setHandleId(String.valueOf(handleID));
                asrModel.setEnd(true);
                for (int i = 0; i < outputData.size(); i++) {
                    Log.d(TAG, "onResult:handleID:" + handleID + ":: outputData: " + outputData.get(i).getKey());
                    String result = null;
                    /**
                     * key的取值以及含义
                     * pgs:progressive格式的结果，即可以实时刷屏
                     * htk:带有分词信息的结果，每一个分词结果占一行
                     * plain:类比于htk，把一句话结果中的所有分词拼成完整一句，若有后处理，则也含有后处理的结果信息，plain是每一段话的最终结果
                     * vad:语音端点检测结果(需要打开vad功能才会返回)bg:前端点，ed:后端点。单位:帧(10ms)
                     * readable:json格式的结果。
                     */
                    String key = outputData.get(i).getKey();   //引擎结果的key
                    byte[] bytes = outputData.get(i).getValue(); //识别结果
                    try {
                        result = new String(bytes, "GBK");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    asrModel.setAsrText(result);
                    Log.d(TAG, "key: " + key + "::result: " + result);

                    if (key.contains("pgs")) {
                        Log.d(TAG, "pgs: " + key + ": \n " + result);
                        if (resultCallBack != null) {
                            resultCallBack.onResultText(result, false);
                        }
                    } else if (key.contains("plain")) {
                        Log.d(TAG, "plain: " + key + ": \n " + result);
                        if (resultCallBack != null) {
                            resultCallBack.onResultText(result, true);
                            isNeedToStartRecognize = resultCallBack.onResultAction(result);
                        }
                    }
                }
                if (outputData.size() > 0 && outputData.get(0).getStatus() == 2) {
                    if (aiHandle != null) {
                        mHandler.removeCallbacksAndMessages(null);
                        Message msg = new Message();
                        msg.what = END;
                        msg.obj = isNeedToStartRecognize;
                        mHandler.sendMessage(msg);
                    }
                    if (audioRecord != null) {
                        audioRecord.stop();
                        audioRecord.release();
                        audioRecord = null;
                        isRecording.set(false);
                    }
                }
            }
        }

        @Override
        public void onEvent(int i, int i1, List<AiResponse> list, Object o) {
            Log.i(TAG, "onEvent:" + i + ",event:" + i1);

        }

        @Override
        public void onError(int i, int i1, String s, Object o) {
            Log.d(TAG, "错误通知，能力执行终止,Ability " + i + " ERROR::" + s + ",err code:" + i1);
        }
    };

    public AsrModel getLastAsrModel() {
        if (!asrModels.isEmpty()) {
            return asrModels.get(asrModels.size() - 1);
        }
        return null;
    }

}
