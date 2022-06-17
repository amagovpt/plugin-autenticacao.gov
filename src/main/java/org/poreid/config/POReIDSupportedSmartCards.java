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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author POReID
 */
public class POReIDSupportedSmartCards {
    private static final String IMPLEMENTING_CLASS = "implementing-class";
    private static final String CACHE_ENABLED = "cache-enabled";
    private static final String VALIDITY = "validity";
    
    private final Map<String, POReIDSupportedSmartCardProperties> propertiesMap = new HashMap<>();
    
    
    public void addSmartCard(String atr, POReIDSupportedSmartCardProperties properties){
        propertiesMap.put(atr, properties);
    }
    
    
    public void addSmartCard(String atr, HashMap<String, Object> map){
        POReIDSupportedSmartCardProperties properties;        
        properties = new POReIDSupportedSmartCardProperties((String)map.get(IMPLEMENTING_CLASS), 
                Boolean.valueOf((String)map.get(CACHE_ENABLED)),
                Integer.parseInt((String)map.get(VALIDITY)));
        propertiesMap.put(atr, properties);
    }
    
    
    public boolean isSupportedSmartCard(String atr){
        return propertiesMap.containsKey(atr);
    }
    
    
    public POReIDSupportedSmartCardProperties getSupportedSmartCardData(String atr){
        return propertiesMap.get(atr);
    }
}
