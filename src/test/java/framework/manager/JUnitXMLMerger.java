package test.java.framework.manager;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

public class JUnitXMLMerger {

    private static String reportFileName = "junit.xml";

    public static void main(String[] args) throws Throwable {
        File reportDirectory = new File(args[0]);
        if (reportDirectory.exists()) {
            JUnitXMLMerger merger = new JUnitXMLMerger();
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

        //Delete copy if it was copied by other mergers
        Path targetReportPath = Paths.get(reportDirectory.toString() + File.separator + reportFileName);
        if (Files.exists(targetReportPath, LinkOption.NOFOLLOW_LINKS)) {
            FileUtils.forceDelete(targetReportPath.toFile());
        }

        File mergedReport = null;
        Collection<File> existingReports = FileUtils.listFiles(reportDirectory, new String[]{"xml"}, true);

        for (File report : existingReports) {
            //only address report files
            if (report.getName().equals(reportFileName)) {

                //if we are on the first pass, copy the directory of the file to use as basis for merge
                if (!new File(report.getParent() + "/index.html").exists()) {
                    FileUtils.forceDelete(new File(report.getParent()));
                } else if (mergedReport == null) {
                    FileUtils.copyFileToDirectory(report, reportDirectory);
                    mergedReport = new File(reportDirectory, reportFileName);
                    //otherwise merge this report into existing master report
                } else {
                    mergeFiles(mergedReport, report);
                }
            }
        }
    }

    public void mergeFiles(File origFile, File overrideFile) {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document orig = documentBuilder.parse(origFile);
            Document override = documentBuilder.parse(overrideFile);

            Node mergedReport = orig.getElementsByTagName("testsuite").item(0);
            Node mergeCandidate = override.getElementsByTagName("testsuite").item(0);

            //Accumulate failures, skipped, tests, time values
            for (String item : Arrays.asList("failures", "skipped", "tests", "time")) {
                sumAttributes(mergedReport, mergeCandidate, item);
            }

            //Combine <testcase> nodes into one file
            NodeList childNodes = mergeCandidate.getChildNodes();

            for (int i = 0; i < childNodes.getLength(); i++) {
                Node importedNode = orig.importNode(childNodes.item(i), true);
                mergedReport.appendChild(importedNode);
            }

            //Save merged report
            DOMSource source = new DOMSource(orig);
            StreamResult result = new StreamResult(origFile);

            TransformerFactory.newInstance().newTransformer().transform(source, result);

        } catch (ParserConfigurationException | SAXException | IOException | TransformerException pce) {
            pce.printStackTrace();
        }
    }

    private void sumAttributes(Node mergedReport, Node mergeCandidate, String itemName) {

        Node attributeToChange = mergedReport.getAttributes().getNamedItem(itemName);
        String mergedReportValue = attributeToChange.getNodeValue();
        String candidateValue = mergeCandidate.getAttributes().getNamedItem(itemName).getNodeValue();

        try {
            attributeToChange.setNodeValue(String.valueOf(
                    Integer.valueOf(mergedReportValue) + Integer.valueOf(candidateValue)));
        } catch (NumberFormatException e) {
            attributeToChange.setNodeValue(String.valueOf(
                    Double.valueOf(mergedReportValue) + Double.valueOf(candidateValue)));
        }
    }
}

