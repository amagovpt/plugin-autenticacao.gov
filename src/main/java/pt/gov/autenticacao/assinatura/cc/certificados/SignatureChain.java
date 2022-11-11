package pt.gov.autenticacao.assinatura.cc.certificados;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UnsupportedLookAndFeelException;
import org.poreid.CertificateChainNotFound;
import org.poreid.POReIDException;
import org.poreid.common.Util;
import pt.gov.autenticacao.Agente;
import pt.gov.autenticacao.Requests;
import pt.gov.autenticacao.common.ContainerException;
import pt.gov.autenticacao.common.ErrorReport;
import pt.gov.autenticacao.common.ErrorReportImpl;
import pt.gov.autenticacao.common.ErrorReportImplPan;
import pt.gov.autenticacao.common.Inputs;
import pt.gov.autenticacao.common.Operation;
import pt.gov.autenticacao.common.Parameter;
import pt.gov.autenticacao.common.Response;
import pt.gov.autenticacao.common.diagnostic.Diagnostic;
import pt.gov.autenticacao.util.der.AuthGovCertificateExtensions;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class SignatureChain extends Operation{
    private SignatureChainContainer container = null;
    
    public SignatureChain(Map<String, String> params, Requests requests) {
        super(params, requests);
    }
    
    @SuppressWarnings("empty-statement")
    private  void doInBackground(){
        class Name {};
        String enclosingMethod = Name.class.getEnclosingMethod().getName();
        
        container = new SignatureChainContainer();
        container.setToken(authToken);
        try {
            List<X509Certificate> certList = cc.getQualifiedSignatureCertificateChain();
            container.setCertificado(certList.get(0).getEncoded());
        container.setCertificadoSubEC(certList.get(1).getEncoded());
        } catch (CertificateChainNotFound | CertificateEncodingException ex) {
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorReport.ErrorType.ERRO_LEITURA, bundle.getString("signature.certificate.read.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
        }
        
         if (null != report) {
            return;
        }
    }
    
    @Override
    public Response doOperation() {
        class Name {
        };
        String enclosingMethod = Name.class.getEnclosingMethod().getName();
        Inputs containerInputs = null;

        bundle = ResourceBundle.getBundle(Operation.class.getSimpleName(), locale);
        Agente.HELP.setNewHelpPageLocation(getParameter(Parameter.HELP_PAGE_LOCATION, ""));
        if (validateSecurityParameters(AuthGovCertificateExtensions.readData, getParameter(Parameter.SOCSP_STAPLE, "").getBytes(), getParameter(Parameter.HELP_PAGE_LOCATION, "").getBytes())) {
            try {
                Util.setLookAndFeel();
                if (detectReaderAndCard()) {
                    doInBackground();
                    if (report == null) {
                        containerInputs = container.getInputs();
                    }
                }
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(SignatureChain.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(SignatureChain.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(SignatureChain.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedLookAndFeelException ex) {
                Logger.getLogger(SignatureChain.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ContainerException ex) {
                report = new ErrorReportImplPan(authGovCert.getPublicKey(), ErrorReport.ErrorType.ERRO_GENERICO, bundle.getString("key.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
            } finally {
                if (null != cc) {
                    try {
                        cc.close();
                    } catch (POReIDException ignore) {
                    }
                }
            }
        }

        if (null != report) {
            return new Response(report.getInputs());
        }
        if (null != containerInputs) {
            return new Response(containerInputs);
        }

        return new Response();
    }

    @Override
    public void action(ActionType b) {
    }
    
}
