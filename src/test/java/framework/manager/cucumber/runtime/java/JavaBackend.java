package test.java.framework.manager.cucumber.runtime.java;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import gherkin.formatter.model.Step;
import test.java.framework.manager.cucumber.runtime.*;
import test.java.framework.manager.cucumber.runtime.Runtime;
import test.java.framework.manager.cucumber.runtime.io.MultiLoader;
import test.java.framework.manager.cucumber.runtime.io.ResourceLoader;
import test.java.framework.manager.cucumber.runtime.io.ResourceLoaderClassFinder;
import test.java.framework.manager.cucumber.runtime.snippets.FunctionNameGenerator;
import test.java.framework.manager.cucumber.runtime.snippets.SnippetGenerator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

public class JavaBackend implements Backend {
    private SnippetGenerator snippetGenerator = new SnippetGenerator(new JavaSnippet());
    private final ObjectFactory objectFactory;
    private final ClassFinder classFinder;

    private final MethodScanner methodScanner;
    private RuntimeGlue glue;

    /**
     * The constructor called by reflection by default.
     */
    public JavaBackend(ResourceLoader resourceLoader) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        methodScanner = new MethodScanner(classFinder);
        objectFactory = loadObjectFactory(classFinder);
    }

    public JavaBackend(ObjectFactory objectFactory) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        methodScanner = new MethodScanner(classFinder);
        this.objectFactory = objectFactory;
    }

    public JavaBackend(ObjectFactory objectFactory, ClassFinder classFinder) {
        this.objectFactory = objectFactory;
        this.classFinder = classFinder;
        methodScanner = new MethodScanner(classFinder);
    }

    public static ObjectFactory loadObjectFactory(ClassFinder classFinder) {
        ObjectFactory objectFactory;
        try {
            Reflections reflections = new Reflections(classFinder);
            objectFactory = reflections.instantiateExactlyOneSubclass(
                    ObjectFactory.class, "test.java.framework.manager.cucumber.runtime", new Class[0], new Object[0]);
        } catch (TooManyInstancesException e) {
            System.out.println(getMultipleObjectFactoryLogMessage());
            objectFactory = new DefaultJavaObjectFactory();
        } catch (NoInstancesException e) {
            objectFactory = new DefaultJavaObjectFactory();
        }
        return objectFactory;
    }

    @Override
    public void loadGlue(RuntimeGlue glue, List<String> gluePaths) {
        this.glue = glue;
        methodScanner.scan(this, gluePaths);
    }

    /**
     * Convenience method for frameworks that wish to load glue from methods explicitly (possibly
     * found with a different mechanism than Cucumber's built-in classpath scanning).
     *
     * @param glue          where stepdefs and hooks will be added.
     * @param method        a candidate method.
     * @param glueCodeClass the class implementing the method. Must not be a subclass of the class implementing the method.
     */
    public void loadGlue(RuntimeGlue glue, Method method, Class<?> glueCodeClass) {
        this.glue = glue;
        methodScanner.scan(this, method, glueCodeClass);
    }

    @Override
    public void setUnreportedStepExecutor(Runtime executor) {
        //Not used here yet
    }

    @Override
    public void buildWorld() {
        objectFactory.start();
    }

    @Override
    public void disposeWorld() {
        objectFactory.stop();
    }

    @Override
    public String getSnippet(Step step, FunctionNameGenerator functionNameGenerator) {
        return snippetGenerator.getSnippet(step, functionNameGenerator);
    }

    void addStepDefinition(Annotation annotation, Method method) {
        try {
            objectFactory.addClass(method.getDeclaringClass());

            //If annotation timeout not specified, use stepTimeout config if specified, otherwise use default one
            long annotationTimeout = timeoutMillis(annotation);
            Long timeout = annotationTimeout == 0L ?
                    Long.parseLong(System.getProperty("stepTimeout", "200000")) :
                    annotationTimeout;

            glue.addStepDefinition(new JavaStepDefinition(method, pattern(annotation), timeout, objectFactory));
        } catch (DuplicateStepDefinitionException e) {
            throw e;
        } catch (Throwable e) {
            throw new CucumberException(e);
        }
    }

    private Pattern pattern(Annotation annotation) throws Throwable {
        Method regexpMethod = annotation.getClass().getMethod("value");
        String regexpString = (String) Utils.invoke(annotation, regexpMethod, 0);
        return Pattern.compile(regexpString);
    }

    private long timeoutMillis(Annotation annotation) throws Throwable {
        Method regexpMethod = annotation.getClass().getMethod("timeout");
        return (Long) Utils.invoke(annotation, regexpMethod, 0);
    }

    void addHook(Annotation annotation, Method method) {
        objectFactory.addClass(method.getDeclaringClass());

        if (annotation.annotationType().equals(Before.class)) {
            String[] tagExpressions = ((Before) annotation).value();

            //If annotation timeout not specified, use stepTimeout config if specified, otherwise use default one
            long annotationTimeout = ((Before) annotation).timeout();
            Long timeout = annotationTimeout == 0L ?
                    Long.parseLong(System.getProperty("stepTimeout", "200000")) :
                    annotationTimeout;

            glue.addBeforeHook(new JavaHookDefinition(
                    method, tagExpressions, ((Before) annotation).order(), timeout, objectFactory));
        } else {
            String[] tagExpressions = ((After) annotation).value();

            //If annotation timeout not specified, use stepTimeout config if specified, otherwise use default one
            long annotationTimeout = ((After) annotation).timeout();
            Long timeout = annotationTimeout == 0L ?
                    Long.parseLong(System.getProperty("stepTimeout", "200000")) :
                    annotationTimeout;

            glue.addAfterHook(new JavaHookDefinition(
                    method, tagExpressions, ((After) annotation).order(), timeout, objectFactory));
        }
    }

    private static String getMultipleObjectFactoryLogMessage() {
        return "More than one Cucumber ObjectFactory was found in the classpath\n\n" +
                "You probably may have included, for instance, cucumber-spring AND cucumber-guice as part of\n" +
                "your dependencies. When this happens, Cucumber falls back to instantiating the\n" +
                "DefaultJavaObjectFactory implementation which doesn't provide IoC.\n" +
                "In order to enjoy IoC features, please remove the unnecessary dependencies from your classpath.\n";
    }
}
