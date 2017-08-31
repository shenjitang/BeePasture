/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author xiaolie
 */
public class OkHttpTools implements HttpService {
    private static final Log LOGGER = LogFactory.getLog(OkHttpTools.class);
    private OkHttpClient httpClient;
    private static Proxy httpProxy;
    private static Proxy httpsProxy;
    
    static {
        if (System.getProperty("httpProxy", "true").equalsIgnoreCase("true")) {
            Map proxyMap = ProxyService.getInstance().getProxy(false);
            if (proxyMap != null) {
                String ipPort = (String) proxyMap.get("ip");
                String ip = org.apache.commons.lang3.StringUtils.substringBefore(ipPort, ":");
                Integer port = Integer.valueOf(org.apache.commons.lang3.StringUtils.substringAfter(ipPort, ":"));
                if (checkHttpProxy(ip, port)) {
                    httpProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
                    LOGGER.info("================proxy: " + ip + ":" + port + " 可用 ================");
                } else {
                    LOGGER.info("================proxy: " + ip + ":" + port + " 不可用 ================");
                }
            }
            proxyMap = ProxyService.getInstance().getProxy(true);
            if (proxyMap != null) {
                String ipPort = (String) proxyMap.get("ip");
                String ip = org.apache.commons.lang3.StringUtils.substringBefore(ipPort, ":");
                Integer port = Integer.valueOf(org.apache.commons.lang3.StringUtils.substringAfter(ipPort, ":"));
                if (checkHttpsProxy(ip, port)) {
                    httpsProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
                    LOGGER.info("================proxy: " + ip + ":" + port + " 可用 ================");
                } else {
                    LOGGER.info("================proxy: " + ip + ":" + port + " 不可用 ================");
                }
            }
        }
    }

    public OkHttpTools() {
        this(false);
    }
    
    public OkHttpTools(Boolean ssl) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS);
        Proxy proxy = ssl?httpsProxy:httpProxy ;
        if (proxy != null) {
            LOGGER.info("================use proxy: " + proxy.address().toString() + " ================");
            builder = builder.proxy(proxy);
        }
        httpClient = builder.build();
    }
    
    
    @Override
    public String doGet(String url, Map heads, String encoding) throws Exception {
        Request request = buildHeadInRequest(url, heads).build();
        return execute(request, encoding);
    }

    @Override
    public String doPost(String url, String postBody, Map<String, String> heads, String encoding) throws Exception {
        MediaType mediaType = getMediaType(heads);
        Request request = buildHeadInRequest(url, heads).post(RequestBody.create(mediaType, postBody)).build();
        return execute(request, encoding);
    }
    
    @Override
    public String doPost(String url, Map<String, String> formParams, Map<String, String> heads, String encoding) throws Exception {
        FormBody.Builder formBuilder = new FormBody.Builder();
        for (String key : formParams.keySet()) {
            formBuilder = formBuilder.add(key, formParams.get(key));
        }
        Request request = buildHeadInRequest(url, heads).post(formBuilder.build()).build();
        return execute(request, encoding);
    }

    @Override
    public String dataImage(String url) throws IOException {  
        String imgType = url.substring(url.lastIndexOf(".") + 1);
        Request request = buildHeadInRequest(url, null).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                InputStream input = response.body().byteStream();
                try {  
                    byte[] bytes = IOUtils.toByteArray(input);
                    String str = Base64.getEncoder().encodeToString(bytes);
                    return "data:image/" + imgType + ";base64," + str;
                } finally {  
                    IOUtils.closeQuietly(input);  
                    LOGGER.info("dataImage from url=>" + url);
                }  
            } else {
                LOGGER.warn("FAIL GET " + request.url().toString() + " " + response.code());
            }
        }
        return "";
    }  
    
    @Override
    public void downloadFile(String url, String dir) throws IOException {
        Request request = buildHeadInRequest(url, null).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                InputStream input = response.body().byteStream();
                try {  
                    File file = new File(dir);  
                    FileOutputStream output = FileUtils.openOutputStream(file);  
                    try {  
                        IOUtils.copy(input, output);  
                    } finally {  
                        IOUtils.closeQuietly(output);  
                    }  
                } finally {  
                    IOUtils.closeQuietly(input);  
                    LOGGER.info("dataImage from url=>" + url);
                }  
            } else {
                LOGGER.warn("FAIL GET " + request.url().toString() + " " + response.code());
            }
        }
    }

    private Request.Builder buildHeadInRequest(String url, Map heads) {
        Request.Builder builder = new Request.Builder().url(url);
        if (heads != null) {
            for (Object key : heads.keySet()) {
                builder = builder.addHeader(key.toString(), heads.get(key).toString());
            }
        }
        if (heads == null || !heads.containsKey("User-Agent")) {
            builder = builder.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:49.0) Gecko/20100101 Firefox/49.0");
        }
        return builder;
    }
    
    private String execute(Request request, final String encoding) throws Exception {
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (StringUtils.isBlank(encoding)) {
                    return response.body().string();
                } else {
                    byte[] bytes = response.body().bytes();
                    return new String(bytes, encoding);
                }
            } else {
                LOGGER.warn("FAIL GET " + request.url().toString() + " " + response.code());
            }
        }
        return null;
    }
    
    private MediaType getMediaType (Map<String, String> heads) {
        String mediaType = (String)heads.get("Content-Type");
        if (StringUtils.isBlank(mediaType)) {
            mediaType = "text/plain; charset=utf-8";
        }
        return MediaType.parse(mediaType);
    }

    public static boolean checkHttpProxy(String ip, Integer port) {
        return checkProxy(ip, port, "http://www.smzen.com/");
    }
    
    public static boolean checkHttpsProxy(String ip, Integer port) {
        return checkProxy(ip, port, "https://www.baidu.com/");
    }

    public static boolean checkProxy(String ip, Integer port, String url) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
                .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port)))
                .build();
        Request request = new Request.Builder().url(url).build();
        try {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return true;
                } else {
                    System.out.println("error code=" + response.code());
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return false;
    }
    
}
