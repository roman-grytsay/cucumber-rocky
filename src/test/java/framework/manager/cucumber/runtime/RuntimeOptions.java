package test.java.framework.manager.cucumber.runtime;

import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.util.FixJava;
import test.java.framework.manager.cucumber.api.SnippetType;
import test.java.framework.manager.cucumber.runtime.formatter.ColorAware;
import test.java.framework.manager.cucumber.runtime.formatter.FormatterFactory;
import test.java.framework.manager.cucumber.runtime.formatter.StrictAware;
import test.java.framework.manager.cucumber.runtime.io.ResourceLoader;
import test.java.framework.manager.cucumber.runtime.model.CucumberFeature;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import static test.java.framework.manager.cucumber.runtime.model.CucumberFeature.load;

// IMPORTANT! Make sure USAGE.txt is always uptodate if this class changes.
public class RuntimeOptions {
    public static final String VERSION = ResourceBundle.getBundle("cucumber.version").getString("cucumber-jvm.version");
    public static final String USAGE = FixJava.readResource("/cucumber/api/cli/USAGE.txt");

    private final List<String> glue = new ArrayList<>();
    private final List<Object> filters = new ArrayList<>();
    private final List<Formatter> formatters = new ArrayList<>();
    private final List<String> featurePaths = new ArrayList<>();
    private final List<String> formatterNames = new ArrayList<>();
    private final FormatterFactory formatterFactory;
    private URL dotCucumber;
    private boolean dryRun;
    private boolean strict = false;
    private boolean monochrome = false;
    private SnippetType snippetType = SnippetType.UNDERSCORE;
    private boolean formattersCreated = false;

    /**
     * Create a new instance from a string of options, for example:
     * <p>
     * <pre<{@code "--name 'the fox' --format pretty --strict"}</pre>
     *
     * @param argv the arguments
     */
    public RuntimeOptions(String argv) {
        this(new FormatterFactory(), Shellwords.parse(argv));
    }

    /**
     * Create a new instance from a list of options, for example:
     * <p>
     * <pre<{@code Arrays.asList("--name", "the fox", "--format", "pretty", "--strict");}</pre>
     *
     * @param argv the arguments
     */
    public RuntimeOptions(List<String> argv) {
        this(new FormatterFactory(), argv);
    }

    public RuntimeOptions(Env env, List<String> argv) {
        this(env, new FormatterFactory(), argv);
    }

    public RuntimeOptions(FormatterFactory formatterFactory, List<String> argv) {
        this(new Env("cucumber"), formatterFactory, argv);
    }

    public RuntimeOptions(Env env, FormatterFactory formatterFactory, List<String> argv) {
        this.formatterFactory = formatterFactory;

        argv = new ArrayList<>(argv); // in case the one passed in is unmodifiable.
        parse(argv);

        String cucumberOptionsFromEnv = env.get("cucumber.options");
        if (cucumberOptionsFromEnv != null) {
            parse(Shellwords.parse(cucumberOptionsFromEnv));
        }

        if (formatterNames.isEmpty()) {
            formatterNames.add("progress");
        }
    }

    private void parse(List<String> args) {
        List<Object> parsedFilters = new ArrayList<>();
        List<String> parsedFeaturePaths = new ArrayList<>();
        List<String> parsedGlue = new ArrayList<>();

        while (!args.isEmpty()) {
            String arg = args.remove(0).trim();

            if (arg.equals("--help") || arg.equals("-h")) {
                printUsage();
                System.exit(0);
            } else if (arg.equals("--version") || arg.equals("-v")) {
                System.out.println(VERSION);
                System.exit(0);
            } else if (arg.equals("--glue") || arg.equals("-g")) {
                String gluePath = args.remove(0);
                parsedGlue.add(gluePath);
            } else if (arg.equals("--tags") || arg.equals("-t")) {
                parsedFilters.add(args.remove(0));
            } else if (arg.equals("--format") || arg.equals("-f")) {
                formatterNames.add(args.remove(0));
            } else if (arg.equals("--dotcucumber")) {
                String urlOrPath = args.remove(0);
                dotCucumber = Utils.toURL(urlOrPath);
            } else if (arg.equals("--no-dry-run") || arg.equals("--dry-run") || arg.equals("-d")) {
                dryRun = !arg.startsWith("--no-");
            } else if (arg.equals("--no-strict") || arg.equals("--strict") || arg.equals("-s")) {
                strict = !arg.startsWith("--no-");
            } else if (arg.equals("--no-monochrome") || arg.equals("--monochrome") || arg.equals("-m")) {
                monochrome = !arg.startsWith("--no-");
            } else if (arg.equals("--snippets")) {
                String nextArg = args.remove(0);
                snippetType = SnippetType.fromString(nextArg);
            } else if (arg.equals("--name") || arg.equals("-n")) {
                String nextArg = args.remove(0);
                Pattern patternFilter = Pattern.compile(nextArg);
                parsedFilters.add(patternFilter);
            } else if (arg.startsWith("-")) {
                printUsage();
                throw new CucumberException("Unknown option: " + arg);
            } else {
                parsedFeaturePaths.add(arg);
            }
        }
        if (!parsedFilters.isEmpty()) {
            filters.clear();
            filters.addAll(parsedFilters);
        }
        if (!parsedFeaturePaths.isEmpty()) {
            featurePaths.clear();
            featurePaths.addAll(parsedFeaturePaths);
        }
        if (!parsedGlue.isEmpty()) {
            glue.clear();
            glue.addAll(parsedGlue);
        }
    }

    private void printUsage() {
        System.out.println(USAGE);
    }

    public List<CucumberFeature> cucumberFeatures(ResourceLoader resourceLoader) {
        return load(resourceLoader, featurePaths, filters, System.out);
    }

    List<Formatter> getFormatters() {
        if (!formattersCreated) {
            for (String formatterName : formatterNames) {
                Formatter formatter = formatterFactory.create(formatterName);
                formatters.add(formatter);
                setMonochromeOnColorAwareFormatters(formatter);
                setStrictOnStrictAwareFormatters(formatter);
            }
            formattersCreated = true;
        }
        return formatters;
    }

    public Formatter formatter(ClassLoader classLoader) {
        return (Formatter) Proxy.newProxyInstance(classLoader, new Class<?>[]{Formatter.class}, (target, method, args) -> {
            for (Formatter formatter : getFormatters()) {
                Utils.invoke(formatter, method, 0, args);
            }
            return null;
        });
    }

    public Reporter reporter(ClassLoader classLoader) {
        return (Reporter) Proxy.newProxyInstance(classLoader, new Class<?>[]{Reporter.class}, (target, method, args) -> {
            for (Formatter formatter : formatters) {
                if (formatter instanceof Reporter) {
                    Utils.invoke(formatter, method, 0, args);
                }
            }
            return null;
        });
    }

    private void setMonochromeOnColorAwareFormatters(Formatter formatter) {
        if (formatter instanceof ColorAware) {
            ColorAware colorAware = (ColorAware) formatter;
            colorAware.setMonochrome(monochrome);
        }
    }

    private void setStrictOnStrictAwareFormatters(Formatter formatter) {
        if (formatter instanceof StrictAware) {
            StrictAware strictAware = (StrictAware) formatter;
            strictAware.setStrict(strict);
        }
    }

    public List<String> getGlue() {
        return glue;
    }

    public boolean isStrict() {
        return strict;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public List<String> getFeaturePaths() {
        return featurePaths;
    }

    public URL getDotCucumber() {
        return dotCucumber;
    }

    public void addFormatter(Formatter formatter) {
        formatters.add(formatter);
    }

    public List<Object> getFilters() {
        return filters;
    }

    public boolean isMonochrome() {
        return monochrome;
    }

    public SnippetType getSnippetType() {
        return snippetType;
    }
}
