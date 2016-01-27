package test.java.framework.manager.cucumber.runtime;

import gherkin.formatter.model.Tag;
import test.java.framework.manager.cucumber.api.Scenario;

import java.util.Collection;

public interface HookDefinition {
    /**
     * The source line where the step definition is defined.
     * Example: foo/bar/Zap.brainfuck:42
     *
     * @param detail true if extra detailed location information should be included.
     */
    String getLocation(boolean detail);

    void execute(Scenario scenario) throws Throwable;

    boolean matches(Collection<Tag> tags);

    int getOrder();
}
