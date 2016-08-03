/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;
import com.mongodb.MongoClient;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.shenjitang.mongodbutils.MongoDbOperater;

/**
 *
 * @author xiaolie
 */
public class MongodbResource extends BeeResource {
    private MongoClient mongoClient;
    private MongoDbOperater mongoDbOperater;
    private String databaseName;
    private int batchExecuteCount = 100;

    public MongodbResource() throws Exception {
    }

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param); //To change body of generated methods, choose Tools | Templates.
        if (param.containsKey("batchExecuteCount")) {
            batchExecuteCount = Integer.valueOf(param.get("batchExecuteCount").toString());
        }
        String ip = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        if (StringUtils.isNoneBlank(path) && path.length() > 1) {
            databaseName = path.substring(1);
        }
        mongoClient = new MongoClient(ip, port);
        mongoDbOperater = new MongoDbOperater();
        mongoDbOperater.setMongoClient(mongoClient);
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public void setMongoClient(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public MongoDbOperater getMongoDbOperater() {
        return mongoDbOperater;
    }

    public void setMongoDbOperater(MongoDbOperater mongoDbOperater) {
        this.mongoDbOperater = mongoDbOperater;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }


    @Override
    public void persist(String varName, Object obj, Map persistParams) {
        Map allParam = new HashMap();
        allParam.putAll(this.params);
        allParam.putAll(persistParams);
        
        String dbName = (String) allParam.get("database");
        if (StringUtils.isBlank(dbName)) {
            dbName = databaseName;
        }
        String colName = (String) allParam.get("collection");
        if (StringUtils.isBlank(colName)) {
            colName = varName;
        }
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                mongoDbOperater.insert(dbName, colName, (Map)item);
            }
        } else {
            mongoDbOperater.insert(dbName, colName, (Map)obj);
        }
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        String dbName = (String) params.get("database");
        if (StringUtils.isBlank(dbName)) {
            dbName = databaseName;
        }
        String sql = (String) params.get("sql");
        return mongoDbOperater.find(dbName, sql);
    }
    
}
