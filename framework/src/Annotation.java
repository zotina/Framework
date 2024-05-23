package framework.sprint0;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface Annotation {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Controller {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Get {
        String value();
    }
}
