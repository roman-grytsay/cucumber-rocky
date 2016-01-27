package test.java.framework.manager;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class ReportCleaner {

    public static void main(String[] args) throws Throwable {

        String reportDir = args[0];
        File reportDirectory = new File(reportDir);

        //Delete separate report dirs
        if (new File(reportDir + "index.html").exists()) {
            for (File dir : reportDirectory.listFiles((current, name) -> new File(current, name).isDirectory())) {
                FileUtils.forceDelete(dir);
            }
        }
    }
}
