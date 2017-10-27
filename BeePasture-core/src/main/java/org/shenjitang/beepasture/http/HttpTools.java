/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
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
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
/**
 *
 * @author xiaolie
 */
public class HttpTools implements HttpService {
    private static final Log LOGGER = LogFactory.getLog(HttpTools.class);
    private DefaultHttpClient httpClient;
    //private HttpClientContext context = null;
    private long timeout = 30000L;
    private static HttpRoutePlanner httpRoutePlanner = null;
    private static HttpRoutePlanner httpsRoutePlanner = null;
    
    static {
        if (System.getProperty("httpProxy", "true").equalsIgnoreCase("true")) {
            Map proxyMap = ProxyService.getInstance().getProxy(false);
            if (proxyMap != null) {
                String ipPort = (String) proxyMap.get("ip");
                String ip = org.apache.commons.lang3.StringUtils.substringBefore(ipPort, ":");
                Integer port = Integer.valueOf(org.apache.commons.lang3.StringUtils.substringAfter(ipPort, ":"));
                if (OkHttpTools.checkHttpProxy(ip, port)) {
                    httpRoutePlanner = new HttpRoutePlanner() {

                        @Override
                        public HttpRoute determineRoute(
                                HttpHost target,
                                HttpRequest request,
                                HttpContext context) throws HttpException {
                            return new HttpRoute(target, null,  new HttpHost(ip, port),
                                    "https".equalsIgnoreCase(target.getSchemeName()));
                        }

                    };                    
                    LOGGER.info("================proxy: " + ip + ":" + port + " 可用 ================");
                } else {
                    LOGGER.info("================proxy: " + ip + ":" + port + " 不可用 ================");
                }
            }
            proxyMap = ProxyService.getInstance().getProxy(true);
            if (proxyMap != null) {
                final String ipPort = (String) proxyMap.get("ip");
                final String ip = org.apache.commons.lang3.StringUtils.substringBefore(ipPort, ":");
                Integer port = Integer.valueOf(org.apache.commons.lang3.StringUtils.substringAfter(ipPort, ":"));
                if (OkHttpTools.checkHttpsProxy(ip, port)) {
                    httpsRoutePlanner = new HttpRoutePlanner() {

                        @Override
                        public HttpRoute determineRoute(
                                HttpHost target,
                                HttpRequest request,
                                HttpContext context) throws HttpException {
                            return new HttpRoute(target, null,  new HttpHost(ip, port),
                                    "https".equalsIgnoreCase(target.getSchemeName()));
                        }

                    };                    
                    LOGGER.info("================proxy: " + ip + ":" + port + " 可用 ================");
                } else {
                    LOGGER.info("================proxy: " + ip + ":" + port + " 不可用 ================");
                }
            }
        }
    }
    

//    public HttpTools() {
//        httpClient = new DefaultHttpClient();
//        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
//        httpClient.getParams().setParameter(ClientPNames.CONN_MANAGER_TIMEOUT, 10000L);
//    }
    
    public HttpTools(Boolean ssl) throws Exception {
        if (ssl) {
            httpClient = new SSLClient();
            httpClient.setRoutePlanner(httpRoutePlanner);
        } else {
            httpClient = new DefaultHttpClient();
            httpClient.setRoutePlanner(httpsRoutePlanner);
        }
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        
    }
    
    @Override
    public void downloadFile(String url, Map requestHeaders, String dir, String filename) throws IOException {  
        HttpGet httpget = new HttpGet(url);  
        if (requestHeaders != null) {
            for (Object key : requestHeaders.keySet()) {
                httpget.setHeader(key.toString(), requestHeaders.get(key).toString());
            }
        }
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
//                                System.out.println(new String(filename.getBytes("utf8")));
//                                System.out.println(new String(filename.getBytes("utf8"), "gbk"));
//                                System.out.println(new String(filename.getBytes("gbk")));
//                                System.out.println(new String(filename.getBytes("gbk"), "utf8"));
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
            LOGGER.info("download from url=>" + url + " to dir=>" + dir + " to file=>" + filename);
        }  
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
        responseBody = httpClient.execute(httpget, new ResponseHandler<String> () {

            @Override
            public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                String page;
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
                            if ("gzip".equals(encod)) {
                                page = entity != null ? readHtmlContentFromEntity(entity): null;
                            } else {
                                page = entity != null ? EntityUtils.toString(entity, encod) : null;
                            }
                        }
                        response.getEntity().getContent().close();
                        return page;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
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
        HttpTools tools = new HttpTools(true);
        Map head = new HashMap();
        head.put("HOST", "www.jd.com:443");
        String content = tools.doSSLGet("https://www.jd.com/:443", head, "gbk");
        System.out.println(content);
    }
 
}
