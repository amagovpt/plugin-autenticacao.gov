package pt.gov.autenticacao.common.diagnostic;

/**
 *
 * @author Rui Martinho (rui.martinho@ama.pt)
 */
public class Reader {

    private final String name;
    private final String id;
    private final long status;
    private final long problemId;

    public Reader(String name, String id, long status, long problemId) {
        this.name = name;
        this.id = id;
        this.status = status;
        this.problemId = problemId;
    }

    public String getName() {
        return name;
    }
    
    public String getID(){
        return id;
    }

    public long getStatus() {
        return status;
    }

    public long getProblemId() {
        return problemId;
    }
}
