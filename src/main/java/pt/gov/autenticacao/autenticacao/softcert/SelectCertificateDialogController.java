package pt.gov.autenticacao.autenticacao.softcert;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.UnsupportedLookAndFeelException;
import org.poreid.common.Util;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class SelectCertificateDialogController {
    private boolean cancelled;
    private String selected;
    private Set<String> certSet;
    private SelectCertificateDialog dialog = null;
    private Locale locale;
    
    
    private SelectCertificateDialogController(final Set<String> certSet, Locale locale){  
        try {
            this.certSet = certSet;
            this.locale = locale;
            Util.setLookAndFeel();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            throw new RuntimeException("Não foi possivel criar a janela de dialogo para seleção de certificado");
        }
    }
    
    
    public static SelectCertificateDialogController getInstance(Set<String> certSet, Locale locale){
        return new SelectCertificateDialogController(certSet, locale);
    }
    
    
    public String selectCertificate() throws CanceledSelectionException, CertificateNotFoundException{       
        try {
            if (!certSet.isEmpty()) {
                createDialog();
            } else {
                JOptionPane.showMessageDialog(null, "Não foram encontrados certificados.", "Aviso", JOptionPane.WARNING_MESSAGE);
                throw new CertificateNotFoundException("Não foram encontrados certificados");
            }
        } catch (InterruptedException | InvocationTargetException ex) {
            throw new CanceledSelectionException(ex.getMessage());
        }
        
        if (cancelled){
            throw new CanceledSelectionException("Operação de seleção de certificado cancelada");
        }
        
        return selected;
    }
    
    
    private void createDialog() throws InterruptedException, InvocationTargetException{
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                dialog = new SelectCertificateDialog(certSet, locale, listener);
                dialog.setVisible(true);
            }
        });
    }
    
    
    private DialogEventListener listener = new DialogEventListener() {

        @Override
        public final void onCancel() {
            SelectCertificateDialogController.this.cancelled = true;
        }

        @Override
        public final void onDiagloclosed() {
            SelectCertificateDialogController.this.cancelled = true;
        }

        @SafeVarargs
        @Override
        public final void onContinue(String... certId) {
            SelectCertificateDialogController.this.selected = certId.length > 0 && null!= certId[0] ? certId[0] : null;
        }
    };    
}
