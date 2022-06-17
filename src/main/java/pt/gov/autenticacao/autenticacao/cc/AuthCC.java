package pt.gov.autenticacao.autenticacao.cc;

import pt.gov.autenticacao.common.diagnostic.Diagnostic;
import pt.gov.autenticacao.common.ErrorReportImpl;
import pt.gov.autenticacao.common.RequiredParameterException;
import pt.gov.autenticacao.common.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.ResourceBundle;
import javax.crypto.NoSuchPaddingException;
import org.poreid.CertificateNotFound;
import org.poreid.POReIDException;
import org.poreid.SmartCardFileException;
import pt.gov.autenticacao.common.Inputs;
import pt.gov.autenticacao.common.Operation;
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
import pt.gov.autenticacao.common.Parameter;
import pt.gov.autenticacao.util.der.AuthGovCertificateExtensions;


/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class AuthCC extends Operation {    
    private ArrayList<Attribute> requestedData;
    private AttributeContainer container = null;  
    private String xmlAuthAttrs;
   
    
    public AuthCC(Map<String, String> params, Requests requests){
       super(params, requests);
    }
    
    
    private void loadRequestedData(String requestedDataList) {
        boolean justica = false;
        
        if (null != requestedDataList) {
            requestedData = new ArrayList<>();
            requestedData.add(Attribute.CERTIFICADO);
            String[] attrs = requestedDataList.split(";");

            for (Attribute attrib : Attribute.values()) {
                for (String attr : attrs) {
                    if (attrib.getName().equalsIgnoreCase(attr)) {                        
                        if (Attribute.JUSTICA.getName().equalsIgnoreCase(attr)){
                            justica = true;
                        }
                        requestedData.add(attrib);     
                    }
                }
            }
              
            if (!justica) {
                requestedData.add(Attribute.SOD);
                requestedData.add(Attribute.ID);
            }
        }
    }
    
      
    @SuppressWarnings("empty-statement")
    @Override
    public Response doOperation() { 
        class Name {};     
        String enclosingMethod = Name.class.getEnclosingMethod().getName();
        Inputs containerInputs = null;
          
        bundle = ResourceBundle.getBundle(Operation.class.getSimpleName(), locale);
        
        try {
            String authDataRequested = getParameter(Parameter.AUTH_DATA_REQUESTED, true);
            loadRequestedData(authDataRequested);
            xmlAuthAttrs = new String(Base64.getDecoder().decode(getParameter(Parameter.AUTH_AUTHORIZED_ATTRIBUTES, true).getBytes()), StandardCharsets.UTF_8);
            int usageBits = AuthGovCertificateExtensions.authentication|AuthGovCertificateExtensions.readData;
            Agente.HELP.setNewHelpPageLocation(getParameter(Parameter.HELP_PAGE_LOCATION, ""));
            if (validateSecurityParameters(usageBits, authDataRequested.getBytes(StandardCharsets.UTF_8), 
                    xmlAuthAttrs.getBytes(StandardCharsets.UTF_8), getParameter(Parameter.SOCSP_STAPLE, "").getBytes(), getParameter(Parameter.HELP_PAGE_LOCATION, "").getBytes())) {
                               
                Util.setLookAndFeel();                

                if (detectReaderAndCard()) {
                    doInBackground();
                    if (report == null){
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

    
    @SuppressWarnings("empty-statement")
    private  void doInBackground(){
        class Name {};
        String enclosingMethod = Name.class.getEnclosingMethod().getName();
           
        container = new AttributeContainer();
        container.setToken(authToken);
        container.setAuthorizedAttributes(xmlAuthAttrs);
        container.setPublicKey(authGovCert.getPublicKey());
        for (Attribute attr : requestedData) {
            if (null != report) {
                return;
            }
            switch (attr) {
                case SOD:
                    try {
                        container.setSod(cc.getSOD());
                    } catch (SmartCardFileException ex) {
                        report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_LEITURA, bundle.getString("sod.read.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));                        
                    }
                    break;
                case ID:                                        
                    try {
                        container.setId(cc.getID().getRawData());
                    } catch (SmartCardFileException ex) {
                        report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_LEITURA, bundle.getString("id.read.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));                        
                    }
                    break;
                case CERTIFICADO:                                        
                    try {
                        container.setCertificado(cc.getAuthenticationCertificate().getEncoded());
                    } catch (CertificateNotFound | CertificateEncodingException ex) {
                        report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_LEITURA, bundle.getString("authentication.certificate.read.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));                        
                    }
                    break;
                case MORADA:                    
                    try {
                        container.setMorada(cc.getAddress().getRawData());                    
                    } catch (PinTimeoutException ex) {
                        report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.UTILIZADOR_DEIXOU_EXPIRAR, bundle.getString("address.pin.timeout") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                    } catch (PinEntryCancelledException ex) {
                        report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.UTILIZADOR_RECUSOU_PIN, bundle.getString("address.pin.cancel") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                    } catch (PinBlockedException ex) {
                        report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.PIN_BLOQUEADO, bundle.getString("address.pin.blocked") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                    } catch (POReIDException ex) {
                        report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_LEITURA, bundle.getString("address.read.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                    }
                    break;
                case FOTO:                                        
                    try {
                        container.setFoto(cc.getPhotoData().getPhotoAndCBEFF());
                    } catch (SmartCardFileException ex) {
                        report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_LEITURA, bundle.getString("photo.read.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));                        
                    }
                    break;
            }
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
            container.doSignature((PrivateKey) ks.getKey(POReIDConfig.AUTENTICACAO, null));

        } catch (SignatureException ex) {
            Throwable th = ex.getCause();
            if (th instanceof PinBlockedException) {
                report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.PIN_BLOQUEADO, bundle.getString("authentication.pin.blocked") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
            } else if (th instanceof PinTimeoutException) {
                report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.UTILIZADOR_DEIXOU_EXPIRAR, bundle.getString("authentication.pin.timeout") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
            } else if (th instanceof PinEntryCancelledException) {
                report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.UTILIZADOR_RECUSOU_PIN, bundle.getString("authentication.pin.cancel") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
            } else {                                
                report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
            }
        } catch (ContainerException | KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | InvalidKeyException ex) {
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
        }                 
    }

    
    @Override
    public void action(ActionType b) {
        this.action = b;
    }             
}
