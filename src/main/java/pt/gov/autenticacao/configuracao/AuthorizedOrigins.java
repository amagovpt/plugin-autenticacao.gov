package pt.gov.autenticacao.configuracao;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.poreid.json.JSONObject;
import org.poreid.json.JSONTokener;
import java.util.Base64;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class AuthorizedOrigins {
    private int version;
    private List<String> origins;
    private boolean isInvalid = false;
    
    private AuthorizedOrigins(){
        
    }
    
    
    private AuthorizedOrigins(boolean isInvalid){
        this.isInvalid = isInvalid;
    }
    
    
    public static AuthorizedOrigins create(String b64String){
        boolean invalid = false;
        String[] parts = b64String.split("\\.");
        
        
        if (parts.length != 3){
            return new AuthorizedOrigins(true);
        }
        
        JSONObject header = new JSONObject(new JSONTokener(new String(Base64.getDecoder().decode(parts[0]),StandardCharsets.UTF_8)));
        JSONObject payload = new JSONObject(new JSONTokener(new String(Base64.getDecoder().decode(parts[1]),StandardCharsets.UTF_8)));
        JSONObject signature = new JSONObject(new JSONTokener(new String(Base64.getDecoder().decode(parts[2]),StandardCharsets.UTF_8)));
        
        if (!header.has("alg")){
            return new AuthorizedOrigins(true);
        }
        
        header.getString("alg");
        payload.getJSONArray("origins").toString();
        signature.getString("signature");
        
        AuthorizedOrigins origins = new AuthorizedOrigins();
        origins.origins = Arrays.asList(payload.getJSONArray("origins").toString().split(","));
                
        
        return new AuthorizedOrigins();
    }
    
    
    public AuthorizedOrigins tryUpdateOrigins(String b64String){
        // verificar se versão é superior, se inferior descarta.
        // se é superior, validar criptográficamente a lista
        // se válida, atualiza a lista        
        return this;
    }
    
    
    public int getVersion(){
        return version;
    }
    
    
    public boolean isOriginAuthorized(String origin){
        return origins.contains(origin);
    }
}
