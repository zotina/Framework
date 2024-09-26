package framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface Annotation {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Controller {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Get {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface RestApi {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Param {
        String value();
    }
}
