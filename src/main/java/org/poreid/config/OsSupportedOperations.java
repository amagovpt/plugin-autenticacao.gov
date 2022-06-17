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

/**
 *
 * @author POReID
 */
public class OsSupportedOperations {  
    private final boolean verify;    
    private final boolean modify;    
    private final boolean inject;

    
    public OsSupportedOperations(HashMap<String,String> map){
        verify = (map.containsKey("verify") ? Boolean.valueOf(map.get("verify")) : false);
        modify = (map.containsKey("modify") ? Boolean.valueOf(map.get("modify")) : false);
        inject = (map.containsKey("inject") ? Boolean.valueOf(map.get("inject")) : false);
    }
    
    public boolean isVerify() {
        return verify;
    }
      
    
    public boolean isModify() {
        return modify;
    }
      
    
    public boolean isInject() {
        return inject;
    }       
}
