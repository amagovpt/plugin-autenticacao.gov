package pt.gov.autenticacao.autenticacao.cc.pan;

import java.nio.charset.StandardCharsets;
import pt.gov.autenticacao.common.AbstractContainer;
import pt.gov.autenticacao.common.ContainerException;
import pt.gov.autenticacao.common.Input;
import pt.gov.autenticacao.common.Inputs;
import pt.gov.autenticacao.common.ReturnParameter;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class PanContainer extends AbstractContainer{
    private String pan;
    
                
    public void setPAN(String pan){
        this.pan = pan;
    }
    
        
    public Inputs getInputs() throws ContainerException{
        Inputs inputs = new Inputs();
        
        inputs.addInput(new Input(ReturnParameter.PAN, encrypt(pan.getBytes(StandardCharsets.UTF_8))));
        inputs.addInput(new Input(ReturnParameter.NONCE, token));
        inputs.addInput(new Input(ReturnParameter.KEY, encryptKey()));
        inputs.addInput(new Input(ReturnParameter.IV, ips.getIV()));
        
        return inputs;
    }        
}
