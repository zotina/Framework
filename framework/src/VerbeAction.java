package framework;
public class VerbeAction {
    private String methode;
    private String verbe;
    
    public String getMethode() {
        return methode;
    }
    public void setMethode(String methode) {
        this.methode = methode;
    }
    public String getVerbe() {
        return verbe;
    }
    public void setVerbe(String verbe) {
        this.verbe = verbe;
    }
    public VerbeAction(String methode, String verbe) {
        this.methode = methode;
        this.verbe = verbe;
    }
    public VerbeAction() {
    }
    
}