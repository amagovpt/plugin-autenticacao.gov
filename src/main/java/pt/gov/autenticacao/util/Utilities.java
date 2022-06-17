package pt.gov.autenticacao.util;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.poreid.CertificateChainNotFound;
import org.poreid.common.Util;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Utilities {
    private final static Logger LOGGER = Logger.getLogger(Utilities.class.getName());
    
    public static void checkCertificateAccepted(X509Certificate cert) throws KeyStoreException, CertificateChainNotFound, IOException, NoSuchAlgorithmException, CertificateException {                 
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(Utilities.class.getResourceAsStream("/mordomo.ks"), null);
            Util.getCertificateChain(cert, ks);        
    }
     
     
    public static X509Certificate loadCertificate(String certB64) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(certB64)));
        
        return cert;
    }
    
    
    public static boolean checkSignature(X509Certificate cert, String sigB64 , String AgentToken, String token, final byte[]... parameters) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        PublicKey pubKey = cert.getPublicKey();
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initVerify(pubKey);       
        sig.update(AgentToken.getBytes(StandardCharsets.UTF_8));
        sig.update(token.getBytes(StandardCharsets.UTF_8));
        for (byte[] member : parameters) {
            sig.update(member);
        }
        return sig.verify(Base64.getDecoder().decode(sigB64));
    }
    
    
    /* http://stackoverflow.com/a/10650881/82609 */
    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }
    
    
    public static List<String> buildJVMCommand(String command) {
        List<String> java = new ArrayList<>();
        
        java.add(Paths.get(System.getProperty("java.home"), "bin", command).toString());                
        java.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments()); // obtem parametros da vm, se existirem
        
        return java;
    }
    
    
    public static String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder();

        if (null != e.getCause()) {
            sb.append(e.getCause().getMessage()).append(" - [").append(stackTraceToString(e.getCause())).append("]").append(String.format("%n"));
        }

        sb.append(e.getMessage()).append(" - ");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append(String.format("%n"));
        }

        return sb.toString();
    }
    
    
    public static String addLog(String method, Exception ex){
        return method + "() - [" + ex.getMessage() + "] [" + Utilities.stackTraceToString(ex) + "]";
    }
    
    
    public static void setLookAndFeel() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        LookAndFeel lnf = UIManager.getLookAndFeel();
        if (null == lnf || !"Nimbus".equalsIgnoreCase(lnf.getName())) {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        }
    }
    
    
    public static String idToString(){
        try {            
            InetAddress ip = InetAddress.getLocalHost();            
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            
            while (e.hasMoreElements()) {                
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress i = (InetAddress) ee.nextElement();
                    if (!i.isLoopbackAddress() && !i.isLinkLocalAddress() && i.isSiteLocalAddress()) {
                        ip = i;
                    }
                }
            }
            
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);            
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");            
            return Util.bytesToHex(md.digest(network.getHardwareAddress()));
                                                
        } catch (NoSuchAlgorithmException | NullPointerException | UnknownHostException | SocketException ex) {
            return "não foi possivel obter id";
        }                
    }
    
    /* possibilidade de o OSX não abrir o browser */
    /* http://stackoverflow.com/questions/18004150/desktop-api-is-not-supported-on-the-current-platform?answertab=active#tab-top */
    public static boolean browse(URI uri) {

        if (!browseDESKTOP(uri)){ 
            return openSystemSpecific(uri.toString());
        }

        return true;
    }

    private static boolean openSystemSpecific(String what) {

        EnumOS os = getOs();

        if (os.isLinux()) {
            if (runCommand("kde-open", "%s", what)) return true;
            if (runCommand("gnome-open", "%s", what)) return true;
            if (runCommand("xdg-open", "%s", what)) return true;
        }

        if (os.isMac()) {
            if (runCommand("open", "%s", what)) return true;
        }

        if (os.isWindows()) {
            if (runCommand("explorer", "%s", what)) return true;
        }

        return false;
    }

    private static boolean browseDESKTOP(URI uri) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {                                
                return false;
            }
            
            Desktop.getDesktop().browse(uri);            
            return true;
        } catch (Throwable t) {                        
            return false;            
        }
    }
 
    private static boolean runCommand(String command, String args, String file) {        
        String[] parts = prepareCommand(command, args, file);

        try {
            Process p = Runtime.getRuntime().exec(parts);
            if (p == null) return false;

            try {
                int retval = p.exitValue();
                if (retval == 0) {                    
                    return false;
                } else {                    
                    return false;
                }
            } catch (IllegalThreadStateException itse) {                
                return true;
            }
        } catch (IOException e) {            
            return false;
        }
    }

    private static String[] prepareCommand(String command, String args, String file) {
        List<String> parts = new ArrayList<>();
        parts.add(command);

        if (args != null) {
            for (String s : args.split(" ")) {
                s = String.format(s, file);
                parts.add(s.trim());
            }
        }

        return parts.toArray(new String[parts.size()]);
    }

    public static enum EnumOS {

        linux, macos, solaris, unknown, windows;

        public boolean isLinux() {

            return this == linux || this == solaris;
        }

        public boolean isMac() {

            return this == macos;
        }

        public boolean isWindows() {

            return this == windows;
        }
    }

    public static EnumOS getOs() {

        String s = System.getProperty("os.name").toLowerCase();

        if (s.contains("win")) {
            return EnumOS.windows;
        }

        if (s.contains("mac")) {
            return EnumOS.macos;
        }

        if (s.contains("solaris")) {
            return EnumOS.solaris;
        }

        if (s.contains("sunos")) {
            return EnumOS.solaris;
        }

        if (s.contains("linux")) {
            return EnumOS.linux;
        }

        if (s.contains("unix")) {
            return EnumOS.linux;
        } else {
            return EnumOS.unknown;
        }
    }
}
