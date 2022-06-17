package pt.gov.autenticacao.common;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public enum Parameter {
    
    AUTH_GOV_CERTIFICATE("AuthGovCertificate"),
    AUTH_SUBMIT_URL("AuthSubmitUrl"),
    AUTH_TOKEN_ID("AuthTokenId"),
    AUTH_LOCALE_LANGUAGE("AuthLocaleLanguage"),
    AUTH_LOCALE_COUNTRY("AuthLocaleCountry"),
    AUTH_DATA_REQUESTED("AuthDataRequested"),
    AUTH_AUTHORIZED_ATTRIBUTES("AuthAuthorizedAttributes"),
    AUTH_SIGNATURE("AuthSignature"),
    AGENT_TOKEN("AgentToken"),
    SOCSP_STAPLE("SocspStaple"),
    HELP_PAGE_LOCATION("HelpPageLocation"), 
    CRYPTOGRAPHIC_HASH_ALGORITHM("CryptoHashAlgorithm"),
    CRYPTOGRAPHIC_HASH("CryptoHash");
    
    private final String name;

    private Parameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
