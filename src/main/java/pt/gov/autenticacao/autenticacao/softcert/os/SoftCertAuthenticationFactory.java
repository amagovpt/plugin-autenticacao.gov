package pt.gov.autenticacao.autenticacao.softcert.os;

import java.util.Map;
import pt.gov.autenticacao.Requests;
import pt.gov.autenticacao.common.Operation;
import pt.gov.autenticacao.autenticacao.softcert.SoftCertificatesSelector;
import pt.gov.autenticacao.autenticacao.softcert.os.linux.AuthSoftCertificateLinux;
import pt.gov.autenticacao.autenticacao.softcert.os.osx.AuthSoftCertificateOsx;
import pt.gov.autenticacao.autenticacao.softcert.os.windows.AuthSoftCertificateWindows;
import pt.gov.autenticacao.util.Utilities;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class SoftCertAuthenticationFactory {
    public static Operation create(SoftCertificatesSelector certSelector, Map<String, String> params, Requests requests){
        switch (Utilities.getOs()){
            case windows:
                return new AuthSoftCertificateWindows(certSelector, params, requests);                
            case macos:
                return new AuthSoftCertificateOsx(certSelector, params, requests);
            case linux:
                return new AuthSoftCertificateLinux(params, requests);
            default:
               throw new RuntimeException("Não foi possivel identificar o sistema operativo.");                 
        }       
    }
}
