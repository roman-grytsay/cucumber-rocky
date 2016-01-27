package test.java.framework.manager;

import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import test.java.framework.manager.cucumber.runtime.*;
import test.java.framework.manager.cucumber.runtime.Runtime;
import test.java.framework.manager.cucumber.runtime.io.ResourceLoader;
import test.java.framework.manager.cucumber.runtime.model.CucumberFeature;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ConcurrentRuntime extends Runtime {

    private final ClassLoader classLoader;
    private final RuntimeOptions runtimeOptions;
    private final ResourceLoader resourceLoader;

    private static Set<String> featureNames = new HashSet<>();
    private static final Object runnerLock = new Object();

    public ConcurrentRuntime(ResourceLoader resourceLoader, ClassFinder classFinder, ClassLoader classLoader, RuntimeOptions runtimeOptions) {
        super(resourceLoader, classFinder, classLoader, runtimeOptions);
        this.classLoader = classLoader;
        this.runtimeOptions = runtimeOptions;
        this.resourceLoader = resourceLoader;
    }

    public ConcurrentRuntime(ResourceLoader resourceLoader, ClassLoader classLoader, Collection<? extends Backend> backends, RuntimeOptions runtimeOptions) {
        this(resourceLoader, classLoader, backends, runtimeOptions, StopWatch.SYSTEM, null);
    }

    public ConcurrentRuntime(ResourceLoader resourceLoader, ClassLoader classLoader, Collection<? extends Backend> backends, RuntimeOptions runtimeOptions, RuntimeGlue optionalGlue) {
        this(resourceLoader, classLoader, backends, runtimeOptions, StopWatch.SYSTEM, optionalGlue);
    }

    public ConcurrentRuntime(ResourceLoader resourceLoader, ClassLoader classLoader, Collection<? extends Backend> backends, RuntimeOptions runtimeOptions, StopWatch stopWatch, RuntimeGlue optionalGlue) {
        super(resourceLoader, classLoader, backends, runtimeOptions, stopWatch, optionalGlue);
        this.classLoader = classLoader;
        this.runtimeOptions = runtimeOptions;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run() throws IOException {
        runtimeOptions.cucumberFeatures(resourceLoader).forEach(this::run);
        Formatter formatter = runtimeOptions.formatter(classLoader);

        formatter.done();
        formatter.close();
        printSummary();
    }

    private void run(CucumberFeature cucumberFeature) {
        String feature = cucumberFeature.getPath();
        boolean firstRun;
        synchronized (runnerLock) {
            firstRun = !featureNames.contains(feature);
            featureNames.add(feature);
        }
        if (firstRun) {
            Formatter formatter = runtimeOptions.formatter(classLoader);
            Reporter reporter = runtimeOptions.reporter(classLoader);

            System.out.println("Starting to run scenarios for the feature: '" + feature + "'");
            cucumberFeature.run(formatter, reporter, this);
            System.out.println("Scenarios completed for the feature: '" + feature + "'");
        }
    }

}
