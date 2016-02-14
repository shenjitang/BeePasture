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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
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
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author xiaolie
 */
public class HttpTools {
    private DefaultHttpClient httpClient;
    private long timeout = 300000L;

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
                Header contentTypeHeader = entity.getContentType();
                System.out.println(contentTypeHeader.getName() + "=" + contentTypeHeader.getValue());
                Long len = entity.getContentLength();
                byte[] content = new byte[len.intValue()];
                InputStream in = entity.getContent();
                int idx = 0;
                long begin = System.currentTimeMillis();
                while (true) {
                    int a = in.available();
                    idx += in.read(content, idx, a);
                    if (idx >= len) {
                        break;
                    } else if (System.currentTimeMillis() - begin > timeout) {
                        throw new RuntimeException("下载超时 " +(System.currentTimeMillis() - begin) + "ms ");
                    } else {
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
    
    
    public String doGet(String url, Map heads) throws Exception {
        try {
            HttpGet httpget = new HttpGet(url);
            System.out.println("executing request " + httpget.getURI());
            // Create a response handler
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            if (heads != null) {
                for (Object key : heads.keySet()) {
                    httpget.setHeader(key.toString(), heads.get(key).toString());
                }
            }
            String responseBody = httpClient.execute(httpget, responseHandler);
//            System.out.println("----------------------------------------");
//            System.out.println(responseBody);
//            System.out.println("----------------------------------------");
            return responseBody;
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            // httpclient.getConnectionManager().shutdown();
        }
    }
    
    public String doGet(String url, Map heads, final String encoding) throws Exception {
        if (StringUtils.isBlank(encoding)) {
            return doGet(url, heads);
        }
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
                //System.out.println(headerEncoding.getName() + "=" + headerEncoding.getValue() + "      encoding=" + encoding);
                String encod = encoding;
                if (StringUtils.isBlank(encoding)) {
                    encod =  headerEncoding.getValue();
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), encod));   
                StringBuilder sb = new StringBuilder();   
                String line = null;   
                while ((line = reader.readLine()) != null) {   
                    sb.append(line + "/n");
                }
                return sb.toString();
            }
        });
        
        return responseBody;
    }
    
    public String doGZipGet(String url) throws Exception {
        try {
            HttpGet httpget = new HttpGet(url);
            httpget.addHeader("Accept-Encoding", "gzip, deflate");
            System.out.println("executing request " + httpget.getURI());
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
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.addHeader(entry.getKey(), entry.getValue());
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
        while((line = bufferedReader.readLine()) != null){
            str.append(line);
            str.append("\n");
        }
        if(bufferedReader != null) {
            bufferedReader.close();
        }
        return str.toString();
    }
}
