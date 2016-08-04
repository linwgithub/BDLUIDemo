package linw.bdluidemo;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.baidu.speech.VoiceRecognitionService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import linw.bdlulib.BDSpeakHelper;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    public static final int STATUS_None = 0;
    public static final int STATUS_WaitingReady = 2;
    public static final int STATUS_Ready = 3;
    public static final int STATUS_Speaking = 4;
    public static final int STATUS_Recognition = 5;
    private static final String TAG = "MainActivity";
    private SpeechRecognizer speechRecognizer;
    private int status = STATUS_None;
    private TextView tvResult;
    private TextView tvLog;
    private long speechEndTime = -1;
    private static final int EVENT_ERROR = 11;

    private Button btn;
    private BDSpeakHelper bdSpeakHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvResult = (TextView) findViewById(R.id.tv_result_text);
        tvLog = (TextView) findViewById(R.id.tv_log_text);
        btn = (Button) findViewById(R.id.btn_start);

        //init speechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this, new ComponentName(this, VoiceRecognitionService.class));
        speechRecognizer.setRecognitionListener(this);
        bdSpeakHelper = new BDSpeakHelper(this);
    }

    private static final int REQUEST_UI = 1;

    private void start() {
        Log.e(TAG, "start");
        print("点击了“开始”");
        Intent intent = new Intent();
        bindParams(intent);
        speechEndTime = -1;
        speechRecognizer.startListening(intent);

        tvLog.setText("");
    }

    private void stop() {
        Log.e(TAG, "stop");

        //停止录音，但是识别将继续
        speechRecognizer.stopListening();
        print("点击了“说完了”");
    }

    public void btnClick(View view) {
        int viewid = view.getId();
        switch (viewid) {
            case R.id.btn_start:
//            {
//                Toast.makeText(MainActivity.this, "start", Toast.LENGTH_LONG).show();
//                Intent intent = new Intent("com.baidu.action.RECOGNIZE_SPEECH");
//                intent.putExtra("grammar", "asset:///baidu_speech_grammar.bsg"); // 设置离线的授权文件(离线模块需要授权), 该语法可以用自定义语义工具生成, 链接http://yuyin.baidu.com/asr#m5
//                //intent.putExtra("slot-data", your slots); // 设置grammar中需要覆盖的词条,如联系人名
//                startActivityForResult(intent, 1);
//            }
                switch (status) {
                    case STATUS_None:
                        start();
                        btn.setText("取消");
                        status = STATUS_WaitingReady;
                        break;
                    case STATUS_WaitingReady:
                        cancel();
                        status = STATUS_None;
                        btn.setText("开始");
                        break;
                    case STATUS_Ready:
                        cancel();
                        status = STATUS_None;
                        btn.setText("开始");
                        break;
                    case STATUS_Speaking:
                        stop();
                        status = STATUS_Recognition;
                        btn.setText("识别中");
                        break;
                    case STATUS_Recognition:
                        cancel();
                        status = STATUS_None;
                        btn.setText("开始");
                        break;
                }
                break;
            default:
                break;
        }
    }

    private void cancel() {
        Log.e(TAG, "cancel");

        speechRecognizer.cancel();
        status = STATUS_None;
        print("点击了“取消”");
    }

    public void bindParams(Intent intent) {
        Log.e(TAG, "bindParams");

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("tips_sound", true)) {
            intent.putExtra(Constant.EXTRA_SOUND_START, R.raw.bdspeech_recognition_start);
            intent.putExtra(Constant.EXTRA_SOUND_END, R.raw.bdspeech_speech_end);
            intent.putExtra(Constant.EXTRA_SOUND_SUCCESS, R.raw.bdspeech_recognition_success);
            intent.putExtra(Constant.EXTRA_SOUND_ERROR, R.raw.bdspeech_recognition_error);
            intent.putExtra(Constant.EXTRA_SOUND_CANCEL, R.raw.bdspeech_recognition_cancel);
        }
        if (sp.contains(Constant.EXTRA_INFILE)) {
            String tmp = sp.getString(Constant.EXTRA_INFILE, "").replaceAll(",.*", "").trim();
            intent.putExtra(Constant.EXTRA_INFILE, tmp);
        }
        if (sp.getBoolean(Constant.EXTRA_OUTFILE, false)) {
            intent.putExtra(Constant.EXTRA_OUTFILE, "sdcard/outfile.pcm");
        }
        if (sp.getBoolean(Constant.EXTRA_GRAMMAR, false)) {
            intent.putExtra(Constant.EXTRA_GRAMMAR, "assets:///baidu_speech_grammar.bsg");
        }
        if (sp.contains(Constant.EXTRA_SAMPLE)) {
            String tmp = sp.getString(Constant.EXTRA_SAMPLE, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_SAMPLE, Integer.parseInt(tmp));
            }
        }
        //
        if (sp.contains(Constant.EXTRA_LANGUAGE)) {
            String tmp = sp.getString(Constant.EXTRA_LANGUAGE, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_LANGUAGE, tmp);
            }
        }
        //是否开启智能理解
        intent.putExtra(Constant.EXTRA_NLU, "enable");

        if (sp.contains(Constant.EXTRA_VAD)) {
            String tmp = sp.getString(Constant.EXTRA_VAD, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_VAD, tmp);
            }
        }
        String prop = null;
        if (sp.contains(Constant.EXTRA_PROP)) {
            String tmp = sp.getString(Constant.EXTRA_PROP, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_PROP, Integer.parseInt(tmp));
                prop = tmp;
            }
        }

        // offline asr
        {
            intent.putExtra(Constant.EXTRA_OFFLINE_ASR_BASE_FILE_PATH, "/sdcard/easr/s_1");
            if (null != prop) {
                int propInt = Integer.parseInt(prop);
                if (propInt == 10060) {
                    intent.putExtra(Constant.EXTRA_OFFLINE_LM_RES_FILE_PATH, "/sdcard/easr/s_2_Navi");
                } else if (propInt == 20000) {
                    intent.putExtra(Constant.EXTRA_OFFLINE_LM_RES_FILE_PATH, "/sdcard/easr/s_2_InputMethod");
                }
            }
//            intent.putExtra(Constant.EXTRA_OFFLINE_SLOT_DATA, buildTestSlotData());
        }
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        Log.e(TAG, "onReadyForSpeech");

        //准备就绪-可以开始说话
        //只有当此方法回调之后才能开始说话，否则会影响识别结果。
        status = STATUS_Ready;
        print("\n准备就绪，可以开始说话");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.e(TAG, "onBeginOfSpeech");

        //开始说话-检测到开始说话
        //当用户开始说话，会回调此方法。
        Log.e(TAG, "开始说话");
        btn.setText("说完了");
        print("\n检测到用户的已经开始说话");

    }

    @Override
    public void onRmsChanged(float v) {
//        Log.e(TAG, "onResChanged");

        //音量变化
        //参数为音量值
//        Log.e(TAG, "音量变化");

    }

    @Override
    public void onBufferReceived(byte[] bytes) {
//        Log.e(TAG, "onBufferReceived");
        //参数为每一帧的录音,拼接起来可得到完整录音
    }

    @Override
    public void onEndOfSpeech() {
        Log.e(TAG, "onEndOfSpeech");
        //结束说话时回调
        speechEndTime = System.currentTimeMillis();
        status = STATUS_Recognition;
        print("\n检测到用户的已经停止说话");
        btn.setText("识别中");
    }

    @Override
    public void onError(int i) {
        Log.e(TAG, "onError");

        //识别出错 i:出错码
        Log.e(TAG, "出错码:" + i);
        time = 0;
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
        print("识别失败：" + sb.toString());
        btn.setText("开始");
    }

    @Override
    public void onResults(Bundle results) {
        Log.e(TAG, "onResults");

        //最终结果  (包括语义理解结果?)
        Log.e(TAG, "最终结果");
        long end2finish = System.currentTimeMillis() - speechEndTime;
        status = STATUS_None;
        ArrayList<String> nbest = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        print("识别成功：" + Arrays.toString(nbest.toArray(new String[nbest.size()])));
        String json_res = results.getString("origin_result");
        Log.e(TAG, "json_res:" + json_res);
        try {
            print("origin_result=\n" + new JSONObject(json_res).toString(4));
            Log.e(TAG, new JSONObject(json_res).toString(4));
            JSONObject resultOBJ = new JSONObject(json_res);
            JSONObject content = resultOBJ.getJSONObject("content");
            if (content != null) {
                Log.e(TAG, content.toString());
            } else {
                Log.e(TAG, "item is null ()");
            }
            String json_resStr = content.getString("json_res");
            if (json_resStr != null) {
                Log.e(TAG, "jsong_res != null & :" + json_resStr.toString());
            } else {
                Log.e(TAG, "jsong_res == null");
            }
                if (TextUtils.isEmpty(json_resStr)) {
                    Log.e(TAG, "json_res is null");
                } else {
                    Log.e(TAG, "json_res is not null");
                    JSONObject json_resOBJ = new JSONObject(json_resStr);
                    String parsed_text = json_resOBJ.getString("parsed_text");
                    String raw_test = json_resOBJ.getString("raw_text");
                    JSONArray resultsObj = json_resOBJ.getJSONArray("results");
                    if (resultsObj != null && resultOBJ.length() > 0) {
                        String resultsMainStr = resultsObj.getString(0);
                        JSONObject resultsMainOBJ = new JSONObject(resultsMainStr);
                        String domain = resultsMainOBJ.getString("domain");
                        String intent = resultsMainOBJ.getString("intent");
                        JSONObject commindObject = resultsMainOBJ.getJSONObject("object");
                        int commind_id = commindObject.getInt("id");
                        Log.e(TAG, "conmandID:" + commind_id);
//                        String command_instance = commindObject.getString("instance");
//                        double command_score = commindObject.getDouble("score");
                        Log.e(TAG, resultsObj.toString());
                        switch (commind_id) {
                            case 1:
                                bdSpeakHelper.speak("正在为您开启运动,请选择运动模式");
                                break;
                            case 2:
                                bdSpeakHelper.speak("正在为您停止运动");
                                bdSpeakHelper.speak("运动已停止");
                                break;
                            case 3:
                                bdSpeakHelper.speak("请问有什么需要帮助的?");
                                break;
                            default:
                                break;
                        }
                    } else {
                        bdSpeakHelper.speak("我不明白你在说什么");
                    }
                }
        } catch (Exception e) {
            Log.e(TAG, "error:" + e.getMessage());
            print("origin_result=[warning: bad json]\n" + json_res);
        }
        btn.setText("开始");
        String strEnd2Finish = "";
        if (end2finish < 60 * 1000) {
            strEnd2Finish = "(waited " + end2finish + "ms)";
        }
        tvLog.append(nbest.get(0) + strEnd2Finish);
        tvResult.setText(nbest.get(0));
        time = 0;

    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.e(TAG, "onPartialResults");

        ArrayList<String> nbest = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (nbest.size() > 0) {
            print("\n~临时识别结果：" + Arrays.toString(nbest.toArray(new String[0])));
            tvLog.append("text:" + nbest.get(0));
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
                print("EVENT_ERROR, " + reason);
                break;
            case VoiceRecognitionService.EVENT_ENGINE_SWITCH:
                int type = params.getInt("engine_type");
                print("*引擎切换至" + (type == 0 ? "在线" : "离线"));
                break;
        }
    }

    long time;

    private void print(String msg) {
        long t = System.currentTimeMillis() - time;
        if (t > 0 && t < 100000) {
            tvLog.append(t + "ms, " + msg + "\n");
        } else {
            tvLog.append("" + msg + "\n");
        }
        ScrollView sv = (ScrollView) tvLog.getParent();
        sv.smoothScrollTo(0, 1000000);
        Log.d("TAG", "----" + msg);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == RESULT_OK) {
//            onResults(data.getExtras());
//        }
        if (resultCode == RESULT_OK) {
            Bundle results = data.getExtras();
            ArrayList<String> results_recognition = results.getStringArrayList("results_recognition");
            tvLog.append("识别结果(数组形式): " + results_recognition + "\n");
        }
    }

    @Override
    protected void onDestroy() {
        speechRecognizer.destroy();
        super.onDestroy();
    }
}
