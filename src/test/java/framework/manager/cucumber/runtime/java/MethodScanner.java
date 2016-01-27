package test.java.framework.manager.cucumber.runtime.java;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import test.java.framework.manager.cucumber.runtime.ClassFinder;
import test.java.framework.manager.cucumber.runtime.CucumberException;
import test.java.framework.manager.cucumber.runtime.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static test.java.framework.manager.cucumber.runtime.io.MultiLoader.packageName;

class MethodScanner {
    private final Collection<Class<? extends Annotation>> cucumberAnnotationClasses;

    private final ClassFinder classFinder;

    public MethodScanner(ClassFinder classFinder) {
        this.classFinder = classFinder;
        cucumberAnnotationClasses = findCucumberAnnotationClasses();
    }

    /**
     * Registers step definitions and hooks.
     *
     * @param javaBackend the backend where stepdefs and hooks will be registered
     * @param gluePaths   where to look
     */
    public void scan(JavaBackend javaBackend, List<String> gluePaths) {
        for (String gluePath : gluePaths) {
            for (Class<?> glueCodeClass : classFinder.getDescendants(Object.class, packageName(gluePath))) {
                while (glueCodeClass != null && glueCodeClass != Object.class && !Utils.isInstantiable(glueCodeClass)) {
                    // those can't be instantiated without container class present.
                    glueCodeClass = glueCodeClass.getSuperclass();
                }
                if (glueCodeClass != null) {
                    for (Method method : glueCodeClass.getMethods()) {
                        scan(javaBackend, method, glueCodeClass);
                    }
                }
            }
        }
    }

    /**
     * Registers step definitions and hooks.
     *
     * @param javaBackend   the backend where stepdefs and hooks will be registered.
     * @param method        a candidate for being a stepdef or hook.
     * @param glueCodeClass the class where the method is declared.
     */
    public void scan(JavaBackend javaBackend, Method method, Class<?> glueCodeClass) {
        for (Class<? extends Annotation> cucumberAnnotationClass : cucumberAnnotationClasses) {
            Annotation annotation = method.getAnnotation(cucumberAnnotationClass);
            if (annotation != null) {
                if (!method.getDeclaringClass().isAssignableFrom(glueCodeClass)) {
                    throw new CucumberException(String.format("%s isn't assignable from %s", method.getDeclaringClass(), glueCodeClass));
                }
                if (!glueCodeClass.equals(method.getDeclaringClass())) {
                    throw new CucumberException(String.format("You're not allowed to extend classes that define Step Definitions or hooks. %s extends %s", glueCodeClass, method.getDeclaringClass()));
                }
                if (isHookAnnotation(annotation)) {
                    javaBackend.addHook(annotation, method);
                } else if (isStepdefAnnotation(annotation)) {
                    javaBackend.addStepDefinition(annotation, method);
                }
            }
        }
    }

    private Collection<Class<? extends Annotation>> findCucumberAnnotationClasses() {
        //Annotations from the lib should be used for the proper recognition of defined steps by Intellij Idea
        //Only english is allowed
        Collection<Class<? extends Annotation>> annotations = classFinder.getDescendants(Annotation.class, "cucumber.api.java.en");
        annotations.add(Before.class);
        annotations.add(After.class);
        return annotations;
    }

    private boolean isHookAnnotation(Annotation annotation) {
        Class<? extends Annotation> annotationClass = annotation.annotationType();
        return annotationClass.equals(Before.class) || annotationClass.equals(After.class);
    }

    private boolean isStepdefAnnotation(Annotation annotation) {
        Class<? extends Annotation> annotationClass = annotation.annotationType();
        //Using StepDefAnnotation from the lib, where annotations are taken from
        return annotationClass.getAnnotation(cucumber.runtime.java.StepDefAnnotation.class) != null;
    }
}
