package pt.gov.autenticacao.configuracao;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Configuration {
    private final static Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    private final String STARTUP = "startup";
    private final String COMPATIBILITY_MODE = "compatibility";
    private final String TRUE = Boolean.TRUE.toString();
    private final String FALSE = Boolean.FALSE.toString();
    private final String CONFIG_FILE_NAME = "plugin.Autenticacao.Gov.config";    
    Properties properties = null;    
    private boolean isConfigurationAvailable = false;
    private ConfigurationActions actions;
   
    
    public Configuration(ConfigurationActions actions){
        this.actions = actions;
                
        try (InputStream input = new FileInputStream(actions.getConfigurationFileLocation().resolve(CONFIG_FILE_NAME).toString())){
            properties = new Properties();
            properties.load(input);                        
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Exceção: Configuration(): {0}", ex.getMessage());
            loadGlobalConfig();
        }
        
        isConfigurationAvailable = (null != properties);
    }
    
    
    public boolean isCompatibilityModeEnabled(){
        return (isConfigurationAvailable ? properties.getProperty(COMPATIBILITY_MODE, Boolean.toString(actions.isCompatibilityModeEnabled())).equalsIgnoreCase(TRUE) : actions.isCompatibilityModeEnabled());
    }
    
    
    public boolean isConfigurationAvailable(){
        return isConfigurationAvailable;
    }
    
    
    public boolean isStartupEnabled() {
        return isConfigurationAvailable && properties.getProperty(STARTUP, FALSE).equalsIgnoreCase(TRUE);
    }

    
    public void checkNCorrectStartupShortCut(){
        actions.configureStartup(isStartupEnabled());
    }
    
    
    public final void setStartup(boolean state) {                
        if (isConfigurationAvailable) {
            properties.put(STARTUP, Boolean.toString(state));            
        } else {
            properties = new Properties();
            properties.put(STARTUP, Boolean.toString(state));
            properties.put(COMPATIBILITY_MODE, Boolean.toString(actions.isCompatibilityModeEnabled()));
        } 
        
        try {
            saveConfiguration();
            actions.configureStartup(state);
        } catch (IOException ex){
            LOGGER.log(Level.INFO, "Exceção: setStartup(): {0}", ex.getMessage());
        }
    }
    
    
    public final void setCompatibilityMode(boolean state){
        if (isConfigurationAvailable) {
            properties.put(COMPATIBILITY_MODE, Boolean.toString(state));            
        } else {
            properties = new Properties();
            properties.put(STARTUP, FALSE);
            properties.put(COMPATIBILITY_MODE, Boolean.toString(state));
        }
        
        try {
            saveConfiguration();        
        } catch (IOException ex){
            LOGGER.log(Level.INFO, "Exceção: setCompatibilityMode(): {0}", ex.getMessage());
        }
    }
    
    
    private void saveConfiguration() throws IOException{
        try {      
            Files.createDirectories(actions.getConfigurationFileLocation());
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Exceção: saveConfiguration(): {0}", ex.getMessage());
        }
        
        try (OutputStream output = new FileOutputStream(actions.getConfigurationFileLocation().resolve(CONFIG_FILE_NAME).toString())) {
            properties.store(output, null);
            isConfigurationAvailable = true;
        } catch (IOException ex) {
            throw ex;
        } 
    }
        
    
    private void loadGlobalConfig(){
        try (InputStream input = new FileInputStream(actions.getBinaryFileLocation().resolve(CONFIG_FILE_NAME).toString())){
            properties = new Properties();
            properties.load(input);
            setStartup(Boolean.valueOf(properties.getProperty(STARTUP, FALSE)));
            setCompatibilityMode(Boolean.valueOf(properties.getProperty(COMPATIBILITY_MODE, Boolean.toString(actions.isCompatibilityModeEnabled()))));
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Exceção: Global Configuration(): {0}", ex.getMessage());            
        }        
    }
}
