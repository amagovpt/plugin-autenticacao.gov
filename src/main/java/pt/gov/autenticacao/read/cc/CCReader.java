package pt.gov.autenticacao.read.cc;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;
import javax.crypto.NoSuchPaddingException;
import org.poreid.CertificateChainNotFound;
import org.poreid.POReIDException;
import org.poreid.SmartCardFileException;
import org.poreid.common.Util;
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
import pt.gov.autenticacao.util.der.AuthGovCertificateExtensions;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class CCReader extends Operation {
    private ArrayList<Attribute> requestedData;    
    private CCReadContainer container = null;
    

    public CCReader(Map<String, String> params, Requests requests) {
        super(params, requests);
    }
    
    
    private void loadRequestedData(String requestedDataList) {        
        
        if (null != requestedDataList) {
            requestedData = new ArrayList<>();            
            String[] attrs = requestedDataList.split(";");

            for (Attribute attrib : Attribute.values()) {
                for (String attr : attrs) {
                    if (attrib.getName().equalsIgnoreCase(attr)) {                        
                        requestedData.add(attrib);     
                    }
                }
            }
                          
            requestedData.add(Attribute.SOD);        
        }
    }
        
    
    @Override
    public void action(ActionType b) {
        this.action = b;
    }
    
    
    @SuppressWarnings("empty-statement")
    private  void doInBackground(){
        class Name {};
        String enclosingMethod = Name.class.getEnclosingMethod().getName();
           
        container = new CCReadContainer();
        container.setToken(authToken);        
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
                case AUTH_CHAIN:
                    try {
                        cc.getAuthenticationCertificateChain();
                    } catch (CertificateChainNotFound ex) {
                        report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_LEITURA, bundle.getString("authentication.certificate.read.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                    }
                    break;
                case SIGN_CHAIN:
                    try {
                        cc.getQualifiedSignatureCertificateChain();
                    } catch (CertificateChainNotFound ex) {
                        report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_LEITURA, bundle.getString("signature.certificate.read.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
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
    }
    

    @Override
    @SuppressWarnings("empty-statement")
    public Response doOperation() {
        class Name {};     
        String enclosingMethod = Name.class.getEnclosingMethod().getName();
        Inputs containerInputs = null;
          
        bundle = ResourceBundle.getBundle(Operation.class.getSimpleName(), locale);
        
        try {
            String authDataRequested = getParameter(Parameter.AUTH_DATA_REQUESTED, true);
            loadRequestedData(authDataRequested);
            int usageBits = AuthGovCertificateExtensions.readData;
            Agente.HELP.setNewHelpPageLocation(getParameter(Parameter.HELP_PAGE_LOCATION, ""));
            if (validateSecurityParameters(usageBits, authDataRequested.getBytes(StandardCharsets.UTF_8), getParameter(Parameter.SOCSP_STAPLE, "").getBytes(), 
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
}
