package test.java.framework.manager.cucumber.runtime;

import java.io.PrintStream;
import java.util.List;

public class SummaryPrinter {
    private final PrintStream out;

    public SummaryPrinter(PrintStream out) {
        this.out = out;
    }

    public void print(Runtime runtime) {
        out.println();
        printStats(runtime);
        out.println();
        printErrors(runtime);
        printSnippets(runtime);
    }

    private void printStats(Runtime runtime) {
        runtime.printStats(out);
    }

    private void printErrors(Runtime runtime) {
        for (Throwable error : runtime.getErrors()) {
            error.printStackTrace(out);
            out.println();
        }
    }

    private void printSnippets(Runtime runtime) {
        List<String> snippets = runtime.getSnippets();
        if (!snippets.isEmpty()) {
            out.append("\n");
            out.println("You can implement missing steps with the snippets below:");
            out.println();
            snippets.forEach(out::println);
        }
    }
}
