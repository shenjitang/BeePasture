/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;
import com.mongodb.MongoClient;
import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.shenjitang.mongodbutils.MongoDbOperater;

/**
 *
 * @author xiaolie
 */
public class MongoDbResource {
    private MongoClient mongoClient;
    private MongoDbOperater mongoDbOperater;
    private String databaseName;
    private String url;

    public MongoDbResource(String url) throws Exception {
        this.url = url;
        init();
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
    
    private void init() throws Exception {
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
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
    
}
