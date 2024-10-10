package framework;

import java.util.List;

public class Mapping {
    private String className;
    private List<VerbeAction> verbeActions;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    
    public Mapping(String className, List<VerbeAction> verbeActions) {
        this.className = className;
        this.verbeActions = verbeActions;
    }
    public Mapping() {
    }
    public List<VerbeAction> getVerbeActions(){
        return verbeActions;
    }

    public void setVerbeActions(List<VerbeAction> verbeActions) {
        this.verbeActions = verbeActions;
    }

    
}
