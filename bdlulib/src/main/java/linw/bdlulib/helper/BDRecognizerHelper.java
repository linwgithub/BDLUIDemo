package linw.bdlulib.helper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.baidu.speech.VoiceRecognitionService;

import java.util.ArrayList;
import java.util.Arrays;

import linw.bdlulib.R;
import linw.bdlulib.RecognizerParams;

/**
 * 作者: linw
 * 内容:语音识别以及获得语音的理解工具
 */
public class BDRecognizerHelper implements RecognitionListener {

    public static final int STATUS_None = 0;
    public static final int STATUS_WaitingReady = 2;
    public static final int STATUS_Ready = 3;
    public static final int STATUS_Speaking = 4;
    public static final int STATUS_Recognition = 5;
    private static final String TAG = "BDRecognizerHelper";
    private SpeechRecognizer speechRecognizer;
    private int status = STATUS_None;
    private Context mContext;

    private long speechEndTime = -1;
    private static final int EVENT_ERROR = 11;

    private OnRecognizerListener mOnRecognizerListener;

    public interface OnRecognizerListener {
        void onRecognizer(String jsonString);

        void onError();
    }

    /**
     *
     * @param context 上下文
     * @param onRecognizerListener 语音理解的监听器,获得解析结果后调用
     */
    public BDRecognizerHelper(Context context, OnRecognizerListener onRecognizerListener) {
        this.mContext = context;
        this.mOnRecognizerListener = onRecognizerListener;
    }

    /**
     * 初始化解析器
     */
    private void initRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext,
                new ComponentName(mContext, VoiceRecognitionService.class));
        speechRecognizer.setRecognitionListener(this);
    }

    /**
     * 开启语音识别
     */
    public void start() {
        Intent intent = new Intent();
        bindParams(intent);
        speechEndTime = -1;
        if (speechRecognizer == null) {
            initRecognizer();
        }
        speechRecognizer.startListening(intent);
    }

    /**
     * 设置语音识别的参数
     * @param intent
     */
    public void bindParams(Intent intent) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);

        //tips_sound
        intent.putExtra(RecognizerParams.EXTRA_SOUND_START, R.raw.bdspeech_recognition_start);
        intent.putExtra(RecognizerParams.EXTRA_SOUND_END, R.raw.bdspeech_speech_end);
        intent.putExtra(RecognizerParams.EXTRA_SOUND_SUCCESS, R.raw.bdspeech_recognition_success);
        intent.putExtra(RecognizerParams.EXTRA_SOUND_ERROR, R.raw.bdspeech_recognition_error);
        intent.putExtra(RecognizerParams.EXTRA_SOUND_CANCEL, R.raw.bdspeech_recognition_cancel);

        if (sp.contains(RecognizerParams.EXTRA_INFILE)) {
            String tmp = sp.getString(RecognizerParams.EXTRA_INFILE, "").replaceAll(",.*", "").trim();
            intent.putExtra(RecognizerParams.EXTRA_INFILE, tmp);
        }
        if (sp.getBoolean(RecognizerParams.EXTRA_OUTFILE, false)) {
            intent.putExtra(RecognizerParams.EXTRA_OUTFILE, "sdcard/outfile.pcm");
        }
        if (sp.getBoolean(RecognizerParams.EXTRA_GRAMMAR, false)) {
            intent.putExtra(RecognizerParams.EXTRA_GRAMMAR, "assets:///baidu_speech_grammar.bsg");
        }
        if (sp.contains(RecognizerParams.EXTRA_SAMPLE)) {
            String tmp = sp.getString(RecognizerParams.EXTRA_SAMPLE, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(RecognizerParams.EXTRA_SAMPLE, Integer.parseInt(tmp));
            }
        }
        //
        if (sp.contains(RecognizerParams.EXTRA_LANGUAGE)) {
            String tmp = sp.getString(RecognizerParams.EXTRA_LANGUAGE, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(RecognizerParams.EXTRA_LANGUAGE, tmp);
            }
        }
        //是否开启智能理解
        intent.putExtra(RecognizerParams.EXTRA_NLU, "enable");

        if (sp.contains(RecognizerParams.EXTRA_VAD)) {
            String tmp = sp.getString(RecognizerParams.EXTRA_VAD, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(RecognizerParams.EXTRA_VAD, tmp);
            }
        }
        String prop = null;
        if (sp.contains(RecognizerParams.EXTRA_PROP)) {
            String tmp = sp.getString(RecognizerParams.EXTRA_PROP, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(RecognizerParams.EXTRA_PROP, Integer.parseInt(tmp));
                prop = tmp;
            }
        }

        // offline asr
        {
            intent.putExtra(RecognizerParams.EXTRA_OFFLINE_ASR_BASE_FILE_PATH, "/sdcard/easr/s_1");
            if (null != prop) {
                int propInt = Integer.parseInt(prop);
                if (propInt == 10060) {
                    intent.putExtra(RecognizerParams.EXTRA_OFFLINE_LM_RES_FILE_PATH, "/sdcard/easr/s_2_Navi");
                } else if (propInt == 20000) {
                    intent.putExtra(RecognizerParams.EXTRA_OFFLINE_LM_RES_FILE_PATH, "/sdcard/easr/s_2_InputMethod");
                }
            }
//            intent.putExtra(RecognizerParams.EXTRA_OFFLINE_SLOT_DATA, buildTestSlotData());
        }
    }

    /**
     * 停止语音识别
     */
    public void stop() {
        //停止录音，但是识别将继续
        speechRecognizer.stopListening();
    }

    /**
     * 销毁识别
     */
    public void destroyRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        //准备就绪-可以开始说话
        //只有当此方法回调之后才能开始说话，否则会影响识别结果。
        status = STATUS_Ready;
        Log.d(TAG, "onReadyForSpeech \n准备就绪，可以开始说话");
    }

    @Override
    public void onBeginningOfSpeech() {
        //开始说话-检测到开始说话
        //当用户开始说话，会回调此方法。
    }

    @Override
    public void onRmsChanged(float v) {
        //Log.e(TAG, "onResChanged");
        //音量变化
        //参数为音量值
    }

    @Override
    public void onBufferReceived(byte[] bytes) {
        //Log.e(TAG, "onBufferReceived");
        //参数为每一帧的录音,拼接起来可得到完整录音
    }

    @Override
    public void onEndOfSpeech() {
        //结束说话时回调
        speechEndTime = System.currentTimeMillis();
        status = STATUS_Recognition;
    }

    @Override
    public void onError(int i) {
        //识别出错 i:出错码
        Log.e(TAG, "onError:出错码:" + i);
        status = STATUS_None;
        StringBuilder sb = new StringBuilder();
        switch (i) {
            case SpeechRecognizer.ERROR_AUDIO:
                sb.append("音频问题");
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                sb.append("没有语音输入");
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                sb.append("其它客户端错误");
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                sb.append("权限不足");
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                sb.append("网络问题");
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                sb.append("没有匹配的识别结果");
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                sb.append("引擎忙");
                break;
            case SpeechRecognizer.ERROR_SERVER:
                sb.append("服务端错误");
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                sb.append("连接超时");
                break;
        }
        sb.append(":" + i);
        Log.e(TAG, "error:" + sb);
    }

    @Override
    public void onResults(Bundle results) {
        Log.e(TAG, "onResults");

        long end2finish = System.currentTimeMillis() - speechEndTime;
        status = STATUS_None;
        ArrayList<String> nbest = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        Log.e(TAG, "识别成功：" + Arrays.toString(nbest.toArray(new String[nbest.size()])));
        String json_res = results.getString("origin_result");
        Log.e(TAG, "origin_result=\n" + json_res);//new JSONObject(json_res).toString(4));
        mOnRecognizerListener.onRecognizer(json_res);
        String strEnd2Finish = "";
        if (end2finish < 60 * 1000) {
            strEnd2Finish = "(waited " + end2finish + "ms)";
        }
        Log.e(TAG, strEnd2Finish);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> nbest = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (nbest.size() > 0) {
            Log.d(TAG, "\n~临时识别结果：" + Arrays.toString(nbest.toArray(new String[0])));
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        //识别时间返回
        //i :事件类型
        //bundle:参数
        switch (eventType) {
            case EVENT_ERROR:
                String reason = params.get("reason") + "";
                Log.d(TAG, "EVENT_ERROR, " + reason);
                break;
            case VoiceRecognitionService.EVENT_ENGINE_SWITCH:
                int type = params.getInt("engine_type");
                Log.d(TAG, "*引擎切换至" + (type == 0 ? "在线" : "离线"));
                break;
        }
    }
}
