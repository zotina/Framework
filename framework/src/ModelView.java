package framework;

import java.util.HashMap;

public class ModelView {
    private String url;
    private HashMap<String, Object> data = new HashMap<>();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }

    public ModelView() {
    }

    public ModelView(String url) {
        this.url = url;
    }

    public void add(String VariableName, Object value) {
        this.data.put(VariableName, value);
    }

}