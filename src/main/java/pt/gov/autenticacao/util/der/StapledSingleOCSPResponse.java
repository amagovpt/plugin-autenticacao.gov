package pt.gov.autenticacao.util.der;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CRLReason;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.x500.X500Principal;
import org.poreid.CertificateChainNotFound;
import org.poreid.common.Util;
import pt.gov.autenticacao.util.Utilities;

public final class StapledSingleOCSPResponse {

    public enum ResponseStatus {
        SUCCESSFUL,            // Response has valid confirmations
        MALFORMED_REQUEST,     // Illegal request
        INTERNAL_ERROR,        // Internal error in responder
        TRY_LATER,             // Try again later
        UNUSED,                // is not used
        SIG_REQUIRED,          // Must sign the request
        UNAUTHORIZED           // Request unauthorized
    };
    private static final ResponseStatus[] rsvalues = ResponseStatus.values();
    
    private static final ObjectIdentifier NONCE_EXTENSION_OID = ObjectIdentifier.newInternal(new int[]{ 1, 3, 6, 1, 5, 5, 7, 48, 1, 2});

    private static final Debug debug = Debug.getInstance("certpath");
    private static final boolean dump = debug != null && Debug.isOn("ocsp");
    private static final ObjectIdentifier OCSP_BASIC_RESPONSE_OID = ObjectIdentifier.newInternal(new int[] { 1, 3, 6, 1, 5, 5, 7, 48, 1, 1});
    private static final int CERT_STATUS_GOOD = 0;
    private static final int CERT_STATUS_REVOKED = 1;
    private static final int CERT_STATUS_UNKNOWN = 2;

    // ResponderID CHOICE tags
    private static final int NAME_TAG = 1;
    private static final int KEY_TAG = 2;

    // Object identifier for the OCSPSigning key purpose
    private static final String KP_OCSP_SIGNING_OID = "1.3.6.1.5.5.7.3.9";

    // Default maximum clock skew in milliseconds (15 minutes)
    // allowed when checking validity of OCSP responses
    private static final int DEFAULT_MAX_CLOCK_SKEW = 900000;

    /**
     * Integer value indicating the maximum allowable clock skew, in seconds,
     * to be used for the OCSP check.
     */
    private static final int MAX_CLOCK_SKEW = initializeClockSkew();

    /**
     * Initialize the maximum allowable clock skew by getting the OCSP
     * clock skew system property. If the property has not been set, or if its
     * value is negative, set the skew to the default.
     */
    private static int initializeClockSkew() {
        /*Integer tmp = java.security.AccessController.doPrivileged(   RUIM
                new GetIntegerAction("com.sun.security.ocsp.clockSkew"));
        if (tmp == null || tmp < 0) {
            return DEFAULT_MAX_CLOCK_SKEW;
        }
        // Convert to milliseconds, as the system property will be
        // specified in seconds
        return tmp * 1000;*/
        return DEFAULT_MAX_CLOCK_SKEW;
    }

    // an array of all of the CRLReasons (used in SingleResponse)
    private static final CRLReason[] values = CRLReason.values();

    private final ResponseStatus responseStatus;
    private final Map<CertId, SingleResponse> singleResponseMap;
    private final AlgorithmId sigAlgId;
    private final byte[] signature;
    private final byte[] tbsResponseData;
    private final byte[] responseNonce;
    private List<X509Certificate> certs;
    private X509Certificate signerCert = null;
    private X500Principal responderName = null;
    private KeyIdentifier responderKeyId = null;
    private KeyStore ks;

    /*
     * Create an OCSP response from its ASN.1 DER encoding.
     */
    public StapledSingleOCSPResponse(byte[] bytes) throws CertPathValidatorException {
        try {
            ks = KeyStore.getInstance("JKS");
            ks.load(Utilities.class.getResourceAsStream("/mordomo.ks"), null);

            DerValue der = new DerValue(bytes);
            if (der.tag != DerValue.tag_Sequence) {
                throw new CertPathValidatorException("Bad encoding in OCSP response: "
                        + "expected ASN.1 SEQUENCE tag.");
            }
            DerInputStream derIn = der.getData();

            // responseStatus
            int status = derIn.getEnumerated();
            if (status >= 0 && status < rsvalues.length) {
                responseStatus = rsvalues[status];
            } else {
                // unspecified responseStatus
                throw new CertPathValidatorException("Unknown OCSPResponse status: " + status);
            }
            if (debug != null) {
                debug.println("OCSP response status: " + responseStatus);
            }
            if (responseStatus != ResponseStatus.SUCCESSFUL) {
                // no need to continue, responseBytes are not set.
                singleResponseMap = Collections.emptyMap();
                certs = new ArrayList<>();
                sigAlgId = null;
                signature = null;
                tbsResponseData = null;
                responseNonce = null;
                return;
            }

            // responseBytes
            der = derIn.getDerValue();
            if (!der.isContextSpecific((byte) 0)) {
                throw new CertPathValidatorException("Bad encoding in responseBytes element "
                        + "of OCSP response: expected ASN.1 context specific tag 0.");
            }
            DerValue tmp = der.data.getDerValue();
            if (tmp.tag != DerValue.tag_Sequence) {
                throw new CertPathValidatorException("Bad encoding in responseBytes element "
                        + "of OCSP response: expected ASN.1 SEQUENCE tag.");
            }

            // responseType
            derIn = tmp.data;
            ObjectIdentifier responseType = derIn.getOID();
            if (responseType.equals((Object) OCSP_BASIC_RESPONSE_OID)) {
                if (debug != null) {
                    debug.println("OCSP response type: basic");
                }
            } else {
                if (debug != null) {
                    debug.println("OCSP response type: " + responseType);
                }
                throw new CertPathValidatorException("Unsupported OCSP response type: "
                        + responseType);
            }

            // BasicOCSPResponse
            DerInputStream basicOCSPResponse
                    = new DerInputStream(derIn.getOctetString());

            DerValue[] seqTmp = basicOCSPResponse.getSequence(2);
            if (seqTmp.length < 3) {
                throw new CertPathValidatorException("Unexpected BasicOCSPResponse value");
            }

            DerValue responseData = seqTmp[0];

            // Need the DER encoded ResponseData to verify the signature later
            tbsResponseData = seqTmp[0].toByteArray();

            // tbsResponseData
            if (responseData.tag != DerValue.tag_Sequence) {
                throw new CertPathValidatorException("Bad encoding in tbsResponseData "
                        + "element of OCSP response: expected ASN.1 SEQUENCE tag.");
            }
            DerInputStream seqDerIn = responseData.data;
            DerValue seq = seqDerIn.getDerValue();

            // version
            if (seq.isContextSpecific((byte) 0)) {
                // seq[0] is version
                if (seq.isConstructed() && seq.isContextSpecific()) {
                    //System.out.println ("version is available");
                    seq = seq.data.getDerValue();
                    int version = seq.getInteger();
                    if (seq.data.available() != 0) {
                        throw new CertPathValidatorException("Bad encoding in version "
                                + " element of OCSP response: bad format");
                    }
                    seq = seqDerIn.getDerValue();
                }
            }

            // responderID
            short tag = (byte) (seq.tag & 0x1f);
            if (tag == NAME_TAG) {
                responderName = new X500Principal(seq.getData().toByteArray());
                if (debug != null) {
                    debug.println("Responder's name: " + responderName);
                }
            } else if (tag == KEY_TAG) {
                responderKeyId = new KeyIdentifier(seq.getData().getOctetString());
                if (debug != null) {
                    debug.println("Responder's key ID: "
                            + Debug.toString(responderKeyId.getIdentifier()));
                }
            } else {
                throw new IOException("Bad encoding in responderID element of "
                        + "OCSP response: expected ASN.1 context specific tag 0 or 1");
            }

            // producedAt
            seq = seqDerIn.getDerValue();
            if (debug != null) {
                Date producedAtDate = seq.getGeneralizedTime();
                debug.println("OCSP response produced at: " + producedAtDate);
            }

            // responses
            DerValue[] singleResponseDer = seqDerIn.getSequence(1);
            singleResponseMap = new HashMap<>(singleResponseDer.length);
            if (debug != null) {
                debug.println("OCSP number of SingleResponses: "
                        + singleResponseDer.length);
            }
            for (DerValue singleResponseDer1 : singleResponseDer) {
                SingleResponse singleResponse = new SingleResponse(singleResponseDer1);
                singleResponseMap.put(singleResponse.getCertId(), singleResponse);
            }

            // responseExtensions
            byte[] nonce = null;
            if (seqDerIn.available() > 0) {
                seq = seqDerIn.getDerValue();
                if (seq.isContextSpecific((byte) 1)) {
                    DerValue[] responseExtDer = seq.data.getSequence(3);
                    for (DerValue responseExtDer1 : responseExtDer) {
                        Extension ext = new Extension(responseExtDer1);
                        if (debug != null) {
                            debug.println("OCSP extension: " + ext);
                        }
                        // Only the NONCE extension is recognized
                        if (ext.getExtensionId().equals((Object) NONCE_EXTENSION_OID)) {
                            nonce = ext.getExtensionValue();
                        } else if (ext.isCritical()) {
                            throw new CertPathValidatorException(
                                    "Unsupported OCSP critical extension: "
                                    + ext.getExtensionId());
                        }
                    }
                }
            }
            responseNonce = nonce;

            // signatureAlgorithmId
            sigAlgId = AlgorithmId.parse(seqTmp[1]);

            // signature
            signature = seqTmp[2].getBitString();

            // if seq[3] is available , then it is a sequence of certificates
            if (seqTmp.length > 3) {
                // certs are available
                DerValue seqCert = seqTmp[3];
                if (!seqCert.isContextSpecific((byte) 0)) {
                    throw new CertPathValidatorException("Bad encoding in certs element of "
                            + "OCSP response: expected ASN.1 context specific tag 0.");
                }
                DerValue[] derCerts = seqCert.getData().getSequence(3);
                certs = new ArrayList<>(derCerts.length);
                try {
                    for (int i = 0; i < derCerts.length; i++) {
                        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(derCerts[i].toByteArray()));
                        certs.add(cert);

                        if (debug != null) {
                            debug.println("OCSP response cert #" + (i + 1) + ": "
                                    + cert.getSubjectX500Principal());
                        }
                    }
                } catch (CertificateException ce) {
                    throw new CertPathValidatorException("Bad encoding in X509 Certificate", ce);
                }
            } else {
                certs = new ArrayList<>();
            }
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            throw new CertPathValidatorException(ex);
        }
    }
    
    private List<CertId> buildCertIds(X509Certificate certToVerify) throws CertPathValidatorException {        
        List<CertId> certIds = new ArrayList<>();        

        try {           
            List<X509Certificate> list = (List<X509Certificate>) Util.getCertificateChain(certToVerify, ks);
            certIds.add(new CertId(list.get(1), certToVerify));
        } catch (CertificateChainNotFound | IOException ex) {
            throw new CertPathValidatorException(ex);
        }
        if (!certIds.isEmpty()) {
            return certIds;
        }

        throw new CertPathValidatorException("SOCSP response does not include a response for a certificate supplied in the SOCSP request");
    }
    
    public void verify(X509Certificate certToVerify) throws CertPathValidatorException {        
        try {
            List<X509Certificate> list = (List<X509Certificate>) Util.getCertificateChain(certToVerify, ks);
            this.verify(certToVerify, list.get(1), null, new Date());
        } catch (CertificateChainNotFound ex) {
            Logger.getLogger(StapledSingleOCSPResponse.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
    
    public void verify(X509Certificate certToVerify, X509Certificate responderCert) throws CertPathValidatorException {
        this.verify(certToVerify, null, responderCert, new Date());
    }

    public void verify(X509Certificate certToVerify, X509Certificate issuerCert, X509Certificate responderCert, Date date) throws CertPathValidatorException {
        List<CertId> certIds;
        
        switch (responseStatus) {
            case SUCCESSFUL:
                break;
            case TRY_LATER:
            case INTERNAL_ERROR:
                throw new CertPathValidatorException(
                    "OCSP response error: " + responseStatus, null, null, -1,
                    BasicReason.UNDETERMINED_REVOCATION_STATUS);
            case UNAUTHORIZED:
            default:
                throw new CertPathValidatorException("OCSP response error: " +
                                                     responseStatus);
        }

        certIds = buildCertIds(certToVerify);
        
        // Check that the response includes a response for all of the
        // certs that were supplied in the request
        for (CertId certId : certIds) {
            SingleResponse sr = getSingleResponse(certId);
            if (sr == null) {
                if (debug != null) {
                    debug.println("No response found for CertId: " + certId);
                }
                throw new CertPathValidatorException(
                    "OCSP response does not include a response for a " +
                    "certificate supplied in the OCSP request");
            }
            if (debug != null) {
                debug.println("Status of certificate (with serial number " +
                    certId.getSerialNumber() + ") is: " + sr.getCertStatus());
            }
        }
      
        if (signerCert == null) {
            if (null != issuerCert){
                certs.add(issuerCert);
            }
            if (responderCert != null) {
                certs.add(responderCert);
            }            

            if (responderName != null) {
                for (X509Certificate cert : certs) {
                    if (cert.getSubjectX500Principal().equals(responderName)) {
                        signerCert = cert;
                        break;
                    }
                }
            } else if (responderKeyId != null) {
                for (X509Certificate cert : certs) {
                    // Match responder's key identifier against the cert's SKID
                    // This will match if the SKID is encoded using the 160-bit
                    // SHA-1 hash method as defined in RFC 5280.                    
                    KeyIdentifier certKeyId = new KeyIdentifier(cert.getExtensionValue(PKIXExtensions.SubjectKey_Id.toString()));
                    if (responderKeyId.equals(certKeyId)) {
                        signerCert = cert;
                        break;
                    } else {
                        // The certificate does not have a SKID or may have
                        // been using a different algorithm (ex: see RFC 7093).
                        // Check if the responder's key identifier matches
                        // against a newly generated key identifier of the
                        // cert's public key using the 160-bit SHA-1 method.
                        try {
                            certKeyId = new KeyIdentifier(cert.getPublicKey());
                        } catch (IOException e) {
                            // ignore
                        }
                        if (responderKeyId.equals(certKeyId)) {
                            signerCert = cert;
                            break;
                        }
                    }
                }
            }
        }

        /*try {
            // Locate the signer cert
            Files.copy(new ByteArrayInputStream(signerCert.getEncoded()),Paths.get("/home/ruim/ocsp.cert.pem"));
        } catch (CertificateEncodingException | IOException ex) {
            Logger.getLogger(SimpleOCSPResponse.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        
        // Check whether the signer cert returned by the responder is trusted
        if (signerCert != null) {
            // Check if the response is signed by the issuing CA
            if (signerCert.equals(issuerCert)) {
                if (debug != null) {
                    debug.println("OCSP response is signed by the target's " +
                        "Issuing CA");
                }
                // cert is trusted, now verify the signed response

            // Check if the response is signed by a trusted responder
            } else if (signerCert.equals(responderCert)) {
                if (debug != null) {
                    debug.println("OCSP response is signed by a Trusted " +
                        "Responder");
                }
                // cert is trusted, now verify the signed response

            // Check if the response is signed by an authorized responder
            } else if (null != issuerCert && signerCert.getIssuerX500Principal().equals(
                       issuerCert.getSubjectX500Principal())) {

                // Check for the OCSPSigning key purpose
                try {
                    List<String> keyPurposes = signerCert.getExtendedKeyUsage();
                    if (keyPurposes == null ||
                        !keyPurposes.contains(KP_OCSP_SIGNING_OID)) {
                        throw new CertPathValidatorException(
                            "Responder's certificate not valid for signing " +
                            "OCSP responses");
                    }
                } catch (CertificateParsingException cpe) {
                    // assume cert is not valid for signing
                    throw new CertPathValidatorException(
                        "Responder's certificate not valid for signing " +
                        "OCSP responses", cpe);
                }

                // Check algorithm constraints specified in security property
                // "jdk.certpath.disabledAlgorithms".
                AlgorithmChecker algChecker = new AlgorithmChecker(
                                    new TrustAnchor(issuerCert, null));
                algChecker.init(false);
                algChecker.check(signerCert, Collections.<String>emptySet());

                // check the validity
                try {
                    if (date == null) {
                        signerCert.checkValidity();
                    } else {
                        signerCert.checkValidity(date);
                    }
                } catch (CertificateException e) {
                    throw new CertPathValidatorException(
                        "Responder's certificate not within the " +
                        "validity period", e);
                }

                // check for revocation
                //
                // A CA may specify that an OCSP client can trust a
                // responder for the lifetime of the responder's
                // certificate. The CA does so by including the
                // extension id-pkix-ocsp-nocheck.
                //              
                if (signerCert.getExtensionValue(PKIXExtensions.SimpleOCSPNoCheck_Id.toString()) == null) {
                    throw new CertPathValidatorException("Responder's certificate is not authorized to sign OCSP responses (SimpleOCSPNoCheck_Id)");
                }

                // verify the signature
                try {
                    signerCert.verify(issuerCert.getPublicKey());
                    if (debug != null) {
                        debug.println("OCSP response is signed by an " +
                            "Authorized Responder");
                    }
                    // cert is trusted, now verify the signed response

                } catch (GeneralSecurityException e) {
                    signerCert = null;
                }
            } else {
                throw new CertPathValidatorException(
                    "Responder's certificate is not authorized to sign " +
                    "OCSP responses");
            }
        }

        // Confirm that the signed response was generated using the public
        // key from the trusted responder cert
        if (signerCert != null) {
            // Check algorithm constraints specified in security property
            // "jdk.certpath.disabledAlgorithms".
            AlgorithmChecker.check(signerCert.getPublicKey(), sigAlgId);

            if (!verifySignature(signerCert)) {
                throw new CertPathValidatorException(
                    "Error verifying OCSP Response's signature");
            }
        } else {
            // Need responder's cert in order to verify the signature
            throw new CertPathValidatorException(
                "Unable to verify OCSP Response's signature");
        }
        
        long now = (date == null) ? System.currentTimeMillis() : date.getTime();
        Date nowPlusSkew = new Date(now + MAX_CLOCK_SKEW);
        //Date nowMinusSkew = new Date(now - MAX_CLOCK_SKEW);        
        for (SingleResponse sr : singleResponseMap.values()) {
            if (debug != null) {
                String until = "";
                if (sr.nextUpdate != null) {
                    until = " until " + sr.nextUpdate;
                }
                debug.println("Response's validity interval is from " +
                              sr.thisUpdate + until);
            }

            /*// Check that the test date is within the validity interval
            if ((sr.thisUpdate != null && nowPlusSkew.before(sr.thisUpdate)) ||
                (sr.nextUpdate != null && nowMinusSkew.after(sr.nextUpdate)))
            {
                throw new CertPathValidatorException(
                                      "Response is unreliable: its validity " +
                                      "interval is out-of-date");
            }*/
            
            Calendar cal = Calendar.getInstance();            
            cal.setTime(date);
            cal.add(Calendar.DATE, -1); // validade 24h             
            
            if (!cal.getTime().before(sr.thisUpdate) || !nowPlusSkew.after(sr.thisUpdate)){
                throw new CertPathValidatorException("Stapled response is unreliable: its validity interval is out-of-date");
            }
        }
    }

    /**
     * Returns the OCSP ResponseStatus.
     */
    ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    /*
     * Verify the signature of the OCSP response.
     */
    private boolean verifySignature(X509Certificate cert)
        throws CertPathValidatorException {

        try {
            Signature respSignature = Signature.getInstance(sigAlgId.getName());
            respSignature.initVerify(cert.getPublicKey());
            respSignature.update(tbsResponseData);

            if (respSignature.verify(signature)) {
                if (debug != null) {
                    debug.println("Verified signature of OCSP Response");
                }
                return true;

            } else {
                if (debug != null) {
                    debug.println(
                        "Error verifying signature of OCSP Response");
                }
                return false;
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException |
                 SignatureException e)
        {
            throw new CertPathValidatorException(e);
        }
    }

    /**
     * Returns the SingleResponse of the specified CertId, or null if
     * there is no response for that CertId.
     */
    SingleResponse getSingleResponse(CertId certId) {
        return singleResponseMap.get(certId);
    }

    /*
     * Returns the certificate for the authority that signed the OCSP response.
     */
    X509Certificate getSignerCertificate() {
        return signerCert; // set in verify()
    }

    /*
     * A class representing a single OCSP response.
     */
    final static class SingleResponse implements StapledSingleOCSPResponse.RevocationStatus {
        private final CertId certId;
        private final CertStatus certStatus;
        private final Date thisUpdate;
        private final Date nextUpdate;
        private final Date revocationTime;
        private final CRLReason revocationReason;
        private final Map<String, Extension> singleExtensions;

        private SingleResponse(DerValue der) throws IOException {
            if (der.tag != DerValue.tag_Sequence) {
                throw new IOException("Bad ASN.1 encoding in SingleResponse");
            }
            DerInputStream tmp = der.data;

            certId = new CertId(tmp.getDerValue().data);
            DerValue derVal = tmp.getDerValue();
            short tag = (byte)(derVal.tag & 0x1f);
            if (tag ==  CERT_STATUS_REVOKED) {
                certStatus = CertStatus.REVOKED;
                revocationTime = derVal.data.getGeneralizedTime();
                if (derVal.data.available() != 0) {
                    DerValue dv = derVal.data.getDerValue();
                    tag = (byte)(dv.tag & 0x1f);
                    if (tag == 0) {
                        int reason = dv.data.getEnumerated();
                        // if reason out-of-range just leave as UNSPECIFIED
                        if (reason >= 0 && reason < values.length) {
                            revocationReason = values[reason];
                        } else {
                            revocationReason = CRLReason.UNSPECIFIED;
                        }
                    } else {
                        revocationReason = CRLReason.UNSPECIFIED;
                    }
                } else {
                    revocationReason = CRLReason.UNSPECIFIED;
                }
                // RevokedInfo
                if (debug != null) {
                    debug.println("Revocation time: " + revocationTime);
                    debug.println("Revocation reason: " + revocationReason);
                }
            } else {
                revocationTime = null;
                revocationReason = CRLReason.UNSPECIFIED;
                if (tag == CERT_STATUS_GOOD) {
                    certStatus = CertStatus.GOOD;
                } else if (tag == CERT_STATUS_UNKNOWN) {
                    certStatus = CertStatus.UNKNOWN;
                } else {
                    throw new IOException("Invalid certificate status");
                }
            }

            thisUpdate = tmp.getGeneralizedTime();

            if (tmp.available() == 0)  {
                // we are done
                nextUpdate = null;
            } else {
                derVal = tmp.getDerValue();
                tag = (byte)(derVal.tag & 0x1f);
                if (tag == 0) {
                    // next update
                    nextUpdate = derVal.data.getGeneralizedTime();

                    if (tmp.available() == 0)  {
                        // we are done
                    } else {
                        derVal = tmp.getDerValue();
                        tag = (byte)(derVal.tag & 0x1f);
                    }
                } else {
                    nextUpdate = null;
                }
            }
            // singleExtensions
            if (tmp.available() > 0) {
                derVal = tmp.getDerValue();
                if (derVal.isContextSpecific((byte)1)) {
                    DerValue[] singleExtDer = derVal.data.getSequence(3);
                    singleExtensions =
                        new HashMap<>
                            (singleExtDer.length);
                    for (DerValue singleExtDer1 : singleExtDer) {
                        Extension ext = new Extension(singleExtDer1);
                        if (debug != null) {
                            debug.println("OCSP single extension: " + ext);
                        }
                        // We don't support any extensions yet. Therefore, if it
                        // is critical we must throw an exception because we
                        // don't know how to process it.
                        if (ext.isCritical()) {
                            throw new IOException(
                                    "Unsupported OCSP critical extension: " +
                                            ext.getExtensionId());
                        }
                        singleExtensions.put(ext.getId(), ext);
                    }
                } else {
                    singleExtensions = Collections.emptyMap();
                }
            } else {
                singleExtensions = Collections.emptyMap();
            }
        }

        /*
         * Return the certificate's revocation status code
         */
        @Override public CertStatus getCertStatus() {
            return certStatus;
        }

        private CertId getCertId() {
            return certId;
        }

        @Override public Date getRevocationTime() {
            return (Date) revocationTime.clone();
        }

        @Override public CRLReason getRevocationReason() {
            return revocationReason;
        }

        @Override
        public Map<String, Extension> getSingleExtensions() {
            return Collections.unmodifiableMap(singleExtensions);
        }

        /**
         * Construct a string representation of a single OCSP response.
         */
        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("SingleResponse:  \n");
            sb.append(certId);
            sb.append("\nCertStatus: ").append(certStatus).append("\n");
            if (certStatus == CertStatus.REVOKED) {
                sb.append("revocationTime is ").append(revocationTime).append("\n");
                sb.append("revocationReason is ").append(revocationReason).append("\n");
            }
            sb.append("thisUpdate is ").append(thisUpdate).append("\n");
            if (nextUpdate != null) {
                sb.append("nextUpdate is ").append(nextUpdate).append("\n");
            }
            return sb.toString();
        }
    }
    
    static interface RevocationStatus {
        public enum CertStatus { GOOD, REVOKED, UNKNOWN };

        /**
         * Returns the revocation status.
         * @return 
         */
        CertStatus getCertStatus();
        /**
         * Returns the time when the certificate was revoked, or null
         * if it has not been revoked.
         * @return 
         */
        Date getRevocationTime();
        /**
         * Returns the reason the certificate was revoked, or null if it
         * has not been revoked.
         * @return 
         */
        CRLReason getRevocationReason();

        /**
         * Returns a Map of additional extensions.
         * @return 
         */
        Map<String, Extension> getSingleExtensions();
    }
}