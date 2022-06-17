package pt.gov.autenticacao.autenticacao.softcert.selectors;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import pt.gov.autenticacao.autenticacao.softcert.SoftCertificatesSelector;
import pt.gov.autenticacao.autenticacao.softcert.Tuple;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class CSSoftCertificatesSelector  implements SoftCertificatesSelector{
    private final String CSCertIssuer = "CAMARA DOS SOLICITADORES";
    private final String CSOUSubject = "CERTIFICADO DE SOLICITADOR";
    private final static Logger LOGGER = Logger.getLogger(CSSoftCertificatesSelector.class.getName());
    
    private boolean isCSCert(X509Certificate cert){
        return cert.getIssuerX500Principal().getName().toUpperCase().contains(CSCertIssuer) && cert.getSubjectX500Principal().getName().toUpperCase().contains(CSOUSubject);
    }
    
    
    @Override
    public HashMap<String,Tuple> getCertificateList(KeyStore ks) {
        HashMap<String,Tuple> hash = new HashMap<>();
        List<String> temp = new ArrayList<>();
        
        
        try {
            java.util.Enumeration en = ks.aliases();

            while (en.hasMoreElements()) {
                String aliasKey = (String) en.nextElement();
                X509Certificate cert = (X509Certificate) ks.getCertificate(aliasKey);

                if (null != cert) {
                    if (isCSCert(cert)) {
                        List<Rdn> rdn = new LdapName(cert.getSubjectX500Principal().getName()).getRdns();
                        for (Rdn rdn11 : rdn) {
                            if (rdn11.getType().equalsIgnoreCase("CN")) {                                                                
                                if (ks.entryInstanceOf(aliasKey, KeyStore.PrivateKeyEntry.class)) {
                                    hash.put((String) rdn11.getValue(), new Tuple(aliasKey, aliasKey));
                                } else {
                                    if (!hash.containsKey((String) rdn11.getValue())) {
                                        hash.put((String) rdn11.getValue(), new Tuple(aliasKey, null));
                                    }                                    
                                }
                                break;
                            }
                        }
                    }                                                                                        
                } else {
                    temp.add(aliasKey);
                }
            }
            
            if (!temp.isEmpty()){                
                for (String alias : temp) {
                    for (String key : hash.keySet()){
                        String certAlias = hash.get(key).getAliasCertificate();                        
                        if (alias.contains(certAlias) || certAlias.contains(alias)){
                            hash.get(key).setAliasPrivateKey(alias);
                            break;
                        }
                    }                   
                }
            }
        } catch (KeyStoreException | InvalidNameException ex) {
            LOGGER.log(Level.SEVERE, null, ex);        
        }

        return hash;
    }
}

