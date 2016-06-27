/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.net.URI;
import java.util.HashMap;
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
public class CamelContextResource implements BeeResource {
    private ApplicationContext springContext;
    private CamelContext camelContext;
    private ProducerTemplate camelProducer;
    private ConsumerTemplate camelConsumer;

    @Override
    public void init(URI uri, Map param) throws Exception {
        String schema = uri.getScheme();
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
        String url = "camelContext:cameltest.xml";
        URI uri = URI.create(url);
        CamelContextResource resource = new CamelContextResource();
        resource.init(uri, null);
        
    }
    
}
