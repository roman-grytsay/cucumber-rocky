package test.java.framework.manager.cucumber.runtime.table;

import test.java.framework.manager.cucumber.api.DataTable;

public class TableDiffException extends RuntimeException {
    private final DataTable from;
    private final DataTable to;
    private final DataTable diff;

    public TableDiffException(DataTable from, DataTable to, DataTable diff) {
        super("Tables were not identical:\n" + diff.toString());
        this.from = from;
        this.to = to;
        this.diff = diff;
    }

    /**
     * @return the left side of the diff
     */
    public DataTable getFrom() {
        return from;
    }

    /**
     * @return the right side of the diff
     */
    public DataTable getTo() {
        return to;
    }

    /**
     * @return the diff itself - represented as a table
     */
    public DataTable getDiff() {
        return diff;
    }
}
