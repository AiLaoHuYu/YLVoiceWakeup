package com.yl.ylvoicewakeup.utils;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PcmUtils {
    /**
     * 保存 PCM 数据到文件
     * @param pcmData  原始PCM数据
     * @param dir      目标目录（如 context.getExternalFilesDir("pcm")）
     * @param fileName 文件名（如 "audio.pcm"）
     */
    public  String  savePcmToFile(byte[] pcmData, File dir, String fileName) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(pcmData);
            Log.i("PCM", "文件已保存: " + file.getAbsolutePath());
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e("PCM", "保存失败", e);
            return "";
        }
    }
}
