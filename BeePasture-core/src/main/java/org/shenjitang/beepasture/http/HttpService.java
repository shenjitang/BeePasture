/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.http;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author xiaolie
 */
public interface HttpService {
    public void downloadFile(String url, Map requestHeaders, String dir, String filename) throws IOException;
    public String dataImage(String url) throws IOException;
    public String doGet(String url, Map heads, String encoding) throws Exception;
    public String doPost(String url, String postBody, Map<String, String> heads, String encoding) throws Exception;
    public String doPost(String url, Map<String, String> formParams, Map<String, String> heads, String encoding) throws Exception;
}
