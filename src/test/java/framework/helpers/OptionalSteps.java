package test.java.framework.helpers;

import java.util.List;

public interface OptionalSteps {

    /**
     * To return list of keywords, step needs to contain to be removed from scenario during runtime
     *
     * @return List of keywords
     */
    List<String> getExcludedAttributes();

}
