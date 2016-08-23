package org.shenjitang.beepasture.epl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.map.MapEventBean;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.shenjitang.beepasture.function.ScriptTemplateExecuter;

/**
 * Created by xiaolie on 2015/7/20.
 */
public class StreamInService {
    private Log logger = LogFactory.getLog(StreamInService.class);

    /*
    public void setTemplate(ScriptTemplateExecuter template) {
        this.template = template;
    }*/

    private ScriptTemplateExecuter template;
    public StreamInService() throws IOException {
        template = new ScriptTemplateExecuter();
    }

    private String name;
    private EventType eventType;
    private Map streamInDataTypeParams;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCamelProcedure(ProducerTemplate camelProcedure) {
        this.camelProcedure = camelProcedure;
    }

    private ProducerTemplate camelProcedure;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }


    public void setStreamInDataTypeParams(Map streamInDataTypeParams) {
        this.streamInDataTypeParams = streamInDataTypeParams;
    }

    public void streamIn(Object body, EPRuntime runtime) throws Exception {
        String dataType = (String)streamInDataTypeParams.get("type");
        if ("csv".equalsIgnoreCase(dataType)) {
            List<String> inList = new ArrayList();
            String data = (String)body;

            String fieldSplit = (String)streamInDataTypeParams.get("split");
            Map fieldDefine = (Map) streamInDataTypeParams.get("fields");

            String[] fieldValues = data.split(fieldSplit);
            Map streamObj = new HashMap();
            for (Object field : fieldDefine.keySet()) {
                Map v = (Map) fieldDefine.get(field);
                Integer idx = (Integer) v.get("index");
                String type = (String) v.get("type");
                if (type == null) {
                    type = "String";
                }
                String script = (String) v.get("script");
                String vv = fieldValues[idx];
                if (script != null) {
                    vv = template.expressCalcu(script, vv, null);
                }
                if (type.toLowerCase().startsWith("int")) {
                    BigDecimal db = new BigDecimal(vv);
                    streamObj.put(field, db.intValue());
                } else if (type.toLowerCase().startsWith("long")) {
                    BigDecimal db = new BigDecimal(vv);
                    streamObj.put(field, db.longValue());
                } else if (type.toLowerCase().startsWith("float")) {
                    streamObj.put(field, Float.valueOf(vv));
                } else if (type.toLowerCase().startsWith("double")) {
                    streamObj.put(field, Double.valueOf(vv));
                } else if (type.toLowerCase().startsWith("bool")) {
                    streamObj.put(field, Boolean.valueOf(vv));
                } else {
                    streamObj.put(field, vv.toString());
                }
            }
            if (runtime == null) {
                System.out.println("Esper runtime is null, skip...");
            } else {
                MapEventBean eventBean = new MapEventBean((Map) streamObj, eventType);
                runtime.sendEvent(eventBean);
            }

        } else {
            logger.warn("type:" + dataType + " not support yet! only csv support now.");
        }
    }
}
