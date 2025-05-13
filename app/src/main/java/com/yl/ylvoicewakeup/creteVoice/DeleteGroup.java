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
 * 删除声纹特征库
 */
public class DeleteGroup {
    private String requestUrl;
    private String APPID;
    private String apiSecret;
    private String apiKey;
    public String groupId;
    //解析Json
    private static Gson json = new Gson();

    //构造函数,为成员变量赋值
    public DeleteGroup(String requestUrl, String APPID, String apiSecret, String apiKey, String groupId) {
        this.requestUrl = requestUrl;
        this.APPID = APPID;
        this.apiSecret = apiSecret;
        this.apiKey = apiKey;
        this.groupId = groupId;
    }

    //提供给主函数调用的方法
    public static void doDeleteGroup(String requestUrl, String APPID, String apiSecret, String apiKey, String groupId,NetDeleteGroup netDeleteGroup) {
        DeleteGroup deleteGroup = new DeleteGroup(requestUrl, APPID, apiSecret, apiKey, groupId);
        deleteGroup.doRequest(new NetDeleteGroup() {
            @Override
            public void OnSuccessGroup(String success) {
                try {
                    System.out.println("resp=>" + success);
                    JsonParse myJsonParse = json.fromJson(success, JsonParse.class);
                    String textBase64Decode = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        textBase64Decode = new String(Base64.getDecoder().decode(myJsonParse.payload.deleteGroupRes.text), "UTF-8");
                    }
                    Log.d("解码：", "OnSuccessGroup: "+textBase64Decode);
                    JSONObject jsonObject = JSON.parseObject(textBase64Decode);
                    System.out.println("text字段Base64解码后=>" + jsonObject);
                    netDeleteGroup.OnSuccessGroup(jsonObject.toString());
                } catch (UnsupportedEncodingException e) {
                    netDeleteGroup.OnErrorGroup();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void OnErrorGroup() {
                netDeleteGroup.OnErrorGroup();
            }
        });

    }

    /**
     * 请求主方法
     *
     * @return 返回服务结果
     * @throws Exception 异常
     */
    public void doRequest(final NetDeleteGroup netDeleteGroup) {
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
                    netDeleteGroup.OnSuccessGroup(response);
                } else {
                    is = httpURLConnection.getErrorStream();
                    String error = "删除特征库错误码：" + responseCode + ", 信息：" + readAllBytes(is);
                    Log.e("错误", error);
                    netDeleteGroup.OnErrorGroup();
                }
            } catch (IOException e) {
                Log.e("错误", "请求异常: " + e.getMessage());
                netDeleteGroup.OnErrorGroup();
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
        Log.d(TAG, "buildParam: "+groupId);
        String param = "{" +
                "    \"header\": {" +
                "        \"app_id\": \"" + APPID + "\"," +
                "        \"status\": 3" +
                "    }," +
                "    \"parameter\": {" +
                "        \"s782b4996\": {" +
                "            \"func\": \"deleteGroup\"," +
                //这里填上所需要的groupId
                "            \"groupId\": \""+groupId+"\"," +
                "            \"deleteGroupRes\": {" +
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
        public DeleteGroupRes deleteGroupRes;
    }

    class DeleteGroupRes {
        public String compress;
        public String encoding;
        public String format;
        public String text;
    }

    //添加删除回调方法，通过回调获取是否删除成功
    public interface NetDeleteGroup {
        void OnSuccessGroup(String success);

        void OnErrorGroup();
    }
}
