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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
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
import org.shenjitang.beepasture.core.HrefElementCorrector;

/**
 *
 * @author xiaolie
 */
public class OkHttpTools implements HttpService {
    private static final Log LOGGER = LogFactory.getLog(OkHttpTools.class);
    private OkHttpClient httpClient;
    private static Proxy httpProxy;
    private static Proxy httpsProxy;
    private Map<String, List<Cookie>> cookieStore = new HashMap();
    
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
        httpClient = builder.cookieJar(new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
                LOGGER.info("================set cookie to " + httpUrl.host() + " ================");
                cookieStore.put(httpUrl.host(), list);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl httpUrl) {
                List<Cookie> cookies = cookieStore.get(httpUrl.host());
                LOGGER.info("================locad cookie from " + httpUrl.host() + " === " + (cookies == null ? "null" : cookies.size()));
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }
            
        }).build();
        LOGGER.info("================ httpClient " + httpClient.toString() + " ================");
    }
    
    
    @Override
    public String doGet(String url, Map heads, String encoding) throws Exception {
        Request request = buildHeadInRequest(url, heads).build();
        return execute(request, encoding);
    }

    @Override
    public Map<String, String> doHead(String url, Map heads) throws Exception {
        Map<String, String> headMap = new HashMap();
        Request request = buildHeadInRequest(url, heads).head().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Headers headers = response.headers();
                System.out.println("===========response headers=============");
                for (String headName : headers.names()) {
                    String headValue = headers.get(headName);
                    System.out.println(headName + ": " + headValue);
                    headMap.put(headName, headValue);
                }
                System.out.println("===========end=============");
            } else {
                LOGGER.warn("FAIL GET " + request.url().toString() + " " + response.code());
            }
        }
        return headMap;
    }

    @Override
    public String doPost(String url, String postBody, Map<String, String> heads, String encoding) throws Exception {
        System.out.println("POST: " + postBody);
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
    
    protected String fetchFilename(String url, Response response) {
        //从response的head Content-Disposition中得到文件名。
        String contentDisp = response.header("Content-Disposition");
        if (StringUtils.isNotBlank(contentDisp)) {
            String[] pair = contentDisp.split(";");
            for (String kv : pair) {
                String[] kva = kv.split("=");
                if (kva.length == 2) {
                    if ("filename".equalsIgnoreCase(kva[0].trim())) {
                        return kva[1];
                    }
                }
            }
        }
        //从url中得到文件名
        String urlLastPart = StringUtils.substringAfterLast(url, "/");
        if (urlLastPart.contains(".") && !urlLastPart.contains("?")) {
            return urlLastPart;
        }
        //实在不行就算一个
        URI uri = URI.create(url);
        String contentType = response.header("Content-Type");
        String extName = HrefElementCorrector.CONTENT_TYPE_MAP.get(contentType);
        if (extName == null) {
            try {
                extName = contentType.split("/")[1].trim();
            } catch (Exception e) {
                LOGGER.warn(contentType, e);
            }
        }
        return uri.getHost().replaceAll("\\.", "_") + "_" + System.currentTimeMillis() + "." + extName;
        
    }
    
    @Override
    public String downloadFile(String url, Map requestHeaders, String dir, String filename) throws IOException {
        Request request = buildHeadInRequest(url, requestHeaders).build();
        final StringBuilder returnFilename = new StringBuilder();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Headers headers = response.headers();
                System.out.println("===========response headers=============");
                for (String headName : headers.names()) {
                    String headValue = headers.get(headName);
                    System.out.println(headName + ": " + headValue);
                }
                System.out.println("===========end=============");
                if (StringUtils.isBlank(filename)) {
                    filename = fetchFilename(url, response);
                }
                filename = filename.replaceAll(" ", "");
                InputStream input = response.body().byteStream();
                File file = new File(dir, filename);  
                try {  
                    FileOutputStream output = FileUtils.openOutputStream(file);  
                    try {  
                        IOUtils.copy(input, output);  
                    } finally {  
                        IOUtils.closeQuietly(output);  
                    }  
                } finally {  
                    IOUtils.closeQuietly(input);  
//                    LOGGER.info("dataImage from url=>" + url);
                }  
                returnFilename.append(file.getCanonicalPath());
            } else {
                LOGGER.warn("FAIL GET " + request.url().toString() + " " + response.code());
            }
        }
        return returnFilename.toString();
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
        String mediaType = null;
        if (heads != null) {
            mediaType = (String)heads.get("Content-Type");
        }
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
