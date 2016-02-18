package test.java.framework.manager;

import org.testng.IAnnotationTransformer;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.Test;
import test.java.framework.ManagerPrototype;
import test.java.framework.SessionPrototype;
import test.java.framework.helpers.OptionalSteps;
import test.java.framework.manager.cucumber.api.CucumberOptions;
import test.java.framework.manager.cucumber.runtime.RuntimeOptions;
import test.java.framework.manager.cucumber.runtime.model.StepContainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

@CucumberOptions
public abstract class TestNGCucumberListenerPrototype implements IHookable, IAnnotationTransformer {

    private ManagerPrototype manager;

    /**
     * Options:
     * <p>
     * -g, --glue PATH                        Where glue code (step definitions and hooks) is loaded from.
     * -f, --format FORMAT[:PATH_OR_URL]      How to format results. Goes to STDOUT unless PATH_OR_URL is specified.
     * Built-in FORMAT types: junit, html, pretty, progress, json, usage,
     * rerun. FORMAT can also be a fully qualified class name.
     * -t, --tags TAG_EXPRESSION              Only run scenarios tagged with tags matching TAG_EXPRESSION.
     * -n, --name REGEXP                      Only run scenarios whose names match REGEXP.
     * -d, --[no-]-dry-run                    Skip execution of glue code.
     * -m, --[no-]-monochrome                 Don't colour terminal output.
     * -s, --[no-]-strict                     Treat undefined and pending steps as errors.
     * --snippets [underscore|camelcase]  Naming convention for generated snippets. Defaults to underscore.
     * --dotcucumber PATH_OR_URL          Where to write out runtime information. PATH_OR_URL can be a file system
     * path or a URL.
     * -v, --version                          Print version.
     * -h, --help                             You're looking at it.
     */
    @Test(priority = 5, groups = "cucumber", description = "Runs Cucumber Features")
    public void runCukes() {

        StepContainer.setManager(manager);
        StepContainer.setOptionalSteps(getCucumberOptionalSteps());

        String client = SessionPrototype.isMobile() ? "mobile" : "web";

        long id = Thread.currentThread().getId();

        RuntimeOptions options = new RuntimeOptions(Arrays.asList(
                "src/test/resources/features/" + client + "/",
                "--glue", "test.java.steps." + client,
                "--format", "html:target/cucumber/" + id,
                "--format", "json:target/cucumber/" + id + "/cucumber.json",
                "--format", "junit:target/cucumber/" + id + "/junit.xml"
        ));
        TestNGCucumberConcurrentRunner runner = new TestNGCucumberConcurrentRunner(getClass(), options);
        runner.runCukes();
    }

    @Override
    public void run(IHookCallBack iHookCallBack, ITestResult iTestResult) {
        manager = getManager();
        manager.setUpNoBrowser();
        iHookCallBack.runTestMethod(iTestResult);
    }

    /**
     * Specify maven goal, e.g.:
     * clean test "-Dcucumber.options=--tags @smoke --tags ~@debug" -DthreadCount=2
     */
    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        int threadCount = Integer.parseInt(System.getProperty("threadCount", "1"));
        annotation.setInvocationCount(threadCount);
        annotation.setThreadPoolSize(threadCount);
    }

    protected abstract OptionalSteps getCucumberOptionalSteps();

    protected abstract ManagerPrototype getManager();
}