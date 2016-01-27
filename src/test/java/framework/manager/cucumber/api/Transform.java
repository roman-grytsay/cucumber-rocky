package test.java.framework.manager.cucumber.api;

import java.lang.annotation.*;

/**
 * An annotation to specify how a Step Definition argument is transformed.
 *
 * @see cucumber.api.Transformer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Documented
public @interface Transform {
    Class<? extends Transformer<?>> value();
}
