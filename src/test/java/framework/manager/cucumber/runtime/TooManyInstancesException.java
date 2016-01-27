package test.java.framework.manager.cucumber.runtime;

import java.util.Collection;

public class TooManyInstancesException extends CucumberException {

    public TooManyInstancesException(Collection instances) {
        super(createMessage(instances));
    }

    private static String createMessage(Collection instances) {
        return "Expected only one instance, but found too many: " + instances;
    }
}
