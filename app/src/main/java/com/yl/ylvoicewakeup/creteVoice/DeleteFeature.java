package com.yl.ylvoicewakeup.creteVoice;

import static android.content.ContentValues.TAG;

import android.os.Build;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
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
 * <p>Title : 非实时引擎demo</p>
 * <p>Description : http服务通用demo</p>
 * <p>Date : 2020/8/4 </p>
 *
 * @author : aicloud
 */
public class DeleteFeature {
    private String requestUrl;
    private String APPID;
    private String apiSecret;
    private String apiKey;
    //解析Json
    private static Gson json = new Gson();
    public String featureId;
    public String groupId;
    public CreateLogotype createLogotype;

    //构造函数,为成员变量赋值
    public DeleteFeature(String requestUrl, String APPID, String apiSecret, String apiKey, CreateLogotype createLogotype,String groupId,String featureId) {
        this.requestUrl = requestUrl;
        this.APPID = APPID;
        this.apiSecret = apiSecret;
        this.apiKey = apiKey;
        this.createLogotype = createLogotype;
        this.groupId = groupId;
        this.featureId = featureId;
    }

    //提供给主函数调用的方法
    public static void doDeleteFeature(String requestUrl, String APPID, String apiSecret, String apiKey, CreateLogotype createLogotype,String groupId,String featureId, NetCallDeleteCrete netCallDeleteCrete) {
        DeleteFeature deleteFeature = new DeleteFeature(requestUrl, APPID, apiSecret, apiKey, createLogotype,groupId,featureId);
        deleteFeature.doRequest(new NetCallDeleteCrete() {
            @Override
            public void OnSuccess(String success) {
                try {
                    System.out.println("resp=>" + success);
                    JsonParse myJsonParse = json.fromJson(success, JsonParse.class);
                    String textBase64Decode = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        textBase64Decode = new String(Base64.getDecoder().decode(myJsonParse.payload.deleteFeatureRes.text), "UTF-8");
                    }
                    Log.d("解码", "OnSuccess: "+textBase64Decode);
                    JSONObject jsonObject = JSON.parseObject(textBase64Decode);
                    netCallDeleteCrete.OnSuccess(jsonObject.toString());
                } catch (UnsupportedEncodingException e) {
                    netCallDeleteCrete.OnError();
                    throw new RuntimeException(e);
                }
            }
            @Override
            public void OnError() {
                netCallDeleteCrete.OnError();
            }
        });
    }

    /**
     * 请求主方法
     *
     * @return 返回服务结果
     * @throws Exception 异常
     */
    public void doRequest(final NetCallDeleteCrete netCallDeleteCrete) {
        new Thread(() -> {
            HttpURLConnection httpURLConnection = null;
            InputStream is = null;
            OutputStream out = null;
            try {
                URL realUrl = new URL(buildRequetUrl());
                URLConnection connection = realUrl.openConnection();
                httpURLConnection = (HttpURLConnection) connection;
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-type", "application/json");

                out = httpURLConnection.getOutputStream();
                String params = buildParam();
                System.out.println("params=>" + params);
                out.write(params.getBytes());
                out.flush();
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode == httpURLConnection.HTTP_OK) {
                    is = httpURLConnection.getInputStream();
                    String response = readAllBytes(is);
                    Log.d("查询结果", "doRequest: " + response);
                    netCallDeleteCrete.OnSuccess(response);
                } else {
                    is = httpURLConnection.getErrorStream();
                    String error = "删除声纹特征错误码：" + responseCode + ", 信息：" + readAllBytes(is);
                    Log.e("错误", error);
                    netCallDeleteCrete.OnError();
                }
            } catch (IOException e) {
                Log.e("错误", "请求异常: " + e.getMessage());
                netCallDeleteCrete.OnError();
                throw new RuntimeException(e);
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
     * 处理请求URL
     * 封装鉴权参数等
     *
     * @return 处理后的URL
     */
    public String buildRequetUrl() {
        URL url = null;
        // 替换调schema前缀 ，原因是URL库不支持解析包含ws,wss schema的url
        String httpRequestUrl = requestUrl.replace("ws://", "http://").replace("wss://", "https://");
        try {
            url = new URL(httpRequestUrl);
            //获取当前日期并格式化
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());

            String host = url.getHost();
            if (url.getPort() != 80 && url.getPort() != 443) {
                host = host + ":" + String.valueOf(url.getPort());
            }
            StringBuilder builder = new StringBuilder("host: ").append(host).append("\n").//
                    append("date: ").append(date).append("\n").//
                    append("POST ").append(url.getPath()).append(" HTTP/1.1");
            Charset charset = Charset.forName("UTF-8");
            Mac mac = Mac.getInstance("hmacsha256");
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
            String sha = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sha = Base64.getEncoder().encodeToString(hexDigits);
            }

            String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
            String authBase = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                authBase = Base64.getEncoder().encodeToString(authorization.getBytes(charset));
            }
            return String.format("%s?authorization=%s&host=%s&date=%s", requestUrl, URLEncoder.encode(authBase), URLEncoder.encode(host), URLEncoder.encode(date));

        } catch (Exception e) {
            throw new RuntimeException("assemble requestUrl error:" + e.getMessage());
        }
    }

    /**
     * 组装请求参数
     * 直接使用示例参数，
     * 替换部分值
     *
     * @return 参数字符串
     */
    private String buildParam() throws IOException {
        Log.d(TAG, "buildParam: "+groupId);
        Log.d(TAG, "buildParam: "+featureId);
        String param = "{" +
                "    \"header\": {" +
                "        \"app_id\": \"" + APPID + "\"," +
                "        \"status\": 3" +
                "    }," +
                "    \"parameter\": {" +
                "        \"s782b4996\": {" +
                "            \"func\": \"deleteFeature\"," +
                //这里填上所需要的groupId
                "            \"groupId\": \""+groupId+"\"," +
                //这里填上所需要的featureId
                "            \"featureId\": \""+featureId+"\"," +
                "            \"deleteFeatureRes\": {" +
                "                \"encoding\": \"utf8\"," +
                "                \"compress\": \"raw\"," +
                "                \"format\": \"json\"" +
                "            }" +
                "        }" +
                "    }" +
                "}";
        return param;
    }

    /**
     * 读取流数据
     *
     * @param is 流
     * @return 字符串
     * @throws IOException 异常
     */
    private String readAllBytes(InputStream is) throws IOException {
        byte[] b = new byte[1024];
        StringBuilder sb = new StringBuilder();
        int len = 0;
        while ((len = is.read(b)) != -1) {
            sb.append(new String(b, 0, len, "utf-8"));
        }
        return sb.toString();
    }

    //Json解析
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
        //根据model的取值不同,名字有所变动。
        public DeleteFeatureRes deleteFeatureRes;
    }

    class DeleteFeatureRes {
        public String compress;
        public String encoding;
        public String format;
        public String text;
    }

    public interface NetCallDeleteCrete {
        void OnSuccess(String success);

        void OnError();
    }
}