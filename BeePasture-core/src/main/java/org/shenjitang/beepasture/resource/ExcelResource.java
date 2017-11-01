/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.util.Map;
import org.shenjitang.beepasture.core.GatherStep;
import org.shenjitang.beepasture.resource.util.ExcelParser;

/**
 *
 * @author xiaolie
 */
public class ExcelResource extends FileResource {

    @Override
    public void persist(GatherStep gatherStep, String varName, Object obj, Map params) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object loadResource(GatherStep gatherStep, Map loadParam) throws Exception {
        return ExcelParser.parseExcel(file, null);
    }
    
}
