package pt.gov.autenticacao.autenticacao.softcert;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class CertificateNotFoundException extends Exception {

    public CertificateNotFoundException(String msg){
        super(msg);
    }
}
