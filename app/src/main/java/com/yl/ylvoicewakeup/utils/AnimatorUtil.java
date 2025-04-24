package com.yl.ylvoicewakeup.utils;

import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

public class AnimatorUtil {

    public void startJumpAnimation(ImageView imageView) {
        // 创建一个垂直方向的上下跳动动画
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, -0.2f);
        animation.setDuration(1000); // 动画时长 1 秒
        animation.setRepeatCount(Animation.INFINITE); // 无限重复
        animation.setRepeatMode(Animation.REVERSE); // 重复模式为反转

        imageView.startAnimation(animation);
    }

    // 停止动画并重置位置
    public void stopJumpAnimation(ImageView imageView) {
        imageView.clearAnimation();
    }

}
