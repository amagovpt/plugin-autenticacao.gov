package pt.gov.autenticacao.configuracao;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import mslinks.ShellLink;
import pt.gov.autenticacao.common.diagnostic.Diagnostic;
import static pt.gov.autenticacao.configuracao.ConfigurationActions.SUN_JAVA_COMMAND;
import pt.gov.autenticacao.util.Utilities;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class WindowsConfigurationActions implements ConfigurationActions{
    private final static Logger LOGGER = Logger.getLogger(WindowsConfigurationActions.class.getName());
    private String path = System.getProperty("launch4j.exefile", Paths.get(new File("").getAbsolutePath()).toString());
    private final Path configurationFileLocation = Paths.get(System.getenv("APPDATA"),"plugin Autenticacao.Gov");
    private final String JAVA_BIN = "javaw";
    
    @Override
    public boolean configureStartup(boolean enable) {
        Diagnostic.detectUserSwitch();
        Path startupFolderLocation = startupLocation();
                
        if (null != startupFolderLocation) {            
            Path shortCutLocation = startupLocation().resolve(Paths.get(path).getFileName().toString().replaceFirst("exe$", "lnk"));
            if (enable) {
                try {                    
                    return (null != ShellLink.createLink(path, shortCutLocation.toString()));
                } catch (IOException ex) {
                    LOGGER.log(Level.INFO, "Exceção: configureStartup(): {0}", ex.getMessage());
                }
            } else {
                try {
                    return Files.deleteIfExists(shortCutLocation);
                } catch (IOException ex) {
                    LOGGER.log(Level.INFO, "Exceção: configureStartup(): {0}", ex.getMessage());
                }
            }
        }
        
        return false;
    }    
    
    
    @Override
    public Path getConfigurationFileLocation() {
        try {      
            Files.createDirectories(configurationFileLocation);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Exceção: getConfigurationFileLocation(): {0}", ex.getMessage());
        }
        
        return configurationFileLocation;
    }
    
    
    private Path startupLocation(){
        HashMap<String, String> locationMap = new HashMap<>();
        locationMap.put("XP", "Start Menu\\Programs\\Startup");
        locationMap.put("POS_XP", "AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup");

        for (String location : locationMap.values()) {
            try {
                return Paths.get(System.getProperty("user.home"), location).toRealPath();
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, "Exceção: startupLocation(): {0}", ex.getMessage());
            }
        }
        
        return null;
    }   
    
    /* baseado em http://lewisleo.blogspot.jp/2012/08/programmatically-restart-java.html */
    @Override
    public void restartApplication() {
        final String comando = System.getProperty(SUN_JAVA_COMMAND);
        final List<String> java = Utilities.buildJVMCommand(JAVA_BIN);
        final String[] comandos = comando.split(" "); // problemático no caso de paths com espaços.

        //path sem espaços e sem parametro || path com espaços e sem parametro
        if ((comandos.length >= 1 && comandos[comandos.length - 1].endsWith(".jar"))) {
            java.add("-jar");
            java.add(comando);  
        } else {
            //path sem espaços e com parametro || path com espaços e com parametro
            if (comandos.length >= 2 && comandos[comandos.length - 2].endsWith(".jar")) {
                java.add("-jar");
                java.add(comando.replaceFirst(" "+comandos[comandos.length - 1] + "$", ""));                
                java.add(comandos[comandos.length - 1]);
            } else {
                //executável com path com/sem espaços sem parametro
                if (comandos[comandos.length - 1].endsWith(".exe")) {
                    java.clear();
                    java.add(comando);
                } else {
                    //executável com path com/sem espaços com parametro (será ignorado)
                    if (comandos.length > 1 && comandos[comandos.length - 2].endsWith(".exe")) {
                        java.clear();                        
                        java.add(comando.replaceFirst(" "+comandos[comandos.length - 1] + "$", ""));
                    }
                }
            }
        }  
        
        Runtime.getRuntime().addShutdownHook(new Thread("restart thread") {
            @Override
            public void run() {
                try {
                    /*Files.write(Paths.get("c:\\Users\\ruim\\Desktop\\COMANDOS.TXT"), ("Windows\ncomandos = " + System.getProperty(SUN_JAVA_COMMAND) + "\n java = " + java.toString()).getBytes());                    
                    ProcessBuilder pb = new ProcessBuilder(java);
                    pb.redirectOutput(new File("c:\\Users\\ruim\\Desktop\\OUTPUT.TXT"));
                    pb.redirectError(new File("c:\\Users\\ruim\\Desktop\\ERROR.TXT"));
                    pb.start();*/
                    
                    new ProcessBuilder(java).start();
                } catch (IOException e) {
                    /*try {
                        Files.write(Paths.get("c:\\Users\\ruim\\Desktop\\COMANDOS2.TXT"), ("exceção = "+e.getMessage()+"\nWindows\ncomandos = " + System.getProperty(SUN_JAVA_COMMAND) + "\n java = " + java.toString()).getBytes());
                    } catch (IOException ex) {
                        Logger.getLogger(LinuxConfigurationActions.class.getName()).log(Level.SEVERE, null, ex);
                    }*/
                }
            }
        });
        System.exit(0);
    }

    @Override
    public Path getDesktopLocation() {        
        return Paths.get(javax.swing.filechooser.FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath());
    }
    
    @Override
    public boolean isJava9PlusFixed() {
        return true; // a aplicação em windows inclui uma JRE compatível.
    }

    @Override
    public Path getBinaryFileLocation() {
        return Paths.get(path).getParent();
    }

    @Override
    public boolean isCompatibilityModeAvailable() {
        return true;
    }
    
    @Override
    public boolean isCompatibilityModeEnabled() {
        return false;
    }
}
