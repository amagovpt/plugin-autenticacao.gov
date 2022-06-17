package pt.gov.autenticacao.autenticacao.cc.pan;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.ResourceBundle;
import javax.crypto.NoSuchPaddingException;
import org.poreid.CardFactory;
import org.poreid.CardNotPresentException;
import org.poreid.CardTerminalNotPresentException;
import org.poreid.POReIDException;
import org.poreid.SmartCardFileException;
import org.poreid.UnknownCardException;
import pt.gov.autenticacao.common.Response;
import pt.gov.autenticacao.common.Inputs;
import pt.gov.autenticacao.common.Operation;
import org.poreid.dialogs.selectcard.CanceledSelectionException;
import pt.gov.autenticacao.Agente;
import pt.gov.autenticacao.Requests;
import pt.gov.autenticacao.common.ContainerException;
import pt.gov.autenticacao.common.ErrorReport.ErrorType;
import pt.gov.autenticacao.common.ErrorReportImplPan;
import pt.gov.autenticacao.common.Parameter;
import pt.gov.autenticacao.common.diagnostic.Diagnostic;
import pt.gov.autenticacao.util.der.AuthGovCertificateExtensions;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class PanReader extends Operation{    
    private PanContainer container;
    
    
    public PanReader(Map<String, String> params, Requests requests) {        
        super(params, requests);
    }
                    
    
    @SuppressWarnings("empty-statement")
    @Override
    public Response doOperation() {
        class Name {};
        String enclosingMethod = Name.class.getEnclosingMethod().getName();
        Inputs inputs = null;                
        boolean retry = true;
        
        bundle = ResourceBundle.getBundle(Operation.class.getSimpleName(), locale);
        Agente.HELP.setNewHelpPageLocation(getParameter(Parameter.HELP_PAGE_LOCATION, ""));
        if (validateSecurityParameters(AuthGovCertificateExtensions.readData, getParameter(Parameter.SOCSP_STAPLE, "").getBytes(), getParameter(Parameter.HELP_PAGE_LOCATION, "").getBytes())) {
            do { // interação com o middleware do token dos notários, uma espera de 1s deve resolver o problema.
                try {                    
                    cc = CardFactory.getCard();

                    container = new PanContainer();
                    container.setPAN(cc.getID().getDocumentNumberPAN());
                    byte[] key = new byte[16];
                    System.arraycopy(cc.getChallenge(), 0, key, 0, 8);
                    System.arraycopy(cc.getChallenge(), 0, key, 8, 8);
                    container.setSecretKey(key);
                    container.setPublicKey(authGovCert.getPublicKey());
                    container.setToken(authToken);

                    if (report == null) {
                        inputs = container.getInputs();
                    }                    
                } catch (CardTerminalNotPresentException ex) {
                    report = new ErrorReportImplPan(authGovCert.getPublicKey(), ErrorType.LEITOR_NAO_DETETADO_SSO, bundle.getString("no.card.reader.sso") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                } catch (UnknownCardException | CardNotPresentException ex) {
                    if (retry) { // interação com o middleware do token dos notários, uma espera de 1s deve resolver o problema.
                        retry = false;
                        try {
                            Thread.sleep(1000); // interação com o middleware do token dos notários, uma espera de 1s deve resolver o problema.
                        } catch (InterruptedException ex1) {/*nada para fazer*/}
                    } else {
                        report = new ErrorReportImplPan(authGovCert.getPublicKey(), ErrorType.CARTAO_NAO_DETETADO_SSO, bundle.getString("no.card.sso") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                    }
                } catch (CanceledSelectionException ex) {
                    report = new ErrorReportImplPan(authGovCert.getPublicKey(), ErrorType.UTILIZADOR_CANCELOU, bundle.getString("generic.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                } catch (POReIDException ex) {
                    report = new ErrorReportImplPan(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, bundle.getString("generic.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                } catch (SmartCardFileException ex) {
                    report = new ErrorReportImplPan(authGovCert.getPublicKey(), ErrorType.ERRO_LEITURA, bundle.getString("id.read.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                } catch (ContainerException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
                    report = new ErrorReportImplPan(authGovCert.getPublicKey(), ErrorType.ERRO_GENERICO, bundle.getString("key.error") + Diagnostic.getCCLogInfo(cc, ex, enclosingMethod));
                } finally {
                    if (null != cc && (null != report || null != inputs)) {
                        try {
                            cc.close();
                        } catch (POReIDException ignore) {
                        }
                    }
                }
            } while (!retry && null == report && null == inputs);
        }
        
        if (null != report) {
            return new Response(report.getInputs());
        }
        if (null != inputs) {
            return new Response(inputs);
        }

        return new Response();
    }           

    
    @Override
    public void action(ActionType b) {        
    }
}
