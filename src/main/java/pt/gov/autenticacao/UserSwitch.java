package pt.gov.autenticacao;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import pt.gov.autenticacao.configuracao.Configuration;
import pt.gov.autenticacao.configuracao.ConfigurationBuilder;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class UserSwitch {
    private final static Logger LOGGER = Logger.getLogger(UserSwitch.class.getName());
    private static Agente agente = null;
    
    
    static void setService(Agente agente){
        UserSwitch.agente = agente;
    }
    
    
    static void stopServer(){
        LOGGER.log(Level.INFO, "UserSwitch stopServer() - uptime = {0}", ManagementFactory.getRuntimeMXBean().getUptime());
        UserSwitch.agente.stopServer();
    }
    
    
    static void startServer(){
        LOGGER.log(Level.INFO, "UserSwitch startServer() - uptime = {0}", ManagementFactory.getRuntimeMXBean().getUptime());
        UserSwitch.agente.startServer(new Configuration(ConfigurationBuilder.build()).isCompatibilityModeEnabled());
    }
    
    
    static void restartApplication() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                LOGGER.log(Level.INFO, "UserSwitch restartApplication() - uptime = {0}", ManagementFactory.getRuntimeMXBean().getUptime());
                ConfigurationBuilder.build().restartApplication();
            }
        };
        
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 3, 3, TimeUnit.SECONDS);
    }
}
