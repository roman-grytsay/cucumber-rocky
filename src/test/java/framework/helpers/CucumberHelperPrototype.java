package test.java.framework.helpers;

import gherkin.formatter.model.DataTableRow;
import test.java.framework.LangPrototype;
import test.java.framework.SessionPrototype;

import java.util.ArrayList;
import java.util.List;

public abstract class CucumberHelperPrototype {

    /**
     * Enum with translation keys for a web app
     *
     * @return enum class e.g. Lang.class
     */
    protected abstract Class<? extends Enum<?>> getWebLang();

    /**
     * Enum with translation keys for a mobile app
     *
     * @return enum class e.g. MobileLang.class
     */
    protected abstract Class<? extends Enum<?>> getMobileLang();

    /**
     * <pre>
     * Replacing cucumber table values.
     * By default replacing translation keys provided by {@link #getWebLang()} and {@link #getMobileLang()}
     *
     * Override method, calling super in the beginning to extend functionality
     *
     * @param rows cucumber step (not to confuse with scenario outline example) table rows
     * @return modified table to be used for execution of the tests and reports
     */
    public List<DataTableRow> replaceCucumberTableValues(List<DataTableRow> rows) {

        List<DataTableRow> resultRows = new ArrayList<>();

        if (rows == null) {
            resultRows = null;
        } else {
            for (DataTableRow row : rows) {
                List<String> replacedCells = new ArrayList<>();
                for (String cell : row.getCells()) {
                    String replacedCell = replaceCucumberStepValue(cell);
                    replacedCells.add(replacedCell);
                }
                DataTableRow replacedRow = new DataTableRow(row.getComments(), replacedCells, row.getLine(), row.getDiffType());
                resultRows.add(replacedRow);
            }
        }
        return resultRows;
    }

    /**
     * <pre>
     * Replacing cucumber table values.
     * By default replacing translation keys provided by {@link #getWebLang()} and {@link #getMobileLang()}
     *
     * Override method, calling super in the beginning to extend functionality
     *
     * @param step cucumber step to replace
     * @return modified step to be used for execution of the tests and reports
     */
    public String replaceCucumberStepValue(String step) {
        String resultStep = replaceTranslationKeys(step, getWebLang());
        resultStep = replaceTranslationKeys(resultStep, getMobileLang());
        return resultStep;
    }

    protected String replaceTranslationKeys(String step, Class<? extends Enum<?>> translations) {
        String resultStep = step;
        if (translations != null) {
            for (Enum<?> lang : translations.getEnumConstants()) {
                if (step.contains(lang.name())) {
                    String replacedValue = SessionPrototype.getTranslation((LangPrototype) lang);
                    replacedValue = replacedValue == null ? lang.name() : replacedValue;
                    resultStep = step.replace(lang.name(), replacedValue);
                }
            }
        }
        return resultStep;
    }

}
