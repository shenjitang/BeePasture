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
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
/**
 *
 * @author xiaolie
 */
public class HttpTools4 implements HttpService {
    private static final Log LOGGER = LogFactory.getLog(HttpTools4.class);
    private CloseableHttpClient httpClient;
    //private HttpClientContext context = null;
    private long timeout = 30000L;

    public HttpTools4() {
        CookieStore cookieStore = new BasicCookieStore();
        httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .setConnectionTimeToLive(10000L, TimeUnit.MILLISECONDS).build();
    }
    
    public HttpTools4(Boolean ssl) throws Exception {
        if (ssl) {
            httpClient = new SSLClient();
        } else {
            CookieStore cookieStore = new BasicCookieStore();
            HttpClientConnectionManager connMrg = createConnMng();
            httpClient = HttpClients.custom()
                    .setConnectionManager(connMrg)
                    .setDefaultCookieStore(cookieStore)
                    .setConnectionTimeToLive(10000L, TimeUnit.MILLISECONDS).build();
        }
    }
    @Override
    public String downloadFile(String url, Map requestHeaders, String dir, String filename) throws IOException {  
        HttpGet httpget = new HttpGet(url);  
        if (requestHeaders != null) {
            for (Object key : requestHeaders.keySet()) {
                httpget.setHeader(key.toString(), requestHeaders.get(key).toString());
            }
        }
        try {
            HttpResponse response = httpClient.execute(httpget);  
            if (StringUtils.isBlank(filename)) {
                Header header = response.getLastHeader("Content-Disposition");
                if (header != null) {
                    String contentDisp = header.getValue();
                    if (org.apache.commons.lang.StringUtils.isNotBlank(contentDisp)) {
                        String[] pair = contentDisp.split(";");
                        for (String kv : pair) {
                            String[] kva = kv.split("=");
                            if (kva.length == 2) {
                                if ("filename".equalsIgnoreCase(kva[0].trim())) {
                                    filename = kva[1];
                                }
                            }
                        }
                    }
                }
                if (StringUtils.isBlank(filename)) {
                    URL ourl = new URL(url);
                    filename = ourl.getHost().replaceAll(".", "_")+ "_" + System.currentTimeMillis();
                }
            }
            HttpEntity entity = response.getEntity();  
            InputStream input = null;  
            try {  
                input = entity.getContent();  
                File file = new File(dir, filename);  
                FileOutputStream output = FileUtils.openOutputStream(file);  
                try {  
                    IOUtils.copy(input, output);  
                } finally {  
                    IOUtils.closeQuietly(output);  
                }  
            } finally {  
                IOUtils.closeQuietly(input);  
            }  
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                LOGGER.warn("download httpclient.close", e);
            }
        }
        return filename;
    }  
    
    public void download2(String url, final String fileName) throws Exception {
        HttpGet httpget = new HttpGet(url);
        try {
        String responseBody = httpClient.execute(httpget, new ResponseHandler<String> () {
            
            @Override
            public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                File file = new File(fileName);  
                FileOutputStream output = FileUtils.openOutputStream(file);  
                HttpEntity entity = response.getEntity();
                System.out.println("*********** isStreaming:" + entity.isStreaming() + " entity class:" + entity.getClass().getName());
                entity.writeTo(output);
                output.flush();
                output.close();
                return null;
            }

        });
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                LOGGER.warn("download httpclient.close", e);
            }
        }
    }
    
    @Override
    public String doGet(String url, Map heads, final String encoding) throws Exception {
        HttpGet httpget = new HttpGet(url);
        if (heads != null) {
            for (Object key : heads.keySet()) {
                httpget.setHeader(key.toString(), heads.get(key).toString());
            }
        }
        if (heads == null || !heads.containsKey("User-Agent")) {
            httpget.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:49.0) Gecko/20100101 Firefox/49.0");
        }
        String responseBody = null;
        try {
        responseBody = httpClient.execute(httpget, new ResponseHandler<String> () {

            @Override
            public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                String page = null;
                try {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        Header headerEncoding = entity.getContentEncoding();
                        String encod =  headerEncoding==null?null:headerEncoding.getValue();
                        if (encod == null) {
                            Header contentType = entity.getContentType();
                            if (contentType != null) {
                                HeaderElement[] hes = contentType.getElements();
                                for (HeaderElement he : hes) {
                                    String heName = he.getName();// mime-type 比如：application/json
                                    NameValuePair pair = he.getParameterByName("charset");
                                    if (pair != null) {
                                        encod = pair.getValue();
                                    }
                                }
                            }
                        }
                        if (StringUtils.isBlank(encod)) {
                            encod = encoding;
                        }
                        if (StringUtils.isBlank(encod)) {
                            byte[] bytes = EntityUtils.toByteArray(entity);
                            page = new String(bytes, "GBK");
                            try {
                                int idx = page.indexOf("charset=");
                                String x = page.substring(idx + 8, idx + 20).toLowerCase();
                                if (x.startsWith("\"") || x.startsWith("\'")) {
                                    x = x.substring(1);
                                }
                                if (x.indexOf("\"") > 0) {
                                    encod = x.substring(0, x.indexOf("\""));
                                } else if (x.indexOf("\'") > 0) {
                                    encod = x.substring(0, x.indexOf("\'"));
                                } else if (x.indexOf(" ") > 0) {
                                    encod = x.substring(0, x.indexOf(" "));
                                } else {
                                    encod = x;
                                }
                                if (!encod.toLowerCase().contains("gb")) {
                                    page = new String(bytes, encod);
                                }
                            } catch (Exception e) {
                                LOGGER.info("unknow charset user gbk for default " + e.getMessage());
                            }                            
                        } else {
                            page = entity != null ? EntityUtils.toString(entity, encod) : null;
                        }
                        response.getEntity().getContent().close();
                    } else {
                        response.getEntity().getContent().close();
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                    
                } catch (Exception e) {
                    response.getEntity().getContent().close();
                }
                return page;
            }
        });
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                LOGGER.warn("doGet httpclient.close", e);
            }
        }
        return responseBody;
    }    
    
    
    @Override
    public String doPost(String url, String postBody, Map<String, String> headers, String encoding) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                Object value = entry.getValue();
                httpPost.addHeader(entry.getKey(), value == null?"":value.toString());
            }
        }
        httpPost.setEntity(new StringEntity(postBody, "utf-8"));
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        try {
            String responseBody = httpClient.execute(httpPost, responseHandler);
//            System.out.println("----------------------------------------");
//            System.out.println(responseBody);
//            System.out.println("----------------------------------------");
            return responseBody;
        } catch (HttpResponseException e) {
            System.out.println(e.getStatusCode() + "  " + e.getMessage());
            e.printStackTrace();
            return "";
        } finally {
            httpClient.close();
        }
    }
    
    @Override
    public String doPost(String url, Map<String, String> formParams, Map<String, String> headers, String encoding) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.addHeader(entry.getKey(), entry.getValue());
        }
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : formParams.entrySet()) {
            params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        HttpEntity entity = new UrlEncodedFormEntity(params);
        httpPost.setEntity(entity);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        try {
            return httpClient.execute(httpPost, responseHandler);
        } finally {
            httpClient.close();
        }
    }
    

    public String doSSLGet(String url, Map heads, final String encoding) throws Exception {
        String result = null;
        final SSLClient client = new SSLClient();  
        HttpGet httpget = new HttpGet(url);
        if (heads != null) {
            for (Object key : heads.keySet()) {
                httpget.setHeader(key.toString(), heads.get(key).toString());
            }
        }
        HttpResponse response = client.execute(httpget);
        if (response != null) {
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                result = EntityUtils.toString(resEntity, encoding);
            }
        }
        return result;
    }
    
    public static void main(String[] args) throws Exception {
        HttpTools4 tools = new HttpTools4();
        Map head = new HashMap();
        head.put("HOST", "www.jd.com:443");
        String content = tools.doSSLGet("https://www.jd.com/:443", head, "gbk");
        System.out.println(content);
    }

    private HttpClientConnectionManager createConnMng() {

        // SSL context for secure connections can be created either based on
        // system or application specific properties.
        SSLContext sslcontext = SSLContexts.createSystemDefault();

        // Create a registry of custom connection socket factories for supported
        // protocol schemes.
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.INSTANCE)
            .register("https", new SSLConnectionSocketFactory(sslcontext))
            .build();

        // Create a connection manager with custom configuration.
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        // Create socket configuration
        SocketConfig socketConfig = SocketConfig.custom()
            //.setSoKeepAlive(true)
            .setSoTimeout(1000)
            //.setTcpNoDelay(true)
            .build();
        // Configure the connection manager to use socket configuration either
        // by default or for a specific host.
        connManager.setDefaultSocketConfig(socketConfig);
        // Validate connections after 1 sec of inactivity
        connManager.setValidateAfterInactivity(1000);

        // Configure total max or per route limits for persistent connections
        // that can be kept in the pool or leased by the connection manager.
        connManager.setMaxTotal(100);
        connManager.setDefaultMaxPerRoute(10);
        return connManager;
    }

    @Override
    public String dataImage(String url) throws IOException {
        String imgType = url.substring(url.lastIndexOf(".") + 1);
        HttpGet httpget = new HttpGet(url);  
        HttpResponse response = httpClient.execute(httpget);  
        HttpEntity entity = response.getEntity();  
        InputStream input = null;  
        try {  
            input = entity.getContent();
            byte[] bytes = IOUtils.toByteArray(input);
            String str = Base64.getEncoder().encodeToString(bytes);
            return "data:image/" + imgType + ";base64," + str;
        } finally {  
            IOUtils.closeQuietly(input);  
            LOGGER.info("dataImage from url=>" + url);
        }  
    }

    @Override
    public Map<String, String> doHead(String url, Map heads) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
 
}
