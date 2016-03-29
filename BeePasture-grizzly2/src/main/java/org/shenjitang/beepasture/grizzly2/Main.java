package org.shenjitang.beepasture.grizzly2;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.netflix.discovery.EurekaClient;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;

import java.io.IOException;
import java.net.URI;

/**
 * Main class.
 *
 */
public class Main {
    // Base URI the Grizzly HTTP server will listen on
    //public static final String BASE_URI = "http://localhost:8080/bee/";
    public static final String BASE_URI = "http://0.0.0.0:8081/bee/";
    private static DynamicPropertyFactory configInstance;

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in org.shenjitang.beepasture.grizzly2 package
        final ResourceConfig rc = new ResourceConfig().packages("org.shenjitang.beepasture.service.impl");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
        Boolean eureka = Boolean.valueOf(System.getProperty("eureka", "true"));
        if (eureka) {
            System.out.println("start eureka reigster service...");
            registerWithEureka();
            waiteForEurekaRegister();
        }
        System.in.read();
        server.stop();
    }
    
    public static void registerWithEureka() {        
        // Register with Eureka  
        configInstance = com.netflix.config.DynamicPropertyFactory.getInstance();
        ApplicationInfoManager applicationInfoManager = ApplicationInfoManager.getInstance();
        DiscoveryManager.getInstance().initComponent(
                new MyDataCenterInstanceConfig(),
                new DefaultEurekaClientConfig());
        ApplicationInfoManager.getInstance().setInstanceStatus(
                InstanceStatus.UP);
    }

    public static void waiteForEurekaRegister() {
        //String vipAddress = "beePastureService.shenjitang.org";
        String vipAddress = configInstance.getStringProperty("eureka.vipAddress", "sampleservice.mydomain.net").get();
        System.out.println(">>> vipAddress=" + vipAddress);
        InstanceInfo nextServerInfo = null;
        while (nextServerInfo == null) {
            try {
                nextServerInfo = DiscoveryManager.getInstance()
                        .getDiscoveryClient()
                        .getNextServerFromEureka(vipAddress, false);
            } catch (Throwable e) {
                System.out
                        .println("Waiting for service to register with eureka..");

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block  
                    e1.printStackTrace();
                }

            }
        }
        System.out.println("Service started and ready to process requests..");
    }
}

