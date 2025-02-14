package framework;

import java.util.List;

public class Mapping {
    private String className;
    private List<VerbeAction> verbeActions;
    private boolean needAuth=false;
    private String profil;


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

    public boolean isNeedAuth() {
        return needAuth;
    }

    public void setNeedAuth(boolean needAuth) {
        this.needAuth = needAuth;
    }

    public String getProfil() {
        return profil;
    }

    public void setProfil(String profil) {
        this.profil = profil;
    }

    
    
}
