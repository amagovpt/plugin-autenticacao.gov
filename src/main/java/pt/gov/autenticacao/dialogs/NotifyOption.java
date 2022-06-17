package pt.gov.autenticacao.dialogs;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public interface NotifyOption {
    
    public enum ActionType {
        HELP,
        TIMEOUT,
        CANCEL,
        PROGRESS
    }
    
    void action(ActionType b);    
}
