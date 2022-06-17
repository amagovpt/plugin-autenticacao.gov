package pt.gov.autenticacao;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
import pt.gov.autenticacao.configuracao.Configuration;
import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import javax.swing.UIManager;
import org.poreid.common.Util;
import pt.gov.autenticacao.configuracao.ConfigurationBuilder;
import pt.gov.autenticacao.dialogs.about.About;
import pt.gov.autenticacao.util.Utilities;


public class TaskTray {
    private boolean supported = false;
    private SystemTray tray;
    private TrayIcon trayIcon;
    private final String code;
    private final String ACTIVAR = "Iniciar o plugin Autenticação.Gov automaticamente";    
    private final Configuration configuration;
            
    
    
    public TaskTray(String code, Configuration configuration){
        this.code = code;
        this.configuration = configuration;
    }
    
    
    public boolean tryTray() {
        UIManager.getLookAndFeelDefaults().put("TabbedPane:TabbedPaneTab.contentMargins", new Insets(10, 100, 0, 0));

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                supported = tryDisplayTray();
            }
        });

        return supported;
    }
    
    
    private boolean tryDisplayTray(){                
        boolean isAvailable = false;
        
        
        if (!SystemTray.isSupported()) {
            return isAvailable;
        }
                                                
        tray = SystemTray.getSystemTray();
        trayIcon = new TrayIcon(Util.getImage("/icone.png").getScaledInstance(tray.getTrayIconSize().width, tray.getTrayIconSize().height, Image.SCALE_SMOOTH), "plugin Autenticação.Gov");                
        final PopupMenu popup = createPopupMenu();
        trayIcon.setPopupMenu(popup);
        
        try {            
            tray.add(trayIcon);
            isAvailable = true;
        } catch (AWTException e) {
            return isAvailable;
        }
        
        return isAvailable;                
    }    
    
    
    private PopupMenu createPopupMenu() {
        final PopupMenu popup = new PopupMenu();
        
        MenuItem atualizacao = new MenuItem("Verificar atualizações");
        atualizacao.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            Utilities.browse(new URI(Agente.HELP.getCheckForUpdatesLocation()));
                        } catch (URISyntaxException ignore) {
                        }
                    }
                });
        popup.add(atualizacao);
        popup.addSeparator();
        
        if (null != configuration) {
            CheckboxMenuItem startup = new CheckboxMenuItem(ACTIVAR, configuration.isStartupEnabled());
            startup.addItemListener(
                    new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent e) {
                            configuration.setStartup(e.getStateChange() == ItemEvent.SELECTED);
                        }
                    });
            popup.add(startup);
            popup.addSeparator();
        }                
        
        MenuItem restartItem = new MenuItem("Reiniciar plugin Autenticação.Gov");
        restartItem.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {                                                
                        ConfigurationBuilder.build().restartApplication();
                    }
                });
        popup.add(restartItem);
        popup.addSeparator();
        
        MenuItem diagnostico = new MenuItem("Diagnosticar plugin");
        diagnostico.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            if (!isCompatibilityModeEnabled()) {
                                Utilities.browse(new URI("http://127.0.0.1:" + Agente.port_ + "/network"));
                            } else {
                                Utilities.browse(new URI("https://m" + (new Random().nextInt(19) + 1) + ".mordomo.gov.pt:" + Agente.port_ + "/network"));
                            }
                        } catch (URISyntaxException ignore) {
                        }
                    }
                });
        popup.add(diagnostico);  
        popup.addSeparator();
        
        if (ConfigurationBuilder.build().isCompatibilityModeAvailable()) {
            CheckboxMenuItem compatibilityModeMenu = new CheckboxMenuItem("Modo de compatibilidade", isCompatibilityModeEnabled());
            compatibilityModeMenu.addItemListener(
                    new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    configuration.setCompatibilityMode(e.getStateChange() == ItemEvent.SELECTED);
                    ConfigurationBuilder.build().restartApplication();
                }
            });
            popup.add(compatibilityModeMenu);
            popup.addSeparator();
        }
        
        MenuItem aboutItem = new MenuItem("Sobre este plugin");
        aboutItem.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {                        
                        About.showAbout(code);
                    }
                });
        popup.add(aboutItem);
        popup.addSeparator();       
        MenuItem exitItem = new MenuItem("Sair");                        
        exitItem.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        tray.remove(trayIcon);
                        System.exit(0);
                    }
                });
        popup.add(exitItem);      
        return popup;
    }  
    
    
    private boolean isCompatibilityModeEnabled(){
        return (null != configuration ? configuration.isCompatibilityModeEnabled() : false);
    }
}
