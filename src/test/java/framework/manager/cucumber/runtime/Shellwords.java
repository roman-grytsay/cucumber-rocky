package test.java.framework.manager.cucumber.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Shellwords {
    private static final Pattern SHELLWORDS_PATTERN = Pattern.compile("[^\\s']+|'([^']*)'");

    public static List<String> parse(String cmdline) {
        List<String> matchList = new ArrayList<>();
        Matcher shellWordsMatcher = SHELLWORDS_PATTERN.matcher(cmdline);
        while (shellWordsMatcher.find()) {
            if (shellWordsMatcher.group(1) != null) {
                matchList.add(shellWordsMatcher.group(1));
            } else {
                matchList.add(shellWordsMatcher.group());
            }
        }
        return matchList;
    }
}
