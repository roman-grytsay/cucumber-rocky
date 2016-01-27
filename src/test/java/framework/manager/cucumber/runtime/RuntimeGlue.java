package test.java.framework.manager.cucumber.runtime;

import gherkin.I18n;
import gherkin.deps.com.google.gson.Gson;
import gherkin.deps.com.google.gson.GsonBuilder;
import gherkin.formatter.Argument;
import gherkin.formatter.model.Step;
import test.java.framework.manager.cucumber.runtime.autocomplete.MetaStepdef;
import test.java.framework.manager.cucumber.runtime.autocomplete.StepdefGenerator;
import test.java.framework.manager.cucumber.runtime.io.ResourceLoader;
import test.java.framework.manager.cucumber.runtime.io.URLOutputStream;
import test.java.framework.manager.cucumber.runtime.io.UTF8OutputStreamWriter;
import test.java.framework.manager.cucumber.runtime.model.CucumberFeature;
import test.java.framework.manager.cucumber.runtime.xstream.LocalizedXStreams;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.*;

import static java.util.Collections.emptyList;
import static test.java.framework.manager.cucumber.runtime.model.CucumberFeature.load;

public class RuntimeGlue {
    private static final List<Object> NO_FILTERS = emptyList();

    private final Map<String, StepDefinition> stepDefinitionsByPattern = new TreeMap<>();
    private final List<HookDefinition> beforeHooks = new ArrayList<>();
    private final List<HookDefinition> afterHooks = new ArrayList<>();

    private final UndefinedStepsTracker tracker;
    private final LocalizedXStreams localizedXStreams;

    public RuntimeGlue(UndefinedStepsTracker tracker, LocalizedXStreams localizedXStreams) {
        this.tracker = tracker;
        this.localizedXStreams = localizedXStreams;
    }

    public void addStepDefinition(StepDefinition stepDefinition) {
        StepDefinition previous = stepDefinitionsByPattern.get(stepDefinition.getPattern());
        if (previous != null) {
            throw new DuplicateStepDefinitionException(previous, stepDefinition);
        }
        stepDefinitionsByPattern.put(stepDefinition.getPattern(), stepDefinition);
    }

    public void addBeforeHook(HookDefinition hookDefinition) {
        beforeHooks.add(hookDefinition);
        Collections.sort(beforeHooks, new HookComparator(true));
    }

    public void addAfterHook(HookDefinition hookDefinition) {
        afterHooks.add(hookDefinition);
        Collections.sort(afterHooks, new HookComparator(false));
    }

    public List<HookDefinition> getBeforeHooks() {
        return beforeHooks;
    }

    public List<HookDefinition> getAfterHooks() {
        return afterHooks;
    }

    public StepDefinitionMatch stepDefinitionMatch(String featurePath, Step step, I18n i18n) {
        List<StepDefinitionMatch> matches = stepDefinitionMatches(featurePath, step);
        try {
            if (matches.size() == 0) {
                tracker.addUndefinedStep(step, i18n);
                return null;
            }
            if (matches.size() == 1) {
                return matches.get(0);
            } else {
                throw new AmbiguousStepDefinitionsException(matches);
            }
        } finally {
            tracker.storeStepKeyword(step, i18n);
        }
    }

    private List<StepDefinitionMatch> stepDefinitionMatches(String featurePath, Step step) {
        List<StepDefinitionMatch> result = new ArrayList<>();
        for (StepDefinition stepDefinition : stepDefinitionsByPattern.values()) {
            List<Argument> arguments = stepDefinition.matchedArguments(step);
            if (arguments != null) {
                result.add(new StepDefinitionMatch(arguments, stepDefinition, featurePath, step, localizedXStreams));
            }
        }
        return result;
    }

    public void writeStepdefsJson(ResourceLoader resourceLoader, List<String> featurePaths, URL dotCucumber) {
        if (dotCucumber != null) {
            List<CucumberFeature> features = load(resourceLoader, featurePaths, NO_FILTERS);
            List<MetaStepdef> metaStepdefs = new StepdefGenerator().generate(stepDefinitionsByPattern.values(), features);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(metaStepdefs);

            try {
                URL stepdefsUrl = new URL(dotCucumber, "stepdefs.json");
                Writer stepdefsJson = new UTF8OutputStreamWriter(new URLOutputStream(stepdefsUrl));
                stepdefsJson.append(json);
                stepdefsJson.close();
            } catch (IOException e) {
                throw new CucumberException("Failed to write stepdefs.json", e);
            }
        }
    }
}
