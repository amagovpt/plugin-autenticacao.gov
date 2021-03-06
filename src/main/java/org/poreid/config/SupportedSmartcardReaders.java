/*
 * The MIT License
 *
 * Copyright 2019 Rui Martinho (rmartinho@gmail.com), António Braz (antoniocbraz@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.poreid.config;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author POReID
 */
public class SupportedSmartcardReaders {
    private static final String GENERIC = "generic reader";
    private static final String ALIAS = "alias";
    private static final String PROPERTIES = "properties";
    
    private final HashMap<String, SmartCardReaderProperties> supportedReaders = new HashMap<>();
    
    
    public SupportedSmartcardReaders(){        
    }
    
    
    public void addSmartcardReaders(HashMap<String, Object> map){
        SmartCardReaderProperties properties = new SmartCardReaderProperties((HashMap<String, Object>)map.get(PROPERTIES));
        ArrayList<String> aliases = (ArrayList<String>)map.get(ALIAS);                 
        
        aliases.forEach((alias) -> {            
            supportedReaders.put(alias, properties);            
        });
    }
    
    
    public String getImplementingClass(String alias) {
        SmartCardReaderProperties properties;
        if (supportedReaders.containsKey(alias)) {
            properties = supportedReaders.get(alias);
        } else {
            properties = supportedReaders.get(GENERIC);
        }
        return properties.getImplementingClass();
    }
    
    
    public SmartCardReaderProperties getSmartCardReaderProperties(String alias){
        SmartCardReaderProperties properties;
        
        if (supportedReaders.containsKey(alias)) {
            properties = supportedReaders.get(alias);
        } else {
            properties = supportedReaders.get(GENERIC);
        }
        
        return properties;
    }
}
