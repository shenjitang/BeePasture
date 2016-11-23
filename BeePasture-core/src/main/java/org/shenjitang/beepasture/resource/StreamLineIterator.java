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
        InputStream stream;
        BufferedReader reader;
        String line = null;
        
        public StreamLineIterator(InputStream stream, String encoding) throws FileNotFoundException, UnsupportedEncodingException {
            this.stream = stream;
            reader = new BufferedReader(new InputStreamReader(stream, encoding));
        }
        
        @Override
        public boolean hasNext() {
            try {
                line = reader.readLine();
                if (line == null) {
                    stream.close();
                    return false;
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    stream.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
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
    
