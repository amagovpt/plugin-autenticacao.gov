package pt.gov.autenticacao;

import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import pt.gov.autenticacao.util.Utilities;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Requests {
    private final int timeout;
    private final int period;
    private final ConcurrentHashMap<UUID, Date> cmap;

    
    protected Requests(int timeout) {
        this.timeout = timeout;
        this.period = timeout*2;
        cmap = new ConcurrentHashMap<>();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable periodicTask = new Runnable() {
            @Override
            public void run() {                
                purgeOldRequests();
            }
        };

        executor.scheduleAtFixedRate(periodicTask, 0, period, TimeUnit.SECONDS);        
    }

    
    protected void addRequest(UUID uuid, Date date){
        cmap.put(uuid, date);
    }
    
    
    public boolean isValid(UUID uuid) {
        Date date = cmap.get(uuid);

        if (null != date) {
            cmap.remove(uuid);
            return checkValidity(date);
        }

        return false;
    }

    
    public boolean isEmpty(){
        return cmap.isEmpty();
    }
    
    
    private void purgeOldRequests() {
        Set<ConcurrentHashMap.Entry<UUID, Date>> set = cmap.entrySet();
        for (ConcurrentHashMap.Entry<UUID, Date> entry : set) {
            if (!checkValidity(entry.getValue())){
                set.remove(entry);
            }            
        }
    }

                
    private boolean checkValidity(Date inicio) {
        return timeout > Utilities.getDateDiff(inicio, new Date(), TimeUnit.SECONDS);
    }
}
