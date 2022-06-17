package pt.gov.autenticacao.common;

import java.util.ArrayList;
import java.util.List;
import org.poreid.json.JSONObject;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Inputs {    
    private final List<Input> list = new ArrayList<>();
    
    
    public Inputs addInput(Input input){
        list.add(input);
        
        return this;
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Input input : list) {
            sb.append(input.toString());
        }
        
        return sb.toString();
    }
    
    
    public String toJSON(){
        JSONObject obj = new JSONObject();
        
        for (Input input : list) {
            input.buildJSON(obj);
        }
        
        return obj.toString();
    }
}
