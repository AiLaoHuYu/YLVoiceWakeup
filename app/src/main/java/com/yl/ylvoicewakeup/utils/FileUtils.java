package com.yl.ylvoicewakeup.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileUtils {

    public static byte[] readStream(String filePath) throws Exception {
        FileInputStream fs = new FileInputStream(filePath);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while (-1 != (len = fs.read(buffer))) {
            outStream.write(buffer, 0, len);
        }
        outStream.close();
        fs.close();
        return outStream.toByteArray();
    }


    public static void writeFile(String path, byte[] bytes) {
        boolean append = false;
        try {
            File file = new File(path);
            if (file.exists()) {
                append = true;
            } else {
                file.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(path, append);//指定写到哪个路径中
            FileChannel fileChannel = out.getChannel();
            fileChannel.write(ByteBuffer.wrap(bytes)); //将字节流写入文件中
            fileChannel.force(true);//强制刷新
            fileChannel.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        return sdDir.toString();
    }

    public static void copyAssetsToSDCard(Context context, String assetDir, String targetDir) {
        try {
            // 2. 遍历 Assets 目录
            AssetManager assetManager = context.getAssets();
            String[] files = assetManager.list(assetDir);
            if (files == null || files.length == 0) {
                Log.w("FileUtils", "Assets 目录为空: " + assetDir);
                return;
            }

            for (String fileName : files) {
                String assetSubPath = assetDir + File.separator + fileName;
                String targetSubPath = targetDir + File.separator + fileName;

                // 3. 判断是文件还是目录
                if (isAssetDirectory(assetManager, assetSubPath)) {
                    new File(targetSubPath).mkdir();
                    // 递归处理子目录
                    copyAssetsToSDCard(context, assetSubPath, targetSubPath);
                } else {
                    // 4. 检查目标文件是否存在
                    File targetFile = new File(targetSubPath);
                    if (targetFile.exists()) {
                        Log.i("FileUtils", "文件已存在，跳过: " + targetSubPath);
                        continue;
                    }

                    // 5. 执行复制操作
                    try (InputStream in = assetManager.open(assetSubPath);
                         OutputStream out = new FileOutputStream(targetSubPath)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                        Log.i("FileUtils", "文件复制成功: " + targetSubPath);
                    } catch (IOException e) {
                        Log.e("FileUtils", "文件复制失败: " + targetSubPath, e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("FileUtils", "遍历 Assets 目录失败", e);
        }
    }

    /**
     * 判断 Assets 中的路径是否为目录
     */
    private static boolean isAssetDirectory(AssetManager assetManager, String path) {
        try {
            String[] list = assetManager.list(path);
            return list != null && list.length > 0;
        } catch (IOException e) {
            return false;
        }
    }

}
