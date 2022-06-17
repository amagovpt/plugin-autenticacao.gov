/*
 * The MIT License
 *
 * Copyright 2014, 2015, 2016 Rui Martinho (rmartinho@gmail.com), António Braz (antoniocbraz@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.poreid.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.poreid.CacheStatus;
import org.poreid.json.JSONArray;
import org.poreid.json.JSONObject;
import org.poreid.json.JSONTokener;


/**
 *
 * @author POReID
 */
public class POReIDConfig {
    public static final String LAF = "javax.swing.plaf.nimbus.NimbusLookAndFeel";
    public static final String LAF_SHORT_NAME = "Nimbus";
    public static final String GENERIC_READER = "generic reader";
    public static final String POREID = "POReID";
    public static final String RSA = "RSA";
    public static final String DIGITAL_SIGNATURE = "Signature";
    public static final String NONE = "NONE";
    public static final String SUPPORTED_KEY_CLASSES = "SupportedKeyClasses";
    public static final String KEYSTORE = "KeyStore";
    public static final String KEY_MANAGER_FACTORY = "KeyManagerFactory";
    public static final String AUTENTICACAO = "Autenticacao";
    public static final String ASSINATURA = "Assinatura";
    public static final String cacheDirectory = ".poreidcache";
    public static final String cacheLocation = System.getProperty("user.home") + System.getProperty("file.separator") + cacheDirectory + System.getProperty("file.separator");    
    public static final String IMAGE_ERROR_LOCATION = "/org/poreid/images/erro.png";
    public static final String IMAGE_WARNING_LOCATION = "/org/poreid/images/aviso.png";
    public static final String IMAGE_SIGNATURE_LOCATION = "/org/poreid/images/assinatura.png";
    public static final String BACKGROUND_SIGNATURE_LOCATION = "/org/poreid/images/fundo-assinatura.png";
    public static final String BACKGROUND_SMALL_SIGNATURE_LOCATION = "/org/poreid/images/fundo-assinatura-min.png";
    public static final String IMAGE_AUTHENTICATION_LOCATION = "/org/poreid/images/autenticacao.png";
    public static final String BACKGROUND_AUTHENTICATION_LOCATION = "/org/poreid/images/fundo-autenticacao.png";
    public static final String BACKGROUND_SMALL_AUTHENTICATION_LOCATION = "/org/poreid/images/fundo-autenticacao-min.png";
    public static final int NO_CACHE_THRESHOLD = 0;
    private static final String I18N_BUNDLE_LOCATION = "org.poreid.i18n.";
    private static final String CONFIGURACAO = "/org/poreid/config/poreid.config.json";
    private static final POReIDSupportedSmartCards poreidSupportedCards = new POReIDSupportedSmartCards();
    private static final SupportedSmartcardReaders smartcardSupportedReaders = new SupportedSmartcardReaders();
    private static final String WINDOWS = "windows";
    private static final String LINUX = "linux";
    private static final String MACOS = "mac";
    private static int cacheThreshold;
    private static boolean allowExternalPinCaching;
    private static Locale locale;
    private static boolean timedInteractionStatus;
    private static int timedInteractionValue;
    
    private static final String POREID_SUPPORTED_SMARTCARDS = "poreid-supported-smartcards";
    private static final String ATR = "atr";
    private static final String PROPERTIES = "properties";
    private static final String SUPPORTED_SMARTCARD_READERS = "supported-smartcard-readers";
    private static final String CACHE_THRESHOLD = "cache-threshold";
    private static final String ALLOW_EXTERNAL_PIN_CACHING = "allow-external-pin-caching";
    private static final String TIMED_INTERACTION = "timed-interaction";
    private static final String PERIOD = "period";
    private static final String ENABLED = "enabled";
    private static final String DEFAULT_LOCALE = "default-locale";
    private static final String LANGUAGE = "language";
    private static final String COUNTRY = "country";
    
    
    private static final int version = 0x02;
    
    static {
        String json;
        try (Stream<String> stream = new BufferedReader(new InputStreamReader(POReIDConfig.class.getResourceAsStream(CONFIGURACAO))).lines()){        
            json = stream.collect(Collectors.joining(System.lineSeparator()));

            JSONObject object = new JSONObject(new JSONTokener(json));
            JSONArray supportedCards = object.getJSONArray(POREID_SUPPORTED_SMARTCARDS);
            supportedCards.toList().forEach((t) -> {
                HashMap<String, Object> slide = (HashMap) t;
                poreidSupportedCards.addSmartCard((String) slide.get(ATR), (HashMap) slide.get(PROPERTIES));
            });

            JSONArray supportedReaders = object.getJSONArray(SUPPORTED_SMARTCARD_READERS);
            supportedReaders.toList().forEach((t) -> {
                HashMap<String, Object> slide = (HashMap) t;
                smartcardSupportedReaders.addSmartcardReaders(slide);
            });

            cacheThreshold = object.getInt(CACHE_THRESHOLD);
            allowExternalPinCaching = object.getBoolean(ALLOW_EXTERNAL_PIN_CACHING);
            timedInteractionValue = object.getJSONObject(TIMED_INTERACTION).getInt(PERIOD);
            timedInteractionStatus = object.getJSONObject(TIMED_INTERACTION).getBoolean(ENABLED);
            locale = new Locale(object.getJSONObject(DEFAULT_LOCALE).getString(LANGUAGE), object.getJSONObject(DEFAULT_LOCALE).getString(COUNTRY));   
        }
    }
    

    public static int getPOReIDVersion(){
        return version;
    }
    
    
    public static ResourceBundle getBundle(String simpleName, Locale locale){
        return ResourceBundle.getBundle(I18N_BUNDLE_LOCATION + simpleName, locale);
    }
    
    
    public static String getSmartCardImplementingClassName(String atr) {
        String implementingClass = null;

        if (poreidSupportedCards.isSupportedSmartCard(atr)){
            implementingClass = poreidSupportedCards.getSupportedSmartCardData(atr).getImplementingClass();
        }
        
        return implementingClass;
    }
    
    
    public static CacheStatus getSmartCardCacheStatus(String atr){
        CacheStatus cacheStatus = null;
        
        
        if (poreidSupportedCards.isSupportedSmartCard(atr)){
            cacheStatus = new CacheStatus(poreidSupportedCards.getSupportedSmartCardData(atr).isCacheEnabled(), poreidSupportedCards.getSupportedSmartCardData(atr).getValidity());                        
        }
        
        return cacheStatus;
    }
    
    
    public static Locale getDefaultLocale(){
        return locale;
    }
    
    
    public static String getSmartCardReaderImplementingClassName(String readerName) {
        return smartcardSupportedReaders.getImplementingClass(readerName);
    }
    
    
    public static boolean getVerifyPinSupport(String readerName, String smartCardImplementingClass) {
        SmartCardReaderProperties properties = smartcardSupportedReaders.getSmartCardReaderProperties(readerName);
        boolean pinPadVerifyPinSupported = true;
        
        if (properties.isSmartCardSupported(smartCardImplementingClass)){
            pinPadVerifyPinSupported = properties.isCardVerifyCapable(smartCardImplementingClass);
        }
        
        return (properties.isReaderVerifySupported(detectOS()) && pinPadVerifyPinSupported);
    }
    
    
    public static boolean getModifyPinSupport(String readerName, String smartCardImplementingClass) {
        SmartCardReaderProperties properties = smartcardSupportedReaders.getSmartCardReaderProperties(readerName);
        boolean pinPadModifyPinSupported = true;
        
        if (properties.isSmartCardSupported(smartCardImplementingClass)){
            pinPadModifyPinSupported = properties.isCardModifyCapable(smartCardImplementingClass);
        }
        
        return (properties.isReaderModifySupported(detectOS()) && pinPadModifyPinSupported);
    }
    
    
    public static boolean getOSInjectPinSupport(String readerName) {
        return smartcardSupportedReaders.getSmartCardReaderProperties(readerName).isReaderInjectSupported(detectOS());
    }
    
    
    public static boolean isExternalPinCachePermitted(){
        return allowExternalPinCaching;
    }
    
    
    private static String detectOS() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return WINDOWS;
        }
        if (os.contains("mac")) {
            return MACOS;
        }
        if (os.contains("nux")) {
            return LINUX;
        }
        
        throw new RuntimeException("Não foi possivel identificar o sistema operativo.");
    }
    
    
    public static boolean isTimedInteractionEnabled(){
        return timedInteractionStatus;
    }
    
    
    public static int timedInteractionPeriod() {
        return timedInteractionStatus ? timedInteractionValue : 0;
    }
    
    
    public static int getCacheThreshold(){
        return cacheThreshold;
    }
}
