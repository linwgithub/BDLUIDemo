package linw.bdlulib.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import linw.bdlulib.helper.BDSpeakHelper;
import linw.bdlulib.helper.BDWakeUpHelper;

/**
 * 作者: linw
 * 内容:语音唤醒服务,在接收到设定的指令(你好小布)后唤醒,并开启语音识别服务。
 */
public class WeakUpService extends Service {

    private static final String TAG = "WeakUpService";

    private BDWakeUpHelper mBDWakeUpHelper;
    private BDSpeakHelper mBDSpeakHelper;

    public WeakUpService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mBDWakeUpHelper = new BDWakeUpHelper(this, weakUpListener);
        mBDSpeakHelper = new BDSpeakHelper(this);
        mBDSpeakHelper.setOnSpeakListener(onSpeakListener);
        mBDWakeUpHelper.startWeakUp();
        return super.onStartCommand(intent, flags, startId);
    }

    BDSpeakHelper.OnSpeakListener onSpeakListener = new BDSpeakHelper.OnSpeakListener() {
        @Override
        public void onSpeakFinish() {
            Log.e(TAG, "onSpeakFinish");

        }
    };

    BDWakeUpHelper.OnWeakUpListener weakUpListener = new BDWakeUpHelper.OnWeakUpListener() {
        @Override
        public void onWeakUpResult(boolean bResult) {
            if (bResult) {
                Intent intent = new Intent(WeakUpService.this, RecognizerService.class);
                startService(intent);
            }
        }
    };

    @Override
    public void onDestroy() {
        // TODO: 销毁唤醒还需测试
        mBDWakeUpHelper.destroyWeakUp();
        super.onDestroy();
    }
}
