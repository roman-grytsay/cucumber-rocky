package test.java.framework.manager.cucumber.runtime.autocomplete;

import gherkin.formatter.Argument;
import gherkin.formatter.model.Step;
import test.java.framework.manager.cucumber.runtime.StepDefinition;
import test.java.framework.manager.cucumber.runtime.model.CucumberFeature;
import test.java.framework.manager.cucumber.runtime.model.CucumberTagStatement;

import java.util.*;

/**
 * Generates metadata to be used for Code Completion: https://github.com/cucumber/gherkin/wiki/Code-Completion
 */
public class StepdefGenerator {
    private static final Comparator<StepDefinition> STEP_DEFINITION_COMPARATOR = (a, b) -> a.getPattern().compareTo(b.getPattern());

    private static final Comparator<CucumberTagStatement> CUCUMBER_TAG_STATEMENT_COMPARATOR = (a, b) -> a.getVisualName().compareTo(b.getVisualName());

    public List<MetaStepdef> generate(Collection<StepDefinition> stepDefinitions, List<CucumberFeature> features) {
        List<MetaStepdef> result = new ArrayList<>();

        List<StepDefinition> sortedStepdefs = new ArrayList<>();
        sortedStepdefs.addAll(stepDefinitions);
        Collections.sort(sortedStepdefs, STEP_DEFINITION_COMPARATOR);
        for (StepDefinition stepDefinition : sortedStepdefs) {
            MetaStepdef metaStepdef = new MetaStepdef();
            metaStepdef.source = stepDefinition.getPattern();
            metaStepdef.flags = "";
            for (CucumberFeature feature : features) {
                List<CucumberTagStatement> cucumberTagStatements = feature.getFeatureElements();
                for (CucumberTagStatement tagStatement : cucumberTagStatements) {
                    List<Step> steps = tagStatement.getSteps();
                    for (Step step : steps) {
                        List<Argument> arguments = stepDefinition.matchedArguments(step);
                        if (arguments != null) {
                            MetaStepdef.MetaStep ms = new MetaStepdef.MetaStep();
                            ms.name = step.getName();
                            for (Argument argument : arguments) {
                                MetaStepdef.MetaArgument ma = new MetaStepdef.MetaArgument();
                                ma.offset = argument.getOffset();
                                ma.val = argument.getVal();
                                ms.args.add(ma);
                            }
                            metaStepdef.steps.add(ms);
                        }
                    }
                }
                Collections.sort(cucumberTagStatements, CUCUMBER_TAG_STATEMENT_COMPARATOR);
            }
            result.add(metaStepdef);
        }
        return result;
    }

}
