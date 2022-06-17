package pt.gov.autenticacao;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;
import pt.gov.autenticacao.util.Utilities;

/**
 * baseado no código: http://nerdydevel.blogspot.com/2012/07/run-only-single-java-application-instance.html
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class SingleInstance {
    private static SingleInstance instance;
    private File lockFile = null;
    private FileLock lock = null;
    private FileChannel lockChannel = null;
    private FileOutputStream lockStream = null;
    private final static Logger LOGGER = Logger.getLogger(SingleInstance.class.getName());
        
    
    private SingleInstance() {
    }
    
    
    private SingleInstance(String key) throws Exception {
        String info = "Autenticação.gov.pt";
        MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        Path lockFilePath = Paths.get(System.getProperty("java.io.tmpdir"), new BigInteger(1, md.digest(key.getBytes())).toString(16));
        lockFile = lockFilePath.toFile();
        lockStream = new FileOutputStream(lockFile); 
        if (Utilities.getOs().isWindows()){
            info = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        }
        lockStream.write(info.getBytes());
        lockChannel = lockStream.getChannel();
        lock = lockChannel.tryLock();
        if (lock == null) {
            throw new Exception("tryLock()");
        }
    }


    private void release() throws Throwable {
        if (lock.isValid()) {
            lock.release();
        }
        if (lockStream != null) {
            lockStream.close();
        }
        if (lockChannel.isOpen()) {
            lockChannel.close();
        }
        if (lockFile.exists()) {
            lockFile.delete();
        }
    }

    
    @Override
    protected void finalize() throws Throwable {
        this.release();
        super.finalize();
    }

    
    public static boolean setLock(String key) {
        if (instance != null) {
            return true;
        }

        try {
            instance = new SingleInstance(key);
        } catch (Exception ex) {
            instance = null;            
            LOGGER.log(Level.INFO, "Não foi possivel instanciar o plugin Autenticação.Gov, Motivo: {0}", ex.getMessage());
            return false;
        }

        Runtime.getRuntime().addShutdownHook(new Thread("single instance") {
            @Override
            public void run() {
                SingleInstance.releaseLock();
            }
        });
        return true;
    }


    public static void releaseLock() {
        try {
            if (instance != null) {
                instance.release();
            }
        } catch (Throwable ex) {
            LOGGER.log(Level.INFO, "Erro na libertação do lock, Motivo: {0}", ex.getMessage());
        }
    }
}
