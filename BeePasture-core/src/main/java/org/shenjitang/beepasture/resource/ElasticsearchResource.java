/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import com.alibaba.fastjson.JSONObject;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

/**
 *
 * @author xiaolie
 */
public class ElasticsearchResource extends BeeResource {
    private TransportClient client;
    private String index;
    private String type;
    private String cluster;

    public ElasticsearchResource() {
        super();
    }

    public void close() {
        client.close();
    }

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param);
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

    @Override
    public void persist(String varName, Object obj, Map persistParams) throws Exception {
        Map allParam = new HashMap();
        allParam.putAll(this.params);
        allParam.putAll(persistParams);
        String _index = (String)allParam.get("_index");
        if (StringUtils.isBlank(_index)) {
            _index = index;
        }
        String _type = (String)allParam.get("_type");
        if (StringUtils.isBlank(_type)) {
            _type = type;
        }
        String idField = (String)allParam.get("_id");
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                try {
                    String _id = null;
                    if (org.codehaus.plexus.util.StringUtils.isNotBlank(idField)) {
                        _id = ((Map)item).remove(idField).toString();
                    } else if (((Map)item).containsKey("_id")) {
                        _id = ((Map)item).remove("_id").toString();
                    }
                    if (((Map)item).containsKey("_id")) {
                        ((Map)item).remove("_id");
                    }
                    String indexContent = JSONObject.toJSONString(item);
                    if (org.codehaus.plexus.util.StringUtils.isBlank(_id)) {
                        IndexRequest indexRequest = new IndexRequest(_index, _type).source(indexContent);
                        client.index(indexRequest);
                    } else {
                        IndexRequest indexRequest = new IndexRequest(_index, _type, _id).source(indexContent);
                        UpdateRequest updateRequest = new UpdateRequest(_index, _type, _id)
                               .doc(indexContent)
                               .upsert(indexRequest);              
                        client.update(updateRequest).get();       
                    }
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        } else {
            String _id = null;
            if (org.codehaus.plexus.util.StringUtils.isNotBlank(idField)) {
                _id = (String) ((Map) obj).remove(idField);
            }
            String indexContent = JSONObject.toJSONString(obj);
            if (org.codehaus.plexus.util.StringUtils.isBlank(_id)) {
                IndexRequest indexRequest = new IndexRequest(_index, _type).source(indexContent);
                client.index(indexRequest);
            } else {
                IndexRequest indexRequest = new IndexRequest(_index, _type, _id).source(indexContent);
                UpdateRequest updateRequest = new UpdateRequest(_index, _type, _id)
                        .doc(indexContent)
                        .upsert(indexRequest);
                client.update(updateRequest).get();
            }
        }    
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        List resultList = new ArrayList();
        Object queryFragments = loadParam.get("query");
        SearchRequestBuilder builder = client.prepareSearch(index);
        if (StringUtils.isNotBlank(type)) {
            builder = builder.setTypes(type);
        }
        if (queryFragments instanceof String) {
            builder = builder.setSource((String)queryFragments);
        } else if (queryFragments instanceof Map) {
            builder = builder.setSource((Map)queryFragments);
        }
        SearchResponse response = builder.execute().actionGet();
        //String str = response.toString();
        //return str;
        SearchHit[] searchHits = response.getHits().getHits();
        for (int i = 0; i < searchHits.length; i++) {
            SearchHit hit = searchHits[i];
            Map record = new HashMap();
            record.put("_id", hit.getId());
            if (hit.sourceAsMap() != null) {
                record.putAll(hit.sourceAsMap());
            }
            resultList.add(record);
//            for (String key : hit.fields().keySet()) {
//                SearchHitField field = hit.fields().get(key);
//                record.put(field.getName(), field.getName());
//            }
//            resultList.add(record);
        }
        return resultList;
    }
}
