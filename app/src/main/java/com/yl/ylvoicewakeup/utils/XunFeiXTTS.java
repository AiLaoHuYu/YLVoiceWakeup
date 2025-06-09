package com.yl.ylvoicewakeup.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.iflytek.aikit.core.AeeEvent;
import com.iflytek.aikit.core.AiHandle;
import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.AiInput;
import com.iflytek.aikit.core.AiListener;
import com.iflytek.aikit.core.AiRequest;
import com.iflytek.aikit.core.AiResponse;
import com.iflytek.aikit.core.AiText;
import com.yl.ylvoicewakeup.R;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class XunFeiXTTS {

    private final String TAG = "XunFeiXTTS";
    private String ABILITYID = "e2e44feff";
    XTTSParams xttsParams = new XTTSParams();
    private AiHandle aiHandle;
    private String OUTPUT_DIR;
    private String outFileName;
    private int AEE_END = 1001;

    public void init(Context context){
        OUTPUT_DIR = context.getResources().getString(R.string.workDir)+ "xtts" + File.separator + "output";
        AiHelper.getInst().registerListener(ABILITYID, aiRespListener);// 注册能力结果监听
        deleteAllOutPutDir();
    }

    public void stopTTS(){
        AudioTrackManager.getInstance().stopPlay();
    }

    public void startTTS(String text){
        if (text != null && !"".equals(text)) {
            try {
                outFileName = String.valueOf(System.currentTimeMillis());
                int result = runTTS(xttsParams,
                        new String(text.getBytes(), "utf-8"));
                if (result != ErrorCode.SUCCESS) {

                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {

        }
    }

    /**
     * 删除本地缓存的合成结果音频文件
     */
    private void deleteAllOutPutDir() {
        File dir = new File(OUTPUT_DIR);
        if (dir == null || !dir.exists() || !dir.isDirectory() || dir.listFiles() == null)
            return;
        for (File file : dir.listFiles()) {
            if (file.isFile())
                file.delete(); // 删除所有文件
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * 创建本地缓存的合成结果音频文件
     */
    private void makeOutPutDir() {
        File dir = new File(OUTPUT_DIR);
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * 获取合成结果音频文件路径
     */
    private String getOutFileName(int handleID) {
        return OUTPUT_DIR + File.separator + "OutPut_" + outFileName + ".pcm";
    }

    /**↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓SDK处理逻辑↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓**/
    /**
     * 结果监听回调
     */
    private AiListener aiRespListener = new AiListener(){

        @Override
        public void onResult(int handleID, List<AiResponse> list, Object usrContext) {
            if (null != list && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    byte[] bytes = list.get(i).getValue();
                    if (bytes == null) {
                        continue;
                    }
                    Log.d(TAG,"onResult:handleID:" + handleID + ":" + list.get(i).getKey());
                    makeOutPutDir();
                    FileUtils.writeFile(getOutFileName(handleID), bytes);
                }
            }
        }

        @Override
        public void onEvent(int handleID, int event, List<AiResponse> eventData, Object usrContext) {
            if (event == AeeEvent.AEE_EVENT_END.getValue()) {  // 引擎计算开始事件
                handler.sendEmptyMessage(AEE_END);
            } else if (event == AeeEvent.AEE_EVENT_PROGRESS.getValue()) {  // 引擎计算进度事件
                int pos = -1;
                int len = -1;
                for (int i = 0; i < eventData.size(); i++) {
                    AiResponse aiOutput = eventData.get(i);
                    if (aiOutput.getKey().equals("progress_pos")) {
                        pos = NumberUtils.bytesToInt(aiOutput.getValue());
                    } else if (aiOutput.getKey().equals("progress_len")) {
                        len = NumberUtils.bytesToInt(aiOutput.getValue());
                    }
                }
                Log.d(TAG, "handleID:" + handleID + "进度:" + pos + "/" + len);
            }
        }

        @Override
        public void onError(int handleID, int err, String msg, Object usrContext) {
            String errInfo = "能力运行出错，错误码:"+err+",错误信息:"+msg;
            Log.d(TAG, errInfo);
        }
    };

    public void end(){
        handler.sendEmptyMessage(AEE_END);
    }


    /**
     * 开始合成
     */
    public int runTTS(XTTSParams params, String text) {
        AiInput.Builder paramBuilder = AiInput.builder();
        paramBuilder.param("vcn", params.vcn); //必填参数,发音人：xiaoyan:中文 女 晓燕；xiaofeng:中文 男 晓峰；catherine:英文 女 catherine
        paramBuilder.param("language", params.language);//必填参数,语种：1:中文, 2:英文, 3:法语, 5:日语, 6:俄语, 9:德语, 15:意大利语, 16:韩语, 23:西班牙语, 48:阿拉伯语, 50:阿拉伯语（Eg）, 12:粤语, 8:印地语, 27:泰语
        paramBuilder.param("textEncoding","UTF-8");//必填参数,文本编码：GBK:GBK编码, UTF-8:UTF-8编码, Unicode:Unicode编码
        paramBuilder.param("pitch",params.pitch);//非必填,语调：最小值:0, 最大值:100
        paramBuilder.param("volume",params.volume);//非必填,音量：最小值:0, 最大值:100
        paramBuilder.param("speed",params.speed);//非必填,语速：最小值:0, 最大值:100
        //其他功能参数，请参考集成文档。
        aiHandle = AiHelper.getInst().start(ABILITYID, paramBuilder.build(), null);
        if (aiHandle.getCode() != 0) {
            String errinfo = "start失败:" + aiHandle.getCode();
            Log.d(TAG, errinfo);
            aiHandle = null;
            return ErrorCode.START_ERROR;
        }
        Log.d(TAG, "aiHandle.i:" + aiHandle.getI() + "," + aiHandle.getId());


        AiRequest.Builder dataBuilder = AiRequest.builder();
        //输入文本数据
        AiText input = AiText
                .get("text")
                .data(text) //text为String类型输入文本
                .valid();
        dataBuilder.payload(input);

        int ret = AiHelper.getInst().write(dataBuilder.build(), aiHandle);
        //ret 值为0 写入成功；非0失败，请参照文档中错误码部分排查
        if (ret != 0) {
            String errinfo = "write失败:" + ret;
            Log.d(TAG, errinfo);
            aiHandle = null;
            return ErrorCode.WRITE_ERROR;
        }
        return ErrorCode.SUCCESS;
    }

    private Handler handler = new Handler(new Handler.Callback() {
        /**
         * 合成结束，开始播报
         */
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == AEE_END) {
                if(aiHandle != null){
                    int ret = AiHelper.getInst().end(aiHandle);
                    Log.d(TAG, "AIKit_End：" + ret);
                }
                String result = OUTPUT_DIR + File.separator + "OutPut_" + outFileName + ".pcm";
                Log.d(TAG, "输出的文件路径为:" + result);
                String info = "合成音频存放路径:"+result;
                AudioTrackManager.getInstance().setSampleRate(AudioTrackManager.sampleRateType.SAMPLE_RATE_16k);//播报前先设置采样率
                AudioTrackManager.getInstance().startPlay(result);
            }
            return false;
        }
    });

}
