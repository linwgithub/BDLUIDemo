package linw.bdlulib.helper;

import android.content.Context;
import android.util.AndroidRuntimeException;
import android.util.Log;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * 作者: linw
 * 内容:语音唤醒工具
 */
public class BDWakeUpHelper {

    private static final String TAG = "BDWakeUpHelper";

    private EventManager mWpEventManager;
    private Context mContext;
    private OnWeakUpListener mWeakUpListener;

    public interface OnWeakUpListener {
        void onWeakUpResult(boolean bResult);
    }

    /**
     * 构造器,上下文要使用服务或应用的?根据实际需求传入或设置
     *
     * @param context
     * @param weakUpListener
     */
    public BDWakeUpHelper(Context context, OnWeakUpListener weakUpListener) {
        this.mWeakUpListener = weakUpListener;
        this.mContext = context;
    }

    private void init() {

        // 1) 创建唤醒事件管理器
        mWpEventManager = EventManagerFactory.create(mContext, "wp");

        // 2) 注册唤醒事件监听器
        mWpEventManager.registerListener(new EventListener() {
            @Override
            public void onEvent(String name, String params, byte[] data, int offset, int length) {
                Log.i(TAG, String.format("event: name=%s, params=%s", name, params));
//                boolean bWeakUp = false;
                try {
                    JSONObject json = new JSONObject(params);
                    if ("wp.data".equals(name)) { // 每次唤醒成功, 将会回调name=wp.data的时间, 被激活的唤醒词在params的word字段
                        String word = json.getString("word");
                        Log.e(TAG, "唤醒成功, 唤醒词: " + word);
//                        bWeakUp = true;
                        mWeakUpListener.onWeakUpResult(true);

                    } else if ("wp.exit".equals(name)) {
                        Log.e(TAG, "唤醒已经停止: " + params);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "error:" + e.getMessage());
                    throw new AndroidRuntimeException(e);
//                } finally {
//                    if (mWeakUpListener != null) {
//                        mWeakUpListener.onWeakUpResult(bWeakUp);
//                    }
                }

            }
        });
    }

    /**
     * 开启唤醒监听
     */
    public void startWeakUp() {
        if (mWpEventManager == null) {
            init();
        }
        // 3) 通知唤醒管理器, 启动唤醒功能
        HashMap params = new HashMap();
        params.put("kws-file", "assets:///WakeUp.bin"); // 设置唤醒资源, 唤醒资源请到 http://yuyin.baidu.com/wake#m4 来评估和导出
        mWpEventManager.send("wp.start", new JSONObject(params).toString(), null, 0, 0);
        Log.d(TAG, "params:" + new JSONObject(params).toString());
    }

    /**
     * 暂停唤醒
     */
    public void pauseWeakUp() {
        mWpEventManager.send("wp.stop", null, null, 0, 0);
    }

    /**
     * 停止唤醒
     */
    public void destroyWeakUp() {
        mWpEventManager.send("wp.exit", null, null, 0, 0);
    }
}
