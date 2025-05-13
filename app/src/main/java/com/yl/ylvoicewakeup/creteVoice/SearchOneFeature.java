package com.yl.ylvoicewakeup.creteVoice;

import android.os.Build;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 声纹识别1:1
 */
public class SearchOneFeature {
    private String requestUrl;
    private String APPID;
    private String apiSecret;
    private String apiKey;
    //音频存放位置
    private static String AUDIO_PATH;

    //解析Json
    private static Gson json = new Gson();
    private static String score;
    private static String featureInfo;
    private static String featureId;
    private CreateLogotype createLogotype;
    private static String  groupId;


    //构造函数,为成员变量赋值
    public SearchOneFeature(String requestUrl, String APPID, String apiSecret, String apiKey, String AUDIO_PATH, CreateLogotype createLogotype,String  groupId,String featureId) {
        this.requestUrl = requestUrl;
        this.APPID = APPID;
        this.apiSecret = apiSecret;
        this.apiKey = apiKey;
        this.AUDIO_PATH = AUDIO_PATH;
        this.createLogotype = createLogotype;
        this.groupId = groupId;
        this.featureId = featureId;
    }

    //提供给主函数调用的方法
    public static Map<String, String> doSearchOneFeature(String requestUrl, String APPID, String apiSecret, String apiKey, String AUDIO_PATH, CreateLogotype createLogotype,String  groupId,String featureId) {
        SearchOneFeature searchOneFeature = new SearchOneFeature(requestUrl, APPID, apiSecret, apiKey, AUDIO_PATH, createLogotype,groupId,featureId);
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        try {
            searchOneFeature.doRequest(new NetSuccess() {
                @Override
                public void OnSuccess(String success) {
                    Map<String, String> resultMap = new HashMap<>();
                    Log.d("对比结果", "resp=>: " + success);
                    JsonParse myJsonParse = json.fromJson(success, JsonParse.class);
                    String textBase64Decode = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            textBase64Decode = new String(Base64.getDecoder().decode(myJsonParse.payload.searchScoreFeaRes.text), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    JSONObject jsonObject = JSON.parseObject(textBase64Decode);
                    Log.d("解码", "text字段Base64解码后=>" + jsonObject);
                    JSONObject objectList = JSON.parseObject(textBase64Decode);
                    resultMap.put("score", objectList.getString("score"));
                    resultMap.put("featureId", objectList.getString("featureId"));
                    resultMap.put("featureInfo", objectList.getString("featureInfo"));
                    future.complete(resultMap);
                    Log.d("解码", "text字段Base64解码后=>" + jsonObject);
                }

                @Override
                public void OnError(String e) {

                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
            e.printStackTrace();
        }
        try {
            return future.get(5, TimeUnit.SECONDS); // 阻塞等待，最多 5 秒
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "请求失败: " + e.getMessage());
            return errorMap;
        }
    }

    /**
     * 请求主方法
     *
     * @return 返回服务结果
     * @throws Exception 异常
     */
    public void doRequest(final NetSuccess netSuccess) throws Exception {
        new Thread(() -> {
            HttpURLConnection httpURLConnection = null;
            OutputStream out = null;
            InputStream is = null;
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
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    is = httpURLConnection.getInputStream();
                    String response = readAllBytes(is);
                    Log.d("1:1", "1：1对比返回结果: " + response);
                    netSuccess.OnSuccess(response);
                } else {
                    is = httpURLConnection.getErrorStream();
                    String error = "HTTP错误" + responseCode + "_" + readAllBytes(is);
                    Log.d("1:1错误", error);
                    netSuccess.OnError(error);
                }
            } catch (Exception e) {
                String error = "请求异常" + e.getMessage();
                netSuccess.OnError(error);
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
            StringBuilder builder = new StringBuilder("host: ").append(host).append("\n").
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
     *
     * @return 参数字符串
     */
    private String buildParam() throws IOException {
        String param = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("1:1比对", "buildParam: "+groupId);
            Log.d("1:1比对", "buildParam: "+featureId);
            Log.d("1:1比对List", "1:N比对List: "+createLogotype.getGroupId());
            Log.d("1:1比对List", "1:N比对List: "+createLogotype.getFeatureId());
            param = "{" +
                    "    \"header\": {" +
                    "        \"app_id\": \"" + APPID + "\"," +
                    "        \"status\": 3" +
                    "    }," +
                    "    \"parameter\": {" +
                    "        \"s782b4996\": {" +
                    "            \"func\": \"searchScoreFea\"," +
                    //这里填上所需要的groupId
                    "            \"groupId\": \"" +groupId + "\"," +
                    //这里填上所需要的featureId
                    "            \"dstFeatureId\": \"" + featureId+ "\"," +
                    "            \"searchScoreFeaRes\": {" +
                    "                \"encoding\": \"utf8\"," +
                    "                \"compress\": \"raw\"," +
                    "                \"format\": \"json\"" +
                    "            }" +
                    "        }" +
                    "    }," +
                    "\"payload\":{" +
                    "    \"resource\": {" +
                    //这里根据不同的音频编码填写不同的编码格式
                    "        \"encoding\": \"raw\"," +
                    "        \"sample_rate\": 16000," +
                    "        \"channels\": 1," +
                    "        \"bit_depth\": 16," +
                    "        \"status\": " + 3 + "," +
                    "        \"audio\": \"" + Base64.getEncoder().encodeToString(read(AUDIO_PATH)) + "\"" +
                    "    }}" +
                    "}";
        }
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

    public static byte[] read(String filePath) throws IOException {
        InputStream in = new FileInputStream(filePath);
        byte[] data = inputStream2ByteArray(in);
        in.close();
        return data;
    }

    private static byte[] inputStream2ByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
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
        public SearchScoreFeaRes searchScoreFeaRes;
    }

    class SearchScoreFeaRes {
        public String compress;
        public String encoding;
        public String format;
        public String text;
    }

    //添加回调
    public interface NetSuccess {
        void OnSuccess(String success);

        void OnError(String e);
    }
}
