package pt.gov.autenticacao;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public enum InstallType {
    JAVA_BUNDLED("cj","C"),
    NO_JAVA_BUNDLED("sj", "S"),    
    VANILLA("vnl", "V"),
    AUTO_DETECT("auto","auto");
    
    private final String type;
    private final String code;
    
    private InstallType(final String type, String color){
        this.type = type;
        this.code = color;
    } 
    
    public String getInstallType(){
        return this.type;
    }
    
    public String getCode(){
        return code;
    }
}
