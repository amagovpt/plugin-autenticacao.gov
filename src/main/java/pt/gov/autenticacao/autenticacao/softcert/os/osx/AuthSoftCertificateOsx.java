package pt.gov.autenticacao.autenticacao.softcert.os.osx;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import pt.gov.autenticacao.Requests;
import pt.gov.autenticacao.common.Operation;
import pt.gov.autenticacao.common.ContainerException;
import pt.gov.autenticacao.common.ErrorReportImpl;
import pt.gov.autenticacao.common.Inputs;
import pt.gov.autenticacao.common.Response;
import pt.gov.autenticacao.autenticacao.softcert.CanceledSelectionException;
import pt.gov.autenticacao.autenticacao.softcert.CertificateNotFoundException;
import pt.gov.autenticacao.autenticacao.softcert.SelectCertificateDialogController;
import pt.gov.autenticacao.autenticacao.softcert.SoftCertContainer;
import pt.gov.autenticacao.autenticacao.softcert.SoftCertificatesSelector;
import pt.gov.autenticacao.autenticacao.softcert.Tuple;
import pt.gov.autenticacao.util.Utilities;
import pt.gov.autenticacao.util.der.AuthGovCertificateExtensions;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class AuthSoftCertificateOsx extends Operation{    
    private final static Logger LOGGER = Logger.getLogger(AuthSoftCertificateOsx.class.getName());
    private final SoftCertificatesSelector certSelector;
    
    public AuthSoftCertificateOsx(SoftCertificatesSelector certSelector, Map<String, String> params, Requests requests){
       super(params, requests);
       this.certSelector = certSelector;
    }  
            
        
    @SuppressWarnings("empty-statement")
    @Override
    public Response doOperation() { 
        class Name {};        
        Inputs containerInputs = null;        
        HashMap<String,Tuple> hash;
        SoftCertContainer container = new SoftCertContainer();
        String alias;
        
        bundle = ResourceBundle.getBundle(Operation.class.getSimpleName(), locale);
                
        try {            
            if (validateSecurityParameters(AuthGovCertificateExtensions.authentication)) {
                KeyStore ks = KeyStore.getInstance("KeychainStore", "Apple");
                ks.load(null, "DUMMY".toCharArray()); // https://bugs.openjdk.java.net/browse/JDK-8062264
                hash = certSelector.getCertificateList(ks);
                alias = hash.get(SelectCertificateDialogController.getInstance(hash.keySet(), locale).selectCertificate()).getAliasPrivateKey();
                PrivateKey pk = (PrivateKey) ks.getKey(alias, "DUMMY".toCharArray()); // https://bugs.openjdk.java.net/browse/JDK-8062264
                container.setCertificado(ks.getCertificate(alias).getEncoded());
                container.setToken(authToken);
                container.doSignature(pk);

                containerInputs = container.getInputs();
            }
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.INFO, "URL inválido, exceção: {0}", ex );
            return new Response();
        } catch (NoSuchProviderException | ContainerException | KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | InvalidKeyException | SignatureException ex) {
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorReportImpl.ErrorType.ERRO_GENERICO, ex.getMessage() + Utilities.addLog(Name.class.getEnclosingMethod().getName(), ex));        
        } catch (CanceledSelectionException ex) {
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorReportImpl.ErrorType.UTILIZADOR_CANCELOU, ex.getMessage());
        } catch (CertificateNotFoundException ex) {
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorReportImpl.ErrorType.NAO_FORAM_ENCONTRADOS_CERTIFICADOS, ex.getMessage());
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
