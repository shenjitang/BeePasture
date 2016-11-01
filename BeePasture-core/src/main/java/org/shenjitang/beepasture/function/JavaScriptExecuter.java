/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author xiaolie
 */
public class JavaScriptExecuter {
    protected static final Log LOGGER = LogFactory.getLog(JavaScriptExecuter.class);
    static ScriptEngineManager manager = new ScriptEngineManager();
    public static Object exec(String script, Map params) {
        try {
            ScriptEngine engine = manager.getEngineByName("javascript");
            if (params != null) {
                for (Object key : params.keySet()) {
                    engine.put((String)key, params.get(key));
                }
            }
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            Object res = engine.eval(script);
            if (script.contains("_result")) {
                res = engine.get("_result");
            }
            return res;
        } catch (Exception e) {
            LOGGER.warn(script, e);
            return null;
        }
    }
}
