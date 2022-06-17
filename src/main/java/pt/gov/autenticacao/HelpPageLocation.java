package pt.gov.autenticacao;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class HelpPageLocation {
    private final String defaultLocation = "https://autenticacao.gov.pt/fa/ajuda/AutenticacaoGovPT.aspx";
    private String location = null;
    
    
    public void setNewHelpPageLocation(String newLocation){
        if (null != newLocation && !newLocation.isEmpty()){
            this.location = newLocation;
        }
    }
    
    
    public void resetHelpPageLocation(){
        this.location = null;
    }
    
    
    public String getHelpPageLocation(){
        return (null != location) ? location : defaultLocation;
    }
    
    
    public String getDefaultHelpPageLocation(){
        return defaultLocation;               
    }
    
    public String getCheckForUpdatesLocation(){
        return defaultLocation +"?v="+Service.VERSION;
    }
}
