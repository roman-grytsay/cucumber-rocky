package test.java.framework.mobile;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.ScreenOrientation;
import test.java.framework.Bindings;
import test.java.framework.SessionPrototype;

import java.util.HashMap;

/**
 * Wrappers for webDriver/appium methods applicable for mobile devices only
 */
public abstract class MobileBindings extends Bindings {

    public MobileBindings(SessionPrototype instance) {
        super(instance);
    }

    @Override
    protected AppiumDriver getDriverInstance() {
        return (AppiumDriver) super.getDriverInstance();
    }

    /**
     * Performs screen re-orientation on the device
     *
     * @return device orientation to set
     */
    protected ScreenOrientation setOrientation(ScreenOrientation orientation) {
        try {
            getDriverInstance().rotate(orientation);
        } catch (Exception e) {
            fail("Unable to change device orientation! " + e.getMessage());
        }
        return orientation;
    }

    /**
     * Gets current orientation of the device
     *
     * @return device orientation
     */
    protected ScreenOrientation getOrientation() {
        try {
            return getDriverInstance().getOrientation();
        } catch (Exception e) {
            fail("Unable to get device orientation! " + e.getMessage());
            return null;
        }
    }

    /**
     * Taps on the screen at specified location
     *
     * @param x horizontal element position
     * @param y vertical element position
     */
    protected void tap(final double x, final double y) {
        executeJS("mobile: tap",
                new HashMap<String, Double>() {{
                    put("tapCount", (double) 1);
                    put("touchCount", (double) 1);
                    put("duration", 0.5);
                    put("x", x);
                    put("y", y);
                }}
        );
    }

    protected void scrollToMobileElement(String element, String text) {
        HashMap<String, String> scrollObject = new HashMap<>();
        scrollObject.put("text", text);
        scrollObject.put("direction", "down");
        scrollObject.put("element", element);
        getDriverInstance().executeScript("mobile: scrollTo", scrollObject);
    }

    protected void scrollMobileElement(String el) {
        HashMap<String, String> scrollObj = new HashMap<>();
        scrollObj.put("element", el);
        scrollObj.put("direction", "up");
        getDriverInstance().executeScript("mobile: scroll", scrollObj);
    }

    protected void swipe(final int startX, final int startY, final int endX, final int endY, final int duration) {
        try {
            getDriverInstance().swipe(startX, startY, endX, endY, duration);
        } catch (Exception e) {
            fail("Swiping failed" + e.getMessage());
        }
    }

    /**
     * Method clicks back button and check whether page refreshes
     * If page is not refreshed - method waits and re-checks in 1 second until timeout expires
     * Exception thrown if timeout expired and page is not refreshed
     */
    protected void goBack() {
        String sourceBefore = getSource();
        getDriverInstance().navigate().back();
        String sourceAfter = getSource();
        if (sourceBefore.equals(sourceAfter)) {
            goBack();
        }
    }

    protected void launchApp() {
        getDriverInstance().launchApp();
    }

    protected void closeApp() {
        getDriverInstance().closeApp();
    }

    protected void resetApp() {
        getDriverInstance().resetApp();
    }

    protected int getScreenWidth() {
        return getDriverInstance().manage().window().getSize().getWidth();
    }

    protected int getScreenHeight() {
        return getDriverInstance().manage().window().getSize().getHeight();
    }

    protected void tap(final String locator) {
        click(locator);
    }

    protected void hideKeyboard() {
        try {
            getDriverInstance().hideKeyboard();
        } catch (Exception ignored) {
        }
    }

    protected void sendKeyFromKeyboard(int keyNumber) {
        getDriverInstance().sendKeyEvent(keyNumber);
    }

    protected void clearText(String el) {
        getElement(el).clear();
    }

    protected void switchToWebView() {
        getDriverInstance().context("WEBVIEW_com.carmudi.android.debug.release");

    }

    protected void switchToNativeView() {
        getDriverInstance().context("NATIVE_APP");

    }

    protected String getCurrentContext() {
        return getDriverInstance().getContext();
    }

    public void scrollScreenTillElementNotPresent(String control) {
        int count = 0;
        while (!waitForElementPresent(control, pauseS) & count < 5) {
            swipe(getScreenWidth() / 2, getScreenHeight() / 2, getScreenWidth() / 2, (getScreenHeight() / 2) - 150, 0);
            count++;
        }
    }
}
