package pt.gov.autenticacao.assinatura.cc;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.ResourceBundle;
import javax.crypto.NoSuchPaddingException;
import org.poreid.CertificateNotFound;
import org.poreid.POReIDException;
import org.poreid.common.Util;
import org.poreid.config.POReIDConfig;
import org.poreid.crypto.POReIDKeyStoreParameter;
import org.poreid.crypto.POReIDProvider;
import org.poreid.dialogs.pindialogs.PinBlockedException;
import org.poreid.dialogs.pindialogs.PinEntryCancelledException;
import org.poreid.dialogs.pindialogs.PinTimeoutException;
import pt.gov.autenticacao.Agente;
import pt.gov.autenticacao.Requests;
import pt.gov.autenticacao.common.ContainerException;
import pt.gov.autenticacao.common.ErrorReport.ErrorType;
import pt.gov.autenticacao.common.ErrorReportImpl;
import pt.gov.autenticacao.common.Inputs;
import pt.gov.autenticacao.common.Operation;
import pt.gov.autenticacao.common.Parameter;
import pt.gov.autenticacao.common.RequiredParameterException;
import pt.gov.autenticacao.common.Response;
import pt.gov.autenticacao.common.diagnostic.Diagnostic;
import pt.gov.autenticacao.util.Base64;
import pt.gov.autenticacao.util.der.AuthGovCertificateExtensions;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Signature extends Operation {
    private SignatureContainer container = null;
    private String hashAlgorithm;
    private byte[] cryptoHash;
    
    public Signature(Map<String, String> params, Requests requests) {
        super(params, requests);
    }

    
    @SuppressWarnings("empty-statement")
    private  void doInBackground(){
        class Name {};
        String enclosingMethod = Name.class.getEnclosingMethod().getName();
        
        container = new SignatureContainer();
        container.setToken(authToken);        
        container.setPublicKey(authGovCert.getPublicKey());
        container.setCryptoHashBytes(cryptoHash);
        container.setHashAlgorithm(hashAlgorithm);
                
        try {
            container.setCertificado(cc.getQualifiedSignatureCertificate().getEncoded());
        } catch (CertificateNotFound | CertificateEncodingException ex) {
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_LEITURA, bundle.getString("signature.certificate.read.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
        }
        
        if (null != report) {
            return;
        }
        
        try {                        
            byte[] key = new byte[16];
            System.arraycopy(cc.getChallenge(), 0, key, 0, 8);
            System.arraycopy(cc.getChallenge(), 0, key, 8, 8);
            container.setSecretKey(key);            
        } catch (POReIDException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, bundle.getString("key.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
        }
        
        if (null != report) {
            return;
        }
        
        try {           
            Security.addProvider(new POReIDProvider());
            POReIDKeyStoreParameter ksParam = new POReIDKeyStoreParameter();
            ksParam.setCard(cc);

            KeyStore ks = KeyStore.getInstance(POReIDConfig.POREID);
            ks.load(ksParam);                        
            container.doSignature((PrivateKey) ks.getKey(POReIDConfig.ASSINATURA, null));

        } catch (SignatureException ex) {
            Throwable th = ex.getCause();
            if (th instanceof PinBlockedException) {
                report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.PIN_BLOQUEADO, bundle.getString("signature.pin.blocked") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
            } else if (th instanceof PinTimeoutException) {
                report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.UTILIZADOR_DEIXOU_EXPIRAR, bundle.getString("signature.pin.timeout") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
            } else if (th instanceof PinEntryCancelledException) {
                report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.UTILIZADOR_RECUSOU_PIN, bundle.getString("signature.pin.cancel") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
            } else {                                
                report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
            }
        } catch (ContainerException | KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | InvalidKeyException ex) {
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
        }
    }
    
    
    @Override
    @SuppressWarnings("empty-statement")
    public Response doOperation() {
        class Name {};     
        String enclosingMethod = Name.class.getEnclosingMethod().getName();
        Inputs containerInputs = null;
          
        bundle = ResourceBundle.getBundle(Operation.class.getSimpleName(), locale);
        
        try {
            hashAlgorithm = getParameter(Parameter.CRYPTOGRAPHIC_HASH_ALGORITHM, true);
            cryptoHash = Base64.getDecoder().decode(getParameter(Parameter.CRYPTOGRAPHIC_HASH, true));            
            int usageBits = AuthGovCertificateExtensions.signature;
            Agente.HELP.setNewHelpPageLocation(getParameter(Parameter.HELP_PAGE_LOCATION, ""));
            if (validateSecurityParameters(usageBits, cryptoHash, hashAlgorithm.getBytes(), getParameter(Parameter.SOCSP_STAPLE, "").getBytes(), 
                    getParameter(Parameter.HELP_PAGE_LOCATION, "").getBytes())) {
                Util.setLookAndFeel();

                if (detectReaderAndCard()) {
                    doInBackground();
                    if (report == null) {
                        containerInputs = container.getInputs();
                    }
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, bundle.getString("dialog.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
        } catch (ContainerException ex) {
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, bundle.getString("key.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
        } catch (RequiredParameterException ex) {
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, bundle.getString("required.parameter") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
        } finally {
            if (null != cc) {
                try {
                    cc.close();
                } catch (POReIDException ignore) {
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
        this.action = b;
    }    
}
