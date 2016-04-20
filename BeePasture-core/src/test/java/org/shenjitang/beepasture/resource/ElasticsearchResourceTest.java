/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.net.URI;
import org.elasticsearch.client.Client;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author xiaolie
 */
public class ElasticsearchResourceTest {
    
    public ElasticsearchResourceTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of init method, of class ElasticsearchResource.
     */
    @Test
    public void testInit() throws Exception {
        System.out.println("init");
        String uriStr = "elasticsearch://120.26.232.150:9300,121.41.74.119:9300/index";
        URI uri = URI.create(uriStr);
        ElasticsearchResource instance = new ElasticsearchResource();
        instance.init(uri);
        assertEquals(instance.getIndex(), "index");
        assertNull(instance.getType());
        uriStr = "elasticsearch://120.26.232.150:9300,121.41.74.119:9300/index/type";
        uri = URI.create(uriStr);
        instance = new ElasticsearchResource();
        instance.init(uri);
        assertEquals(instance.getIndex(), "index");
        assertEquals(instance.getType(), "type");
        instance.close();
        uriStr = "elasticsearch://120.26.232.150:9300,121.41.74.119:9300";
        uri = URI.create(uriStr);
        instance = new ElasticsearchResource();
        instance.init(uri);
        assertNull(instance.getType());
        assertNull(instance.getIndex());
        instance.close();
        uriStr = "elasticsearch://120.26.232.150:9300,121.41.74.119:9300/";
        uri = URI.create(uriStr);
        instance = new ElasticsearchResource();
        instance.init(uri);
        assertNull(instance.getType());
        assertNull(instance.getIndex());
        instance.close();
    }

}
