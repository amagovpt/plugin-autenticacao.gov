package pt.gov.autenticacao.read.cc;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import org.poreid.json.JSONArray;
import pt.gov.autenticacao.common.AbstractContainer;
import pt.gov.autenticacao.common.ContainerException;
import pt.gov.autenticacao.common.Input;
import pt.gov.autenticacao.common.Inputs;
import pt.gov.autenticacao.common.ReturnParameter;
import pt.gov.autenticacao.util.Base64;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class CCReadContainer extends AbstractContainer {
    private byte[] sod = null;
    private byte[] id = null;
    private byte[] foto = null;
    private byte[] morada = null;
    private List<X509Certificate> chain = null;
    
    
    public void setSod(byte[] sod) {
        this.sod = sod;
    }
    

    public void setId(byte[] id) {
        this.id = id;
    }

    
    public void setFoto(byte[] foto) {
        this.foto = foto;
    }

    
    public void setMorada(byte[] morada) {
        this.morada = morada;
    }
    
    
    public void setChain(List<X509Certificate> chain){
        this.chain = chain;
    }
    
    
    public Inputs getInputs() throws ContainerException{
        Inputs inputs = new Inputs();
        
        if (null != sod){
            inputs.addInput(new Input(ReturnParameter.SOD, encrypt(sod)));
        }
        
        if (null != id){
            inputs.addInput(new Input(ReturnParameter.ID, encrypt(id)));            
        }
        
        if (null != foto){
            inputs.addInput(new Input(ReturnParameter.FOTO,encrypt(foto)));
        }
        
        if (null != morada){
            inputs.addInput(new Input(ReturnParameter.MORADA, encrypt(morada)));
        } 
        
        if (null != chain){           
            try {                
                inputs.addInput(new Input(ReturnParameter.CHAIN, toJsonArray()));
            } catch (CertificateEncodingException ex) {
                throw new ContainerException(ex.getLocalizedMessage(),ex);
            }                        
        }
                
        inputs.addInput(new Input(ReturnParameter.NONCE, token));
        inputs.addInput(new Input(ReturnParameter.KEY, encryptKey()));
        inputs.addInput(new Input(ReturnParameter.IV, ips.getIV()));
        
        return inputs;
    }
    
    
    private String toJsonArray() throws CertificateEncodingException{
        JSONArray array = new JSONArray();
        for (X509Certificate cert : chain){
            array.put(Base64.getEncoder().encodeToString(cert.getEncoded()));
        }
        
        return array.toString();
    }
}
