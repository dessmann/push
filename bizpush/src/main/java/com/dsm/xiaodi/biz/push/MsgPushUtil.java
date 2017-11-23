package com.dsm.xiaodi.biz.push;

import android.content.Context;

import com.igexin.sdk.PushManager;

public class MsgPushUtil {

    // 初始化推送服务
    public static void initPushService(Context applicationContext) {
        PushManager.getInstance().initialize(applicationContext, XiaodiPushService.class);
        PushManager.getInstance().registerPushIntentService(applicationContext, XiaodiIntentService.class);
    }

    // 重启推送服务
    public static void restartPushService(Context applicationContext) {
        // 如果推送服务未开启，则重新开启
        if (!PushManager.getInstance().isPushTurnedOn(applicationContext)) {
            PushManager.getInstance().initialize(applicationContext, XiaodiPushService.class);
            PushManager.getInstance().registerPushIntentService(applicationContext, XiaodiIntentService.class);
        }
    }

    // 停止推送服务
    public static void stopPushService(Context applicationContext) {
        PushManager.getInstance().stopService(applicationContext);
    }

}
