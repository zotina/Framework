package framework;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String url;
    private HashMap<String, Object> data = new HashMap<>();
    private Map<String, String> validationErrors = new HashMap<>();
    private Map<String, Object> validationValues = new HashMap<>();

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

    // New methods for validation errors and values
    public void addError(String key, String errorMessage) {
        this.validationErrors.put(key, errorMessage);
    }

    public Map<String, String> getValidationErrors() {
        return this.validationErrors;
    }

    public void addValidationValue(String key, Object value) {
        this.validationValues.put(key, value);
    }

    public Map<String, Object> getValidationValues() {
        return this.validationValues;
    }

    public void mergeValidationErrors(Map<String, String> errors) {
        if (errors != null) {
            this.validationErrors=errors;
        }
    }

    public void mergeValidationValues(Map<String, Object> values) {
        if (values != null) {
            this.validationValues=values;
        }
    }
}