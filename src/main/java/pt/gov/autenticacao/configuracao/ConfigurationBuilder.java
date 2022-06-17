package pt.gov.autenticacao.configuracao;

import java.nio.file.Path;
import pt.gov.autenticacao.util.Utilities;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class ConfigurationBuilder {
    private static ConfigurationActions configuration = null;

    public static ConfigurationActions build() {
        if (null != configuration) {
            return configuration;
        }

        switch (Utilities.getOs()) {
            case windows:
                configuration = new WindowsConfigurationActions();
                break;
            case macos:
                configuration = new OsxConfigurationActions();
                break;
            default:
                configuration = new LinuxConfigurationActions();
        }

        return configuration;
    }

    public static Path getLogLocation() {
        return build().getConfigurationFileLocation();
    }
}
