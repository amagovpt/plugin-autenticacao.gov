package pt.gov.autenticacao.common;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class ContainerException extends Exception{
    public ContainerException(String msg, Exception ex){
        super(msg, ex);
    }
}
