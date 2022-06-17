package pt.gov.autenticacao.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.SimpleFormatter;
import pt.gov.autenticacao.configuracao.ConfigurationBuilder;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Logger {
    private static final String fileName = "plugin Autenticacao.Gov.log";
    private static FileHandler fileTxt;
    private static SimpleFormatter formatterTxt;

    static public void setup() throws IOException {                
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("");

        logger.setLevel(Level.INFO);
        LogManager.getLogManager().reset();
                
        fileTxt = new FileHandler(ConfigurationBuilder.getLogLocation().resolve(fileName).toString(),true);                
        formatterTxt = new SimpleFormatter();
        fileTxt.setFormatter(formatterTxt);
        logger.addHandler(fileTxt);
    }
    
    
    public static String getLoglocation(){
        return ConfigurationBuilder.getLogLocation().resolve(fileName).toString();
    }
}
