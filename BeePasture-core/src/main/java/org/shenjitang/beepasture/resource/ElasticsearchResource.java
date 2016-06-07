/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

/**
 *
 * @author xiaolie
 */
public class ElasticsearchResource {
    private TransportClient client;
    private String index;
    private String type;
    private String cluster;

    public ElasticsearchResource() {
    }

    public ElasticsearchResource(URI uri) throws UnknownHostException {
        init(uri);
    }
    
    public void close() {
        client.close();
    }
    
    public void init(URI uri) throws UnknownHostException {
        String query = uri.getQuery();
        String[] ipPorts = uri.getAuthority().split(",");
        if (StringUtils.isNoneBlank(query)) {
            String[] querys = query.split("&");
            for (String q : querys) {
                String[] qc = q.split("=");
                if ("cluster".equalsIgnoreCase(qc[0])) {
                    cluster = qc[1];
                }
            }
        }
        if (StringUtils.isBlank(cluster)) {
            client = TransportClient.builder().build();
        } else {
            Settings settings = Settings.settingsBuilder() 
                    .put("cluster.name", cluster).build();
            client = TransportClient.builder().settings(settings).build();
        }
        for (String ipport : ipPorts) {
            String[] ipp = ipport.split(":");
            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ipp[0]), ipp.length>1?Integer.valueOf(ipp[1]):9300));
        }

        
        String path = uri.getPath();
        if (StringUtils.isNotBlank(path)) {
            String[] p = path.split("/");
            if (p.length > 1) {
                index = p[1];
            }
            if (p.length > 2) {
                type = p[2];
            }
        }
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                close();
            }
        });
        Runtime.getRuntime().addShutdownHook(th);
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TransportClient getClient() {
        return client;
    }

    public void setClient(TransportClient client) {
        this.client = client;
    }
    
    public static void main(String[] args) {
        String uriStr = "elasticsearch://120.26.232.150:9300,121.41.74.119:9300/index?cluster=ruiyun_es_cluster&a=b";
        URI uri = URI.create(uriStr);
        System.out.println("Scheme:" + uri.getScheme());
        System.out.println("Authority:" + uri.getAuthority());
        System.out.println("Fragment:" + uri.getFragment());
        System.out.println("host:" + uri.getHost());
        System.out.println("path:" + uri.getPath());
        System.out.println("query:" + uri.getQuery());
        uriStr = "elasticsearch://120.26.232.150:9300,121.41.74.119:9300/index/type";
        uri = URI.create(uriStr);
        System.out.println("Scheme:" + uri.getScheme());
        System.out.println("Authority:" + uri.getAuthority());
        System.out.println("Fragment:" + uri.getFragment());
        System.out.println("host:" + uri.getHost());
        System.out.println("path:" + uri.getPath());
        uriStr = "elasticsearch://120.26.232.150:9300,121.41.74.119:9300";
        uri = URI.create(uriStr);
        System.out.println("Scheme:" + uri.getScheme());
        System.out.println("Authority:" + uri.getAuthority());
        System.out.println("Fragment:" + uri.getFragment());
        System.out.println("host:" + uri.getHost());
        System.out.println("path:" + uri.getPath());
    }
}
