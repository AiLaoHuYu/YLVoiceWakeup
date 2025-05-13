package com.yl.ylvoicewakeup.creteVoice;


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
 * 创建声纹特征库
 */
public class CreateGroup {
    private String requestUrl;
    private String APPID;
    private String apiSecret;
    private String apiKey;
    private CreateLogotype createLogotype;
    //解析Json
    private static Gson json = new Gson();

    //构造函数,为成员变量赋值
    public CreateGroup(String requestUrl, String APPID, String apiSecret, String apiKey, CreateLogotype createLogotype) {
        this.requestUrl = requestUrl;
        this.APPID = APPID;
        this.apiSecret = apiSecret;
        this.apiKey = apiKey;
        this.createLogotype = createLogotype;
    }

    //提供给主函数调用的方法
    public static Map<String, String> doCreateGroup(String requestUrl, String APPID, String apiSecret, String apiKey, CreateLogotype createLogotype) {
        CreateGroup createGroup = new CreateGroup(requestUrl, APPID, apiSecret, apiKey, createLogotype);
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        createGroup.doRequest(new NetCallGroup() {
            @Override
            public void OnSuccess(String success) {
                try {
                    Map<String, String> resultMap = new HashMap<>();
                    Log.d("创建声纹特征库:", "resp=>" + success);
                    JsonParse myJsonParse = json.fromJson(success, JsonParse.class);
                    String textBase64Decode = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        textBase64Decode = new String(Base64.getDecoder().decode(myJsonParse.payload.createGroupRes.text), "UTF-8");
                    }
                    JSONObject jsonObject = JSON.parseObject(textBase64Decode);
                    Log.d("创建声纹特征库", "text字段Base64解码后=>" + jsonObject);
                    // 获取字段和值
                    resultMap.put("code", myJsonParse.header.code);
                    resultMap.put("message",  myJsonParse.header.message);
                    Log.d("TAG", "OnSuccessresultMap: " + resultMap);
                    future.complete(resultMap); // 完成 Future
                } catch (UnsupportedEncodingException e) {
                    future.completeExceptionally(e); // 完成 Future
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void OnError(String err) {
                future.completeExceptionally(new IOException(err)); // 传递错误
                Log.e("1:N比对", "请求失败: " + err);
            }
        });
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
    public void doRequest(final NetCallGroup callGroup) {
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

                // 写入请求参数
                out = httpURLConnection.getOutputStream();
                String params = buildParam();
                Log.d("创建声纹特征库", "params=>" + params);
                out.write(params.getBytes());
                out.flush();
                // 获取响应
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    is = httpURLConnection.getInputStream();
                    String response = readAllBytes(is);
                    Log.d("创建声纹特征库结果", "readAllBytes(is)=>" + response);
                    callGroup.OnSuccess(response); // 成功回调
                } else {
                    is = httpURLConnection.getErrorStream();
                    String error = "Error code: " + responseCode + ", message: " + readAllBytes(is);
                    Log.e("创建声纹特征库错误", error);
                }
            } catch (Exception e) {
                Log.e("创建声纹特征库异常", "Exception: " + e.getMessage());
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
            //String date = "Wed, 10 Jul 2019 07:35:43 GMT";
            String date = format.format(new Date());
            String host = url.getHost();
           /* if (url.getPort()!=80 && url.getPort() !=443){
                host = host +":"+String.valueOf(url.getPort());
            }*/
            StringBuilder builder = new StringBuilder("host: ").append(host).append("\n").//
                    append("date: ").append(date).append("\n").//
                    append("POST ").append(url.getPath()).append(" HTTP/1.1");
            System.err.println(builder);
            Charset charset = Charset.forName("UTF-8");
            Mac mac = Mac.getInstance("hmacsha256");
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
            String sha = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sha = Base64.getEncoder().encodeToString(hexDigits);
            }
            System.out.println("sha:" + sha);

            String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
            String authBase = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                authBase = Base64.getEncoder().encodeToString(authorization.getBytes(charset));
            }
            System.out.println("authBase:" + authBase);
            System.out.println(String.format("%s?authorization=%s&host=%s&date=%s", requestUrl, URLEncoder.encode(authBase), URLEncoder.encode(host), URLEncoder.encode(date)));
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
    private String buildParam() {
        String id = null;
        String groupName = null;
        String groupInfo = null;
        for (int i = 0; i < createLogotype.getGroupId().size(); i++) {
            id = createLogotype.getGroupId().get(i).toString();
        }
        for (int i = 0; i < createLogotype.getGroupName().size(); i++) {
            groupName = createLogotype.getGroupName().get(i).toString();
        }
        for (int i = 0; i < createLogotype.getGroupInfo().size(); i++) {
            groupInfo = createLogotype.getGroupInfo().get(i).toString();
        }
        Log.d("创建id 标识 描述", "buildParam: " + id + "\t" + groupName + "\t" + groupInfo);
        String param = "{" +
                "    \"header\": {" +
                "        \"app_id\": \"" + APPID + "\"," +
                "        \"status\": 3" +
                "    }," +
                "    \"parameter\": {" +
                "        \"s782b4996\": {" +
                "            \"func\": \"createGroup\"," +
                //分组ID
                "            \"groupId\": \"" + id + "\"," +
                //分组名称
                "            \"groupName\": \"" + groupName + "\"," +
                //分组描述
                "            \"groupInfo\": \"" + groupInfo + "\"," +
                "            \"createGroupRes\": {" +
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
        public String code;
        public String message;
        public String sid;
        public int status;
    }

    class Payload {
        //根据model的取值不同,名字有所变动。
        public CreateGroupRes createGroupRes;
    }

    class CreateGroupRes {
        public String compress;
        public String encoding;
        public String format;
        public String text;
    }

    public interface NetCallGroup {
        void OnSuccess(String success);

        void OnError(String err);
    }
}