package pt.gov.autenticacao.autenticacao.softcert;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
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
public class SoftCertContainer extends AbstractContainer {
    private byte[] certificado = null;
    private byte[] signatureBytes = null;

    
    public void setCertificado(byte[] certificado) {
        this.certificado = certificado;
    }

    
    public void doSignature(PrivateKey pk) throws NoSuchAlgorithmException, InvalidKeyException, ContainerException, SignatureException{
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(pk);   
        signature.update(token.getBytes());
        this.signatureBytes = signature.sign();                       
    }
    
    
    public Inputs getInputs() throws ContainerException {
        Inputs inputs = new Inputs();

        inputs.addInput(new Input(ReturnParameter.CERTIFICADO, certificado));
        inputs.addInput(new Input(ReturnParameter.ASSINATURA, signatureBytes));
        inputs.addInput(new Input(ReturnParameter.NONCE, token));

        return inputs;
    }
}
