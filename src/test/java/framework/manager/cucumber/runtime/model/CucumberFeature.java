package test.java.framework.manager.cucumber.runtime.model;

import gherkin.I18n;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.*;
import test.java.framework.manager.cucumber.runtime.FeatureBuilder;
import test.java.framework.manager.cucumber.runtime.Runtime;
import test.java.framework.manager.cucumber.runtime.io.MultiLoader;
import test.java.framework.manager.cucumber.runtime.io.Resource;
import test.java.framework.manager.cucumber.runtime.io.ResourceLoader;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CucumberFeature {
    private final String path;
    private final Feature feature;
    private CucumberBackground cucumberBackground;
    private StepContainer currentStepContainer;
    private final List<CucumberTagStatement> cucumberTagStatements = new ArrayList<>();
    private I18n i18n;
    private CucumberScenarioOutline currentScenarioOutline;

    public static List<CucumberFeature> load(ResourceLoader resourceLoader, List<String> featurePaths, final List<Object> filters, PrintStream out) {
        final List<CucumberFeature> cucumberFeatures = load(resourceLoader, featurePaths, filters);
        if (cucumberFeatures.isEmpty()) {
            if (featurePaths.isEmpty()) {
                out.println(String.format("Got no path to feature directory or feature file"));
            } else if (filters.isEmpty()) {
                out.println(String.format("No features found at %s", featurePaths));
            } else {
                out.println(String.format("None of the features at %s matched the filters: %s", featurePaths, filters));
            }
        }
        return cucumberFeatures;
    }

    public static List<CucumberFeature> load(ResourceLoader resourceLoader, List<String> featurePaths, final List<Object> filters) {
        final List<CucumberFeature> cucumberFeatures = new ArrayList<CucumberFeature>();
        final FeatureBuilder builder = new FeatureBuilder(cucumberFeatures);
        for (String featurePath : featurePaths) {
            if (featurePath.startsWith("@")) {
                loadFromRerunFile(builder, resourceLoader, featurePath.substring(1), filters);
            } else {
                loadFromFeaturePath(builder, resourceLoader, featurePath, filters);
            }
        }
        Collections.sort(cucumberFeatures, new CucumberFeatureUriComparator());
        return cucumberFeatures;
    }

    private static void loadFromRerunFile(FeatureBuilder builder, ResourceLoader resourceLoader, String rerunPath, final List<Object> filters) {
        Iterable<Resource> resources = resourceLoader.resources(rerunPath, null);
        for (Resource resource : resources) {
            String source = builder.read(resource);
            for (String featurePath : source.split(" ")) {
                loadFromFileSystemOrClasspath(builder, resourceLoader, featurePath, filters);
            }
        }
    }

    private static void loadFromFileSystemOrClasspath(FeatureBuilder builder, ResourceLoader resourceLoader, String featurePath, final List<Object> filters) {
        try {
            loadFromFeaturePath(builder, resourceLoader, featurePath, filters);
        } catch (IllegalArgumentException originalException) {
            if (!featurePath.startsWith(MultiLoader.CLASSPATH_SCHEME)) {
                try {
                    loadFromFeaturePath(builder, resourceLoader, MultiLoader.CLASSPATH_SCHEME + featurePath, filters);
                } catch (IllegalArgumentException secondException) {
                    throw originalException;
                }
            } else {
                throw originalException;
            }
        }
    }

    private static void loadFromFeaturePath(FeatureBuilder builder, ResourceLoader resourceLoader, String featurePath, final List<Object> filters) {
        PathWithLines pathWithLines = new PathWithLines(featurePath);
        ArrayList<Object> filtersForPath = new ArrayList<>(filters);
        filtersForPath.addAll(pathWithLines.lines);
        Iterable<Resource> resources = resourceLoader.resources(pathWithLines.path, ".feature");
        for (Resource resource : resources) {
            builder.parse(resource, filtersForPath);
        }
    }

    public CucumberFeature(Feature feature, String path) {
        this.feature = feature;
        this.path = path;
    }

    public void background(Background background) {
        cucumberBackground = new CucumberBackground(this, background);
        currentStepContainer = cucumberBackground;
    }

    public void scenario(Scenario scenario) {
        CucumberTagStatement cucumberTagStatement = new CucumberScenario(this, cucumberBackground, scenario);
        currentStepContainer = cucumberTagStatement;
        cucumberTagStatements.add(cucumberTagStatement);
    }

    public void scenarioOutline(ScenarioOutline scenarioOutline) {
        CucumberScenarioOutline cucumberScenarioOutline = new CucumberScenarioOutline(this, cucumberBackground, scenarioOutline);
        currentScenarioOutline = cucumberScenarioOutline;
        currentStepContainer = cucumberScenarioOutline;
        cucumberTagStatements.add(cucumberScenarioOutline);
    }

    public void examples(Examples examples) {
        currentScenarioOutline.examples(examples);
    }

    public void step(Step step) {
        currentStepContainer.step(step);
    }

    public Feature getGherkinFeature() {
        return feature;
    }

    public List<CucumberTagStatement> getFeatureElements() {
        return cucumberTagStatements;
    }

    public void setI18n(I18n i18n) {
        this.i18n = i18n;
    }

    public I18n getI18n() {
        return i18n;
    }

    public String getPath() {
        return path;
    }

    public void run(Formatter formatter, Reporter reporter, Runtime runtime) {
        formatter.uri(getPath());
        formatter.feature(getGherkinFeature());

        for (CucumberTagStatement cucumberTagStatement : getFeatureElements()) {
            System.out.println("Starting scenario: '" + cucumberTagStatement.getVisualName() + "'");
            //Run the scenario, it should handle before and after hooks
            cucumberTagStatement.run(formatter, reporter, runtime);
            System.out.println("Scenario completed: '" + cucumberTagStatement.getVisualName() + "'");
        }
        formatter.eof();

    }

    private static class CucumberFeatureUriComparator implements Comparator<CucumberFeature> {
        @Override
        public int compare(CucumberFeature a, CucumberFeature b) {
            return a.getPath().compareTo(b.getPath());
        }
    }
}
