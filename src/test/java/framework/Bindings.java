package test.java.framework;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import test.java.framework.helpers.CommonHelper;
import test.java.framework.manager.SoftAssertionError;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrappers for webDriver methods applicable for browsers and mobile
 */
public abstract class Bindings {

    public Bindings(SessionPrototype instance) {
        setSession(instance);
    }

//======================================================================================================================
//Properties

    //Timeout constants
    protected static Integer pauseL;
    protected static Integer pauseM;
    protected static Integer pauseS;

    //Session instance
    private SessionPrototype session;

    protected static String HTTP = "http://";
    protected static String HTTPS = "https://";

//======================================================================================================================
//Methods

    /**
     * Set the given session
     *
     * @param instance session instance
     * @return session from parameters
     */
    protected SessionPrototype setSession(SessionPrototype instance) {
        return this.session = instance;
    }

    /**
     * Get current session instance
     *
     * @return session instance
     */
    public SessionPrototype getSession() {
        return this.session;
    }

    /**
     * Get WebDriver handle from session instance.
     * Shouldn't be used outside framework package and its sub packages.
     *
     * @return webdriver handle
     */
    protected WebDriver getDriverInstance() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String methodName = "";
        for (int i = 0; i < stackTraceElements.length; i++) {
            if (stackTraceElements[i].isNativeMethod()) {
                methodName = stackTraceElements[i - 2].getMethodName();
                break;
            }
        }
        //Abort if calling this method directly
        if (methodName.contains("getDriverInstance")) {
            System.err.println("getDriverInstance() method (WebDriver instance) should be accessed via wrappers/bindings only!\n" +
                    Arrays.toString(stackTraceElements).replaceAll(", ", "\n"));
            System.exit(1);
            return null;
        } else {
            return session.getDriverInstance();
        }
    }

    /**
     * Takes screenshot of current page.
     *
     * @param fileName name to give for screenshot.
     * @return url (or path for local machine) to saved screenshot
     */
    protected String takeScreenshot(String fileName) {
        try {
            //Capture Screenshot
            TakesScreenshot driver = !SessionPrototype.isGrid() || SessionPrototype.isMobile() ?
                    (TakesScreenshot) getDriverInstance() :
                    (TakesScreenshot) new Augmenter().augment(getDriverInstance());

            File tempFile = driver.getScreenshotAs(OutputType.FILE);

            //Name and save file
            String path = String.format("%s%s/%s.png", CommonHelper.getScreenshotsDir(), this.getClass().getName(), fileName);
            File screenShotFile = new File(path);
            FileUtils.copyFile(tempFile, screenShotFile);

            //Create link to screenshot
            return "SCREENSHOT: " + CommonHelper.getFileUrl(path);

        } catch (NullPointerException e) {
            return "No screenshot available.";
        } catch (Exception e) {
            //Suppress exception no need to fail test
            return e.getMessage();
        }
    }

    /**
     * Takes screenshot with default name
     *
     * @return url (or path for local machine) to saved screenshot
     */
    protected String takeScreenshot() {
        return takeScreenshot(CommonHelper.randomStringTime(23));
    }

    /**
     * Get "By" object to locate element
     *
     * @param locator locator of element in xpath=locator; css=locator etc
     * @return by object
     */
    protected By byLocator(final String locator) {
        int index = locator.indexOf('=');
        String errMsg = "Invalid locator identifier: " + locator;
        if (index == -1) fail(errMsg);

        String prefix = locator.substring(0, index);
        String suffix = locator.substring(index + 1);

        switch (prefix) {
            case "xpath":
                return By.xpath(suffix);
            case "css":
                return By.cssSelector(suffix);
            case "link":
                return By.linkText(suffix);
            case "partLink":
                return By.partialLinkText(suffix);
            case "id":
                return By.id(suffix);
            case "name":
                return By.name(suffix);
            case "tag":
                return By.tagName(suffix);
            case "class":
                return By.className(suffix);
            default:
                fail(errMsg);
                return null;
        }
    }

    /**
     * Binding to get Xpath, CSS, Link, Partial link element
     *
     * @param locator locator of element in xpath=locator; css=locator etc
     * @return found WebElement
     */
    protected WebElement getElement(final String locator) {
        return getElement(locator, true);
    }

    /**
     * @param locator          locator of element in xpath=locator; css=locator etc
     * @param screenShotOnFail make screenshot on failed attempt
     * @return found WebElement
     */
    protected WebElement getElement(final String locator, boolean screenShotOnFail) {
        try {
            return getDriverInstance().findElement(byLocator(locator));
        } catch (Exception e) {
            if (screenShotOnFail) {
                fail(e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Binding to get Xpath, CSS, Link, Partial link subElement
     *
     * @param we      webElement handle for which subElement would be found
     * @param locator locator of subElement in xpath=locator; css=locator etc
     * @return found WebElement
     */
    protected WebElement getElement(final WebElement we, final String locator) {
        return getElement(we, locator, true);
    }

    /**
     * @param we               webElement handle for which subElement would be found
     * @param locator          locator of subElement in xpath=locator; css=locator etc
     * @param screenShotOnFail make screenshot on failed attempt
     * @return found WebElement
     */
    protected WebElement getElement(final WebElement we, final String locator, boolean screenShotOnFail) {
        try {
            return we.findElement(byLocator(locator));
        } catch (Exception e) {
            if (screenShotOnFail) fail(e.getMessage());
            throw e;
        }
    }

    /**
     * Binding to return list of WebElements
     *
     * @param locator locator of element in xpath=locator; css=locator etc
     * @return List of webelements
     */
    protected List<WebElement> getElements(final String locator) {
        return getElements(locator, true);
    }

    /**
     * Binding to return list of WebElements
     *
     * @param locator locator of element in xpath=locator; css=locator etc
     * @return List of webelements
     */
    protected List<WebElement> getElements(final String locator, int timeOut) {
        waitForElement(locator, timeOut);
        return getElements(locator, true);
    }

    /**
     * Binding to return list of WebElements
     *
     * @param locator locator of element in xpath=locator; css=locator etc
     * @return List of webelements
     */
    protected List<WebElement> getElements(final String locator, boolean screenShotOnFail) {
        try {
            return getDriverInstance().findElements(byLocator(locator));
        } catch (Exception e) {
            if (screenShotOnFail) fail(e.getMessage());
            throw e;
        }
    }

    /**
     * Binding to get Xpath, CSS, Link, Partial link subElement
     *
     * @param we      webElement handle for which subElement would be found
     * @param locator locator of subElement in xpath=locator; css=locator etc
     * @return List of found WebElement
     */
    protected List<WebElement> getElements(final WebElement we, final String locator) {
        return getElements(we, locator, true);
    }

    /**
     * @param we               webElement handle for which subElement would be found
     * @param locator          locator of subElement in xpath=locator; css=locator etc
     * @param screenShotOnFail make screenshot on failed attempt
     * @return list of founded WebElement
     */
    protected List<WebElement> getElements(final WebElement we, final String locator, boolean screenShotOnFail) {
        try {
            return we.findElements(byLocator(locator));
        } catch (Exception e) {
            if (screenShotOnFail) fail(e.getMessage());
            throw e;
        }
    }

    /**
     * Binding returns select for WebElement
     *
     * @param we webelement of select element
     * @return select object
     */
    protected Select getSelect(final WebElement we) {
        try {
            return new Select(we);
        } catch (Exception e) {
            fail(e.getMessage());
            throw e;
        }
    }

    /**
     * Binding returns select by locator
     *
     * @param locator locator of webelement in format xpath=locator; css=locator  etc
     * @return select object
     */
    protected Select getSelect(final String locator) {
        return getSelect(getElement(locator));
    }

    /**
     * Binding to execute Javascript on the page
     *
     * @param script javascript to execute
     * @param we     webElement handle to apply js
     * @return JS result
     */
    protected Object executeJS(final String script, final WebElement we) {
        try {
            return ((JavascriptExecutor) getDriverInstance()).executeScript(script, we);
        } catch (Exception e) {
            fail(e.getMessage());
            return null;
        }
    }

    /**
     * Binding to execute Javascript on the page
     *
     * @param script  javascript to execute
     * @param locator locator of webelement in format xpath=locator; css=locator  etc
     * @return JS result
     */
    protected Object executeJS(final String script, final String locator) {
        return executeJS(script, getElement(locator));
    }

    /**
     * Execute JS on the page with specified parameters
     *
     * @param script javascript to execute
     * @param params arguments to the script. May be empty.
     * @return JS result
     */
    protected Object executeJS(final String script, final Object... params) {
        try {
            return ((JavascriptExecutor) getDriverInstance()).executeScript(script, params);
        } catch (Exception e) {
            fail(e.getMessage());
            return null;
        }
    }

    /**
     * Switches to the frame of specified webElement
     *
     * @param we             frame webElement
     * @param throughDefault true to switch to defaultContent first;
     */
    protected void switchToFrame(WebElement we, boolean throughDefault) {
        if (throughDefault) switchToDefault();
        getDriverInstance().switchTo().frame(we);
    }

    /**
     * Switches to the frame of specified locator
     *
     * @param frameName      name of the frame
     * @param throughDefault true to switch to defaultContent first;
     */
    protected void switchToFrame(String frameName, boolean throughDefault) {
        if (throughDefault) switchToDefault();
        getDriverInstance().switchTo().frame(frameName);
    }

    /**
     * Switches to the frame with specified index
     *
     * @param frameIndex     frame index (starting from 0)
     * @param throughDefault true to switch to defaultContent first;
     */
    protected void switchToFrame(int frameIndex, boolean throughDefault) {
        if (throughDefault) switchToDefault();
        getDriverInstance().switchTo().frame(frameIndex);
    }

    /**
     * Switches to the frame of specified locator
     *
     * @param we frame webElement to switch to bypass default content
     */
    protected void switchToFrame(WebElement we) {
        switchToFrame(we, false);
    }

    /**
     * Switches to the frame of specified locator
     *
     * @param locator frame locator to switch to bypass default content
     */
    protected void switchToFrame(String locator) {
        switchToFrame(getElement(locator), false);
    }

    /**
     * Switches to default content (can be used after switching to another frames)
     */
    protected void switchToDefault() {
        getDriverInstance().switchTo().defaultContent();
    }

    /**
     * Retrieves full page source
     *
     * @return Source of the current page
     */
    protected String getSource() {
        return getDriverInstance().getPageSource();
    }

    /**
     * Clicks "OK" in the alert window
     */
    protected void alertAccept() {
        sleep(1);
        getDriverInstance().switchTo().alert().accept();
    }

    /**
     * Clicks "Cancel" in the alert window
     */
    protected void alertDismiss() {
        getDriverInstance().switchTo().alert().dismiss();
    }

    /**
     * Gets alert message text
     */
    protected String getAlertText() {
        return getDriverInstance().switchTo().alert().getText();
    }

    /**
     * Binding to click the webElement
     *
     * @param we webElement to click
     */
    protected void click(final WebElement we) {
        try {
            we.click();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Binding to click Xpath, CSS, Link, Partial link element
     *
     * @param locator locator of the element in format xpath=locator; css=locator  etc
     */
    protected void click(final String locator) {
        click(getElement(locator));
    }

    /**
     * Binding to click Xpath, CSS, Link, Partial link element
     *
     * @param locator locator of the element in format xpath=locator; css=locator  etc
     * @param timeOut time to wait for element to appear
     */
    protected void click(final String locator, int timeOut) {
        waitForElement(locator, timeOut);
        click(waitForClickable(locator));
    }

    /**
     * Binding to click rmb on WebElement
     *
     * @param we webElement to click
     */
    protected void contextClick(final WebElement we) {
        new Actions(getDriverInstance()).contextClick(we).perform();
    }

    /**
     * Binding to click rmb Xpath, CSS, Link, Partial link element
     *
     * @param locator locator of the element in format xpath=locator; css=locator  etc
     */
    protected void contextClick(final String locator) {
        contextClick(getElement(locator));
    }

    /**
     * Binding to click rmb Xpath, CSS, Link, Partial link element after waiting
     *
     * @param locator locator of the element in format xpath=locator; css=locator  etc
     * @param timeOut time to wait for element to appear
     */
    protected void contextClick(final String locator, int timeOut) {
        contextClick(waitForElement(locator, timeOut));
    }

    /**
     * Binding to double click element
     *
     * @param we webElement to click
     */
    protected void doubleClick(final WebElement we) {
        if (SessionPrototype.getClient().equals(Client.IE)) {
            executeJS("arguments[0].fireEvent('ondblclick');", we);
        } else {
            executeJS("var evt = document.createEvent('MouseEvents');evt.initMouseEvent(" +
                    "'dblclick',true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0,null);" +
                    "arguments[0].dispatchEvent(evt);", we);
        }
    }

    /**
     * Binding to double click Xpath, CSS, Link, Partial link element
     *
     * @param locator locator of the element in format xpath=locator; css=locator  etc
     */
    protected void doubleClick(final String locator) {
        doubleClick(getElement(locator));
    }

    /**
     * Binding to double click Xpath, CSS, Link, Partial link element after waiting
     *
     * @param locator locator of the element in format xpath=locator; css=locator  etc
     * @param timeOut time to wait
     */
    protected void doubleClick(final String locator, int timeOut) {
        doubleClick(waitForElement(locator, timeOut));
    }

    /**
     * Binding to clear text field and enter text
     *
     * @param we    webElement to type to
     * @param value value to type into the field
     * @param clear true to clear the field first; false to enter text without clearing field
     * @return webElement with edited text
     */
    protected WebElement typeKeys(final WebElement we, final String value, final boolean clear) {
        try {
            if (clear) we.clear();
            we.sendKeys(value);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return we;
    }

    /**
     * Binding to clear text field and enter text
     *
     * @param we    webElement to type to
     * @param value value to type into the field
     * @return webElement with edited text
     */
    protected WebElement typeKeys(final WebElement we, final String value) {
        return typeKeys(we, value, true);
    }

    /**
     * Binding to clear text field and enter text
     *
     * @param locator locator of Element to type to
     * @param value   value to type into the field
     * @return webElement with edited text
     */
    protected WebElement typeKeys(final String locator, final String value) {
        return typeKeys(getElement(locator), value);
    }

    /**
     * Binding to wait for input field; clear text field and enter text
     *
     * @param locator locator of Element to type to
     * @param value   text to type
     * @param timeOut time to wait for input field or option for action without .clear
     * @return webElement with edited text
     */
    protected WebElement typeKeys(final String locator, final String value, int timeOut) {
        return typeKeys(waitForElement(locator, timeOut), value);
    }

    /**
     * Binding to type keys into field which needs to be clicked for edit mode
     *
     * @param clickLocator locator of element to click for edit
     * @param typeLocator  locator of Element to type to
     * @param value        value to type
     */
    protected void clickToType(final String clickLocator, final String typeLocator, final String value) {
        clickToType(clickLocator, typeLocator, value, pauseS);
    }

    /**
     * Binding to type keys into field which needs to be clicked for edit mode.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param clickLocator locator of element to click for edit
     * @param typeLocator  locator of Element to type to
     * @param value        value to type
     * @param timeOut      time to wait for type/edit field to appear
     */
    protected void clickToType(final String clickLocator, final String typeLocator, final String value, int timeOut) {
        click(clickLocator, timeOut);
        typeKeys(typeLocator, value, timeOut).sendKeys(Keys.TAB);
    }

    /**
     * Binding to type keys into field which needs to be clicked for edit mode.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param combinedLocator Both getLocators (click and input) are passed as single String using ||| separator
     * @param value           value to type
     */
    protected void clickToType(final String combinedLocator, final String value) {
        String clickLocator = combinedLocator.substring(0, combinedLocator.indexOf("|||"));
        String typeLocator = combinedLocator.substring(combinedLocator.indexOf("|||") + 3);
        clickToType(clickLocator, typeLocator, value);
    }

    /**
     * Binding to type keys into field which needs to be clicked twice for edit mode
     *
     * @param clickLocator locator of element to double click for edit
     * @param typeLocator  locator of Element to type to
     * @param value        value to type
     */
    protected void doubleClickToType(final String clickLocator, final String typeLocator, final String value) {
        doubleClickToType(clickLocator, typeLocator, value, pauseS);
    }

    /**
     * Binding to type keys into field which needs to be clicked twice for edit mode.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param clickLocator locator of element to click for edit
     * @param typeLocator  locator of Element to type to
     * @param value        value to type
     * @param timeOut      time to wait for type/edit field to appear
     */
    protected void doubleClickToType(final String clickLocator, final String typeLocator, final String value, int timeOut) {
        doubleClick(clickLocator, timeOut);
        typeKeys(typeLocator, value, pauseS).sendKeys(Keys.TAB);
    }

    /**
     * Binding to type keys into field which needs to be clicked twice for edit mode.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param combinedLocator Both getLocators (click and input) are passed as single String using ||| separator
     * @param value           value to type
     */
    protected void doubleClickToType(final String combinedLocator, final String value) {
        String clickLocator = combinedLocator.substring(0, combinedLocator.indexOf("|||"));
        String typeLocator = combinedLocator.substring(combinedLocator.indexOf("|||") + 1);
        doubleClickToType(clickLocator, typeLocator, value);
    }

    /**
     * Binding to set mark on checkbox which needs to be clicked for edit mode
     *
     * @param clickLocator    locator of field to double click
     * @param checkBoxLocator locator of the checkbox
     */
    protected void doubleClickToCheck(final String clickLocator, final String checkBoxLocator) {
        doubleClick(clickLocator);
        check(checkBoxLocator);
        getElement(checkBoxLocator).sendKeys(Keys.TAB);
    }

    /**
     * Binding to select item in dropdown which needs to be clicked for edit mode.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param clickE        webElement of the field to click
     * @param selectLocator locator of the dropdown
     * @param value         value to select
     */
    protected void clickToSelect(final WebElement clickE, final String selectLocator, final String value) {
        click(clickE);
        selectDropDown(selectLocator, value);
        getElement(selectLocator).sendKeys(Keys.TAB);
    }

    /**
     * Binding to select item in dropdown which needs to be clicked for edit mode.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param clickLocator  locator of the field to click
     * @param selectLocator locator of the dropdown
     * @param value         value to select
     */
    protected void clickToSelect(final String clickLocator, final String selectLocator, final String value) {
        clickToSelect(getElement(clickLocator), selectLocator, value);
    }

    /**
     * Binding to select item in dropdown which needs to be clicked for edit mode after waiting.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param clickLocator  locator of the field to click
     * @param selectLocator locator of the dropdown
     * @param value         value to select
     * @param timeOut       time to wait for element to appear
     */
    protected void clickToSelect(final String clickLocator, final String selectLocator, final String value, int timeOut) {
        click(clickLocator, timeOut);
        selectDropDown(selectLocator, value);
        getElement(selectLocator).sendKeys(Keys.TAB);
    }

    /**
     * Binding to select item in dropdown which needs to be clicked for edit mode.
     * Both getLocators are passed as single String using ||| separator
     *
     * @param combinedLocator both getLocators (to click and select) are passed as single String using ||| separator
     * @param value           value to select
     */
    protected void clickToSelect(final String combinedLocator, final String value) {
        String clickLocator = combinedLocator.substring(0, combinedLocator.indexOf("|||"));
        String selectLocator = combinedLocator.substring(combinedLocator.indexOf("|||") + 1);
        clickToSelect(clickLocator, selectLocator, value);
    }

    /**
     * Binding to select value from dropdown which needs to be clicked for edit mode.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param clickLocator  locator of the field to click
     * @param selectLocator locator of the dropdown
     * @param value         index of the item in dropdown to select (starts from 0)
     */
    protected void clickToSelect(final String clickLocator, final String selectLocator, final Integer value) {
        doubleClick(clickLocator);
        selectDropDown(selectLocator, value);
        getElement(selectLocator).sendKeys(Keys.TAB);
    }

    /**
     * Binding to select value in the dropdown by index which needs to be clicked for edit mode
     *
     * @param combinedLocator both getLocators (to click and select) are passed as single String using ||| separator
     * @param value           value to select
     */
    protected void clickToSelect(final String combinedLocator, final Integer value) {
        String clickLocator = combinedLocator.substring(0, combinedLocator.indexOf("|||"));
        String selectLocator = combinedLocator.substring(combinedLocator.indexOf("|||") + 1);
        clickToSelect(clickLocator, selectLocator, value);
    }

    /**
     * Binding to select item in dropdown which needs to be clicked twice for edit mode after waiting.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param clickE        webElement of the field to click
     * @param selectLocator locator of the dropdown
     * @param value         value to select
     */
    protected void doubleClickToSelect(final WebElement clickE, final String selectLocator, final String value) {
        doubleClick(clickE);
        selectDropDown(selectLocator, value);
        getElement(selectLocator).sendKeys(Keys.TAB);
    }

    /**
     * Binding to select item in dropdown which needs to be clicked twice for edit mode after waiting.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param clickLocator  locator of the field to click
     * @param selectLocator locator of the dropdown
     * @param value         value to select
     */
    protected void doubleClickToSelect(final String clickLocator, final String selectLocator, final String value) {
        doubleClickToSelect(getElement(clickLocator), selectLocator, value);
    }

    /**
     * Binding to select item in dropdown which needs to be clicked twice for edit mode after waiting.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param combinedLocator both getLocators (to click and select) are passed as single String using ||| separator
     * @param value           value to select
     */
    protected void doubleClickToSelect(final String combinedLocator, final String value) {
        String clickLocator = combinedLocator.substring(0, combinedLocator.indexOf("|||"));
        String selectLocator = combinedLocator.substring(combinedLocator.indexOf("|||") + 1);
        doubleClickToSelect(clickLocator, selectLocator, value);
    }

    /**
     * Binding to select value from dropdown which needs to be clicked twice for edit mode.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param clickE        webElement of the field to click
     * @param selectLocator locator of the dropdown
     * @param value         index of the item in dropdown to select (starts from 0)
     */
    protected void doubleClickToSelect(final WebElement clickE, final String selectLocator, final Integer value) {
        doubleClick(clickE);
        selectDropDown(selectLocator, value);
        getElement(selectLocator).sendKeys(Keys.TAB);
    }

    /**
     * Binding to select value from dropdown which needs to be clicked twice for edit mode.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param clickLocator  locator of the field to click
     * @param selectLocator locator of the dropdown
     * @param value         index of the item in dropdown to select (starts from 0)
     */
    protected void doubleClickToSelect(final String clickLocator, final String selectLocator, final Integer value) {
        doubleClickToSelect(getElement(clickLocator), selectLocator, value);
    }

    /**
     * Binding to select value from dropdown which needs to be clicked twice for edit mode.
     * Fills actual value and presses "TAB" to submit, otherwise value could be not saved
     *
     * @param combinedLocator both getLocators (to click and select) are passed as single String using ||| separator
     * @param value           index of the item in dropdown to select (starts from 0)
     */
    protected void doubleClickToSelect(final String combinedLocator, final Integer value) {
        String clickLocator = combinedLocator.substring(0, combinedLocator.indexOf("|||"));
        String selectLocator = combinedLocator.substring(combinedLocator.indexOf("|||") + 1);
        doubleClickToSelect(clickLocator, selectLocator, value);
    }

    /**
     * Drag and drop. Drags departure element to destination and releases.
     *
     * @param fromElement WebElement to drag
     * @param toElement   destination to drag. Releases LMB once over this item.
     */
    protected void dragAndDrop(final WebElement fromElement, final WebElement toElement) {
        new Actions(getDriverInstance())
                .clickAndHold(fromElement)
                .moveToElement(toElement)
                .release(toElement)
                .build().perform();
    }

    /**
     * Drag and drop. Drags departure element to destination with offset and releases.
     *
     * @param fromElement WebElement to drag
     * @param toElement   destination to drag. Releases LMB once over this item.
     * @param xOffset     horizontal offset from destination
     * @param yOffset     vertical offset from destination
     */
    protected void dragAndDrop(final String fromElement, final String toElement,
                               final Integer xOffset, final Integer yOffset) {
        WebElement departure = getElement(fromElement);
        WebElement destination = getElement(toElement);
        new Actions(getDriverInstance())
                .clickAndHold(departure)
                .moveToElement(destination, xOffset, yOffset)
                .moveByOffset(xOffset, yOffset)
                .click().release().build().perform();
    }

    /**
     * Drag and drop. Drags departure element to destination and releases.
     *
     * @param departure   locator of element to drag
     * @param destination locator of element to drag to. Releases LMB once over this item.
     */
    protected void dragAndDrop(final String departure, final String destination) {
        dragAndDrop(getElement(departure), getElement(destination));
    }

    /**
     * Drag and drop. Drags departure element to destination and releases.
     *
     * @param departure   locator of element to drag
     * @param destination locator of element to drag to. Releases LMB once over this item.
     * @param timeOut     time to wait for first element to appear in sec
     */
    protected void dragAndDrop(final String departure, final String destination, int timeOut) {
        dragAndDrop(waitForElement(departure, timeOut), getElement(destination));
    }

    /**
     * Scrolls to the element on a page, e.g. in JS table
     *
     * @param focusLocator xpath to the element
     */
    protected WebElement focusOnElement(final String focusLocator) {
        return focusOnElement(focusLocator, 0);
    }

    /**
     * Scrolls to the element on a page, e.g. in JS table
     *
     * @param focusLocator xpath to the element
     * @param timeout      time to wait for element to be present on the page before scroll
     */
    protected WebElement focusOnElement(final String focusLocator, final int timeout) {
        WebElement focusTarget = waitForElement(focusLocator, timeout);
        try {
            new Actions(getDriverInstance())
                    .moveToElement(focusTarget)
                    .perform();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return focusTarget;
    }

    protected void focusClick(final String focusLocator, final int timeout) {
        WebElement focusTarget = waitForElement(focusLocator, timeout, true);
        try {
            new Actions(getDriverInstance())
                    .moveToElement(focusTarget)
                    .click()
                    .perform();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Presses Tab button
     *
     * @param locator start position, from where will be initiated Tab button action
     */
    protected void pressTab(final String locator) {
        getElement(locator).sendKeys(Keys.TAB);
    }

    /**
     * Binding to submit form
     *
     * @param we webElement one of the input fields or Submit button
     */
    protected void submitForm(final WebElement we) {
        we.submit();
    }

    /**
     * Binding to submit form
     *
     * @param locator locator of Element - one of the input fields or Submit button
     */
    protected void submitForm(final String locator) {
        submitForm(getElement(locator));
    }

    /**
     * Scrolls window horizontally and vertically
     *
     * @param x scroll horizontally by value
     * @param y scroll vertically by value
     */
    protected void scroll(int x, int y) {
        executeJS("window.scrollBy(" + x + ", " + y + ")");
    }

    /**
     * Scrolls window horizontally and vertically to center on the specified element
     *
     * @param locator locator of element to scroll to
     */
    protected WebElement scrollToElement(String locator) {
        WebElement el = getElement(locator);
        Point coordinates = el.getLocation();
        scroll(coordinates.x, coordinates.y);
        return el;
    }

    /**
     * Returns text of element
     *
     * @param we webelement with text
     * @return text of webElement
     */
    protected String getText(final WebElement we) {
        try {
            return we.getText();
        } catch (Exception e) {
            error("Unable to locate element to get text! " + e.getMessage());
            return "";
        }
    }

    /**
     * Returns text of element
     *
     * @param locator locator of element with text
     * @return text of webElement
     */
    protected String getText(final String locator) {
        return getText(getElement(locator));
    }

    /**
     * Returns text of element
     *
     * @param locator locator of element with text
     * @param timeOut time to wait for element to appear
     * @return text of webElement
     */
    protected String getText(final String locator, int timeOut) {
        return getText(waitForElement(locator, timeOut));
    }

    /**
     * Returns text of all elements
     *
     * @param webElements list of web elements with text
     * @return list with text of all web elements
     */
    protected List<String> getTexts(final List<WebElement> webElements) {
        try {
            ArrayList<String> texts = new ArrayList<>();
            webElements.forEach(e -> texts.add(getText(e)));
            return texts;
        } catch (Exception e) {
            error("Unable to locate element to get text! " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns text of all elements found by locator
     *
     * @param locator locator of elements with text
     * @return list with text of all web elements
     */
    protected List<String> getTexts(final String locator) {
        return getTexts(getElements(locator));
    }

    /**
     * Returns text of all elements found by locator
     *
     * @param locator locator of elements with text
     * @param timeOut time to wait for elements to appear
     * @return list with text of all web elements
     */
    protected List<String> getTexts(final String locator, int timeOut) {
        return getTexts(getElements(locator, timeOut));
    }

    /**
     * Get selected option text in the dropdown
     *
     * @param we WebElement of the dropdown
     * @return selected option value
     */
    protected String getDropDownValue(final WebElement we) {
        return getSelect(we).getFirstSelectedOption().getText();
    }

    /**
     * Get selected option text in the dropdown
     *
     * @param locator locator of the dropdown
     * @return selected option value
     */
    protected String getDropDownValue(final String locator) {
        return getDropDownValue(getElement(locator));
    }

    /**
     * Returns list of values in dropdown
     *
     * @param locator locator of dropdown
     * @return List of values in dropdown
     */
    protected List<String> getDropDownValues(final String locator) {
        return getSelect(locator).getOptions().stream().map(this::getText).collect(Collectors.toList());
    }

    /**
     * Returns specified attribute value of element
     *
     * @param we        webelement to access
     * @param attribute attribute to return
     * @return attribute value of element
     */
    protected String getAttr(final WebElement we, final String attribute) {
        return we.getAttribute(attribute);
    }

    /**
     * @param locator   locator of element to grab attribute from
     * @param attribute attribute to get
     * @return attribute value
     */
    protected String getAttr(final String locator, final String attribute) {
        return getAttr(getElement(locator), attribute);
    }

    /**
     * Binding to check Checkbox
     *
     * @param we webElement of checkbox to check
     */
    protected void check(final WebElement we) {
        if (!we.isSelected()) {
            we.click();
        }
    }

    /**
     * Binding to check checkbox
     *
     * @param locator locator of checkbox to check
     */
    protected void check(final String locator) {
        check(getElement(locator));
    }

    /**
     * Binding to check checkbox
     *
     * @param locator locator of checkbox to check
     * @param timeOut timeout to wait for checkbox
     */
    protected void check(final String locator, int timeOut) {
        check(waitForElement(locator, timeOut));
    }

    /**
     * Binding to uncheck all Checkboxes
     *
     * @param locator locator by which all needed checkboxes are accessible
     */
    protected void unCheckAll(final String locator) {
        getElements(locator).forEach(this::unCheck);
    }

    /**
     * Unchecks absolutely all checkboxes on the page (with attribute type='checkbox')
     */
    protected void unCheckAll() {
        unCheckAll("xpath=.//*[@type='checkbox']");
    }

    /**
     * Binding to uncheck checkbox
     *
     * @param we WebElement of checkbox to uncheck
     */
    protected void unCheck(final WebElement we) {
        if (we.isSelected()) {
            we.click();
        }
    }

    /**
     * Binding to uncheck checkbox
     *
     * @param locator locator of checkbox to uncheck
     */
    protected void unCheck(final String locator) {
        unCheck(getElement(locator));
    }

    /**
     * Binding to uncheck checkbox
     *
     * @param locator locator of checkbox to uncheck
     * @param timeOut timeout to wait for checkbox
     */
    protected void unCheck(final String locator, int timeOut) {
        unCheck(waitForElement(locator, timeOut));
    }

    /**
     * Binding to select item in dropdown by value
     *
     * @param we     WebElement of the dropdown
     * @param option value to select in the dropdown
     */
    protected void selectDropDown(final WebElement we, final String option) {
        Select sel = getSelect(we);
        try {
            sel.selectByVisibleText(option);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Binding to select item in dropdown by value
     *
     * @param locator locator of the dropdown
     * @param option  value to select in the dropdown
     */
    protected void selectDropDown(final String locator, final String option) {
        selectDropDown(getElement(locator), option);
    }

    /**
     * Binding to select item in dropdown by value
     *
     * @param locator locator of the dropdown
     * @param option  value to select in the dropdown
     */
    protected void selectDropDown(final String locator, final String option, int timeOut) {
        WebElement select = waitForElement(locator, timeOut);
        selectDropDown(select, option);
    }

    /**
     * Binding to select single item in dropdown by index
     *
     * @param we    WebElement of the dropdown
     * @param index integer index of the element to select (starts from 0)
     */
    protected void selectDropDown(final WebElement we, final Integer index) {
        Select sel = getSelect(we);
        try {
            sel.selectByIndex(index);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Binding to select single option in dropdown by index
     *
     * @param locator locator of the dropdown
     * @param index   integer index of the element to select (starts from 0)
     */
    protected void selectDropDown(final String locator, final Integer index) {
        selectDropDown(getElement(locator), index);
    }

    /**
     * Binding to select items in multi-select dropdown by values
     *
     * @param we      WebElement of the multi-select
     * @param options array of options to select
     */
    protected void selectMultiDropDown(final WebElement we, final String[] options) {
        Select select = getSelect(we);
        select.deselectAll();
        for (String item : options) select.selectByVisibleText(item);
    }

    /**
     * Binding to select items in multi-select dropdown by values
     *
     * @param locator locator of dropdown
     * @param options array of options to select
     */
    protected void selectMultiDropDown(final String locator, final String[] options) {
        selectMultiDropDown(getElement(locator), options);
    }

    /**
     * Binding to select items in multi-select dropdown by indexes
     *
     * @param we      WebElement to access dropdown
     * @param indexes array of indexes of options to select
     */
    protected void selectMultiDropDown(final WebElement we, final Integer[] indexes) {
        Select select = getSelect(we);
        try {
            select.deselectAll();
        } catch (java.lang.UnsupportedOperationException ignored) {
        }

        for (Integer item : indexes) select.selectByIndex(item);
    }

    /**
     * Binding to select items in multi-select dropdown by indexes
     *
     * @param locator locator of the multi-select
     * @param indexes array of indexes of options to select
     */
    protected void selectMultiDropDown(final String locator, final Integer[] indexes) {
        selectMultiDropDown(getElement(locator), indexes);
    }

    /**
     * Binding to select all items in multi-select dropdown
     *
     * @param we webElement of the multi-select
     */
    protected void selectMultiDropDownAll(final WebElement we) {
        Select select = getSelect(we);
        select.deselectAll();
        for (int i = 0; i < select.getOptions().size(); i++)
            select.selectByIndex(i);
    }

    /**
     * Binding to select all items in multi-select dropdown
     *
     * @param locator locator of the multi-select
     */
    protected void selectMultiDropDownAll(final String locator) {
        selectMultiDropDownAll(getElement(locator));
    }

    /**
     * Binding to deselect all items in multi-select dropdown
     *
     * @param we webelement of the multi-select
     */
    protected void deselectMultiDropDownAll(final WebElement we) {
        getSelect(we).deselectAll();
    }

    /**
     * Binding to deselect all items in multi-select dropdown
     *
     * @param locator locator of the multi-select
     */
    protected void deselectMultiDropDownAll(final String locator) {
        deselectMultiDropDownAll(getElement(locator));
    }

    /**
     * Binding to deselect item in dropdown by index (integer)
     *
     * @param we    multi-select  webElement
     * @param index index of option to deselect
     */
    protected void deselectMultiDropDown(final WebElement we, final Integer index) {
        getSelect(we).deselectByIndex(index);
    }

    /**
     * Binding to deselect item in multi-select  by index (integer)
     *
     * @param locator locator of the multi-select
     * @param index   index of option to deselect
     */
    protected void deselectMultiDropDown(final String locator, final Integer index) {
        deselectMultiDropDown(getElement(locator), index);
    }

    /**
     * Binding to deselect item in multi-select  by value (text)
     *
     * @param we     dropdown webElement
     * @param option option to deselect
     */
    protected void deselectMultiDropDown(final WebElement we, final String option) {
        getSelect(we).deselectByVisibleText(option);
    }

    /**
     * Binding to deselect item in multi-select  by value (text)
     *
     * @param locator locator of the dropdown
     * @param option  option to deselect
     */
    protected void deselectMultiDropDown(final String locator, final String option) {
        deselectMultiDropDown(getElement(locator), option);
    }

    /**
     * Wait for visibility/presence of element
     *
     * @param locator    locator of element to wait for
     * @param timeOut    time to wait for element
     * @param notVisible element expected to be visible/present. true=present; false=visible
     * @return returns webElement object on success
     */
    protected WebElement waitForElement(final String locator, int timeOut, boolean notVisible) {
        try {
            WebDriverWait wait = new WebDriverWait(getDriverInstance(), timeOut);
            if (notVisible) wait.until(
                    ExpectedConditions.presenceOfElementLocated(byLocator(locator)));
            else wait.until(
                    ExpectedConditions.visibilityOfElementLocated(byLocator(locator)));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return getElement(locator);
    }

    /**
     * Wait for visibility of element
     *
     * @param locator locator of element to wait for
     * @param timeOut time to wait for element
     * @return returns webElement object on success
     */
    protected WebElement waitForElement(final String locator, int timeOut) {
        return waitForElement(locator, timeOut, false);
    }

    /**
     * Wait for visibility of element with default timeout
     *
     * @param locator locator of element to wait for
     * @return returns webElement object on success
     */
    protected WebElement waitForElement(final String locator) {
        return waitForElement(locator, pauseS, false);
    }

    /**
     * Wait for element to appear and be in editable state
     *
     * @param locator locator of element to wait for
     * @return returns webElement object on success
     */
    protected WebElement waitForClickable(final String locator) {
        return waitForClickable(locator, pauseM);
    }

    /**
     * Wait for element to appear and be in editable state
     *
     * @param locator          locator of element to wait for
     * @param timeOutInSeconds timeout to wait element in seconds
     * @return returns webElement object on success
     */
    protected WebElement waitForClickable(final String locator, long timeOutInSeconds) {
        try {
            new WebDriverWait(getDriverInstance(), timeOutInSeconds)
                    .until(ExpectedConditions.elementToBeClickable(byLocator(locator)));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return getElement(locator);
    }

    /**
     * Soft wait for visibility of element. To verify whether element present with timeout
     *
     * @param locator locator of element to wait for
     * @param timeOut time to wait for element
     * @return true if element is present and visible / false otherwise
     */
    protected boolean waitForElementPresent(final String locator, int timeOut) {
        try {
            new WebDriverWait(getDriverInstance(), timeOut)
                    .until(ExpectedConditions.visibilityOfElementLocated(byLocator(locator)));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Soft wait for visibility of element with default timeout
     *
     * @param locator locator of element to wait for
     * @return true if element is present and visible / false otherwise
     */
    protected boolean waitForElementPresent(final String locator) {
        return waitForElementPresent(locator, pauseL);
    }

    /**
     * Wait until element is invisible/not present on the page
     *
     * @param locator locator to element
     * @param timeOut time to wait
     */
    protected void waitForElementNotPresent(final String locator, int timeOut) {
        try {
            WebDriverWait wait = new WebDriverWait(getDriverInstance(), timeOut);
            wait.until(ExpectedConditions.not(ExpectedConditions.visibilityOfElementLocated(byLocator(locator))));
        } catch (Exception e) {
            //If NoSuchElementException, element is not present, goal achieved. Otherwise throw exception
            if (e.getCause() == null ||
                    !e.getCause().getClass().toString().contains("NoSuchElementException")) {
                fail(e.getMessage());
            }
        }
    }

    /**
     * Wait until element is invisible/not present on the page with default timeout
     *
     * @param locator locator to element
     */
    protected void waitForElementNotPresent(final String locator) {
        waitForElementNotPresent(locator, pauseS);
    }

    /**
     * Waits for alert to appear within specified timeout
     *
     * @param timeOut time to wait for alert to appear
     */
    protected void waitForAlert(int timeOut) {
        try {
            WebDriverWait wait = new WebDriverWait(getDriverInstance(), timeOut);
            wait.until(ExpectedConditions.alertIsPresent());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Verifies if alert is present on the page
     *
     * @param timeOut time to wait for alert to appear
     */
    protected boolean isAlertPresent(int timeOut) {
        try {
            WebDriverWait wait = new WebDriverWait(getDriverInstance(), timeOut);
            wait.until(ExpectedConditions.alertIsPresent());
            return true;
        } catch (Exception e) {
            return false;
        }
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
                    error("New window won't appear after waiting for " + timeOut + " sec(s)");
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
     * Verifies whether element is displayed
     *
     * @param we webelement to verify
     * @return true if present; false otherwise
     */
    protected boolean isElementPresent(final WebElement we) {
        try {
            return we.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifies whether element is present and displayed
     *
     * @param locator locator for element to verify
     * @return true if present; false otherwise
     */
    protected boolean isElementPresent(final String locator) {
        try {
            return isElementPresent(getElement(locator, false));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns window handle
     *
     * @return current window handle
     */
    protected String getCurrentWindowHandle() {
        return getDriverInstance().getWindowHandle();
    }

    /**
     * Returns all windows handles
     *
     * @return all windows handles currently accessible by webdriver instance
     */
    protected Set<String> getCurrentWindowsHandles() {
        return getDriverInstance().getWindowHandles();
    }

    /**
     * Switches to window with specified handle
     *
     * @param windowHandle handle of the window to switch to
     */
    protected String switchToWindow(String windowHandle) {
        getDriverInstance().switchTo().window(windowHandle);
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
        if (waitForWindow(oldWindows, pauseL))
            return switchToNewWindow(oldWindows, getCurrentWindowsHandles());
        else {
            fail("Unable to locate new window after waiting for " + pauseL + " sec(s)");
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
     * Close all windows except that with provided handle
     *
     * @param exceptHandle handle to window to leave opened
     */
    protected String closeWindows(String exceptHandle) {
        Set<String> windows = getCurrentWindowsHandles();
        windows.remove(exceptHandle);
        for (String window : windows) {
            switchToWindow(window);
            getDriverInstance().close();
        }
        return switchToWindow(exceptHandle);
    }

    /**
     * Pause test for <pause> seconds
     * Avoid using it unless it's really unavoidable
     * Suggesting usage of waitForElement/waitForElementNotPresent/isElementPresent etc. instead
     *
     * @param pause time to wait in seconds
     */
    protected void sleep(int pause) {
        try {
            Thread.sleep(pause * 1000);
        } catch (InterruptedException ignored) {
        }
    }

//======================================================================================================================
//Assertions/verifications

    /**
     * Verifies whether specified element is present on the page, fails test if not and continues running
     *
     * @param locator locator of the element to verify
     * @param message error message to log, if element is not present
     * @param timeOut time to wait for element to be present
     * @return true if present
     */
    protected boolean verifyElementPresent(String locator, String message, int timeOut) {
        boolean result = false;
        try {
            result = assertElementPresent(locator, message, timeOut);
        } catch (Throwable e) {
            error(e.getMessage());
        }
        return result;
    }

    /**
     * Verifies whether specified element is present on the page, fails test if not and continues running
     *
     * @param locator locator of the element to verify
     * @param message error message to log, if element is not present
     * @return true if present
     */
    protected boolean verifyElementPresent(String locator, String message) {
        return verifyElementPresent(locator, message, 0);
    }

    /**
     * Verifies whether specified element is present on the page, fails test if not and stops current test
     *
     * @param locator locator of the element to verify
     * @param message error message to log, if element is not present
     * @param timeOut time in seconds to wait for the element to be present
     * @return true if present
     */
    protected boolean assertElementPresent(String locator, String message, int timeOut) {
        boolean result = timeOut > 0 ? waitForElementPresent(locator, timeOut) : isElementPresent(locator);
        if (!result) {
            fail(String.format("%s\nElement: %s is not present on the page after %s sec(s)\n\n", message, locator, timeOut));
        }
        return result;
    }

    /**
     * Verifies whether specified element is present on the page, fails test if not and stops current test
     *
     * @param locator locator of the element to verify
     * @param message error message to log, if element is not present
     * @return true if present
     */
    protected boolean assertElementPresent(String locator, String message) {
        return assertElementPresent(locator, message, 0);
    }

    /**
     * Verifies whether specified element is not present on the page, fails test if it is and continues running
     *
     * @param locator locator of the element to verify
     * @param message error message to log, if element is present
     * @param timeOut time to wait for element to disappear
     * @return true if not present
     */
    protected boolean verifyElementNotPresent(String locator, String message, int timeOut) {
        try {
            waitForElementNotPresent(locator, timeOut);
            return true;
        } catch (AssertionError e) {
            error(String.format("%s\nElement: %s is present on the page after %s sec(s)\n\n", message, locator, timeOut));
        }
        return false;
    }

    /**
     * Verifies whether condition is true
     * Marks test as failed and stops test running if condition is false
     *
     * @param condition boolean condition to verify
     */
    protected void assertTrue(boolean condition) {
        try {
            Assert.assertTrue(condition);
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    /**
     * Verifies whether condition is true
     * Marks test as failed and stops test running if condition is false
     *
     * @param condition boolean condition to verify
     * @param message   error message to print on failure
     */
    protected void assertTrue(boolean condition, String message) {
        try {
            Assert.assertTrue(condition, message);
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    /**
     * Verifies whether condition is false
     * Marks test as failed and stops test running if condition is true
     *
     * @param condition boolean condition to verify
     */
    protected void assertFalse(boolean condition) {
        try {
            Assert.assertFalse(condition);
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    /**
     * Verifies whether condition is false
     * Marks test as failed and stops test running if condition is true
     *
     * @param condition boolean condition to verify
     * @param message   error message to print on failure
     */
    protected void assertFalse(boolean condition, String message) {
        try {
            Assert.assertFalse(condition, message);
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    /**
     * Verifies whether two objects are the same.
     * Marks test as failed and stops test running if objects differ
     *
     * @param actual   Object to compare
     * @param expected Object to compare to
     */
    protected void assertEquals(Object actual, Object expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    /**
     * Verifies whether two objects are the same.
     * Marks test as failed and stops test running if objects differ
     *
     * @param actual   Object to compare
     * @param expected Object to compare to
     * @param message  error message to print on failure
     */
    protected void assertEquals(Object actual, Object expected, String message) {
        try {
            Assert.assertEquals(actual, expected, message);
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    /**
     * Verifies whether two objects are not the same and marks test as failed if objects same
     *
     * @param actual   Object to compare
     * @param expected Object to compare to
     */
    protected void assertNotEquals(Object actual, Object expected) {
        try {
            Assert.assertNotEquals(actual, expected);
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    /**
     * Verifies whether two objects are not the same and marks test as failed if objects same
     *
     * @param actual   Object to compare
     * @param expected Object to compare to
     * @param message  error message to print on failure
     */
    protected void assertNotEquals(Object actual, Object expected, String message) {
        try {
            Assert.assertNotEquals(actual, expected, message);
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    /**
     * Verifies whether two objects are the same and marks test as failed if objects differ
     *
     * @param actual   Object to compare
     * @param expected Object to compare to
     */
    protected void verifyEquals(Object actual, Object expected) {
        try {
            Assert.assertEquals(actual, expected);
        } catch (Throwable e) {
            error(e.getMessage());
        }
    }

    /**
     * Verifies whether two objects are the same and marks test as failed if objects differ
     *
     * @param actual   Object to compare
     * @param expected Object to compare to
     * @param message  error message to print on failure
     */
    protected void verifyEquals(Object actual, Object expected, String message) {
        try {
            Assert.assertEquals(actual, expected, message);
        } catch (Throwable e) {
            error(e.getMessage());
        }
    }

    /**
     * Verifies whether two objects are not the same and marks test as failed if objects same
     *
     * @param actual   Object to compare
     * @param expected Object to compare to
     */
    protected boolean verifyNotEquals(Object actual, Object expected) {
        try {
            Assert.assertNotEquals(actual, expected);
        } catch (Throwable e) {
            error(e.getMessage());
        }
        return false;
    }

    /**
     * Verifies whether two objects are not the same and marks test as failed if objects same
     *
     * @param actual   Object to compare
     * @param expected Object to compare to
     * @param message  error message to print on failure
     */
    protected void verifyNotEquals(Object actual, Object expected, String message) {
        try {
            Assert.assertNotEquals(actual, expected, message);
        } catch (Throwable e) {
            error(e.getMessage());
        }
    }

    /**
     * Verifies whether condition is true and marks test as failed if condition is false
     *
     * @param condition boolean condition to verify
     */
    protected void verifyTrue(boolean condition) {
        try {
            Assert.assertTrue(condition);
        } catch (Throwable e) {
            error(e.getMessage());
        }
    }

    /**
     * Verifies whether condition is true and marks test as failed if condition is false
     *
     * @param condition boolean condition to verify
     * @param message   error message to print on failure
     */
    protected void verifyTrue(boolean condition, String message) {
        try {
            Assert.assertTrue(condition, message);
        } catch (Throwable e) {
            error(e.getMessage());
        }
    }

    /**
     * Verifies whether condition is false and marks test as failed if condition is true
     *
     * @param condition boolean condition to verify
     */
    protected void verifyFalse(boolean condition) {
        try {
            Assert.assertFalse(condition);
        } catch (Throwable e) {
            error(e.getMessage());
        }
    }

    /**
     * Verifies whether condition is false and marks test as failed if condition is true
     *
     * @param condition boolean condition to verify
     * @param message   error message to print on failure
     */
    protected void verifyFalse(boolean condition, String message) {
        try {
            Assert.assertFalse(condition, message);
        } catch (Throwable e) {
            error(e.getMessage());
        }
    }

    /**
     * Verifies whether string contains substring and marks test as failed if not
     *
     * @param haystack string to contain substring
     * @param needle   substring to verify
     */
    protected void verifyContains(String haystack, String needle) {
        verifyContains(haystack, needle, "");
    }

    /**
     * Verifies whether string contains substring and marks test as failed if not
     *
     * @param haystack string to contain substring
     * @param needle   substring to verify
     * @param message  error message to print if haystack doesn't contain needle
     */
    protected void verifyContains(String haystack, String needle, String message) {
        if (!haystack.contains(needle)) {
            error(String.format("%s\nString: %s\nExpected to contain: %s\n\n", message, haystack, needle));
        }
    }

    /**
     * Verifies whether string contains substring and marks test as failed if not
     *
     * @param haystack string to contain substring
     * @param needle   substring to verify
     */
    protected void assertContains(String haystack, String needle) {
        assertContains(haystack, needle, "");
    }

    /**
     * Verifies whether string contains substring and marks test as failed if not
     *
     * @param haystack string to contain substring
     * @param needle   substring to verify
     * @param message  error message to print if haystack doesn't contain needle
     */
    protected void assertContains(String haystack, String needle, String message) {
        try {
            verifyContains(haystack, needle, message);
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    /**
     * Verifies whether string does not contain substring and marks test as failed if it does
     *
     * @param haystack string not to contain substring
     * @param needle   substring to verify
     */
    protected void verifyNotContains(String haystack, String needle) {
        verifyNotContains(haystack, needle, "");
    }

    /**
     * Verifies whether string does not contain substring and marks test as failed if it does
     *
     * @param haystack string not to contain substring
     * @param needle   substring to verify
     * @param message  error message to print if haystack contains needle
     */
    protected void verifyNotContains(String haystack, String needle, String message) {
        if (haystack.contains(needle)) {
            error(String.format("%s\nString: %s\nExpected not to contain: %s\n\n", message, haystack, needle));
        }
    }

    /**
     * Verifies whether string does not contain substring and marks test as failed if it does
     *
     * @param haystack string not to contain substring
     * @param needle   substring to verify
     */
    protected void assertNotContains(String haystack, String needle) {
        assertNotContains(haystack, needle, "");
    }

    /**
     * Verifies whether string does not contain substring and marks test as failed if it does
     *
     * @param haystack string not to contain substring
     * @param needle   substring to verify
     * @param message  error message to print if haystack contains needle
     */
    protected void assertNotContains(String haystack, String needle, String message) {
        try {
            verifyNotContains(haystack, needle, message);
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    /**
     * Verifies whether tested list is sorted in specified order
     *
     * @param testedList list to be verified
     * @param asc        true for ascending / false for descending
     */
    protected void verifySorted(List<String> testedList, boolean asc) {
        List<String> sortedList = new ArrayList<>(testedList);
        if (asc) {
            Collections.sort(sortedList);
        } else {
            Collections.sort(sortedList, Collections.reverseOrder());
        }
        verifyEquals(testedList, sortedList,
                String.format("Sorting in asc: %s order is incorrect!", asc));
    }

    /**
     * Marks test as failed and stops test running with the provided message
     *
     * @param message error message
     */
    protected void fail(String message) {
        fail(message, true);
    }

    protected void fail(String message, boolean takeScreenshot) {
        Assert.fail(processMessage(message, takeScreenshot));
    }

    /**
     * Fails a test with the given message and proceeds with the test starting with next step
     *
     * @param message the assertion error message
     */
    protected void error(String message) {
        error(message, true);
    }

    protected void error(String message, boolean takeScreenshot) {
        throw new SoftAssertionError(processMessage(message, takeScreenshot));
    }

    private String processMessage(String message, boolean takeScreenshot) {
        String screenshotPath = message.contains("SCREENSHOT:") || !takeScreenshot ? "" : "\n\n" + takeScreenshot();
        String resultMessage = message.concat(screenshotPath);
        return addSessionInfo(resultMessage);
    }

    private String addSessionInfo(String message) {
        return message.contains("Session info:") ? message :
                String.format("%s\n\n********\nSession info:\nThread ID: %s\nUser: %s\n********\n\n",
                        message, Thread.currentThread().getId(), session.getUser());
    }

}
