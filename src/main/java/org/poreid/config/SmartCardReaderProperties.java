/*
 * The MIT License
 *
 * Copyright 2014, 2015, 2016 Rui Martinho (rmartinho@gmail.com), António Braz (antoniocbraz@gmail.com)
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
import java.util.Map;

/**
 *
 * @author POReID
 */
public class SmartCardReaderProperties {
    private static final String IMPLEMENTING_CLASS = "implementing-class";
    private static final String SUPPORTED_OSES = "supported-oses";
    private static final String SUPPORTED_SMARTCARDS = "supported-smartcards";
    private static final String SUPPORTS = "supports";
    private static final String OS_NAME = "os-name";
    private static final String PROPERTIES = "properties";
    
    private final String implementingClass;
    private final Map<String, OsSupportedOperations> supportedOses = new HashMap<>();
    private final Map<String, SmartCardSupportedOperations> supportedSmartCards = new HashMap<>();
    
    
    
    public SmartCardReaderProperties(HashMap<String,Object> map){       
        implementingClass = (String)map.get(IMPLEMENTING_CLASS);
        ((ArrayList)map.get(SUPPORTED_OSES)).forEach((element) -> {            
            HashMap<String,Object> data = (HashMap)element;            
            supportedOses.put(((String)data.get(OS_NAME)), new OsSupportedOperations((HashMap<String,String>)data.get(SUPPORTS)));
        });
        ((ArrayList)map.get(SUPPORTED_SMARTCARDS)).forEach((element) -> {
            HashMap<String,Object> data = (HashMap)element;
            supportedSmartCards.put((String)data.get(IMPLEMENTING_CLASS), new SmartCardSupportedOperations((HashMap<String,String>)data.get(PROPERTIES)));
        });
    }
    
    
    public String getImplementingClass(){
        return implementingClass;
    }
    
    
    public boolean isSmartCardSupported(String implementingClassName){
        return supportedSmartCards.containsKey(implementingClassName);
    }
    
    
    public boolean isReaderVerifySupported(String osString){
        OsSupportedOperations supported = supportedOses.get(osString);
        
        if (null != supported){
            return supported.isVerify();
        }
        
        return false;
    }
    
    
    public boolean isReaderModifySupported(String osString){
        OsSupportedOperations supported = supportedOses.get(osString);
        
        if (null != supported){
            return supported.isModify();
        }
        
        return false;
    }
    
    
    public boolean isReaderInjectSupported(String osString){
        OsSupportedOperations supported = supportedOses.get(osString);
        
        if (null != supported){
            return supported.isInject();
        }
        
        return false;
    }
    
    
    public boolean isCardModifyCapable(String implementingClassName){
        SmartCardSupportedOperations supported = supportedSmartCards.get(implementingClassName);
        
        if (null != supported){
            return supported.isModifyCapable();
        }
        
        return false;
    }
    
    
    public boolean isCardVerifyCapable(String implementingClassName){
        SmartCardSupportedOperations supported = supportedSmartCards.get(implementingClassName);
        
        if (null != supported){
            return supported.isVerifyCapable();
        }
        
        return false;
    }
}
