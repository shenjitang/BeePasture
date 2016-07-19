/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
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
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import javax.net.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
/**
 *
 * @author xiaolie
 */
public class HttpTools {
    private static final Log LOGGER = LogFactory.getLog(HttpTools.class);
    private DefaultHttpClient httpClient;
    //private HttpClientContext context = null;
    private long timeout = 30000L;

    public HttpTools() {
        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
    }
    
    public void downloadFile(String url, String dir) throws IOException {  
        HttpGet httpget = new HttpGet(url);  
        HttpResponse response = httpClient.execute(httpget);  
        HttpEntity entity = response.getEntity();  
        InputStream input = null;  
        try {  
            input = entity.getContent();  
            File file = new File(dir);  
            FileOutputStream output = FileUtils.openOutputStream(file);  
            try {  
                IOUtils.copy(input, output);  
            } finally {  
                IOUtils.closeQuietly(output);  
            }  
        } finally {  
            IOUtils.closeQuietly(input);  
        }  
    }  
  
    
    public void download(String url, final String fileName) throws Exception {
        HttpGet httpget = new HttpGet(url);
        String responseBody = httpClient.execute(httpget, new ResponseHandler<String> () {
            
            @Override
            public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                HttpEntity entity = response.getEntity();
                //Header contentTypeHeader = entity.getContentType();
                Long len = entity.getContentLength();
                byte[] content = new byte[len.intValue()];
                InputStream in = entity.getContent();
                int idx = 0;
                long begin = System.currentTimeMillis();
                while (true) {
                    int a = in.available();
                    System.out.println("下载，缓存到到：" + a +" 个字节");
                    if (a > 0) {
                        int aa = in.read(content, idx, a);
                        System.out.println("下载，读到：" + a +" 个字节");
                        idx += aa;
                        if (idx >= len) {
                            break;
                        } else if (System.currentTimeMillis() - begin > timeout) {
                            System.out.println("下载超时，共下载到：" + idx+ "个字节的数据");
                            FileUtils.writeByteArrayToFile(new File(fileName), content);
                            throw new RuntimeException("下载超时 " +(System.currentTimeMillis() - begin) + "ms ");
                        } else {
                            try {
                                Thread.sleep(50);
                            } catch (Exception ee) {
                                ee.printStackTrace();
                            }
                        }
                    } else {
                        if (System.currentTimeMillis() - begin > timeout) {
                            System.out.println("下载超时，共下载到：" + idx+ "个字节的数据");
                            throw new RuntimeException("下载超时 " +(System.currentTimeMillis() - begin) + "ms ");
                        }
                            try {
                                Thread.sleep(50);
                            } catch (Exception ee) {
                                ee.printStackTrace();
                            }
                    }
                }
                FileUtils.writeByteArrayToFile(new File(fileName), content);
                return null;
            }

        });
    }
    
    public void download2(String url, final String fileName) throws Exception {
        HttpGet httpget = new HttpGet(url);
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
    }
    
//    public String doGet(String url, Map heads) throws Exception {
//        try {
//            HttpGet httpget = new HttpGet(url);
//            LOGGER.debug("executing request " + httpget.getURI());
//            // Create a response handler
//            ResponseHandler<String> responseHandler = new BasicResponseHandler();
//            if (heads != null) {
//                for (Object key : heads.keySet()) {
//                    httpget.setHeader(key.toString(), heads.get(key).toString());
//                }
//            }
//            return httpClient.execute(httpget, responseHandler);
//        } finally {
//            // When HttpClient instance is no longer needed,
//            // shut down the connection manager to ensure
//            // immediate deallocation of all system resources
//            // httpclient.getConnectionManager().shutdown();
//        }
//    }
//
    public String doGet(String url, Map heads, final String encoding) throws Exception {
//        if (StringUtils.isBlank(encoding)) {
//            return doGet(url, heads);
//        }
        HttpGet httpget = new HttpGet(url);
        if (heads != null) {
            for (Object key : heads.keySet()) {
                httpget.setHeader(key.toString(), heads.get(key).toString());
            }
        }
        String responseBody = httpClient.execute(httpget, new ResponseHandler<String> () {

            @Override
            public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                HttpEntity entity = response.getEntity();
                Header headerEncoding = entity.getContentEncoding();
                Header[] respHeaders = response.getHeaders("Set-Cookie");
                List<Cookie> cookies = httpClient.getCookieStore().getCookies();
                for (Header respHeader : respHeaders) {
                    Cookie cookie = parseSetCookie(respHeader.getValue());
                    if (cookie != null) {
                        boolean find = false;
                        for (int i = 0; i < cookies.size(); i++) {
                            Cookie c = cookies.get(i);
                            if (c.getName().trim().equals(cookie.getName().trim())) {
                                cookies.set(i, cookie);
                                find = true;
                                break;
                            }
                        }
                        if (!find) {
                            cookies.add(cookie);
                        }
                    }
                }
                httpClient.getCookieStore().clear();
                for (Cookie c : cookies) {
                    httpClient.getCookieStore().addCookie(c);
                }
                //setContext();
                //System.out.println(headerEncoding.getName() + "=" + headerEncoding.getValue() + "      encoding=" + encoding);

                String encod =  headerEncoding==null?null:headerEncoding.getValue();
                String charset = null;
                Header contentType = entity.getContentType();
                if (contentType != null) {
                    HeaderElement[] hes = contentType.getElements();
                    for (HeaderElement he : hes) {
                        String heName = he.getName();// mime-type 比如：application/json
                        NameValuePair pair = he.getParameterByName("charset");
                        if (pair != null) {
                            charset = pair.getValue();
                        }
                    }
                }
                if (StringUtils.isBlank(charset)) {
                    charset = encoding;
                }
                int contentLength;
                Header[] cls = response.getHeaders("Content-Length");
                if (cls.length > 0 ) {
                    contentLength = Integer.valueOf(cls[0].getValue());
                } else {
                    contentLength = 1024000;
                }
                if ("gzip".equalsIgnoreCase(encod)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream(contentLength);
                    byte[] bytes = new byte[1024];
                    int count = 0;
                    while(true) {
                        int size = entity.getContent().read(bytes, 0, 1024);
                        if (size < 0) {
                            break;
                        } else {
                            out.write(bytes, 0, size);   
                            count += size;
                        }
                    }
                    return out.toString(charset);
                } else {
                    BufferedReader reader = new BufferedReader(charset==null? new InputStreamReader(entity.getContent()): new InputStreamReader(entity.getContent(), charset));   
                    StringBuilder sb = new StringBuilder();   
                    String line = null;   
                    while ((line = reader.readLine()) != null) {   
                        sb.append(line + "/n");
                    }
                    return sb.toString();
                }
            }
        });
        return responseBody;
    }
    
    public String doGZipGet(String url) throws Exception {
        try {
            HttpGet httpget = new HttpGet(url);
            httpget.addHeader("Accept-Encoding", "gzip, deflate");
            //System.out.println("executing request " + httpget.getURI());
            String responseBody = httpClient.execute(httpget, new ResponseHandler<String>() {
                public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
//                    Header[] contentEncodeings = response.getHeaders("Content-Encoding");
//                    String encod = "";
//                    if (contentEncodeings.length > 0) {
//                        encod = contentEncodeings[0].getValue();
//                    }
                      HttpEntity httpEntity = response.getEntity();
                      String html = readHtmlContentFromEntity(httpEntity);
                      return html;
                }
            });
            return responseBody;
        } finally {
        }
    }
    
    
    public String doPost(String url, String postBody, Map<String, String> headers) throws Exception {
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
        }
    }
    
    public String doPost(String url, Map<String, String> formParams, Map<String, String> headers) throws Exception {
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
        String responseBody = httpClient.execute(httpPost, responseHandler);
//        System.out.println("----------------------------------------");
//        System.out.println(responseBody);
//        System.out.println("----------------------------------------");
        return responseBody;
    }
    
    /**
     * 从response返回的实体中读取页面代码
     * @param httpEntity Http实体
     * @return 页面代码
     * @throws ParseException
     * @throws IOException
     */
    private String readHtmlContentFromEntity(HttpEntity httpEntity) throws ParseException, IOException {
        String html = "";
        Header header = httpEntity.getContentEncoding();
        if(httpEntity.getContentLength() < 2147483647L){            //EntityUtils无法处理ContentLength超过2147483647L的Entity
            if(header != null && "gzip".equals(header.getValue())){
                html = EntityUtils.toString(new GzipDecompressingEntity(httpEntity), "gbk");
            } else {
                html = EntityUtils.toString(httpEntity, "gbk");
            }
        } else {
            InputStream in = httpEntity.getContent();
            String chartSet = ContentType.getOrDefault(httpEntity).getCharset().toString();
            if(header != null && "gzip".equals(header.getValue())){
                html = unZip(in, chartSet);
            } else {
                html = readInStreamToString(in, chartSet);
            }
            if(in != null){
                in.close();
            }
        }
        return html;
    }
    /**
     * 解压服务器返回的gzip流
     * @param in 抓取返回的InputStream流
     * @param charSet 页面内容编码
     * @return 页面内容的String格式
     * @throws IOException
     */
    private String unZip(InputStream in, String charSet) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPInputStream gis = null;
        try {
            gis = new GZIPInputStream(in);
            byte[] _byte = new byte[1024];
            int len = 0;
            while ((len = gis.read(_byte)) != -1) {
                baos.write(_byte, 0, len);
            }
            String unzipString = new String(baos.toByteArray(), charSet);
            return unzipString;
        } finally {
            if (gis != null) {
                gis.close();
            }
            if(baos != null){
                baos.close();
            }
        }
    }
    /**
     * 读取InputStream流
     * @param in InputStream流
     * @return 从流中读取的String
     * @throws IOException
     */
    private String readInStreamToString(InputStream in, String charSet) throws IOException {
        StringBuilder str = new StringBuilder();
        String line;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, charSet));
        try {
            while ((line = bufferedReader.readLine()) != null) {
                str.append(line);
                str.append("\n");
            }
        } finally {
            bufferedReader.close();
        }
        return str.toString();
    }

    public Cookie parseSetCookie(String value) {
        String[] items = value.split(";");
        if (items.length > 0) {
            String[] nameValue = new String[2];
			nameValue[0] = items[0].substring(0, items[0].indexOf("="));
			nameValue[1] = items[0].substring(items[0].indexOf("=") + 1, items[0].length());
            if (nameValue.length > 1) {
                BasicClientCookie cookie = new BasicClientCookie(nameValue[0], nameValue[1]);
                for (int i = 1; i < items.length; i++) {
                    String[] nv = items[i].trim().split("=");
                    if (nv.length > 1) {
                        if (nv[0].trim().equalsIgnoreCase("Domain")) {
                            cookie.setDomain(nv[1].trim());
                        } else if (nv[0].trim().equalsIgnoreCase("Path")) {
                            cookie.setPath(nv[1].trim());
                        } else if (nv[0].trim().equalsIgnoreCase("Commit")) {
                            cookie.setComment(nv[1].trim());
                        } else if (nv[0].trim().equalsIgnoreCase("Version")) {
                            cookie.setVersion(Integer.valueOf(nv[1].trim()));
                        } else if (nv[0].trim().equalsIgnoreCase("Expires")) {
                            try {
                                java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US);
                                format.setTimeZone(TimeZone.getTimeZone("GMT"));
                                cookie.setExpiryDate(format.parse(nv[1].trim()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return cookie;
            }
        }
        return null;
    }
    
//    public static void setContext() {
//        System.out.println("----setContext");
//        context = HttpClientContext.create();
//        Registry<CookieSpecProvider> registry = RegistryBuilder
//                .<CookieSpecProvider>create()
//                .register(CookieSpecs.BEST_MATCH, new BestMatchSpecFactory())
//                .register(CookieSpecs.BROWSER_COMPATIBILITY,
//                        new BrowserCompatSpecFactory()).build();
//        context.setCookieSpecRegistry(registry);
//        context.setCookieStore(cookieStore);
//    }
    
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
        HttpTools tools = new HttpTools();
        Map head = new HashMap();
        head.put("HOST", "www.baidu.com:443");
        String content = tools.doSSLGet("https://www.baidu.com:443", head, "gbk");
        System.out.println(content);
    }
 
}
