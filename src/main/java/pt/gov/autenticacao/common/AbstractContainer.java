package pt.gov.autenticacao.common;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public abstract class AbstractContainer {
    protected Cipher cipher;
    protected PublicKey pubKey;
    protected byte[] key;
    protected SecretKeySpec keySpec;
    protected String token;
    protected IvParameterSpec ips;
    
    
    public void setToken(String token){
        this.token = token;
    }
    
    
    public void setSecretKey(byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException {             
        this.key = key;
        initializeAES();
    }
    
    
    public void setPublicKey(PublicKey pubKey){
        this.pubKey = pubKey;
    }
    
    
    private void initializeAES() throws NoSuchAlgorithmException, NoSuchPaddingException{
        byte iv[] = new byte[16];
        SecureRandom random = new SecureRandom();
        
        cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");       
        keySpec = new SecretKeySpec(key, "AES");                                
        random.nextBytes(iv);
        ips = new IvParameterSpec(iv);
    }
    
    
    protected byte[] encrypt(byte[] data) throws ContainerException{
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec,ips);
            return cipher.doFinal(data);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException ex) {
            throw new ContainerException("Encryption Error", ex);
        }
    }
    
    
    protected byte[] encryptKey() throws ContainerException{
        try {
            Cipher c = Cipher.getInstance("RSA");
            c.init(Cipher.ENCRYPT_MODE, pubKey);
            return c.doFinal(key);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new ContainerException("Key Encryption Error", ex);
        }
    }
}
