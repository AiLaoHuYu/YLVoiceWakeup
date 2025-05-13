package com.yl.ylvoicewakeup.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yl.ylvoicewakeup.service.VoiceWakeupService;

public class CreteFilesBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. 检查 Action 是否匹配
        if ("com.yl.deepseekxunfei.cretemodule.CUSTOM_ACTION".equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, VoiceWakeupService.class);
            context.startService(serviceIntent);
            // 2. 获取传递的数据
            String data = intent.getStringExtra("key"); // 对应发送方的 putExtra("key", value)
            // 3. 处理数据
            Log.d("BroadcastReceiver", "Received data: " + data);
        }
    }
}
