package linw.bdlulib.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import linw.bdlulib.ResultUtil;
import linw.bdlulib.helper.BDRecognizerHelper;
import linw.bdlulib.helper.BDSpeakHelper;

/**
 * 作者: linw
 * 内容:语音识别服务;开启语音识别根据语音识别结果返回解析内容
 */
public class RecognizerService extends Service {

    private static final String TAG = "RecognizerService";
    private static final int SPEECH_FINISH = 0;

    private BDRecognizerHelper mBDRecognizerHelper;
    private BDSpeakHelper mBDSpeakHelper;

    public RecognizerService() {
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SPEECH_FINISH:
                    //开启语音识别
                    mBDRecognizerHelper.start();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        mBDRecognizerHelper = new BDRecognizerHelper(this, recognizerListener);
        mBDSpeakHelper = new BDSpeakHelper(this);
        mBDSpeakHelper.setOnSpeakListener(onSpeakListener);
        mBDSpeakHelper.speak("您好请问有什么需要帮助的?");

        return super.onStartCommand(intent, flags, startId);
    }

    BDSpeakHelper.OnSpeakListener onSpeakListener = new BDSpeakHelper.OnSpeakListener() {
        @Override
        public void onSpeakFinish() {
            Message msg = new Message();
            msg.what = SPEECH_FINISH;
            handler.sendMessage(msg);
        }
    };

    BDRecognizerHelper.OnRecognizerListener recognizerListener = new BDRecognizerHelper.OnRecognizerListener() {
        @Override
        public void onRecognizer(String json_res) {
            ResultUtil resultUtil = new ResultUtil(json_res);
            int command_id = resultUtil.getCommandId();
            switch (command_id) {
                case 1:
                    mBDSpeakHelper.speak("正在为您开启运动,请选择运动模式");
                    break;
                case 2:
                    mBDSpeakHelper.speak("正在为您停止运动");
                    mBDSpeakHelper.speak("运动已停止");
                    break;
                case 3:
                    mBDSpeakHelper.speak("请问有什么需要帮助的?");
                    break;
                default:
                    break;
            }
            stopSelf();
        }

        @Override
        public void onError() {
            mBDSpeakHelper.speak("我不明白你在说什么");
        }
    };

    @Override
    public void onDestroy() {
        Log.e(TAG, "destroy");
        if (mBDRecognizerHelper != null) {
            mBDRecognizerHelper.destroyRecognizer();
        }
        if (mBDSpeakHelper != null) {
            // TODO: 16/8/9 是否需要销毁?
        }
        super.onDestroy();
    }
}
