package linw.bdlulib;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 作者: linw
 * 内容:分解百度语义理解的结果
 */
public class ResultUtil {

    private static final String TAG = "ResultUtil";
    public static final String OBJ_CONTENT = "content";
    public static final String STR_JSON_RES = "json_res";
    public static final String ARR_RESULTS = "results";
    public static final String OBJ_OBJECT = "object";
    public static final String INT_ID = "id";

    private String mJson_str;

    /**
     * 构造器
     * @param json_str 语义解析结果的json字符串
     */
    public ResultUtil(String json_str) {
        this.mJson_str = json_str;
    }

    /**
     * 获取自定义指令的序号
     * @return 指令序号
     */
    public int getCommandId() {
        try {
            JSONObject resultOBJ = new JSONObject(mJson_str);
            JSONObject content = resultOBJ.getJSONObject(OBJ_CONTENT);
//            if (content != null) {
//                Log.e(TAG, content.toString());
//            } else {
//                Log.e(TAG, "item is null ()");
//            }
            String json_resStr = content.getString(STR_JSON_RES);
//            if (json_resStr != null) {
//                Log.e(TAG, "jsong_res != null & :" + json_resStr.toString());
//            } else {
//                Log.e(TAG, "jsong_res == null");
//            }
            if (TextUtils.isEmpty(json_resStr)) {
                Log.e(TAG, "json_resStr == null");
                return -1;
            }
            JSONObject json_resOBJ = new JSONObject(json_resStr);
            //String parsed_text = json_resOBJ.getString("parsed_text");
            //String raw_test = json_resOBJ.getString("raw_text");
            JSONArray resultsObj = json_resOBJ.getJSONArray(ARR_RESULTS);
            if (resultsObj != null && resultOBJ.length() > 1) {
                String resultsMainStr = resultsObj.getString(0);
                JSONObject resultsMainOBJ = new JSONObject(resultsMainStr);
                //String domain = resultsMainOBJ.getString("domain");
                //String intent = resultsMainOBJ.getString("intent");
                JSONObject commandObject = resultsMainOBJ.getJSONObject(OBJ_OBJECT);
                int command_id = commandObject.getInt(INT_ID);
                //String command_instance = commandObject.getString("instance");
                //double command_score = commandObject.getDouble("score");
                return command_id;
            } else {
                return -1;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

}
