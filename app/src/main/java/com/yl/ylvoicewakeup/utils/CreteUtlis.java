package com.yl.ylvoicewakeup.utils;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//创建音纹识别需要的文件
public class CreteUtlis {
    private MediaRecorder mediaRecorder = new MediaRecorder();
    private File recordFile;
    private WeakReference<Context> weakReference;
    private List<File> filePathList = new ArrayList<>();
    private boolean isRecording = false;
    private AudioRecord audioRecord;
    private int sampleRateInHz = 16000;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private Thread recordingThread;

    // 创建录音文件路径
    public String createAudioFilePath(Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "VOICE_" + timeStamp + ".pcm";
        Log.e("TAG", "createAudioFilePath: " +context.getExternalCacheDir().getAbsolutePath() );
        return context.getExternalCacheDir().getAbsolutePath() + "/" + fileName;
    }

    public void startRecord(WeakReference<Context> weakReference,String filePath) {
        this.weakReference = weakReference;
        recordFile = new File(filePath);
        //如果存在，就先删除再创建
        if (recordFile.exists()) {
            recordFile.delete();
        }
        try {
            recordFile.createNewFile();
        } catch (IOException e) {
            throw new IllegalStateException("未能创建" + recordFile.toString());
        }
        if (filePathList.size() == 2) {
            filePathList.clear();
        }
        filePathList.add(recordFile);
        try {
            //输出流
            OutputStream os = new FileOutputStream(recordFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_IN_MONO, audioEncoding);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, AudioFormat.CHANNEL_IN_MONO, audioEncoding, bufferSize);
            isRecording = true;
            audioRecord.startRecording();
            recordingThread = new Thread(() -> {
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    byte[] buffer = new byte[bufferSize];
                    while (isRecording) {
                        int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                } catch (IOException e) {
                    Log.d("错误", "startRecord: "+e);
                }
            });
            recordingThread.start();
            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // 开始录音
    public static void startRecording(String filePath) {
        try {
            MediaRecorder mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            mMediaRecorder.prepare();
            /* ④开始 */
            mMediaRecorder.start();
        } catch (IOException e) {
            Log.d("录音初始化失败", "录音初始化失败", e);
        }
    }
    // 停止录音
    public void stopRecording() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            } catch (Exception e) {
                Log.e("AudioRecord", "停止录音失败", e);
            }
        }
        if (recordingThread != null) {
            try {
                recordingThread.join(); // 等待线程结束
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
