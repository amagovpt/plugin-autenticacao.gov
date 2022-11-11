package pt.gov.autenticacao.assinatura.cc;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import org.poreid.POReIDException;
import org.poreid.PkAlias;
import org.poreid.RSAPaddingSchemes;
import org.poreid.cc.CitizenCard;
import org.poreid.dialogs.pindialogs.PinBlockedException;
import org.poreid.dialogs.pindialogs.PinEntryCancelledException;
import org.poreid.dialogs.pindialogs.PinTimeoutException;
import pt.gov.autenticacao.common.AbstractContainer;
import pt.gov.autenticacao.common.ContainerException;
import pt.gov.autenticacao.common.Input;
import pt.gov.autenticacao.common.Inputs;
import pt.gov.autenticacao.common.ReturnParameter;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class SignatureContainer extends AbstractContainer {
    private byte[] signatureBytes = null;
    private byte[] certificado = null;
    private byte[] cryptoHashBytes = null;
    private String hashAlgorithm;
    private CitizenCard cc = null;

    public SignatureContainer(CitizenCard cc){
        this.cc = cc;
    }
    
    public void setCertificado(byte[] certificado) {
        this.certificado = certificado;
    }

    
    public void setCryptoHashBytes(byte[] cryptoHashBytes) {
        this.cryptoHashBytes = cryptoHashBytes;
    }
    
    
    void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm.toUpperCase();
    }
    
    
    public void doSignature() throws SignatureException{                
        
        try {
           this.signatureBytes =  cc.sign(cryptoHashBytes,null,hashAlgorithm, PkAlias.ASSINATURA, RSAPaddingSchemes.PKCS1);
        } catch (POReIDException | PinBlockedException | PinEntryCancelledException | PinTimeoutException ex) {
            throw new SignatureException(ex);
        }
    }
    
    
    public Inputs getInputs() throws ContainerException {
        Inputs inputs = new Inputs();
                                
        inputs.addInput(new Input(ReturnParameter.CERTIFICADO, certificado));
        inputs.addInput(new Input(ReturnParameter.SIGNATURE_BYTES, encrypt(signatureBytes)));
        inputs.addInput(new Input(ReturnParameter.NONCE, token));
        inputs.addInput(new Input(ReturnParameter.KEY, encryptKey()));
        inputs.addInput(new Input(ReturnParameter.IV, ips.getIV()));
        
        return inputs;
    }               
}
