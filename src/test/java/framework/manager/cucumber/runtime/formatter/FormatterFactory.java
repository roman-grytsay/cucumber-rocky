package test.java.framework.manager.cucumber.runtime.formatter;

import gherkin.formatter.Formatter;
import test.java.framework.manager.cucumber.runtime.CucumberException;
import test.java.framework.manager.cucumber.runtime.io.URLOutputStream;
import test.java.framework.manager.cucumber.runtime.io.UTF8OutputStreamWriter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static test.java.framework.manager.cucumber.runtime.Utils.toURL;

/**
 * This class creates {@link gherkin.formatter.Formatter} instances (that may also implement {@link gherkin.formatter.Reporter} from
 * a String.
 * <p>
 * The String is of the form name[:output] where name is either a fully qualified class name or one of the built-in short names.
 * output is optional for some formatters (and mandatory for some) and must refer to a path on the file system.
 * <p>
 * The formatter class must have a constructor that is either empty or takes a single argument of one of the following types:
 * <ul>
 * <li>{@link Appendable}</li>
 * <li>{@link java.io.File}</li>
 * <li>{@link java.net.URL}</li>
 * <li>{@link java.net.URI}</li>
 * </ul>
 */
public class FormatterFactory {
    private final Class[] CTOR_ARGS = new Class[]{null, Appendable.class, URI.class, URL.class, File.class};

    private static final Map<String, Class<? extends Formatter>> FORMATTER_CLASSES = new HashMap<String, Class<? extends Formatter>>() {{
        put("null", NullFormatter.class);
        put("junit", JUnitFormatter.class);
        put("html", HTMLFormatter.class);
        put("pretty", CucumberPrettyFormatter.class);
        put("progress", ProgressFormatter.class);
        put("json", CucumberJSONFormatter.class);
        put("usage", UsageFormatter.class);
        put("rerun", RerunFormatter.class);
    }};
    private static final Pattern FORMATTER_WITH_FILE_PATTERN = Pattern.compile("([^:]+):(.*)");
    private Appendable defaultOut = new PrintStream(System.out) {
        @Override
        public void close() {
            // We have no intention to close System.out
        }
    };

    public Formatter create(String formatterString) {
        Matcher formatterWithFile = FORMATTER_WITH_FILE_PATTERN.matcher(formatterString);
        String formatterName;
        String path = null;
        if (formatterWithFile.matches()) {
            formatterName = formatterWithFile.group(1);
            path = formatterWithFile.group(2);
        } else {
            formatterName = formatterString;
        }
        Class<? extends Formatter> formatterClass = formatterClass(formatterName);
        try {
            return instantiate(formatterString, formatterClass, path);
        } catch (IOException | URISyntaxException e) {
            throw new CucumberException(e);
        }
    }

    private Formatter instantiate(String formatterString, Class<? extends Formatter> formatterClass, String pathOrUrl) throws IOException, URISyntaxException {
        for (Class ctorArgClass : CTOR_ARGS) {
            Constructor<? extends Formatter> constructor = findConstructor(formatterClass, ctorArgClass);
            if (constructor != null) {
                Object ctorArg = convertOrNull(pathOrUrl, ctorArgClass);
                try {
                    if (ctorArgClass == null) {
                        return constructor.newInstance();
                    } else {
                        if (ctorArg == null) {
                            throw new CucumberException(String.format("You must supply an output argument to %s. Like so: %s:output", formatterString, formatterString));
                        }
                        return constructor.newInstance(ctorArg);
                    }
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new CucumberException(e);
                } catch (InvocationTargetException e) {
                    throw new CucumberException(e.getTargetException());
                }
            }
        }
        throw new CucumberException(String.format("%s must have a constructor that is either empty or a single arg of one of: %s", formatterClass, asList(CTOR_ARGS)));
    }

    private Object convertOrNull(String pathOrUrl, Class ctorArgClass) throws IOException, URISyntaxException {
        if (ctorArgClass == null) {
            return null;
        }
        if (ctorArgClass.equals(URI.class)) {
            if (pathOrUrl != null) {
                return new URI(pathOrUrl);
            }
        }
        if (ctorArgClass.equals(URL.class)) {
            if (pathOrUrl != null) {
                return toURL(pathOrUrl);
            }
        }
        if (ctorArgClass.equals(File.class)) {
            if (pathOrUrl != null) {
                return new File(pathOrUrl);
            }
        }
        if (ctorArgClass.equals(Appendable.class)) {
            if (pathOrUrl != null) {
                return new UTF8OutputStreamWriter(new URLOutputStream(toURL(pathOrUrl)));
            } else {
                return defaultOutOrFailIfAlreadyUsed();
            }
        }
        return null;
    }

    private Constructor<? extends Formatter> findConstructor(Class<? extends Formatter> formatterClass, Class<?> ctorArgClass) {
        try {
            if (ctorArgClass == null) {
                return formatterClass.getConstructor();
            } else {
                return formatterClass.getConstructor(ctorArgClass);
            }
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private Class<? extends Formatter> formatterClass(String formatterName) {
        Class<? extends Formatter> formatterClass = FORMATTER_CLASSES.get(formatterName);
        if (formatterClass == null) {
            formatterClass = loadClass(formatterName);
        }
        return formatterClass;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Formatter> loadClass(String className) {
        try {
            return (Class<? extends Formatter>) Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new CucumberException("Couldn't load formatter class: " + className, e);
        }
    }

    private Appendable defaultOutOrFailIfAlreadyUsed() {
        try {
            if (defaultOut != null) {
                return defaultOut;
            } else {
                throw new CucumberException("Only one formatter can use STDOUT. If you use more than one formatter you must specify output path with FORMAT:PATH_OR_URL");
            }
        } finally {
            defaultOut = null;
        }
    }
}
