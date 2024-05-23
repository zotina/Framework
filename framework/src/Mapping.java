package framework.sprint0;

public class Mapping {
    private String className;
    private String methodeName;

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

    public Mapping(String className, String methodeName) {
        this.className = className;
        this.methodeName = methodeName;
    }
}
