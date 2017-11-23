package com.dsm.xiaodi.biz.push;

import android.content.Context;
import android.os.Build;

import com.alibaba.android.arouter.launcher.ARouter;
import com.dsm.libarouter.service.IMainProvider;
import com.dsm.platform.config.ConfigSharedPreferences;
import com.dsm.platform.util.SharedPreferencesUtil;
import com.dsm.platform.util.log.LogUtil;
import com.igexin.sdk.GTIntentService;
import com.igexin.sdk.PushConsts;
import com.igexin.sdk.PushManager;
import com.igexin.sdk.message.FeedbackCmdMessage;
import com.igexin.sdk.message.GTCmdMessage;
import com.igexin.sdk.message.GTTransmitMessage;
import com.igexin.sdk.message.SetTagCmdMessage;

/**
 * 继承GTIntentService 接收来自个推的消息, 所有消息在线程中回调, 如果注册了该服务, 则务必要在AndroidManifest中声明, 否则无法接受消息<br>
 * onReceiveMessageData 处理透传消息<br>
 * onReceiveClientId 接收 cid <br>
 * onReceiveOnlineState cid 离线上线通知 <br>
 * onReceiveCommandResult 各种事件处理回执 <br>
 */
public class XiaodiIntentService extends GTIntentService {

    private IMainProvider mainProvider;

    public IMainProvider getMainProvider() {
        if (mainProvider == null) {
            mainProvider = (IMainProvider) ARouter.getInstance().build("/main/service").navigation();
        }
        return mainProvider;
    }

    @Override
    public void onReceiveServicePid(Context context, int pid) {
        LogUtil.i(TAG, "onReceiveServicePid -> " + pid);
    }

    @Override
    public void onReceiveClientId(Context context, String clientid) {
        // 第三方应用需要将CID上传到第三方服务器，并且将当前用户帐号和CID进行关联，以便日后通过用户帐号查找CID进行消息推送
        LogUtil.e(TAG, "onReceiveClientId -> " + "clientid = " + clientid);
        updatePushInfo(context, clientid);
    }

    @Override
    public void onReceiveMessageData(Context context, GTTransmitMessage gtTransmitMessage) {
        String appid = gtTransmitMessage.getAppid();
        String taskid = gtTransmitMessage.getTaskId();
        String messageid = gtTransmitMessage.getMessageId();
        byte[] payload = gtTransmitMessage.getPayload();
        String pkg = gtTransmitMessage.getPkgName();
        String cid = gtTransmitMessage.getClientId();

        // 第三方回执调用接口，actionid范围为90000-90999，可根据业务场景执行
        boolean result = PushManager.getInstance().sendFeedbackMessage(context, taskid, messageid, 90001);
        LogUtil.i(TAG, "call sendFeedbackMessage = " + (result ? "success" : "failed"));
        LogUtil.i(TAG, "onReceiveMessageData -> " + "appid = " + appid + "\ntaskid = " + taskid
            + "\nmessageid = " + messageid + "\npkg = " + pkg + "\ncid = " + cid);

        if (payload == null) {
            LogUtil.e(TAG, "receiver payload = null");
            return;
        }

        String data = new String(payload);
        LogUtil.i(TAG, "receiver payload = " + data);

        getMainProvider().handlerPushMessage(context, data);
    }

    @Override
    public void onReceiveOnlineState(Context context, boolean online) {
        LogUtil.i(TAG, "onReceiveOnlineState -> " + (online ? "online" : "offline"));
    }

    @Override
    public void onReceiveCommandResult(Context context, GTCmdMessage gtCmdMessage) {
        LogUtil.i(TAG, "onReceiveCommandResult -> " + gtCmdMessage);

        int action = gtCmdMessage.getAction();
        if (action == PushConsts.SET_TAG_RESULT) {
            setTagResult((SetTagCmdMessage) gtCmdMessage);
        } else if ((action == PushConsts.THIRDPART_FEEDBACK)) {
            feedbackResult((FeedbackCmdMessage) gtCmdMessage);
        }
    }

    private void setTagResult(SetTagCmdMessage setTagCmdMsg) {
        String sn = setTagCmdMsg.getSn();
        String code = setTagCmdMsg.getCode();

        String text = "设置标签失败, 未知异常";
        switch (Integer.valueOf(code)) {
            case PushConsts.SETTAG_SUCCESS:
                text = "设置标签成功";
                break;

            case PushConsts.SETTAG_ERROR_COUNT:
                text = "设置标签失败, tag数量过大, 最大不能超过200个";
                break;

            case PushConsts.SETTAG_ERROR_FREQUENCY:
                text = "设置标签失败, 频率过快, 两次间隔应大于1s且一天只能成功调用一次";
                break;

            case PushConsts.SETTAG_ERROR_REPEAT:
                text = "设置标签失败, 标签重复";
                break;

            case PushConsts.SETTAG_ERROR_UNBIND:
                text = "设置标签失败, 服务未初始化成功";
                break;

            case PushConsts.SETTAG_ERROR_EXCEPTION:
                text = "设置标签失败, 未知异常";
                break;

            case PushConsts.SETTAG_ERROR_NULL:
                text = "设置标签失败, tag 为空";
                break;

            case PushConsts.SETTAG_NOTONLINE:
                text = "还未登录成功";
                break;

            case PushConsts.SETTAG_IN_BLACKLIST:
                text = "该应用已经在黑名单中,请联系售后支持!";
                break;

            case PushConsts.SETTAG_NUM_EXCEED:
                text = "已存 tag 超过限制";
                break;

            default:
                break;
        }

        LogUtil.i(TAG, "settag result sn = " + sn + ", code = " + code + ", text = " + text);
    }

    private void feedbackResult(FeedbackCmdMessage feedbackCmdMsg) {
        String appid = feedbackCmdMsg.getAppid();
        String taskid = feedbackCmdMsg.getTaskId();
        String actionid = feedbackCmdMsg.getActionId();
        String result = feedbackCmdMsg.getResult();
        long timestamp = feedbackCmdMsg.getTimeStamp();
        String cid = feedbackCmdMsg.getClientId();

        LogUtil.i(TAG, "onReceiveCommandResult -> " + "appid = " + appid + "\ntaskid = " + taskid
            + "\nactionid = " + actionid + "\nresult = " + result + "\ncid = " + cid + "\ntimestamp = " + timestamp);
    }

    // 将ClientId上传到服务器
    private void updatePushInfo(Context context, final String clientid) {
        String username = SharedPreferencesUtil.getString(context, ConfigSharedPreferences.USERNAME, "");
        getMainProvider().updateUserPushInfo(username, "android", "", clientid, Build.SERIAL);
    }
}
