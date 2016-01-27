package test.java.framework.manager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * A Cucumber JS report merger, based on Tristan McCarthy's Merger
 *
 * @see - http://www.opencredo.com/2013/07/02/running-cucumber-jvm-tests-in-parallel/
 * @see - https://github.com/tristanmccarthy/Cucumber-JVM-Parallel
 */
public class JSReportMerger {

    private static String reportFileName = "report.js";

    public static void main(String[] args) throws Throwable {
        File reportDirectory = new File(args[0]);
        if (reportDirectory.exists()) {
            JSReportMerger merger = new JSReportMerger();
            merger.mergeReports(reportDirectory);
        }
    }

    /**
     * Merge all reports together into master report in given reportDirectory
     *
     * @param reportDirectory directory with all subdirs with reports for merge
     * @throws Exception
     */
    public void mergeReports(File reportDirectory) throws Throwable {

        // check if other reporter already copied json report to target directory
        // if so, then delete it, so that we can merge all the sub reports properly
        Path targetReportPath = Paths.get(reportDirectory.toString() + File.separator + reportFileName);
        if (Files.exists(targetReportPath, LinkOption.NOFOLLOW_LINKS)) {
            FileUtils.forceDelete(targetReportPath.toFile());
        }

        File mergedReport = null;
        Collection<File> existingReports = FileUtils.listFiles(reportDirectory, new String[]{"js"}, true);

        for (File report : existingReports) {
            //only address report files
            if (report.getName().equals(reportFileName)) {

                //if we are on the first pass, copy the directory of the file to use as basis for merge
                if (!new File(report.getParent() + "/index.html").exists()) {
                    FileUtils.forceDelete(new File(report.getParent()));

                } else if (mergedReport == null) {
                    FileUtils.copyDirectory(report.getParentFile(), reportDirectory);
                    mergedReport = new File(reportDirectory, reportFileName);
                    //otherwise merge this report into existing master report
                } else {
                    mergeFiles(mergedReport, report);
                }
            }
        }
    }

    /**
     * merge source file into target
     *
     * @param target final report
     * @param source report to be merged
     */
    public void mergeFiles(File target, File source) throws Throwable {

        //merge report files
        String targetReport = FileUtils.readFileToString(target);
        String sourceReport = FileUtils.readFileToString(source);

        FileUtils.writeStringToFile(target, targetReport + sourceReport);
    }

}