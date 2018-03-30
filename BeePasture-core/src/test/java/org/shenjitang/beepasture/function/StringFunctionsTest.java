/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.function;

import java.util.Date;
import java.util.Map;
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
public class StringFunctionsTest {
    
    public StringFunctionsTest() {
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
     * Test of unicode2str method, of class StringFunctions.
     */
    @Test
    public void testUnicode2str() {
        System.out.println("unicode2str");
        String str = "abc\\u93B444\\u93B4\\ww\\2w\\u93B4\\u93B4\\1";
        String expResult = "abc鎴44鎴\\ww\\2w鎴鎴\\1";
        String result = StringFunctions.unicode2str(str);
        assertEquals(expResult, result);
    }

    
}
