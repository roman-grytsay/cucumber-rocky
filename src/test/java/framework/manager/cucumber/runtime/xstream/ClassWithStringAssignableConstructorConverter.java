package test.java.framework.manager.cucumber.runtime.xstream;

import cucumber.deps.com.thoughtworks.xstream.converters.SingleValueConverter;
import test.java.framework.manager.cucumber.runtime.CucumberException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

class ClassWithStringAssignableConstructorConverter implements SingleValueConverter {
    private final Constructor ctor;

    ClassWithStringAssignableConstructorConverter(Constructor constructor) {
        this.ctor = constructor;
    }

    @Override
    public String toString(Object obj) {
        return obj.toString();
    }

    @Override
    public Object fromString(String str) {
        try {
            return ctor.newInstance(str);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CucumberException(e);
        } catch (InvocationTargetException e) {
            throw new CucumberException(e.getTargetException());
        }
    }

    @Override
    public boolean canConvert(Class type) {
        return ctor.getDeclaringClass().equals(type);
    }

}
