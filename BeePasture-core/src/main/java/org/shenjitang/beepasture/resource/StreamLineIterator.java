/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import org.shenjitang.beepasture.resource.util.ResourceUtils;

/**
 *
 * @author xiaolie
 */
public class StreamLineIterator implements Iterator<Object> {
        BufferedReader reader;
        String line = null;
        
        public StreamLineIterator(InputStream stream, String encoding) throws FileNotFoundException, UnsupportedEncodingException {
            reader = new BufferedReader(new InputStreamReader(stream, encoding));
        }

        @Override
        public boolean hasNext() {
            try {
                line = reader.readLine();
                return line != null;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public Object next() {
            return line;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
}
    
