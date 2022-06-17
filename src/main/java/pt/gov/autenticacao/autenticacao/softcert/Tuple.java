package pt.gov.autenticacao.autenticacao.softcert;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Tuple {
    private String aliasCertificate;
    private String aliasPrivateKey;

    public Tuple(String aliasCertificate, String aliasPrivateKey){
        this.aliasCertificate = aliasCertificate;
        this.aliasPrivateKey = aliasPrivateKey;
    }
    
    public String getAliasCertificate() {
        return aliasCertificate;
    }

    public void setAliasCertificate(String aliasCertificate) {
        this.aliasCertificate = aliasCertificate;
    }

    public String getAliasPrivateKey() {
        return aliasPrivateKey;
    }

    public void setAliasPrivateKey(String aliasPrivateKey) {
        this.aliasPrivateKey = aliasPrivateKey;
    }        
}
