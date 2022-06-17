package pt.gov.autenticacao.read.cc;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public enum Attribute {
    SOD("sod"),
    ID("id"),
    FOTO("foto"),
    MORADA("morada"), 
    AUTH_CHAIN("authChain"),
    SIGN_CHAIN("signChain");

    private final String name;

    private Attribute(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
