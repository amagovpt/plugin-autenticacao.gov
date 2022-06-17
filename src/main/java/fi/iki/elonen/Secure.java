package fi.iki.elonen;

import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;

/**
 *
 * @author ruim
 */
public class Secure {

    private final KeyManagerFactory keyManagerFactory;
    private final KeyStore ks;

    public Secure() throws Exception {
        ks = KeyStore.getInstance("JKS");
        ks.load(Secure.class.getResourceAsStream("/secure.jks"), "488b6d2e-d185-4948-b7e6-afc95a6efa52".toCharArray());
        keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(ks, "47229a4d-d6b7-45e7-94e6-714912e43c26".toCharArray());
    }

    public KeyManagerFactory getKMF() {
        return keyManagerFactory;
    }

    public KeyStore getKS() {
        return ks;
    }

    public static void main(String[] args) {
        try {
            Secure s = new Secure();

        } catch (Exception ex) {
            Logger.getLogger(Secure.class.getName()).log(Level.SEVERE, null, ex);
        }
  
    }
}
