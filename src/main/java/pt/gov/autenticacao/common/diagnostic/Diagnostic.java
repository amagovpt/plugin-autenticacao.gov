package pt.gov.autenticacao.common.diagnostic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.poreid.SmartCardFileException;
import org.poreid.cc.CitizenCard;
import pt.gov.autenticacao.Service;
import pt.gov.autenticacao.util.Utilities;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Diagnostic {
    private final static Logger LOGGER = Logger.getLogger(Diagnostic.class.getName());
    private static final boolean loaded;
    private static boolean isDetecting = false;

    static {
        loaded = loadLibrary("detectReader");
    }
    
    private static native ArrayList<Reader> findReaders();
    private static native void listenMessages();
    
    private static boolean loadLibrary(String libName) {
        File temp;
        
        switch (Utilities.getOs()) {
            case linux:
                libName = "lib" + libName + (System.getProperty("os.arch").contains("64") ? "64" : "32") + ".so";
                break;
            case windows:
                libName = libName + (System.getProperty("os.arch").contains("64") ? "64" : "32") + ".dll";
                break;
            case macos:
                libName = "lib" + libName + ".dylib";
                break;
            default:
                return false;
        }        
        
        try {
            deleteAllTemporariesFiles(libName);
            temp = createTemporaryFile(libName);
            System.load(temp.getAbsolutePath());
        } catch (IOException ex) {
            return false;
        }

        return true;   
    }

    private static File createTemporaryFile(String libName) throws IOException {
        try (InputStream is = Diagnostic.class.getResourceAsStream(libName)) {

            String[] fileName = libName.split("\\.");
            File tempFile = File.createTempFile(fileName[0], "." + fileName[1]);
            tempFile.deleteOnExit();

            try (OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                return tempFile;
            }
        }
    }

    private static void deleteAllTemporariesFiles(String libName) {
        String regexp = libName.split("\\.")[0] + "*";

        try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(System.getProperty("java.io.tmpdir")), regexp)) {
            for (Path path : paths) {
                Files.delete(path);
            }
        } catch (IOException ex) { /* ignorar */
        }
    }
    
    public static String readerInfo(){
        StringBuilder readerInfo = new StringBuilder();
        
        if (loaded) {
            List<Reader> list = findReaders();
            readerInfo.append("leitores(").append(list.size()).append("): ");
            for (Reader r : list) {
                readerInfo.append("(").append(r.getName());
                readerInfo.append(";").append(r.getID());
                readerInfo.append(";").append(r.getStatus());
                readerInfo.append(";").append(r.getProblemId()).append(")");
            }            
        }
        
        return readerInfo.toString();
    }
 
    public static boolean isReaderAvailable() {
        if (loaded) {
            List<Reader> list = findReaders();            
            for (Reader r : list) {
                /*  Se encontra algum leitor mas tem erro então
                    não é problema aplicacional: erro do dispositivo / drivers / etc
                    consequência: não reinicia a aplicação
                */
                if (r.getProblemId() > 0){
                    LOGGER.log(Level.INFO, "isReaderAvailable() - Existem leitores no sistema - mas existem problemas");
                    return false;
                }
            }
            // se chegou aqui ou não tem leitores ou os leitores não apresentam erros.
            LOGGER.log(Level.INFO, "isReaderAvailable() - Existem leitores no sistema [{0}]",!findReaders().isEmpty());
            return !findReaders().isEmpty();
        }
        return false;
    }

    public static String getCCLogInfo(CitizenCard cc, String enclosingMethod) {
        return getCCLogInfo(cc, null, null, enclosingMethod);
    }

    public static String getCCLogInfo(CitizenCard cc, Exception ex, String enclosingMethod) {
        return getCCLogInfo(cc, ex, null, enclosingMethod);
    }

    public static String getCCLogInfo(CitizenCard cc, Exception ex, Integer tentativas, String enclosingMethod) {
        StringBuilder sbDiag = new StringBuilder();
        String pan = "ND";
        String versao = "ND";

        if (null != cc) {
            try {
                pan = cc.getID().getDocumentNumberPAN();
                versao = cc.getID().getDocumentVersion();
            } catch (SmartCardFileException e) {
            }
        }

        sbDiag.append("versão app: ").append(Service.VERSION).append(";");
        sbDiag.append("so: ").append(System.getProperty("os.name")).append(";");
        sbDiag.append("versão: ").append(System.getProperty("os.version")).append(";");
        if (null != tentativas) {
            sbDiag.append("tentativas: ").append(tentativas).append(";");
        }
        sbDiag.append(readerInfo()).append(";");
        sbDiag.append("pan: ").append(pan).append(";");
        sbDiag.append("versão cc: ").append(versao).append(";");
        sbDiag.append("versão java: ").append(System.getProperty("java.version")).append(";");
        sbDiag.append("vendor: ").append(System.getProperty("java.vendor")).append(";");
        sbDiag.append("id: ").append(Utilities.idToString()).append(";");
        if (null != ex) {
            sbDiag.append(ex.getMessage()).append(";").append(Utilities.addLog(enclosingMethod, ex));
        }

        LOGGER.log(Level.INFO, "getCCLogInfo() - {0}", sbDiag.toString());
        return sbDiag.toString();
    }
    
    public static void detectUserSwitch() {
        if (!isDetecting) {
            Thread t;

            t = new Thread(new Runnable() {
                @Override
                public void run() {
                    listenMessages();
                }
            }, "detect user switch");

            t.start();
            isDetecting = true;
        }
    }
}
