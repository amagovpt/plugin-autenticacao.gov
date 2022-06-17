package pt.gov.autenticacao.util.der;

import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */

public class AuthGovCertificateExtensions{                   
    public static final int readData = 1 << 2;
    public static final int signature = 1 << 1;
    public static final int authentication = 1 << 0;
           
    /* definição ASN.1:
     * AuthorizedUsages ::= BIT STRING {
     *     authentication           (0),
     *     signature                (1),
     *     read                     (2) }
     */
    public static boolean isUsageAuthorized(X509Certificate cert, int usageBits) {
        try {
            byte[] value = cert.getExtensionValue("1.3.6.1.4.1.47668.1.1");
            
            if (null == value){
                return false;
            }
            int bits = new DerValue(new DerValue(value).getOctetString()).getUnalignedBitString().toByteArray()[0] & 0xff;
            return (usageBits == (bits & usageBits));
        } catch (IOException ex) {
            return false;
        }
    }
    
    public static boolean needsValidation(X509Certificate cert){
        return null != cert.getExtensionValue("1.3.6.1.4.1.47668.1.2");
    }
}
