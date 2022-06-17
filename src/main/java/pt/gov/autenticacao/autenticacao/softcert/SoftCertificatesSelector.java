package pt.gov.autenticacao.autenticacao.softcert;

import java.security.KeyStore;
import java.util.HashMap;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public interface SoftCertificatesSelector {
    public HashMap<String,Tuple> getCertificateList(KeyStore ks);
}
