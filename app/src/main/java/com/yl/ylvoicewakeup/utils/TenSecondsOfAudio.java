package com.yl.ylvoicewakeup.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class TenSecondsOfAudio {
    private static final int SAMPLE_RATE = 16000; // 16kHz
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = 2 * SAMPLE_RATE; // 2秒的缓冲区（示例）

    private AudioRecord audioRecord;
    private byte[] buffer;
    private byte[] ringBuffer; // 环形缓冲区（存储最近5秒）
    private int ringBufferPos = 0;
    private boolean isRecording = false;

    public TenSecondsOfAudio(Context context) {
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT,Math.max(BUFFER_SIZE, minBufferSize));
        // 初始化环形缓冲区（5秒的PCM 16bit数据）
        ringBuffer = new byte[5 * SAMPLE_RATE * 2]; // 16bit = 2字节
        buffer = new byte[BUFFER_SIZE];
    }
    public void startRecording() {
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.d("TAG", "startRecording: AudioRecord初始化失败");
            throw new IllegalStateException("AudioRecord初始化失败");
        }
        isRecording = true;
        audioRecord.startRecording();
        new Thread(() -> {
            // 在录音线程中：
            while (isRecording) {
                int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    synchronized (this) {
                        for (int i = 0; i < bytesRead; i++) {
                            ringBuffer[ringBufferPos] = buffer[i];
                            ringBufferPos = (ringBufferPos + 1) % ringBuffer.length; // 确保回绕
                        }
                    }
                }
            }
        }).start();
    }
    public synchronized byte[] getLast5Seconds() {
        int totalBytes = 5 * SAMPLE_RATE * 2; // 5秒的PCM数据
        byte[] result = new byte[totalBytes];

        synchronized (this) {
            if (ringBufferPos >= totalBytes) {
                // 情况1：缓冲区足够，直接拷贝最后5秒
                System.arraycopy(ringBuffer, ringBufferPos - totalBytes, result, 0, totalBytes);
            } else {
                // 情况2：需要环形拼接（数据不足5秒或跨越缓冲区末尾）
                int availableBytes = ringBufferPos; // 实际可用的数据量
                if (availableBytes <= 0) {
                    return result; // 返回空数组（无数据）
                }

                // 计算两部分拷贝的长度
                int part1 = Math.min(ringBuffer.length - (totalBytes - ringBufferPos), availableBytes);
                int part2 = Math.min(ringBufferPos, availableBytes - part1);

                if (part1 > 0) {
                    System.arraycopy(ringBuffer, ringBuffer.length - part1, result, 0, part1);
                }
                if (part2 > 0) {
                    System.arraycopy(ringBuffer, 0, result, part1, part2);
                }
            }
        }
        return result;
    }
    //释放
    public void stopRecording() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }
}
