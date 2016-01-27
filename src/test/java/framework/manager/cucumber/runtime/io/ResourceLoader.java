package test.java.framework.manager.cucumber.runtime.io;

public interface ResourceLoader {
    Iterable<Resource> resources(String path, String suffix);
}
