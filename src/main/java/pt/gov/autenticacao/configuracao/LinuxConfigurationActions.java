package pt.gov.autenticacao.configuracao;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import static pt.gov.autenticacao.configuracao.ConfigurationActions.SUN_JAVA_COMMAND;
import pt.gov.autenticacao.util.Utilities;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class LinuxConfigurationActions implements ConfigurationActions{
    private final static Logger LOGGER = Logger.getLogger(LinuxConfigurationActions.class.getName());
    private final String filename = "plugin-autenticacao-gov.desktop";
    private final Path local = Paths.get(System.getProperty("user.home"),".config/autostart",filename);
    private final Path shortCutLocation = Paths.get("/usr/share/applications", filename);
    private final Path configurationFileLocation = Paths.get(System.getProperty("user.home"),".config/plugin-Autenticacao.Gov");
    private final String JAVA9ADDMODULE = "--add-modules=java.xml.bind";
    private final String JAVA2DXRENDER ="-Dsun.java2d.xrender=false";
    private final String JAVA_BIN = "java";
    private final Path applicationLocation;
    
    
    public LinuxConfigurationActions(){        
        Path currentDirectory = Paths.get(new File("").getAbsolutePath());        
        Path jarFilename = Paths.get(System.getProperty(SUN_JAVA_COMMAND).split("(?<=[.]jar)")[0]); // incluir o .jar - http://stackoverflow.com/questions/3481828/how-to-split-a-string-in-java                              
        Path jarFilenameParent = jarFilename.getParent() == null ? Paths.get("") : jarFilename.getParent();
        applicationLocation = (!jarFilename.isAbsolute()) ? currentDirectory.resolve(jarFilenameParent) : jarFilenameParent;
        
        DetectUpdate update = new DetectUpdate(applicationLocation,jarFilename.getFileName().toString());
        update.init();
    }
    
    
    @Override
    public boolean configureStartup(boolean enable) {                
        if (enable){
            if (Files.exists(shortCutLocation)){
                try {
                    Files.createDirectories(local.getParent());
                    return (null != Files.createSymbolicLink(local, shortCutLocation));
                } catch (IOException ex) {
                    LOGGER.log(Level.INFO, "Exceção: configureStartup(): {0}", ex.getMessage());
                }
            }
        } else {
            try {
                return Files.deleteIfExists(local);
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
        final LinkedHashSet<String> java = new LinkedHashSet<>();
        java.addAll(Utilities.buildJVMCommand(JAVA_BIN));
        final String[] comandos = comando.split(" "); // problemático no caso de paths com espaços.        
        java.add(JAVA2DXRENDER);
        if (!isJava9PlusFixed()) {            
            java.add(JAVA9ADDMODULE);
        }        
        java.add("-jar"); 
                
        //path sem espaços e sem parametro || path com espaços e sem parametro
        if (comandos.length >= 1 && comandos[comandos.length - 1].endsWith(".jar")) {            
            java.add(comando);
        }
        //path sem espaços e com parametro || path com espaços e com parametro
        if (comandos.length >= 2 && comandos[comandos.length - 2].endsWith(".jar")) {            
            java.add(comando.replaceFirst(" "+comandos[comandos.length - 1] + "$", ""));
            java.add(comandos[comandos.length - 1]);
        }
               
        Runtime.getRuntime().addShutdownHook(new Thread("restart thread") {
            @Override
            public void run() {
                try {
                    /*Files.write(Paths.get("/home/ruim/Desktop/COMANDOS.TXT"), ("LINUX\ncomandos = " + System.getProperty(SUN_JAVA_COMMAND) + "\n java = " + java.toString()).getBytes());                    
                    ProcessBuilder pb = new ProcessBuilder(java);
                    pb.redirectOutput(new File("/home/ruim/Desktop/OUTPUT.TXT"));
                    pb.redirectError(new File("/home/ruim/Desktop/ERROR.TXT"));
                    pb.start(); */                    
                    new ProcessBuilder(new ArrayList<>(java)).start();
                } catch (IOException e) {
                    /*try {
                        Files.write(Paths.get("/home/ruim/Desktop/COMANDOS2.TXT"), ("LINUX\ncomandos = " + System.getProperty(SUN_JAVA_COMMAND) + "\n java = " + java.toString()).getBytes());
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
        Path path = Paths.get(System.getProperty("user.home"), "Desktop");
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            return path;
        } else {
            return Paths.get(System.getProperty("user.home"));
        }
    }
 
    @Override
    public boolean isJava9PlusFixed() {
        if (Utilities.buildJVMCommand(JAVA_BIN).toString().matches(".*"+JAVA9ADDMODULE+".*")){            
            return true;
        } 
                
        int javaVersion;
        try {
            Method runTimeVersion = Runtime.class.getMethod("version");
            Object version = runTimeVersion.invoke(null);
            Method versionMajor = runTimeVersion.getReturnType().getMethod("major");
            javaVersion = (int) versionMajor.invoke(version);
        } catch (Exception ex) {
            javaVersion = Integer.parseInt(System.getProperty("java.specification.version").substring(2));
        }
        return (javaVersion < 9);
    }
    
    /*
    @Override
    public boolean isJava9Fixed() {
        if (Utilities.buildJVMCommand().toString().matches(".*"+JAVA9ADDMODULE+".*")){            
            return false;
        } 
                
        int javaVersion;
        try {
            Method runTimeVersion = Runtime.class.getMethod("version");
            Object version = runTimeVersion.invoke(null);
            Method versionMajor = runTimeVersion.getReturnType().getMethod("major");
            javaVersion = (int) versionMajor.invoke(version);
        } catch (Exception ex) {
            javaVersion = Integer.parseInt(System.getProperty("java.specification.version").substring(2));
        }
        return (javaVersion >= 9);
    }
    */

    @Override
    public Path getBinaryFileLocation() {
        return applicationLocation;
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
