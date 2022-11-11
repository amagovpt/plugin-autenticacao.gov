package pt.gov.autenticacao.common;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.crypto.NoSuchPaddingException;
import java.util.Base64;

/**
 * 
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class ErrorReportImplPan extends AbstractContainer implements ErrorReport {
    private final String estado;
    private final String descricao;
    
    
    public ErrorReportImplPan(PublicKey pubKey, ErrorType tipo, String descricao){
        this.estado = tipo.name();
        this.descricao = descricao;
        this.pubKey = pubKey;          
    }
                 
    @Override
    @SuppressWarnings("empty-statement")
    public Inputs getInputs() {
        class Name {};
        Inputs inputs = new Inputs();
        SecureRandom generator = new SecureRandom();
                        
        try {
            key = new byte[16];
            generator.nextBytes(key);            
            setSecretKey(key);
            setPublicKey(pubKey);
            
            inputs.addInput(new Input(ReturnParameter.PAN, "")); // erro na obtenção do pan, mas vamos forçar autenticação por via normal
            inputs.addInput(new Input(ReturnParameter.ESTADO, estado.getBytes()));
            inputs.addInput(new Input(ReturnParameter.DESCRICAO, encrypt(descricao.getBytes())));            
            inputs.addInput(new Input(ReturnParameter.KEY, encryptKey()));
            inputs.addInput(new Input(ReturnParameter.IV, ips.getIV()));
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | ContainerException ex) {
            inputs = new Inputs();
            inputs.addInput(new Input(ReturnParameter.ESTADO, estado.getBytes()));
            inputs.addInput(new Input(ReturnParameter.DESCRICAO, Base64.getEncoder().encodeToString(("Erro interno - "+ex.getLocalizedMessage() + " - " + Name.class.getEnclosingMethod().getName()).getBytes())));           
        }
        
        return inputs;
    }
}
