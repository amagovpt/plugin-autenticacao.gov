package pt.gov.autenticacao.autenticacao.softcert;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public interface DialogEventListener {

    void onCancel();

    void onDiagloclosed();
    
    void onContinue(String... data);
}
