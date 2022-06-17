package pt.gov.autenticacao.assinatura.cc;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
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

    
    public void setCertificado(byte[] certificado) {
        this.certificado = certificado;
    }

    
    public void setCryptoHashBytes(byte[] cryptoHashBytes) {
        this.cryptoHashBytes = cryptoHashBytes;
    }
    
    
    void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm.toUpperCase();
    }
    
    
    public void doSignature(PrivateKey pk) throws NoSuchAlgorithmException, InvalidKeyException, ContainerException, SignatureException{                
        String signatureAlgorithm;
        
        switch(hashAlgorithm){
            case "SHA-1":
                signatureAlgorithm = "SHA1withRSA";
                break;
            case "SHA-256":
                signatureAlgorithm = "SHA256withRSA";
                break;
            default:
                signatureAlgorithm = "FALHA";
        }
        
        java.security.Signature signature = java.security.Signature.getInstance(signatureAlgorithm);
        signature.initSign(pk);   
        signature.update(cryptoHashBytes);
        this.signatureBytes = signature.sign();                       
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
