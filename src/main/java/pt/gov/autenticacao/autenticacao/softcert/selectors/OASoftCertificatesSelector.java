package pt.gov.autenticacao.autenticacao.softcert.selectors;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import pt.gov.autenticacao.autenticacao.softcert.SoftCertificatesSelector;
import pt.gov.autenticacao.autenticacao.softcert.Tuple;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class OASoftCertificatesSelector  implements SoftCertificatesSelector{
    private final String OACertSubject = "ORDEM DOS ADVOGADOS - RA";
    private final String OACertRegExp = "- ORDEM DOS ADVOGADOS";
    private final Pattern pattern = Pattern.compile(OACertRegExp, Pattern.CASE_INSENSITIVE);
    private final static Logger LOGGER = Logger.getLogger(OASoftCertificatesSelector.class.getName());
    
    private boolean isOACert(X509Certificate cert){
        return cert.getSubjectX500Principal().getName().toUpperCase().contains(OACertSubject);
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
                    if (isOACert(cert)) {
                        List<Rdn> rdn = new LdapName(cert.getSubjectX500Principal().getName()).getRdns();
                        for (Rdn rdn11 : rdn) {
                            if (rdn11.getType().equalsIgnoreCase("CN")) {
                                Matcher matcher = pattern.matcher((String) rdn11.getValue());
                                if (matcher.find()) {                                    
                                    if (ks.entryInstanceOf(aliasKey, KeyStore.PrivateKeyEntry.class)) {
                                        hash.put(matcher.replaceFirst(""), new Tuple(aliasKey, aliasKey));
                                    } else {
                                        hash.put(matcher.replaceFirst(""), new Tuple(aliasKey, null));
                                    }
                                    break;
                                }
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

