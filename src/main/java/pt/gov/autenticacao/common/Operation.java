package pt.gov.autenticacao.common;

import pt.gov.autenticacao.util.Utilities;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.poreid.CardFactory;
import org.poreid.CardNotPresentException;
import org.poreid.CardTerminalNotPresentException;
import org.poreid.CertificateChainNotFound;
import org.poreid.POReIDException;
import org.poreid.UnknownCardException;
import org.poreid.cc.CitizenCard;
import org.poreid.dialogs.selectcard.CanceledSelectionException;
import pt.gov.autenticacao.Requests;
import pt.gov.autenticacao.common.ErrorReport.ErrorType;
import pt.gov.autenticacao.common.diagnostic.Diagnostic;
import pt.gov.autenticacao.configuracao.ConfigurationBuilder;
import pt.gov.autenticacao.dialogs.NotifyOption;
import pt.gov.autenticacao.dialogs.hardware.MissingHardwareDialog;
import pt.gov.autenticacao.util.Base64;
import pt.gov.autenticacao.util.der.AuthGovCertificateExtensions;
import pt.gov.autenticacao.util.der.StapledSingleOCSPResponse;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public abstract class Operation implements NotifyOption {
    protected final Map<String, String> params;
    protected ErrorReport report = null;
    protected X509Certificate authGovCert;
    protected String authToken;
    protected ResourceBundle bundle;
    protected final Locale locale;
    protected final String DEFAULT_LANGUAGE = "pt";
    protected final String DEFAULT_COUNTRY = "PT";
    protected final Requests requests;
    protected CitizenCard cc = null;
    protected boolean tupleDetected = false;        
    protected ActionType action = ActionType.PROGRESS;    
    private static final int TIMEOUT = 85; // 5s inicializacao, 30 segs para pin + leitura (caso sem morada!) = 120s timeout
    private final int TENTATIVAS_AJUDA = 2;
    
    
    public Operation(Map<String, String> params, Requests requests){  
       this.params = params;
       this.requests = requests;
       String language = getParameter(Parameter.AUTH_LOCALE_LANGUAGE);
       String country = getParameter(Parameter.AUTH_LOCALE_COUNTRY);
       this.locale = new Locale(null==language ? DEFAULT_LANGUAGE: language, null==country ? DEFAULT_COUNTRY: country);       
    }
    
    
    @SuppressWarnings("empty-statement")
    protected boolean validateSecurityParameters(int usageBits, final byte[]... parameters){
        class Name {};
        String agentToken;
        boolean sig;
        
        try {
            agentToken = getParameter(Parameter.AGENT_TOKEN, true);
            if (!requests.isValid(UUID.fromString(agentToken))){
                return false;
            }
        } catch (RequiredParameterException ex) {
            return false;
        }

        try {
            authGovCert = Utilities.loadCertificate(getParameter(Parameter.AUTH_GOV_CERTIFICATE,true));
            Utilities.checkCertificateAccepted(authGovCert);
        } catch (CertificateChainNotFound | RequiredParameterException ex){
            return false;
        } catch (NoSuchAlgorithmException | IOException | KeyStoreException | CertificateException ex){               
            report = new ErrorReportImpl(ErrorReportImpl.ErrorType.ERRO_GENERICO, bundle.getString("certificate.error") + Utilities.addLog(Name.class.getEnclosingMethod().getName(), ex));
            return false;
        }
                
        if (!AuthGovCertificateExtensions.isUsageAuthorized(authGovCert, usageBits)){
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorReportImpl.ErrorType.ERRO_PERMISSOES, bundle.getString("not.authorized"));
            return false;
        }
              
        try {
            if (AuthGovCertificateExtensions.needsValidation(authGovCert) && (null != getParameter(Parameter.SOCSP_STAPLE, true))) {                
                new StapledSingleOCSPResponse(Base64.getDecoder().decode(getParameter(Parameter.SOCSP_STAPLE, true))).verify(authGovCert);
            }
        } catch (RequiredParameterException | CertPathValidatorException ex) {
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorReportImpl.ErrorType.ERRO_GENERICO, bundle.getString("required.parameter") + Utilities.addLog(Name.class.getEnclosingMethod().getName(), ex));
            return false;
        }
        
        try {
            authToken = getParameter(Parameter.AUTH_TOKEN_ID, true);
            sig = Utilities.checkSignature(authGovCert, getParameter(Parameter.AUTH_SIGNATURE, true), agentToken, authToken, parameters);
        } catch (RequiredParameterException ex){
            return false;
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex){ 
            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorReportImpl.ErrorType.ERRO_GENERICO, bundle.getString("required.parameter") + Utilities.addLog(Name.class.getEnclosingMethod().getName(), ex));
            return false;
        }
        
        return sig;
    }
    
    
    protected final String getParameter(Parameter key){
        return params.get(key.getName());
    }
    
    
    protected final String getParameter(Parameter key, String defaultValue){
        String value = params.get(key.getName());
       
        return (null != value) ? value : defaultValue;
    }
    
                
    protected final String getParameter(Parameter key, boolean required) throws RequiredParameterException {
        String value = params.get(key.getName());
        if (null == value && required) {
            throw new RequiredParameterException("Parametro obrigatório (" + key + ") não fornecido");
        }

        return value;
    }
    
    
    @SuppressWarnings(value = "empty-statement")
    public abstract Response doOperation();
    
    
    @SuppressWarnings("empty-statement")
    protected boolean detectReaderAndCard() {
        class Name {};
        String enclosingMethod = Name.class.getEnclosingMethod().getName();
        Date inicio = new Date();
        boolean retry = true;
        int tentativas = 0;
        
        do {            
            try {
                tentativas++;
                cc = CardFactory.getCard(locale);             
                tupleDetected = true;                
            } catch (CardTerminalNotPresentException ex) {
                try {
                    final long delay = Utilities.getDateDiff(inicio, new Date(), TimeUnit.SECONDS);
                    final boolean showHelp = tentativas > TENTATIVAS_AJUDA;
                    
                    if (TIMEOUT > delay) {
                        java.awt.EventQueue.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                MissingHardwareDialog dialog = new MissingHardwareDialog(MissingHardwareDialog.DialogType.MISSING_READER, Operation.this, locale, delay, showHelp);
                                dialog.setVisible(true);
                            }
                        });
                    } else {
                        action = ActionType.TIMEOUT;
                    }
                    
                    switch (action) {
                        case HELP:
                            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.LEITOR_NAO_DETETADO_AJUDA, bundle.getString("no.card.reader.help") + Diagnostic.getCCLogInfo(cc, ex, tentativas, enclosingMethod));
                            restart(Diagnostic.isReaderAvailable());
                            break;
                        case CANCEL:
                            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.LEITOR_NAO_DETETADO_UTILIZADOR_CANCELOU, bundle.getString("no.card.reader.user.canceled") + Diagnostic.getCCLogInfo(cc, ex, tentativas, enclosingMethod));
                            restart(Diagnostic.isReaderAvailable());
                            break;
                        case TIMEOUT:
                            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.LEITOR_NAO_DETETADO_TIMEOUT, bundle.getString("no.card.reader.timeout") + Diagnostic.getCCLogInfo(cc, ex, tentativas, enclosingMethod));
                            restart(Diagnostic.isReaderAvailable());
                            break;
                    }
                                        
                    if (null!=report){
                        break;
                    }
                } catch (InterruptedException | InvocationTargetException iex) {
                    report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, bundle.getString("generic.error") + Diagnostic.getCCLogInfo(cc, iex, enclosingMethod));
                    break;
                }
            } catch (UnknownCardException | CardNotPresentException ex) {                
                try {
                    final long delay = Utilities.getDateDiff(inicio, new Date(), TimeUnit.SECONDS);
                    final boolean showHelp = tentativas > TENTATIVAS_AJUDA;
                    
                    if (TIMEOUT > delay) {
                        if (retry) { // interação com o middleware do token dos notários, uma espera de 1s deve resolver o problema.
                            retry = false;
                            Thread.sleep(1000);
                            tentativas--;
                        } else {
                            java.awt.EventQueue.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    MissingHardwareDialog dialog = new MissingHardwareDialog(MissingHardwareDialog.DialogType.MISSING_CARD, Operation.this, locale, delay, showHelp);
                                    dialog.setVisible(true);
                                }
                            });
                        }
                    } else {
                        action = ActionType.TIMEOUT;
                    }
                    
                    switch (action) {
                        case HELP:
                            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.CARTAO_NAO_DETETADO_AJUDA, bundle.getString("no.card.help") + Diagnostic.getCCLogInfo(cc, ex, tentativas, enclosingMethod));
                            break;
                        case CANCEL:
                            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.CARTAO_NAO_DETETADO_UTILIZADOR_CANCELOU, bundle.getString("no.card.user.canceled") + Diagnostic.getCCLogInfo(cc, ex, tentativas, enclosingMethod));
                            break;
                        case TIMEOUT:
                            report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.CARTAO_NAO_DETETADO_TIMEOUT, bundle.getString("no.card.timeout") + Diagnostic.getCCLogInfo(cc, ex, tentativas, enclosingMethod));
                            break;
                    }
                                                            
                    if (null!=report){
                        break;
                    }
                } catch (InterruptedException | InvocationTargetException iex) {
                    report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, bundle.getString("generic.error") + Diagnostic.getCCLogInfo(cc, iex, enclosingMethod));
                    break;
                }
            } catch (CanceledSelectionException ex){
                report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.UTILIZADOR_CANCELOU, bundle.getString("generic.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                action(ActionType.CANCEL);
            } catch (POReIDException ex){
                report = new ErrorReportImpl(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, bundle.getString("generic.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                action(ActionType.CANCEL);
            }            
        } while (!tupleDetected && action == ActionType.PROGRESS);
        
        return tupleDetected;
    }
    
    
    private void restart(boolean restart) {
        if (restart) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    ConfigurationBuilder.build().restartApplication();
                }
            };

            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.scheduleAtFixedRate(runnable, 0, 3, TimeUnit.SECONDS);
        }
    }
}
