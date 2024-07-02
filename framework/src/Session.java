package framework;

import jakarta.servlet.http.HttpSession;

public class Session {
    private HttpSession requestSession;

    public Session(HttpSession requestSession) {
        this.requestSession = requestSession;
    }

    public Object get(String key) {
        return requestSession.getAttribute(key);
    }

    public void add(String key, Object object) {
        requestSession.setAttribute(key, object);
    }

    public void delete(String key) {
        requestSession.removeAttribute(key);
    }
}
