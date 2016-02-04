package test.java.framework.manager.cucumber.runtime.model;

import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.BasicStatement;
import gherkin.formatter.model.DataTableRow;
import gherkin.formatter.model.Step;
import test.java.framework.helpers.CucumberHelperPrototype;
import test.java.framework.helpers.OptionalSteps;
import test.java.framework.manager.cucumber.runtime.Runtime;

import java.util.ArrayList;
import java.util.List;

public class StepContainer {

    private List<Step> steps = new ArrayList<>();
    final CucumberFeature cucumberFeature;
    private final BasicStatement statement;
    private static CucumberHelperPrototype helper;
    private static OptionalSteps optionalSteps;

    StepContainer(CucumberFeature cucumberFeature, BasicStatement statement) {
        this.cucumberFeature = cucumberFeature;
        this.statement = statement;
    }

    public static void setCucumberHelper(CucumberHelperPrototype helper) {
        StepContainer.helper = helper;
    }

    public static void setOptionalSteps(OptionalSteps optionalSteps) {
        StepContainer.optionalSteps = optionalSteps;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void step(Step step) {
        steps.add(step);
    }

    void format(Formatter formatter) {
        statement.replay(formatter);

        List<Step> formattedSteps = new ArrayList<>();
        List<String> exAttr = null;

        if (optionalSteps != null) {
            exAttr = optionalSteps.getExcludedAttributes();
        }

        for (Step step : getSteps()) {

            List<DataTableRow> rows = step.getRows();

            //If the step to be removed due to presence of a specific word
            if (exAttr != null) {
                //Exclude the whole step if attributes are mentioned
                if (shouldRemoveStep(step, exAttr)) {
                    continue;
                }
                //Exclude table row if attributes are mentioned
                rows = removeRows(step.getRows(), exAttr);
            }

            //Replace scenario keywords
            String updatedStep = updateStep(step);

            //Replace associated scenario table
            List<DataTableRow> updatedRows = updateTableRow(rows);

            Step newStep = new Step(step.getComments(), step.getKeyword(),
                    updatedStep, step.getLine(), updatedRows, step.getDocString());

            //Replace the step with modified one
            formattedSteps.add(newStep);
            //Format the step
            formatter.step(newStep);
        }
        steps = formattedSteps;
    }

    private String updateStep(Step step) {
        if (helper != null) {
            return helper.replaceCucumberStepValue(step.getName());
        } else {
            return step.getName();
        }
    }

    private List<DataTableRow> updateTableRow(List<DataTableRow> rows) {
        if (helper != null) {
            return helper.replaceCucumberTableValues(rows);
        } else {
            return rows;
        }
    }

    private List<DataTableRow> removeRows(List<DataTableRow> rows, List<String> exAttr) {
        if (rows == null) {
            return null;
        }
        List<DataTableRow> replacedRows = new ArrayList<>();

        NEXT_ROW:
        for (DataTableRow row : rows) {
            for (String cell : row.getCells()) {
                for (String a : exAttr) {
                    if (cell.contains(a)) {
                        //If keywords are present in any cell of table, exclude the row
                        continue NEXT_ROW;
                    }
                }
            }
            replacedRows.add(row);
        }
        return replacedRows;
    }

    private boolean shouldRemoveStep(Step step, List<String> exAttr) {
        String stepText = step.getName();
        for (String a : exAttr) {
            if (stepText.contains(a)) {
                return true;
            }
        }
        return false;
    }

    void runSteps(Reporter reporter, Runtime runtime) {
        for (Step step : getSteps()) {
            runStep(step, reporter, runtime);
        }
    }

    void runStep(Step step, Reporter reporter, Runtime runtime) {
        runtime.runStep(cucumberFeature.getPath(), step, reporter, cucumberFeature.getI18n());
    }
}
