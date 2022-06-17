package pt.gov.autenticacao.common;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public interface ErrorReport {

    Inputs getInputs();
    
    public enum ErrorType {
        CARTAO_ERRADO,
        CARTAO_NAO_DETETADO_UTILIZADOR_CANCELOU,
        CARTAO_NAO_DETETADO_AJUDA,
        CARTAO_NAO_DETETADO_TIMEOUT,
        CARTAO_NAO_DETETADO_SSO,
        LEITOR_NAO_DETETADO_UTILIZADOR_CANCELOU,
        LEITOR_NAO_DETETADO_AJUDA,
        LEITOR_NAO_DETETADO_TIMEOUT,
        LEITOR_NAO_DETETADO_SSO,
        UTILIZADOR_RECUSOU_PIN,
        UTILIZADOR_DEIXOU_EXPIRAR,
        UTILIZADOR_CANCELOU,
        PIN_BLOQUEADO,
        ERRO_GENERICO,
        ERRO_PERMISSOES,
        ERRO_LEITURA,
        NAO_FORAM_ENCONTRADOS_CERTIFICADOS,
        AGENTE_NAO_ENCONTRADO,
        AGENTE_OCUPADO;
    }
    
}
