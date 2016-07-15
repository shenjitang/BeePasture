/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.util.Map;
import org.shenjitang.beepasture.resource.util.ExcelParser;

/**
 *
 * @author xiaolie
 */
public class ExcelResource extends FileResource {

    @Override
    public void persist(String varName, Object obj, Map params) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object loadResource(Map loadParam) throws Exception {
        return ExcelParser.parseExcel(file, null);
    }
    
}
