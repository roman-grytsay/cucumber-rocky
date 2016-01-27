package test.java.framework.manager;

import cucumber.api.testng.TestNgReporter;
import test.java.framework.manager.cucumber.api.CucumberOptions;
import test.java.framework.manager.cucumber.runtime.ClassFinder;
import test.java.framework.manager.cucumber.runtime.CucumberException;
import test.java.framework.manager.cucumber.runtime.RuntimeOptions;
import test.java.framework.manager.cucumber.runtime.RuntimeOptionsFactory;
import test.java.framework.manager.cucumber.runtime.io.MultiLoader;
import test.java.framework.manager.cucumber.runtime.io.ResourceLoader;
import test.java.framework.manager.cucumber.runtime.io.ResourceLoaderClassFinder;

import java.io.IOException;

public class TestNGCucumberConcurrentRunner {

    private final ConcurrentRuntime runtime;

    /**
     * Bootstrap the cucumber runtime using @CucumberOptions for options
     *
     * @param clazz Which has the cucumber.api.CucumberOptions and org.testng.annotations.Test annotations
     */
    @SuppressWarnings("unchecked")
    public TestNGCucumberConcurrentRunner(Class clazz) {
        ClassLoader classLoader = clazz.getClassLoader();
        ResourceLoader resourceLoader = new MultiLoader(classLoader);

        RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(clazz, new Class[]{CucumberOptions.class});
        RuntimeOptions runtimeOptions = runtimeOptionsFactory.create();

        TestNgReporter reporter = new TestNgReporter(System.out);
        runtimeOptions.addFormatter(reporter);
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        runtime = new ConcurrentRuntime(resourceLoader, classFinder, classLoader, runtimeOptions);
    }

    /**
     * Bootstrap the cucumber runtime providing @CucumberOptions as a parameter
     *
     * @param clazz          Which has the org.testng.annotations.Test annotations
     * @param runtimeOptions runtime cucumber options
     */
    public TestNGCucumberConcurrentRunner(Class clazz, RuntimeOptions runtimeOptions) {
        ClassLoader classLoader = clazz.getClassLoader();
        ResourceLoader resourceLoader = new MultiLoader(classLoader);

        TestNgReporter reporter = new TestNgReporter(System.out);
        runtimeOptions.addFormatter(reporter);
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        runtime = new ConcurrentRuntime(resourceLoader, classFinder, classLoader, runtimeOptions);
    }

    /**
     * Run the Cucumber features
     */
    public void runCukes() {
        try {
            runtime.run();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        if (!runtime.getErrors().isEmpty()) {
            throw new CucumberException(runtime.getErrors().get(0));
        } else if (runtime.exitStatus() != 0x00) {
            throw new CucumberException("There are pending or undefined steps.");
        }
    }
}
