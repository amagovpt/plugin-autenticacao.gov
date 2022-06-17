package pt.gov.autenticacao.common.diagnostic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.poreid.CardFactory;
import org.poreid.CardNotPresentException;
import org.poreid.CardTerminalNotPresentException;
import org.poreid.CertificateNotFound;
import org.poreid.POReIDException;
import org.poreid.SmartCardFileException;
import org.poreid.UnknownCardException;
import org.poreid.cc.CitizenCard;
import org.poreid.dialogs.pindialogs.PinBlockedException;
import org.poreid.dialogs.pindialogs.PinEntryCancelledException;
import org.poreid.dialogs.pindialogs.PinTimeoutException;
import org.poreid.dialogs.selectcard.CanceledSelectionException;
import pt.gov.autenticacao.Service;
import pt.gov.autenticacao.configuracao.ConfigurationBuilder;
import pt.gov.autenticacao.util.Base64;

/**
 *
 * @author ruim
 */
public class Sod implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(Sod.class.getName());
    private final String fileName = "plugin-analise-" + Service.VERSION + "-" + new SimpleDateFormat("dd-MM-yyyy").format(new Date()) + ".zip";
    private final boolean moreInfo;

    /*public static void main(String[] args) {
        new Sod().run();
    }*/
    
    public Sod(boolean moreinfo){
        this.moreInfo = moreinfo;
    }

    @Override
    public void run() {
        
        String idDigestB64 = null;
        String photoDigestB64 = null;
        String pubKeyDigestB64 = null;
        String addressDigestB64 = null;
        byte[] sod = null;
        
        /* more info */
        byte[] id = null;
        byte[] morada = null;
        /* more info */
        
        byte[] certificadoAutenticacao = null;
        byte[] certificadoAssinatura = null;
        byte[] log = null;
        Path logLocation = Paths.get(pt.gov.autenticacao.util.Logger.getLoglocation());
        CitizenCard cc = null; 
        

        LOGGER.log(Level.INFO, "início - análise.");
        LOGGER.log(Level.INFO, "Leitores detetados: {0}",Diagnostic.readerInfo());
        try {
            cc = CardFactory.getCard(false); // desabilitar a cache durante o diagnóstico

            sod = cc.getSOD();
            
            try {
                
                if (moreInfo){
                    id = cc.getID().getRawData();
                }  
                
                idDigestB64 = Base64.getEncoder().encodeToString(cc.getID().generateDigest());
                photoDigestB64 = Base64.getEncoder().encodeToString(cc.getPhotoData().generateDigest());
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
                LOGGER.log(Level.WARNING, "Exceção: Sod.save(): id, photo digest {0}", ex.getMessage());
            }
            
            try {                
                pubKeyDigestB64 = Base64.getEncoder().encodeToString(cc.getPublicKeyBytes());
            } catch (SmartCardFileException ex) {
                LOGGER.log(Level.WARNING, "Exceção: Sod.save(): chave pública {0}", ex.getMessage());
            }
            
            try {
                
                if (moreInfo){
                    morada = cc.getAddress().getRawData();
                }
                
                addressDigestB64 = Base64.getEncoder().encodeToString(cc.getAddress().generateDigest());                                                               
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException | POReIDException | PinTimeoutException | PinEntryCancelledException | PinBlockedException ex) {
                LOGGER.log(Level.WARNING, "Exceção: Sod.save(): morada {0}", ex.getMessage());
            }
            
            try {                
                certificadoAssinatura = cc.getQualifiedSignatureCertificate().getEncoded();
            } catch (CertificateNotFound | CertificateEncodingException ex) {
                LOGGER.log(Level.WARNING, "Exceção: Sod.save(): certificado Assinatura {0}", ex.getMessage());
            }
            
            try {
                certificadoAutenticacao = cc.getAuthenticationCertificate().getEncoded();                
            } catch (CertificateNotFound | CertificateEncodingException ex) {
                LOGGER.log(Level.WARNING, "Exceção: Sod.save(): certificado Autenticação {0}", ex.getMessage());
            }
            
        } catch (CardTerminalNotPresentException | UnknownCardException | CardNotPresentException | CanceledSelectionException | POReIDException | SmartCardFileException ex) {
            LOGGER.log(Level.WARNING, "Exceção: Sod.save(): leitura {0}", ex.getMessage());
        } finally {
            try {
                if (null != cc) {
                    cc.close();
                }
            } catch (POReIDException ex) {
            }
        }
                
        StringBuilder sb = new StringBuilder();
        sb.append("INFORMAÇÃO\r\n\r\n");
        sb.append("O ficheiro ").append(fileName).append(" contêm informação para diagnóstico que permite verificar a existência de problemas relacionados com a consistência dos dados no cartão de cidadão e validação de certificados.\r\n");
        sb.append("São incluídos:\r\n"); 
        sb.append("    Os resumos criptográficos dos elementos identificativos do cidadão.\r\n");
        sb.append("    Os certificados de autenticação e assinatura digital.\r\n");
        sb.append("    O ficheiro de log da aplicação com o nome \"plugin Autenticacao.Gov.log\".\r\n");  
        if (moreInfo){
            sb.append("    O ficheiro da morada tal como lido do cartão de cidadão. A recolha deste ficheiro serve o propósito de análise da sua estrutura.\r\n");
            sb.append("    O ficheiro de identificação do cidadão tal como lido do cartão de cidadão. A recolha deste ficheiro serve o propósito de análise da sua estrutura.\r\n");
        }
        sb.append("\r\n");                
        sb.append(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z").format(new Date()));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            ZipEntry info = new ZipEntry("info.txt");
            zos.putNextEntry(info);
            zos.write(sb.toString().getBytes());
            zos.closeEntry();
            
            if (moreInfo) {
                if (null != id) {
                    ZipEntry entry = new ZipEntry("id_raw");
                    zos.putNextEntry(entry);
                    zos.write(id);
                    zos.closeEntry();
                }

                if (null != morada) {
                    ZipEntry entry = new ZipEntry("morada_raw");
                    zos.putNextEntry(entry);
                    zos.write(morada);
                    zos.closeEntry();
                }
            }

            if (null != idDigestB64) {
                ZipEntry entry = new ZipEntry("id");
                zos.putNextEntry(entry);
                zos.write(idDigestB64.getBytes());
                zos.closeEntry();
            }

            if (null != photoDigestB64) {
                ZipEntry entry = new ZipEntry("foto");
                zos.putNextEntry(entry);
                zos.write(photoDigestB64.getBytes());
                zos.closeEntry();
            }

            if (null != pubKeyDigestB64) {
                ZipEntry entry = new ZipEntry("chave");
                zos.putNextEntry(entry);
                zos.write(pubKeyDigestB64.getBytes());
                zos.closeEntry();
            }

            if (null != addressDigestB64) {
                ZipEntry entry = new ZipEntry("morada");
                zos.putNextEntry(entry);
                zos.write(addressDigestB64.getBytes());
                zos.closeEntry();
            }

            if (null != sod) {
                ZipEntry entry = new ZipEntry("sod");
                zos.putNextEntry(entry);
                zos.write(sod);
                zos.closeEntry();
            }
            
            if (null != certificadoAutenticacao) {
                ZipEntry entry = new ZipEntry("certificado.autenticacao.cer");
                zos.putNextEntry(entry);
                zos.write(certificadoAutenticacao);
                zos.closeEntry();
            }
            
            if (null != certificadoAssinatura) {
                ZipEntry entry = new ZipEntry("certificado.assinatura.cer");
                zos.putNextEntry(entry);
                zos.write(certificadoAssinatura);
                zos.closeEntry();
            }
            
            
            try {
                log = Files.readAllBytes(logLocation);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Exceção: Sod.save(): recolha de log {0}", ex.getMessage());
            }            
            if (log != null) {
                ZipEntry logFile = new ZipEntry(logLocation.getFileName().toString());
                zos.putNextEntry(logFile);
                zos.write(log);
                zos.closeEntry();
            }
            
            
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Exceção: Sod.save(): construção {0}", ex.getMessage());
            return;
        }                

        try {            
            Files.write(ConfigurationBuilder.build().getDesktopLocation().resolve("plugin-analise-" + Service.VERSION + "-" + new SimpleDateFormat("dd-MM-yyyy").format(new Date()) + ".zip"), baos.toByteArray());
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Exceção: Sod.save(): escrita {0}", ex.getMessage());
        }
        LOGGER.log(Level.INFO, "fim - análise.");
    }

    private byte[] generateDigest(byte[] t) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(t);
        return md.digest();
    }
}
