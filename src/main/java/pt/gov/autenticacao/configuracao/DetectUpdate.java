package pt.gov.autenticacao.configuracao;

import java.io.IOException;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class DetectUpdate {

    private final static Logger LOGGER = Logger.getLogger(DetectUpdate.class.getName());
    private final Path toWatch;
    private final String filename;

    public DetectUpdate(Path toWatch, String filename) {
        this.toWatch = toWatch;
        this.filename = filename;                        
    }

    public void init() {
        try {
            WatchService watcher = toWatch.getFileSystem().newWatchService();
            WatchQueueReader fileWatcher = new WatchQueueReader(watcher);
            Thread th = new Thread(fileWatcher, "update detect");
            th.start();

            toWatch.register(watcher, ENTRY_MODIFY, ENTRY_CREATE);
        } catch (IOException ex) {
            /* não é critico que falhe */
            LOGGER.log(Level.INFO, "Exceção: DetectUpdate(): {0}", ex.getMessage());
        }
    }

    private class WatchQueueReader implements Runnable {

        private final WatchService myWatcher;
        private boolean found = false;

        public WatchQueueReader(WatchService myWatcher) {
            this.myWatcher = myWatcher;
        }

        @Override
        public void run() {
            try {
                WatchKey key = myWatcher.take();                
                while (key != null) {                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == OVERFLOW) {
                            continue;
                        }
                        if (event.kind() == ENTRY_MODIFY || event.kind() == ENTRY_CREATE) {                            
                            Path p = ((Path) key.watchable()).resolve((Path) event.context());
                            if (!found && p.getFileName().toString().equalsIgnoreCase(filename)) {
                                found = true;
                                ConfigurationBuilder.build().restartApplication();
                            }
                        }                       
                    }
                    key.reset();
                    key = myWatcher.take();
                }
            } catch (InterruptedException ex) {
                /* não é critico que falhe */
                LOGGER.log(Level.INFO, "Exceção: DetectUpdate(): {0}", ex.getMessage());
            }
        }
    }
}
