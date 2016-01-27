package test.java.framework.helpers;

import gherkin.formatter.model.DataTableRow;

import java.util.List;

public interface APICucumberHelper {

    List<DataTableRow> replaceCucumberTableValues(List<DataTableRow> rows);

    String replaceCucumberStepValue(String step);

}
