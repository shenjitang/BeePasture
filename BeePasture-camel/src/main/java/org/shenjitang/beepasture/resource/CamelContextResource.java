/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author xiaolie
 */
public class CamelContextResource extends BeeResource {
    private ApplicationContext springContext;
    private CamelContext camelContext;
    private ProducerTemplate camelProducer;
    private ConsumerTemplate camelConsumer;

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param);
        String part = uri.getSchemeSpecificPart();
        System.out.println("******part: " + part);
        if (StringUtils.isNotBlank(part)) {
            springContext = new FileSystemXmlApplicationContext(part);
            camelContext = springContext.getBean(CamelContext.class);
        } else {
            camelContext = new DefaultCamelContext();
        }
        camelProducer = camelContext.createProducerTemplate();
        camelConsumer = camelContext.createConsumerTemplate();
    }

    public ApplicationContext getSpringContext() {
        return springContext;
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    public ProducerTemplate getCamelProducer() {
        return camelProducer;
    }

    public ConsumerTemplate getCamelConsumer() {
        return camelConsumer;
    }
    
    public static void main(String[] args) throws Exception {
//        String url = "camelContext:cameltest.xml";
//        CamelContextResource resource = new CamelContextResource();
//        resource.init(url, null);
        org.shenjitang.beepasture.core.Main.main(new String[] {"../examples/szb_info_camel.yaml"});
        //org.shenjitang.beepasture.core.Main.main(new String[] {"../examples/camel_only.yaml", "-d"});
        
    }

    @Override
    public void persist(String varName, Object obj, Map params) throws Exception {
        String to = (String)params.get("route");
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                try {
                    camelProducer.requestBody(to, item);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
