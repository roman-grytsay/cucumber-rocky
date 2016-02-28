package test.java.framework.web;

import com.google.common.collect.Sets;
import test.java.framework.Bindings;
import test.java.framework.SessionPrototype;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * Wrappers for webDriver methods applicable for browsers only
 */
public abstract class BrowserBindings extends Bindings {

    public BrowserBindings(SessionPrototype instance) {
        super(instance);
    }

    /**
     * Waits until there is more window handles than in initial state
     *
     * @param windowHandles handles to all current windows
     * @param timeOut       approximate time to wait for window
     * @return false if window didn't appear within specified time; true otherwise
     */
    protected boolean waitForWindow(final Set<String> windowHandles, Integer timeOut) {
        try {
            Set<String> newWindowHandles = windowHandles;
            Long time = System.currentTimeMillis() / 1000;

            while (windowHandles.containsAll(newWindowHandles)) {
                Long currentTime = System.currentTimeMillis() / 1000;
                if ((currentTime - time) > timeOut) {
                    error("New window won't appear after waiting for " + timeOut + " second(s)");
                    return false;
                }
                sleep(1);
                newWindowHandles = getCurrentWindowsHandles();
            }
            return true;
        } catch (Exception e) {
            fail(e.getMessage());
            return false;
        }
    }


    /**
     * Returns window handle
     *
     * @return current window handle
     */
    protected String getCurrentWindowHandle() {
        try {
            return getDriverInstance().getWindowHandle();
        } catch (Exception e) {
            fail(e.getMessage());
            throw e;
        }
    }

    /**
     * Returns all windows handles
     *
     * @return all windows handles currently accessible by webdriver instance
     */
    protected Set<String> getCurrentWindowsHandles() {
        try {
            return getDriverInstance().getWindowHandles();
        } catch (Exception e) {
            fail(e.getMessage());
            throw e;
        }
    }

    /**
     * Switches to window with specified handle
     *
     * @param windowHandle handle of the window to switch to
     */
    protected String switchToWindow(String windowHandle) {
        try {
            getDriverInstance().switchTo().window(windowHandle);
        } catch (Exception e) {
            fail(e.getMessage());
            throw e;
        }
        return windowHandle;
    }

    /**
     * Determines and switches to first window that is present in {@code newWindows}
     * and not present in  {@code oldWindows}
     *
     * @param oldWindows Windows that were already opened
     * @param newWindows currently opened windows
     */
    protected String switchToNewWindow(Set<String> oldWindows, Set<String> newWindows) {
        Set<String> openedWindows = Sets.difference(newWindows, oldWindows);
        return switchToWindow(openedWindows.iterator().next());
    }

    /**
     * Determines and switches to first window that is present
     * in list of currently opened windows and not present in  {@code oldWindows}
     *
     * @param oldWindows Windows that were already opened
     */
    protected String switchToNewWindow(Set<String> oldWindows) {
        if (waitForWindow(oldWindows, pauseL)) {
            return switchToNewWindow(oldWindows, getCurrentWindowsHandles());
        } else {
            fail("Unable to locate new window after waiting for " + pauseL + " second(s)");
            return getCurrentWindowHandle();
        }
    }

    /**
     * Determines and switches to first window that is present
     * in list of currently opened windows and not present in  {@code oldWindows}
     *
     * @param oldWindow Window that was already opened
     */
    protected String switchToNewWindow(String oldWindow) {
        return switchToNewWindow(new HashSet<>(Collections.singletonList(oldWindow)));
    }

    /**
     * Closes all windows except that with provided handle
     *
     * @param exceptHandle handle to window to leave opened
     */
    protected String closeWindows(String exceptHandle) {
        Set<String> windows = getCurrentWindowsHandles();
        windows.remove(exceptHandle);

        for (String window : windows) {
            switchToWindow(window);
            closeWindow();
        }
        return switchToWindow(exceptHandle);
    }

    /**
     * Closes current browser window
     */
    protected void closeWindow() {
        try {
            getDriverInstance().close();
        } catch (Exception e) {
            fail(e.getMessage());
        }
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
        executeJS(injectAnchorTag("tagToOpenNewWindow", url));

        //Click on the anchor element
        click("id=tagToOpenNewWindow");

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
        String javaScript = "var anchorTag = document.createElement('a');" +
                "anchorTag.appendChild(document.createTextNode('nwh'));" +
                "anchorTag.setAttribute('id', '%s');" +
                "anchorTag.setAttribute('href', '%s');" +
                "anchorTag.setAttribute('target', '_blank');" +
                "anchorTag.setAttribute('style', 'display:block;');" +
                "document.getElementsByTagName('body')[0].appendChild(anchorTag);";
        return String.format(javaScript, id, url);
    }

    /**
     * Clears all cookies in browser
     */
    protected void clearCookies() {
        try {
            getDriverInstance().manage().deleteAllCookies();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Deletes cookie
     *
     * @param cookieName cookie to delete
     */
    protected void deleteCookie(String cookieName) {
        try {
            getDriverInstance().manage().deleteCookieNamed(cookieName);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Returns current URL location
     *
     * @return fully qualified URL
     */
    protected String getCurrentLocation() {
        try {
            return getDriverInstance().getCurrentUrl();
        } catch (Exception e) {
            fail(e.getMessage());
            throw e;
        }
    }

    /**
     * Returns current Title of the page
     *
     * @return Title of the current browser tab/window
     */
    protected String getPageTitle() {
        try {
            return getDriverInstance().getTitle();
        } catch (Exception e) {
            fail(e.getMessage());
            throw e;
        }
    }

    /**
     * Opens specified url
     *
     * @param url fully qualified URL
     * @return opened page url
     */
    protected String openURL(String url) {
        try {
            getDriverInstance().get(url);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return getCurrentLocation();
    }

    /**
     * Refreshes current page
     */
    protected String refreshPage() {
        try {
            getDriverInstance().navigate().refresh();
        } catch (Exception e) {
            fail(e.getMessage());
        }
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
            error(String.format("Unable to get value from the field: %sinput or %sselect", locator, locator));
        }
        return value;
    }

}
