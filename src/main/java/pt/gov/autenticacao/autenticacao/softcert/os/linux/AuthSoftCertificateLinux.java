package pt.gov.autenticacao.autenticacao.softcert.os.linux;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import pt.gov.autenticacao.Requests;
import pt.gov.autenticacao.common.Operation;
import pt.gov.autenticacao.common.Inputs;
import pt.gov.autenticacao.common.Response;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class AuthSoftCertificateLinux extends Operation{    
    private final static Logger LOGGER = Logger.getLogger(AuthSoftCertificateLinux.class.getName());
    
    public AuthSoftCertificateLinux(Map<String, String> params, Requests requests){
       super(params, requests);
    }  
            
        
    @SuppressWarnings("empty-statement")
    @Override
    public Response doOperation() { 
        class Name {};        
        Inputs containerInputs = null;
        bundle = ResourceBundle.getBundle(Operation.class.getSimpleName(), locale);
        
        /* ISTO NAO ESTA ACABADO!!
        try {
            validateSecurityParameters();
                        
            // procurar chave privada na keystore do sistema operativo 
            
            containerInputs = container.getInputs();       
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {            
            report = new ErrorReportImplCC(ErrorReportImplCC.ErrorType.ERRO_GENERICO, bundle.getString("dialog.error"));
        } catch (UnsupportedEncodingException | JAXBException ex) {            
            report = new ErrorReportImplCC(ErrorReportImplCC.ErrorType.ERRO_GENERICO, bundle.getString("scap.data.error") + Utilities.addLog(Name.class.getEnclosingMethod().getName(), ex));
        } catch (AttributeContainerException ex) {            
            report = new ErrorReportImplCC(ErrorReportImplCC.ErrorType.ERRO_GENERICO, bundle.getString("key.error") + Utilities.addLog(Name.class.getEnclosingMethod().getName(), ex));
        } catch (RequiredParameterException ex) {            
            report = new ErrorReportImplCC(ErrorReportImplCC.ErrorType.ERRO_GENERICO, bundle.getString("required.parameter") + Utilities.addLog(Name.class.getEnclosingMethod().getName(), ex));                    
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.INFO, "URL inválido, exceção: {0}", ex );
            return new Response();
        }*/
        
        return new Response((null != report) ? report.getInputs() : containerInputs);
    }            

    @Override
    public void action(ActionType b) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
