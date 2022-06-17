package pt.gov.autenticacao.configuracao;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import static pt.gov.autenticacao.configuracao.ConfigurationActions.SUN_JAVA_COMMAND;
import pt.gov.autenticacao.util.Utilities;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class OsxConfigurationActions implements ConfigurationActions{
    private final static Logger LOGGER = Logger.getLogger(OsxConfigurationActions.class.getName());
    private final String FILENAME = "Autenticacao.gov.pt.plist";
    private final String BIN_LOCATION = "MacOS/Autenticacao.gov.pt";
    private final String JAVA_BIN = "java";
    private final Path local = Paths.get(System.getProperty("user.home"),"Library/LaunchAgents");
    private final Path classPath = Paths.get(System.getProperty("java.class.path"));
    private final Path configurationFileLocation;
    
    
    public OsxConfigurationActions(){
        String dirName;
        
        try { // ou é uma app ou executou jar diretamente, não é boa prática mas enfim.
            dirName = classPath.getParent().getParent().getParent().getFileName().toString().replace(".app", "");
        } catch (NullPointerException ex) { // executou jar 
            dirName = "plugin Autenticação.Gov";
        }
        
        configurationFileLocation = Paths.get(System.getProperty("user.home"), "Library/Application Support", dirName);
        
        Path currentDirectory = Paths.get(new File("").getAbsolutePath());        
        Path jarFilename = Paths.get(System.getProperty(SUN_JAVA_COMMAND).split("(?<=[.]jar)")[0]); // incluir o .jar - http://stackoverflow.com/questions/3481828/how-to-split-a-string-in-java              
        Path jarFilenameParent = jarFilename.getParent() == null ? Paths.get("") : jarFilename.getParent();
        Path applicationLocation = (!jarFilename.isAbsolute()) ? currentDirectory.resolve(jarFilenameParent) : jarFilenameParent;            
        
        if (!jarFilename.toString().contains(".jar")){
            String path = OsxConfigurationActions.class.getResource(OsxConfigurationActions.class.getSimpleName()+".class").getFile();
            jarFilename = Paths.get((path.split("(?<=[.]jar)")[0]).replaceFirst("^file:", ""));            
        }                                        
                                                       
        DetectUpdate update = new DetectUpdate(applicationLocation,jarFilename.getFileName().toString());
        update.init();
    }
    
    
    @Override
    public boolean configureStartup(boolean enable) {        
        if (enable){
            try {
                Files.createDirectories(local);
                String plist = new Scanner(OsxConfigurationActions.class.getResourceAsStream("/"+FILENAME), "UTF-8").useDelimiter("\\A").next();                                            
                String save = plist.replaceFirst("__PATH__", classPath.getParent().getParent().resolve(BIN_LOCATION).toString());
                Files.write(local.resolve(FILENAME), save.getBytes(Charset.forName("UTF-8")));
            } catch (NullPointerException | IOException ex) {
                LOGGER.log(Level.INFO, "Exceção: configureStartup(): {0}", ex.getMessage());
            }
        } else {
            try {
                return Files.deleteIfExists(local.resolve(FILENAME));
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, "Exceção: configureStartup(): {0}", ex.getMessage());
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

    /* baseado em http://lewisleo.blogspot.jp/2012/08/programmatically-restart-java.html */
    @Override
    public void restartApplication() {
        final String comando = System.getProperty(SUN_JAVA_COMMAND);
        final List<String> java = Utilities.buildJVMCommand(JAVA_BIN);
        final String[] comandos = comando.split(" "); // problemático no caso de paths com espaços.

        //path sem espaços e sem parametro || path com espaços e sem parametro
        if (comandos.length >= 1 && comandos[comandos.length - 1].endsWith(".jar")) {
            java.add("-Dapple.awt.UIElement=true");
            java.add("-jar");
            java.add(comando);
        } else {
            //path sem espaços e com parametro || path com espaços e com parametro
            if (comandos.length >= 2 && comandos[comandos.length - 2].endsWith(".jar")) {
                java.add("-Dapple.awt.UIElement=true");
                java.add("-jar");
                java.add(comando.replaceFirst(" "+comandos[comandos.length - 1] + "$", ""));                
                java.add(comandos[comandos.length - 1]);
            } else { 
                // executado a partir de uma app
                java.clear();
                java.add(Paths.get(System.getProperty("java.class.path")).getParent().getParent().resolve("MacOS/plugin-autenticacao-gov").toString());
            }
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread("restart thread") {
            @Override
            public void run() {
                try {
                    /*Files.write(Paths.get("/Users/ruim/COMANDOS.TXT"), ("Windows\ncomandos = " + System.getProperty(SUN_JAVA_COMMAND) + "\n java = " + java.toString()).getBytes());                    
                    ProcessBuilder pb = new ProcessBuilder(java);
                    pb.redirectOutput(new File("/Users/ruim/OUTPUT.TXT"));
                    pb.redirectError(new File("/Users/ruim/ERROR.TXT"));
                    pb.start();*/
                    
                    new ProcessBuilder(java).start();
                } catch (IOException e) {
                    /*try {
                        Files.write(Paths.get("/Users/ruim/COMANDOS2.TXT"), ("exceção = "+e.getMessage()+"\nWindows\ncomandos = " + System.getProperty(SUN_JAVA_COMMAND) + "\n java = " + java.toString()).getBytes());
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
        try {
            return Paths.get(System.getProperty("user.home"), "Desktop");            
        } catch (Exception ex) {
            return Paths.get(System.getProperty("user.home"));
        }
    }

    @Override
    public boolean isJava9PlusFixed() {
        return true; // a aplicação em osx inclui uma JRE compatível.
    }

    @Override
    public Path getBinaryFileLocation() {
        return classPath.getParent().getParent().resolve(BIN_LOCATION);
    }

    @Override
    public boolean isCompatibilityModeAvailable() {
        return true;
    }
    
    @Override
    public boolean isCompatibilityModeEnabled() {
        return true;
    }
}
