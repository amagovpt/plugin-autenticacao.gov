package pt.gov.autenticacao.common;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class RequiredParameterException extends Exception {

    public RequiredParameterException(String msg){
        super(msg);
    }
}
