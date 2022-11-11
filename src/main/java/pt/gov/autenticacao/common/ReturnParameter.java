package pt.gov.autenticacao.common;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public enum ReturnParameter {
    
    // ERRO
    ESTADO("estado"),
    DESCRICAO("descricao"),
    // CC
    PAN("pan"),
    SOD("sod"),
    ID("id"),
    FOTO("foto"),
    MORADA("morada"),
    SCAP("scap"),    
    // COMUNS
    CERTIFICADO("certificado"),
    CERTIFICADOEC("certificadoSubEC"),
    ASSINATURA("assinatura"),
    NONCE("nonce"),
    KEY("key"),
    IV("iv"),
    //ASSINATURA
    SIGNATURE_BYTES("assinatura"),
    CHAIN("chain");
    
    private final String name;

    private ReturnParameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
