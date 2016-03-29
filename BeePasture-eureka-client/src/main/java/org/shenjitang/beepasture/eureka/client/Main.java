/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.eureka.client;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.client.ClientFactory;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;  
import com.netflix.loadbalancer.RandomRule;  
import com.netflix.loadbalancer.Server;  
/**
 *
 * @author xiaolie
 */
public class Main {
    public static void main(String[] args) {
// Register with Eureka  
//        DynamicPropertyFactory configInstance = com.netflix.config.DynamicPropertyFactory.getInstance();
//        ApplicationInfoManager applicationInfoManager = ApplicationInfoManager.getInstance();
        DiscoveryManager.getInstance().initComponent(
                new MyDataCenterInstanceConfig(),
                new DefaultEurekaClientConfig());
        ApplicationInfoManager.getInstance().setInstanceStatus(
                InstanceStatus.UP);
        

        String url = findServerUriRibbon();
        System.out.println("rul: " + url);
        Client client = Client.create();
        WebResource resource = client.resource(url);
        String res = resource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        System.out.println("response: " + res);
//        Jersey2ApplicationClientFactory clientFactory = Jersey2ApplicationClientFactory.newBuilder().build();
//        Jersey2ApplicationClient jersey2HttpClient = (Jersey2ApplicationClient) clientFactory.newClient(new DefaultEndpoint(url));
    }
    
    public static String findServerUri() {
        String vipAddress = "beepasture.shenjitang.org";
        //System.out.println("vipAddress:" + vipAddress);
        InstanceInfo nextServerInfo = DiscoveryManager.getInstance()
                .getDiscoveryClient()
                .getNextServerFromEureka(vipAddress, false);
        String url = nextServerInfo.getHomePageUrl() + "bee/service";
        System.out.println("server host: " + nextServerInfo.getHostName() + " port:" + nextServerInfo.getPort());
        return url;
    }

    public static String findServerUriRibbon() {
        
        // get LoadBalancer instance from configuration, properties file  
        DynamicServerListLoadBalancer lb = (DynamicServerListLoadBalancer) ClientFactory.getNamedLoadBalancer("eurekaclient");  
        // use RandomRule 's RandomRule algorithm to get a random server from lb 's server list  
        RandomRule randomRule = new RandomRule();  
        Server randomAlgorithmServer = randomRule.choose(lb, null);  
        System.out.println("random algorithm server host:" + randomAlgorithmServer.getHost() + ";port:" + randomAlgorithmServer.getPort());     
        return "http://" + randomAlgorithmServer.getHostPort() + "/bee/service";
    }
}

