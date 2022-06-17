package pt.gov.autenticacao;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.poreid.common.Util;
import pt.gov.autenticacao.assinatura.cc.Signature;
import pt.gov.autenticacao.common.ErrorReportImpl;
import pt.gov.autenticacao.common.Parameter;
import pt.gov.autenticacao.autenticacao.cc.AuthCC;
import pt.gov.autenticacao.autenticacao.cc.pan.PanReader;
/*import pt.gov.autenticacao.autenticacao.softcert.os.SoftCertAuthenticationFactory;
import pt.gov.autenticacao.autenticacao.softcert.selectors.CSSoftCertificatesSelector;
import pt.gov.autenticacao.autenticacao.softcert.selectors.OASoftCertificatesSelector;*/
import pt.gov.autenticacao.read.cc.CCReader;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Service extends NanoHTTPD{    
    private final static Logger LOGGER = Logger.getLogger(Service.class.getName());
    private final String IS_ALIVE = "/isAlive";
    private final String VERIFY = "/network";
    private final String ORIGIN = "origin";    
    public static final String VERSION = "2.0.70";    
    private final int TIMEOUT = 70; //este é o período (entre o is_alive e qualquer operação com o cartão) durante o qual um UUID é valido, valor original 15.
    private final int usedPort;    
    private final String type;    
    private Requests requests;
    private volatile boolean processing = false;
    private String networkTest = null;
    private List<String> authorizedOrigins;
    private final boolean compatibility;
    
    public Service(int port, String type, boolean compatibility) {
        super("127.0.0.1",port);
        this.usedPort = port;
        this.type = type;
        this.requests = new Requests(TIMEOUT);
        this.compatibility = compatibility;
    }
    
    
    @Override
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
        Map<String, String> files = new HashMap<>();
        String responseData = null;
        
        LOGGER.log(Level.INFO, "Pedido -- uri = {0}; method = {1}; uptime = {2}", new Object[]{session.getUri(), session.getMethod(), ManagementFactory.getRuntimeMXBean().getUptime()});
        
        if (NanoHTTPD.Method.POST.equals(session.getMethod()) && session.getUri().equalsIgnoreCase(IS_ALIVE)) {            
            return replyOK(session.getHeaders().get(ORIGIN),replyIsAlive());            
        }
        
        if (NanoHTTPD.Method.OPTIONS.equals(session.getMethod())){
            Boolean acrpn = Boolean.valueOf(session.getHeaders().getOrDefault("access-control-request-private-network", "false"));
            NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse("");
            response.addHeader("access-control-allow-private-network", acrpn.toString());
            return response;              
        }
        
        if (NanoHTTPD.Method.POST.equals(session.getMethod())) {
            if (!requests.isEmpty()) { //TODO: verificar/melhorar estas condições
                if (!processing) { //TODO: verificar/melhorar estas condições
                    try {
                        processing = true;
                        switch (session.getUri()) {
                            case CCServiceProviders.CC_AUTHENTICATION:
                                session.parseBody(files);                                
                                responseData = new AuthCC(session.getParms(), requests).doOperation().toJSON();
                                LOGGER.log(Level.INFO, "Informação enviada ao AGov {0}",responseData);                                
                                //responseData = updateVerifyOrigin(session, new AuthCC(session.getParms(), requests));
                                break;
                            case CCServiceProviders.CC_SSO:
                                session.parseBody(files);
                                responseData = new PanReader(session.getParms(), requests).doOperation().toJSON();
                                break;
                            case CCServiceProviders.CC_READ:
                                session.parseBody(files);
                                responseData = new CCReader(session.getParms(), requests).doOperation().toJSON();
                                break;
                            case CCServiceProviders.CC_SIGN:
                                session.parseBody(files);
                                responseData = new Signature(session.getParms(), requests).doOperation().toJSON();
                                break;
                            /* atualmente não suportado    
                             case AuthProviders.OA_AUTHENTICATION:                        
                             session.parseBody(files);                       
                             responseData = SoftCertAuthenticationFactory.create(new OASoftCertificatesSelector(), session.getParms(), uuid).doAuthentication().toJSON();
                             break;
                             case AuthProviders.CS_AUTHENTICATION:
                             session.parseBody(files);                        
                             responseData = SoftCertAuthenticationFactory.create(new CSSoftCertificatesSelector(), session.getParms(), uuid).doAuthentication().toJSON();
                             break; */
                            default:
                                LOGGER.log(Level.INFO, "método POST não processado uri = {0}; uptime = {1}", new Object[]{session.getUri(), ManagementFactory.getRuntimeMXBean().getUptime()});
                        }
                        if (null != responseData) {
                            return replyOK(session.getHeaders().get(ORIGIN), responseData);
                        }
                    } catch (IOException | NanoHTTPD.ResponseException ex) {
                        LOGGER.log(Level.INFO, "Exceção: método POST não processado uri = {0}; uptime = {1}; exception = {2}", new Object[]{session.getUri(), ManagementFactory.getRuntimeMXBean().getUptime(), ex.getMessage()});
                    } finally {                        
                        processing = false;
                    }
                } else {                    
                    try {
                        session.parseBody(files);
                        String agentToken = session.getParms().get(Parameter.AGENT_TOKEN.getName());
                        if (null != agentToken /*&& UUID.fromString(agentToken).equals(uuid)*/) {
                            LOGGER.log(Level.INFO, "plugin ocupado com outra autenticação - uptime = {0}", ManagementFactory.getRuntimeMXBean().getUptime());
                            return replyOK(session.getHeaders().get(ORIGIN), new pt.gov.autenticacao.common.Response(new ErrorReportImpl(ErrorReportImpl.ErrorType.AGENTE_OCUPADO, "").getInputs()).toJSON());
                        }
                    } catch (Exception ex) { /* ignore */ }
                }

                return replyError();
            }
        }
              
        if (NanoHTTPD.Method.GET.equals(session.getMethod()) && session.getUri().equalsIgnoreCase(VERIFY) && session.getHeaders().get(ORIGIN) == null) {            
            return replyVerify(session.getHeaders().get(ORIGIN));            
        }
                                
        LOGGER.log(Level.INFO, "Pedido não processado uri = {0}; method = {1}; uptime = {2}", new Object[]{session.getUri(), session.getMethod(), ManagementFactory.getRuntimeMXBean().getUptime()});
        return replyError();
    }
    
                
    private String replyIsAlive(){        
        UUID uuid = UUID.randomUUID();
        Date inicio = new Date();
        LOGGER.log(Level.INFO, "Pedido replyIsAlive UUID: {0} {1}", new Object[]{uuid.toString(), inicio.toString()});       
        requests.addRequest(uuid, inicio);
        return MessageFormat.format("'{'\"isAlive\":{0}, \"port\":{1,number,#}, \"version\":\"{2}\", \"uuid\":\"{3}\", \"type\":\"{4}\",\"proto\":\"{5}\"'}'", true, usedPort, VERSION, uuid.toString(), type, (compatibility ? "https" : "http"));     
    } 
    
    
    private NanoHTTPD.Response replyError() {
        Agente.HELP.resetHelpPageLocation();
        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.FOUR_FOUR_FOUR, NanoHTTPD.MIME_PLAINTEXT, "");
        response.addHeader("X-Content-Type-Options", "nosniff");
        
        return response;
    }
    
    
    private NanoHTTPD.Response replyOK(String origin, String json) {
        Agente.HELP.resetHelpPageLocation();
        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_JSON, json);
        response.addHeader("X-Content-Type-Options", "nosniff");        
        
        if (null != origin) {
            response.addHeader("Access-Control-Allow-Origin", origin);
        }
        return response;
    }  
    
    
    private NanoHTTPD.Response replyVerify(String origin) {
        NanoHTTPD.Response response;

        if (null == networkTest) {
            try (InputStream input = Service.class.getResourceAsStream("/Portugal.html")) {                
                StringBuilder sb = new StringBuilder(new String(Util.toByteArray(input),StandardCharsets.UTF_8));
                networkTest = Pattern.compile("__VERSAO_APLICACAO__").matcher(sb).replaceFirst(VERSION);                
            } catch (IOException ex) {
            }
        }

        if (null != networkTest) {            
            response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML, networkTest);
        } else {
            response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
        }

        if (null != origin) {
            response.addHeader("Access-Control-Allow-Origin", origin);
        }
        
        return response;
    }
     
    /*private String updateVerifyOrigin(NanoHTTPD.IHTTPSession session, Operation operation){        
        ConfigurationBuilder.build(). // ver como carregar uma unica vez a lista.
        // Carrega lista
        
        if (authorizedOrigins.contains(session.getHeaders().get(ORIGIN))){
            // verifica se nova lista de origens é valida e grava-a na mesma localização do ficheiro de configuração.
            // atualiza a lista se a lista enviada for mais recente, a lista só é atualizada se for válida criptográficamente
            
            return operation.doOperation().toJSON();
        }
        
        return null;
    }*/
}
