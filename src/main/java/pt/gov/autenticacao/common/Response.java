package pt.gov.autenticacao.common;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Response {        
    private Inputs inputs = null;
    
    
    public Response(){        
    }
    
    public Response(Inputs inputs){
        this.inputs = inputs;        
    }
                    
    public String toJSON(){     
        return (null != inputs) ? inputs.toJSON() : null;
    }
}
