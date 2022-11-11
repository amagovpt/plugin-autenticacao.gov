package pt.gov.autenticacao.common;

import org.poreid.json.JSONObject;
import java.util.Base64;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Input {
    private final String name;
    private final String value;      
    
    public Input(ReturnParameter parameter, String value){
        this.name = parameter.getName();
        this.value = value;
    }
    
    
    public Input(ReturnParameter parameter, byte[] value){
        this.name = parameter.getName();
        this.value = Base64.getEncoder().encodeToString(value);        
    }
    
    
    @Override
    public String toString(){        
        return "<input type='hidden' name='"+name+"' value='"+value+"'/>"; 
    }
    
    public void buildJSON(JSONObject json){
        json.put(name, value);
    }
}
