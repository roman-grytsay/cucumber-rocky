package test.java.framework.web;

import test.java.framework.Bindings;
import test.java.framework.SessionPrototype;

import java.util.Set;


/**
 * Wrappers for webDriver methods applicable for browsers only
 */
public abstract class BrowserBindings extends Bindings {
    public BrowserBindings(SessionPrototype instance) {
        super(instance);
    }

    /**
     * Opens URL in new window
     *
     * @param url fully qualified url to open
     * @return new window handle
     */
    protected String openWindow(String url) {

        //Record old handles
        Set<String> oldHandles = getCurrentWindowsHandles();

        //Inject an anchor element
        executeJS(injectAnchorTag("name", url));

        //Click on the anchor element
        click("xpath=.//*[@id='name']");

        //Switch focus to the new window and work on the popup window
        return switchToNewWindow(oldHandles);
    }

    /**
     * Generates JS injection script for creating anchor on the page
     *
     * @param id  new id of element to inject
     * @param url url to open on click
     * @return JS script to inject
     */
    private String injectAnchorTag(String id, String url) {
        return String.format("var anchorTag = document.createElement('a'); " +
                        "anchorTag.appendChild(document.createTextNode('nwh'));" +
                        "anchorTag.setAttribute('id', '%s');" +
                        "anchorTag.setAttribute('href', '%s');" +
                        "anchorTag.setAttribute('target', '_blank');" +
                        "anchorTag.setAttribute('style', 'display:block;');" +
                        "document.getElementsByTagName('body')[0].appendChild(anchorTag);",
                id, url
        );
    }

    /**
     * Clears all cookies in browser
     */
    protected void clearCookies() {
        getDriverInstance().manage().deleteAllCookies();
    }

    /**
     * Returns current URL location
     *
     * @return fully qualified URL
     */
    protected String getCurrentLocation() {
        return getDriverInstance().getCurrentUrl();
    }

    /**
     * Returns current Title of the page
     *
     * @return Title of the current browser tab/window
     */
    protected String getPageTitle() {
        return getDriverInstance().getTitle();
    }

    /**
     * Opens specified url
     *
     * @param url fully qualified URL
     * @return opened page url
     */
    protected String openURL(String url) {
        getDriverInstance().get(url);
        return getCurrentLocation();
    }

    /**
     * Refreshes current page
     */
    protected String refreshPage() {
        getDriverInstance().navigate().refresh();
        return getCurrentLocation();
    }

    /**
     * Gets current value from input or select field (dropdown)
     *
     * @param locator locator of the element without specifying node (e.g. /example// to get text from /example//input)
     * @return value
     */
    protected String getValue(String locator) {
        String value = null;
        try {
            value = getText(locator + "input");
        } catch (Exception e) {
            try {
                value = getDropDownValue(locator + "select");
            } catch (Exception ignored) {
            }
        }
        if (value == null) {
            error(String.format("Unable to get value from field: %sinput or %sselect", locator, locator));
        }
        return value;
    }

}
