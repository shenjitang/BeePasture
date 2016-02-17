/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.junit.Ignore;

/**
 *
 * @author xiaolie
 */
public class ScriptTemplateExecuterTest extends TestCase {
    
    public ScriptTemplateExecuterTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of expressCalcu method, of class ScriptTemplateExecuter.
     */
    public void testExpressCalcu_String_Map() throws Exception {
        System.out.println("expressCalcu");
        String str = "${date(),dateFormat=\"yyyy-MM-dd\"} *** ${str.substring(abc, 0, str.indexOf(abc, \"*\"))}";
        Map params = new HashMap();
        params.put("abc", "123*456");
        ScriptTemplateExecuter instance = new ScriptTemplateExecuter();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String expResult = format.format(new Date()) + " *** 123";
        String result = instance.expressCalcu(str, params);
        assertEquals(expResult, result);
    }

    /**
     * Test of expressCalcu method, of class ScriptTemplateExecuter.
     */
//    @Ignore
//    public void testExpressCalcu_3args() throws Exception {
//        System.out.println("expressCalcu");
//        String str = "";
//        String it = "";
//        Map params = null;
//        ScriptTemplateExecuter instance = new ScriptTemplateExecuter();
//        String expResult = "";
//        String result = instance.expressCalcu(str, it, params);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    
}
