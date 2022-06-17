package pt.gov.autenticacao.autenticacao.cc;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public enum Attribute {
    SOD("sod"),
    ID("id"),
    FOTO("foto"),
    MORADA("morada"), 
    CERTIFICADO("certificado"),
    JUSTICA("justica");

    private final String name;

    private Attribute(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
