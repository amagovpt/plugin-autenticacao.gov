package pt.gov.autenticacao;

import pt.gov.autenticacao.configuracao.Configuration;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import pt.gov.autenticacao.configuracao.ConfigurationBuilder;
import pt.gov.autenticacao.dialogs.error.ErrorDialog;
import pt.gov.autenticacao.util.Utilities;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Agente implements Update{
    private final static Logger LOGGER = Logger.getLogger(Agente.class.getName());
    private final int[] supportedPorts = {35153, 43456, 47920, 57379, 64704};
    private final String key = "fcb8e3b5-63d8-4f0e-9457-39680a924d1e";
    public static final HelpPageLocation HELP = new HelpPageLocation();
    public static final String DOMAIN = "mordomo.gov.pt";
    private static final String EMPTY_STRING = "";
    private Configuration config;    
    protected static int port_;
    String reason;
    private Service agente = null;
    private InstallType it;
    private boolean flp = true; 
    

    public static void main(String[] args) {
        //System.setProperty("javax.net.debug","ssl,handshake");
        try {             
            pt.gov.autenticacao.util.Logger.setup();
        } catch (IOException ex) {
            System.out.println("Não é possivel efetuar log do plugin "+ex.getMessage());
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("version")){
            System.out.println(Service.VERSION);
            System.exit(0);
        }
        
        /*if (!ConfigurationBuilder.build().isJava9PlusFixed()) {
            LOGGER.log(Level.INFO, "Aviso: Java 9 ou superior detetado, é necessário reiniciar a aplicação com os parâmetros corretos.");
            ConfigurationBuilder.build().restartApplication();
        }*/
        
        new Agente().init(args.length > 0 ? args[0] : EMPTY_STRING);
    }

    
    public void init(String type) {        
        it = installationType(type);
                        
        try {                
            Utilities.setLookAndFeel();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            LOGGER.log(Level.INFO, "Erro: Não foí possivel utilizar o L&F Nimbus. {0}", ex.getMessage());
            System.exit(0);
        }
        
        if (!SingleInstance.setLock(key)){            
            LOGGER.log(Level.INFO, "Aviso: O plugin já está em execução.");
            JOptionPane.showMessageDialog(null, "O plugin já se encontra iniciado.", "Aviso", JOptionPane.WARNING_MESSAGE);            
            System.exit(0);
        }
                
        config = new Configuration(ConfigurationBuilder.build()); 
        startServer(config.isCompatibilityModeEnabled());

        if (reason.isEmpty()) {
            if (!InstallType.VANILLA.equals(it)) {                
                config = new Configuration(ConfigurationBuilder.build());                
                if (!config.isConfigurationAvailable()) {
                    UIManager.getLookAndFeelDefaults().put("OptionPane.sameSizeButtons", true);
                    String[] options;
                    int i;
                    if (Utilities.getOs().isWindows()) {                    
                        options = new String[]{"   Sim   ", "   Não   "};
                        i = 0;
                    } else {
                        options = new String[]{"   Não   ", "   Sim   "};
                        i = 1;
                    }
                    JOptionPane optionPane = new JOptionPane("Iniciar o plugin Autenticação.Gov sempre que iniciar o sistema?", JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options, options[i]);
                    JDialog dialog = optionPane.createDialog("Aviso");
                    dialog.setAlwaysOnTop(true);
                    dialog.setVisible(true);
                    Object value = optionPane.getValue();
                    config.setStartup(null != value && (options[i].equalsIgnoreCase(value.toString())));                    
                } else {                                        
                    config.checkNCorrectStartupShortCut();
                }
            }
                        
            if (isTrayAvailable()) {
                new GnomeTray(it.getCode(), config).setVisible(true);                
            } else {
                TaskTray tray = new TaskTray(it.getCode(), config);
                tray.tryTray();
            }
            UserSwitch.setService(this);
        } else {
            logAndExit();
        }
    }
    
    
    private boolean isTrayAvailable() {
        boolean gnome3 = false;
        
        if (Utilities.getOs().isLinux()) {        
            String ds = System.getenv("XDG_CURRENT_DESKTOP");
            gnome3 = (null != ds && ds.trim().toLowerCase().contains("gnome"));
        }
        //System.out.println("temos gnome: "+gnome3);
        return gnome3;
    } 
    
    
    private InstallType installationType(String type) {                                
                               
        for (InstallType c : InstallType.values()) {
            if (c.getInstallType().equalsIgnoreCase(type)) {
                if (!c.equals(InstallType.AUTO_DETECT)) {
                    return c;
                }
                
                Path javaPath =  Paths.get(System.getProperty("java.home"));
                Path agentParentPath =  Paths.get(System.getProperty("launch4j.exefile", "a/b")).getParent();
                                                
                return javaPath.startsWith(agentParentPath) ? InstallType.JAVA_BUNDLED : InstallType.NO_JAVA_BUNDLED;
            }
        }

        return InstallType.VANILLA;
    }
    
    
    private void logAndExit() {
        LOGGER.log(Level.INFO, "Não foi possivel associar o plugin Autenticação.Gov a um porto, Motivo: {0}", reason);
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    ErrorDialog dialog = new ErrorDialog(new Locale("pt", "PT"));
                    dialog.setVisible(true);
                }
            });            
            System.exit(0);
        } catch (InterruptedException | InvocationTargetException iex) {
            LOGGER.log(Level.INFO, "Não foi possivel associar o plugin Autenticação.Gov a um porto, Motivo: {0} {1}", new Object[]{reason, iex.getMessage()});
        }
    } 
    
    
    void startServer(boolean compatibility){ 
        LOGGER.log(Level.INFO, "A iniciar... versão = {0}", Service.VERSION);
        for (int port : supportedPorts) {
            agente = new Service(port, it.getInstallType(), compatibility);
            try {                                
                agente.start(this, compatibility);
                check();
                reason = EMPTY_STRING;
                port_ = port;
                break;
            } catch (Exception ex) {
                reason = ex.getMessage();
            }
        }                
    }
    
    
    void stopServer(){
        LOGGER.log(Level.INFO, "A tentar parar...");
        if (!agente.stop()){
            LOGGER.log(Level.INFO, "Erro na tentativa de paragem no servidor do Agente. Terminando plugin.");
            System.exit(0);
        } else {
            LOGGER.log(Level.INFO, "Paragem no servidor do Agente.");
        }
    } 
    
    
    @Override
    public void check() {
        if (flp) {
            if (agente.needsUpdate()) {
                LOGGER.log(Level.INFO, "O certificado utilizado pelo plugin Autenticação.Gov encontra-se expirado.");
                JOptionPane optionPane = new JOptionPane("O certificado utilizado pelo plugin Autenticação.Gov está expirado.\n"
                        + "Após premir o botão OK será direcionado para a página de atualização do plugin Autenticação.Gov.", JOptionPane.OK_OPTION);
                JDialog dialog = optionPane.createDialog("Aviso");
                dialog.setAlwaysOnTop(true);
                dialog.setVisible(true);
                try {
                    Utilities.browse(new URI(Agente.HELP.getCheckForUpdatesLocation()));
                } catch (URISyntaxException ex) {
                    LOGGER.log(Level.INFO, "Não foi possível aceder à página de distribuição da aplicação. {0}", ex.getReason());
                }
            }
        }
        flp = !flp;
    }
}
