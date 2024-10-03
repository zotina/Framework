package framework;

public class Mapping {
    private String className;
    private String methodeName;
    private String verbes;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodeName() {
        return methodeName;
    }

    public void setMethodeName(String methodeName) {
        this.methodeName = methodeName;
    }

    public Mapping() {
    }

    
    public Mapping(String className, String methodeName, String verbes) {
        this.className = className;
        this.methodeName = methodeName;
        this.verbes = verbes;
    }

    public String getVerbes() {
        return verbes;
    }

    public void setVerbes(String verbes) {
        this.verbes = verbes;
    }

}
