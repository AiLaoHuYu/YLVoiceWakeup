package com.yl.ylvoicewakeup.creteVoice;

import android.os.Build;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 查询音频特征列表
 */
public class QueryFeatureList {
    private String requestUrl;
    private String APPID;
    private String apiSecret;
    private String apiKey;
    private CreateLogotype createLogotype;

    //解析Json
    private static Gson json = new Gson();
    String id = null;

    //构造函数,为成员变量赋值
    public QueryFeatureList(String requestUrl, String APPID, String apiSecret, String apiKey, CreateLogotype createLogotype,String id) {
        this.requestUrl = requestUrl;
        this.APPID = APPID;
        this.apiSecret = apiSecret;
        this.apiKey = apiKey;
        this.createLogotype = createLogotype;
        this.id = id;
    }

    //提供给主函数调用的方法
    public static void doQueryFeatureList(String requestUrl, String APPID, String apiSecret,
                                          String apiKey, CreateLogotype createLogotype,String  id, final NetCall call) {
        QueryFeatureList queryFeatureList = new QueryFeatureList(requestUrl, APPID, apiSecret, apiKey, createLogotype,id);
        queryFeatureList.doRequest(new NetCall() {
            @Override
            public void OnSuccess(String success) {
                try {
                    JsonParse myJsonParse = json.fromJson(success, JsonParse.class);
                    String textBase64Decode = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        textBase64Decode = new String(Base64.getDecoder().decode(myJsonParse.payload.queryFeatureListRes.text), "UTF-8");
                    }
                    JSONArray jsonArray = JSON.parseArray(textBase64Decode);
                    Log.d("查询特征列表", "text字段Base64解码后=>" + jsonArray);
                    call.OnSuccess(jsonArray.toString());
                } catch (Exception e) {
                    Log.e("错误", "解析结果失败: " + e.getMessage());
                    call.OnError();
                }
            }
            @Override
            public void OnError() {
                call.OnError();
            }
        });
    }

    /**
     * 请求主方法（异步）
     */
    public void doRequest(final NetCall call) {
        new Thread(() -> {
            HttpURLConnection httpURLConnection = null;
            OutputStream out = null;
            InputStream is = null;
            try {
                URL realUrl = new URL(buildRequetUrl());
                httpURLConnection = (HttpURLConnection) realUrl.openConnection();
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-type", "application/json");

                out = httpURLConnection.getOutputStream();
                String params = buildParam();
                Log.d("查询特征列表", "params=>" + params);
                out.write(params.getBytes());
                out.flush();

                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    is = httpURLConnection.getInputStream();
                    String response = readAllBytes(is);
                    Log.d("查询特征列表结果", response);
                    call.OnSuccess(response);
                } else {
                    is = httpURLConnection.getErrorStream();
                    String error = "错误码：" + responseCode + ", 信息：" + readAllBytes(is);
                    Log.e("错误", error);
                    call.OnError();
                }
            } catch (Exception e) {
                Log.e("错误", "请求异常: " + e);
                call.OnError();
            } finally {
                try {
                    if (out != null) out.close();
                    if (is != null) is.close();
                    if (httpURLConnection != null) httpURLConnection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 处理请求URL（与CreateFeature一致）
     */
    public String buildRequetUrl() {
        URL url = null;
        String httpRequestUrl = requestUrl.replace("ws://", "http://").replace("wss://", "https://");
        try {
            url = new URL(httpRequestUrl);
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());

            String host = url.getHost();
            if (url.getPort() != 80 && url.getPort() != 443) {
                host = host + ":" + url.getPort();
            }

            StringBuilder builder = new StringBuilder("host: ").append(host).append("\n")
                    .append("date: ").append(date).append("\n")
                    .append("POST ").append(url.getPath()).append(" HTTP/1.1");

            Charset charset = Charset.forName("UTF-8");
            Mac mac = Mac.getInstance("hmacsha256");
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
            String sha = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sha = Base64.getEncoder().encodeToString(hexDigits);
            }

            String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                    apiKey, "hmac-sha256", "host date request-line", sha);
            String authBase = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                authBase = Base64.getEncoder().encodeToString(authorization.getBytes(charset));
            }

            return String.format("%s?authorization=%s&host=%s&date=%s",
                    requestUrl, URLEncoder.encode(authBase), URLEncoder.encode(host), URLEncoder.encode(date));
        } catch (Exception e) {
            throw new RuntimeException("组装URL失败: " + e.getMessage());
        }
    }

    /**
     * 组装请求参数
     */
    private String buildParam() {

        Log.d("查询特征列表", "groupId: " + id);

        return "{" +
                "    \"header\": {" +
                "        \"app_id\": \"" + APPID + "\"," +
                "        \"status\": 3" +
                "    }," +
                "    \"parameter\": {" +
                "        \"s782b4996\": {" +
                "            \"func\": \"queryFeatureList\"," +
                "            \"groupId\": \"" + id + "\"," +
                "            \"queryFeatureListRes\": {" +
                "                \"encoding\": \"utf8\"," +
                "                \"compress\": \"raw\"," +
                "                \"format\": \"json\"" +
                "            }" +
                "        }" +
                "    }" +
                "}";
    }

    /**
     * 读取流数据（与CreateFeature一致）
     */
    private String readAllBytes(InputStream is) throws IOException {
        byte[] b = new byte[1024];
        StringBuilder sb = new StringBuilder();
        int len;
        while ((len = is.read(b)) != -1) {
            sb.append(new String(b, 0, len, "utf-8"));
        }
        return sb.toString();
    }

    // Json解析类
    class JsonParse {
        public Header header;
        public Payload payload;
    }

    class Header {
        public int code;
        public String message;
        public String sid;
        public int status;
    }

    class Payload {
        public QueryFeatureListRes queryFeatureListRes;
    }

    class QueryFeatureListRes {
        public String compress;
        public String encoding;
        public String format;
        public String text;
    }

    // 回调接口（与CreateFeature一致）
    public interface NetCall {
        void OnSuccess(String success);
        void OnError();
    }
}