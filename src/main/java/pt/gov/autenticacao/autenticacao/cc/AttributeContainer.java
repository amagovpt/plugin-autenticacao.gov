package pt.gov.autenticacao.autenticacao.cc;

import java.nio.charset.StandardCharsets;
import pt.gov.autenticacao.common.Input;
import pt.gov.autenticacao.common.Inputs;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import pt.gov.autenticacao.common.AbstractContainer;
import pt.gov.autenticacao.common.ContainerException;
import pt.gov.autenticacao.common.ReturnParameter;


/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class AttributeContainer extends AbstractContainer{
    private byte[] sod = null;
    private byte[] id = null;
    private byte[] foto = null;
    private byte[] morada = null;
    private byte[] certificado = null;
    private byte[] signatureBytes = null;
    private String authorizedAttributes; // para passar a json no futuro ou remover.
    
    
    private enum Hash{
        SHA1("SHA-1"),
        SHA256("SHA-256");
        
        private final String name;    
    
        private Hash(String name){
            this.name = name;
        }
        
        public String getName(){
            return name;
        }
    }
    
    
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
    
    
    public void setCertificado(byte[] certificado){
        this.certificado = certificado;
    } 
    
    
    public void doSignature(PrivateKey pk) throws NoSuchAlgorithmException, InvalidKeyException, ContainerException, SignatureException{
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(pk);   
        signature.update(getXMLBytes());
        this.signatureBytes = signature.sign();                       
    }            
    
    
    public void setAuthorizedAttributes(String authorizedAttributes){
        this.authorizedAttributes = authorizedAttributes;
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
        
        inputs.addInput(new Input(ReturnParameter.SCAP, Base64.getEncoder().encodeToString(authorizedAttributes.getBytes())));
        
        inputs.addInput(new Input(ReturnParameter.CERTIFICADO, certificado));
        inputs.addInput(new Input(ReturnParameter.ASSINATURA, signatureBytes));
        inputs.addInput(new Input(ReturnParameter.NONCE, token));
        inputs.addInput(new Input(ReturnParameter.KEY, encryptKey()));
        inputs.addInput(new Input(ReturnParameter.IV, ips.getIV()));
        
        return inputs;
    }
                
                 
    private byte[] getXMLBytes(Hash... hash) throws ContainerException {
        try {
            authorizedAttributes = authorizedAttributes.replaceAll("xsr:|:xsr", "").replaceFirst("(ccptDigestValue>)", "$1"+getAtributesResume(hash.length> 0 ? hash[0] : Hash.SHA1));                        
            return authorizedAttributes.getBytes(StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException ex) {
            throw new ContainerException("HASH NoSuchAlgorithmException", ex);
        }   
    }
    
    
    private String getAtributesResume(Hash hash) throws NoSuchAlgorithmException {
        MessageDigest md;

        md = MessageDigest.getInstance(hash.getName());

        if (null != sod) {
            md.update(sod);
        }

        if (null != id) {
            md.update(id);
        }

        if (null != foto) {
            md.update(foto);
        }

        if (null != morada) {
            md.update(morada);
        }
        
        md.update(token.getBytes());

        return Base64.getEncoder().encodeToString(md.digest());
    }             
}
