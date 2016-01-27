package test.java.framework.manager.cucumber.runtime;

import java.util.Collection;

public interface ClassFinder {
    <T> Collection<Class<? extends T>> getDescendants(Class<T> parentType, String packageName);
}
