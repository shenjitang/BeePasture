/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.event.map.MapEventBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.shenjitang.beepasture.core.BeeGather;
import org.shenjitang.beepasture.resource.BeeResource;

/**
 *
 * @author xiaolie
 */
public class EsperProcess {
    private final Log logger = LogFactory.getLog(EsperProcess.class);

    private final Map<String, EventType> eventTypeMap = new HashMap();
    private EPServiceProvider epService;
    private EPAdministrator admin;
    private EPStatement state;
    private EPRuntime runtime;
    private boolean started = false;

    public EsperProcess() {
    }

    public void start(Map eplProcessMap) throws Exception{
        epService = EPServiceProviderManager.getDefaultProvider();
        admin = epService.getEPAdministrator();
        Map eventTypes = (Map)eplProcessMap.get("eventType");
        for (Object name : eventTypes.keySet()) {
            addEventType((String) name, (Map) eventTypes.get(name));
        }
        List eplList = (List)eplProcessMap.get("epl");
        state = createEpl(eplList);
        final Map returnMap = (Map)eplProcessMap.get("save");
        final String to = (String)returnMap.get("to");
        final BeeResource saveToResource = BeeGather.getInstance().getResourceMng().getResource(to);
        
        state.addListener(new UpdateListener() {
            @Override
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                if (newEvents != null)  {
                    EventBean first = newEvents[0];
                    saveToResource.persist(null, first, returnMap);
                }
            }            
        });
        runtime = epService.getEPRuntime();
        started = true;
    }


    public void addEventType(String name, Map<String, String> fields) {
        Map typeMap = new HashMap();
        for (String key : fields.keySet()) {
            String value = fields.get(key).trim();
            if (value.toLowerCase().startsWith("str")) {
                typeMap.put(key, String.class);
            } else if (value.toLowerCase().startsWith("int")) {
                typeMap.put(key, int.class);
            } else if (value.toLowerCase().startsWith("long")) {
                typeMap.put(key, long.class);
            } else if (value.toLowerCase().startsWith("float")) {
                typeMap.put(key, float.class);
            } else if (value.toLowerCase().startsWith("double")) {
                typeMap.put(key, double.class);
            } else if (value.toLowerCase().startsWith("bool")) {
                typeMap.put(key, boolean.class);
            }
        }
        admin.getConfiguration().addEventType(name, typeMap);
        eventTypeMap.put(name, admin.getConfiguration().getEventType(name));
    }

    protected EPStatement createEpl(List epls) {
        for (Object epl : epls) {
            logger.debug("createEpl:" + epl);
            state = admin.createEPL((String)epl);
        }
        return state;
    }

    public void streamIn(Map body, Map params) throws Exception {
        if (started) {
            if (runtime == null) {
                System.out.println("Esper runtime is null, skip...");
            } else {
                String eventTypeName = (String)params.get("eventType");
                MapEventBean eventBean = new MapEventBean(body, eventTypeMap.get(eventTypeName));
                runtime.sendEvent(eventBean);
            }
        } else {
            logger.warn("EsperProcess have not finish started now. skip stream in ...");
        }
    }

}
