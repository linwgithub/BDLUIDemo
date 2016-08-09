package linw.bdluidemo;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import linw.bdlulib.ResultUtil;
import linw.bdlulib.helper.BDRecognizerHelper;
import linw.bdlulib.helper.BDSpeakHelper;
import linw.bdlulib.service.WeakUpService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView tvResult;
    private TextView tvLog;

    private Button btn;
    private BDSpeakHelper mBDSpeakHelper;
    private BDRecognizerHelper mBDRecognizerHelper;

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    mBDRecognizerHelper.start();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvResult = (TextView) findViewById(R.id.tv_result_text);
        tvLog = (TextView) findViewById(R.id.tv_log_text);
        btn = (Button) findViewById(R.id.btn_start);
        mBDSpeakHelper = new BDSpeakHelper(this);
        mBDSpeakHelper.setOnSpeakListener(new BDSpeakHelper.OnSpeakListener() {
            @Override
            public void onSpeakFinish() {
                Log.e(TAG, "speech ! ");
                Message msg = new Message();
                msg.what = 0;
                handler.sendMessage(msg);
            }
        });

        mBDRecognizerHelper = new BDRecognizerHelper(this, new BDRecognizerHelper.OnRecognizerListener() {
            @Override
            public void onRecognizer(String jsonString) {
                ResultUtil resultUtil = new ResultUtil(jsonString);
                int commandId = resultUtil.getCommandId();
                Log.e(TAG, " id:" + commandId);
            }

            @Override
            public void onError() {

            }
        });
    }

    public void btnClick(View view) {
        int viewid = view.getId();
        switch (viewid) {
            case R.id.btn_start:
                mBDSpeakHelper.speak("您好请问有什么需要帮助的?");
//                mBDRecognizerHelper.start();
                break;
            case R.id.btn_weakup:
                Intent weakUpIntent = new Intent(this, WeakUpService.class);
                startService(weakUpIntent);
                break;
            default:
                break;
        }
    }

}
