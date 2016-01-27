package test.java.framework.manager.cucumber.api;

import test.java.framework.manager.cucumber.runtime.CucumberException;
import test.java.framework.manager.cucumber.runtime.snippets.CamelCaseConcatenator;
import test.java.framework.manager.cucumber.runtime.snippets.Concatenator;
import test.java.framework.manager.cucumber.runtime.snippets.FunctionNameGenerator;
import test.java.framework.manager.cucumber.runtime.snippets.UnderscoreConcatenator;

public enum SnippetType {
    UNDERSCORE("underscore", new UnderscoreConcatenator()),
    CAMELCASE("camelcase", new CamelCaseConcatenator());

    private final String name;
    private final Concatenator concatenator;

    SnippetType(String name, Concatenator concatenator) {
        this.name = name;
        this.concatenator = concatenator;
    }

    public static SnippetType fromString(String name) {
        for (SnippetType snippetType : SnippetType.values()) {
            if (name.equalsIgnoreCase(snippetType.name)) {
                return snippetType;
            }
        }
        throw new CucumberException(String.format("Unrecognized SnippetType %s", name));
    }

    public FunctionNameGenerator getFunctionNameGenerator() {
        return new FunctionNameGenerator(concatenator);
    }
}
