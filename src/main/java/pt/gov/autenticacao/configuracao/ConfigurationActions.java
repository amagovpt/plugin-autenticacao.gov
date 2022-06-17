package pt.gov.autenticacao.configuracao;

import java.nio.file.Path;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public interface ConfigurationActions {
    public static final String SUN_JAVA_COMMAND = "sun.java.command";
    
    boolean configureStartup(boolean state);
    Path getConfigurationFileLocation();
    Path getDesktopLocation();
    void restartApplication();
    boolean isJava9PlusFixed();
    Path getBinaryFileLocation();
    boolean isCompatibilityModeAvailable();
    boolean isCompatibilityModeEnabled();
}
