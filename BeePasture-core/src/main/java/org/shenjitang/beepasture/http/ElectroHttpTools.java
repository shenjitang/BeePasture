/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author xiaolie
 */
public class ElectroHttpTools implements HttpService, Runnable {
    private static final Log LOGGER = LogFactory.getLog(ElectroHttpTools.class);

    public ElectroHttpTools() {
        this(false);
    }
    
    public ElectroHttpTools(Boolean ssl) {
        
    }
    
    
    @Override
    public String doGet(String url, Map heads, String encoding) throws Exception {
        return ElectroHttpProxy.getInstance().doGet(url, heads, encoding);
    }
    
    public static void main(String[] args) throws Exception {
        String url = "http://weixin.sogou.com/weixin?type=1&s_from=input&query=Tle13676832302";
        ElectroHttpTools tools = new ElectroHttpTools();
        String page = tools.doGet(url, null, "gbk");
        System.out.println(page);
    }



    @Override
    public String printRequest() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String downloadFile(String url, Map requestHeaders, String dir, String filename, String postBody) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String dataImage(String url) throws IOException {
        if (url.contains("data:image")) {
            return url.substring(url.indexOf("data:image"));
        }
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        String imgType = url.substring(url.lastIndexOf(".") + 1);
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
        return url;
    }

    @Override
    public Map<String, String> doHead(String url, Map heads) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String doPost(String url, String postBody, Map<String, String> heads, String encoding) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String doPost(String url, Map<String, String> formParams, Map<String, String> heads, String encoding) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
