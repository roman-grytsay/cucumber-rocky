package test.java.framework;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.apache.commons.lang3.SystemUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.Assert;
import test.java.framework.helpers.CommonHelper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public abstract class ManagerPrototype {

//======================================================================================================================
//Properties

    //Flag for loading properties
    private static Boolean isFirstLoad = true;

    private static final Object propertiesLock = new Object();

    private SessionPrototype session;
    private static HashMap<Thread, SessionPrototype> sessions = new HashMap<>();

    //Number of tries to launch browser
    private int tries = 0;

    protected static final String HTTP = "http://";
    protected static final String HUB_PART_URL = "/wd/hub";

//======================================================================================================================
//Setters, getters

    /**
     * Get current session instance
     *
     * @return session instance
     */
    @SuppressWarnings("unchecked")
    public static <T extends SessionPrototype> T getCurrentSession() {
        return (T) sessions.get(Thread.currentThread());
    }

    /**
     * Set current session instance
     *
     * @return session instance
     */
    public static SessionPrototype setCurrentSession(SessionPrototype session) {
        return sessions.put(Thread.currentThread(), session);
    }

    /**
     * Has to be overridden to return Session. Session has to extend SessionPrototype
     *
     * @return new instance of Session
     */
    public abstract SessionPrototype getNewSession();

    public void setSession(SessionPrototype session) {
        this.session = session;
    }

    public SessionPrototype getSession() {
        return this.session;
    }

//======================================================================================================================
//Setting up, reading config, etc

    /**
     * Sets up the environment:
     * Reads and loads parameters from Config.parameters file;
     * Initiates WebDriver session;
     * Sets screenshot directory name for current Test Case;
     * Clears ScreenShots dir if local machine;
     *
     * @return loaded and set instance of session
     */
    public SessionPrototype setUp() {
        setUpNoBrowser();

        //Init WebDriver session
        startDriver();
        return session;
    }

    /**
     * Sets up the environment:
     * Reads and loads parameters from Config.parameters file;
     * Sets screenshot directory name for current Test Case;
     * Clears ScreenShots dir if local machine;
     *
     * @return loaded and set instance of session
     */
    public SessionPrototype setUpNoBrowser() {
        session = getNewSession();
        //All threads wait for properties to load
        loadProperties();

        //Init WebDriver session
        setCurrentSession(session);
        return session;
    }

    public void startDriver() {
        try {
            launchDriver();
        } catch (MalformedURLException e) {
            fail("Domain name / URL provided is invalid!\n".concat(e.getMessage()));
        }
    }

    /**
     * Login to all possible HTTP/HTTPS combinations that will be used throughout tests. Basic auth.
     * Providing login credentials with each request during test will not work, since some pages redirect HTTP > HTTPS
     * and authentication is required again at this point
     */
    protected abstract void authenticate();

    /**
     * Method to be used inside {@link #authenticate()}
     * Call this method within {@link #authenticate()} to log in (basic auth)
     * to all urls that will require login during test
     *
     * @param url fully qualified URL with credentials, e.g.
     */
    protected void authenticate(String url) {
        try {
            session.getDriverInstance().get(url);
        } catch (Throwable e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            fail(String.format("Failed to authenticate url: %s\n\n%s\n\n%s",
                    url, e.getMessage(), errors.toString()));
        }
    }

    /**
     * If application requires user to have some properties during scenarios, set it up here, store in Session
     */
    public abstract void setupSessionUser();

    /**
     * Loads Session for running tests from Config.Properties file
     */
    public void loadProperties() {
        synchronized (propertiesLock) {
            if (!isFirstLoad) {
                return;
            }
            isFirstLoad = false;

            Properties props;
            try {
                props = CommonHelper.readConfig();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
                return;
            }

            String clientName = CommonHelper.getProperty(props, "client", "ff");
            try {
                SessionPrototype.setClient(clientName);
            } catch (Exception e) {
                String errMsg = "Incorrect client value specified in the config file: '%s'\n" +
                        "Should be one of these: %s\n\n";
                fail(String.format(errMsg, clientName, Arrays.asList(Client.values())) + e.getMessage());
            }

            SessionPrototype.setUsersCount(Integer.valueOf(CommonHelper.getProperty(props, "usersCount", "1")));
            SessionPrototype.setDebug(Boolean.valueOf(CommonHelper.getProperty(props, "debug", "false")));
            SessionPrototype.setLanguage(CommonHelper.getProperty(props, "language"));

            SessionPrototype.setSeleniumHub(CommonHelper.getProperty(props, "seleniumHub"));
            SessionPrototype.setGrid(Boolean.valueOf(CommonHelper.getProperty(props, "grid", "false")));
            SessionPrototype.setProjectPath(CommonHelper.getProperty(props, "jenkinsProjectPath"));

            String appiumServer = SessionPrototype.isGrid() ?
                    SessionPrototype.getSeleniumHub() : CommonHelper.getProperty(props, "appiumServer");
            SessionPrototype.setAppiumServer(HTTP + appiumServer + HUB_PART_URL);

            SessionPrototype.setPathToApp(CommonHelper.getProperty(props, "pathToApp"));
            SessionPrototype.setClientOSVersion(CommonHelper.getProperty(props, "version"));
            SessionPrototype.setAppActivity(CommonHelper.getProperty(props, "appActivity"));
            SessionPrototype.setAppPackage(CommonHelper.getProperty(props, "appPackage"));

            Bindings.pauseL = Integer.valueOf(CommonHelper.getProperty(props, "pauseLong"));
            Bindings.pauseM = Integer.valueOf(CommonHelper.getProperty(props, "pauseMedium"));
            Bindings.pauseS = Integer.valueOf(CommonHelper.getProperty(props, "pauseShort"));

            loadAdditionalProperties(props);

            if (SessionPrototype.isDebug()) {
                CommonHelper.clearScreenShotsDir();
                CommonHelper.clearDownloadsDir();
            }
        }
    }

    protected abstract void loadAdditionalProperties(Properties properties);

    /**
     * Starts specified in config browser, maximizes window
     */
    protected void launchDriver() throws MalformedURLException {

        switch (SessionPrototype.getClient()) {
            case IE:
                startInternetExplorer();
                break;
            case FF:
                startFirefox();
                break;
            case GC:
                startChrome();
                break;
            case IOS:
                startIOS();
                break;
            case ANDROID:
                startAndroid();
                break;
        }
        if (SessionPrototype.isMobile()) {
            configureMobileSession();
        } else {
            configureDesktopSession();
        }
    }

    protected abstract void configureMobileSession();

    protected void configureDesktopSession() {
        session.getDriverInstance().manage().timeouts().pageLoadTimeout(2, TimeUnit.MINUTES);

        if (SessionPrototype.isGrid() || !SystemUtils.IS_OS_LINUX) {
            session.getDriverInstance().manage().window().maximize();
        }
        authenticate();
    }

    /**
     * Launches Internet Explorer natively or on the remote machine according to session
     */
    protected void startInternetExplorer() throws MalformedURLException {
        if (SessionPrototype.isGrid()) {
            DesiredCapabilities cap = DesiredCapabilities.internetExplorer();
            session.setDriverInstance(new RemoteWebDriver(new URL(HTTP + SessionPrototype.getSeleniumHub() + HUB_PART_URL), cap));
        } else {
            session.setDriverInstance(new InternetExplorerDriver());
        }
    }

    /**
     * Launches Internet Explorer natively or on the remote machine according to session
     */
    protected void startChrome() throws MalformedURLException {
        if (SessionPrototype.isGrid()) {
            DesiredCapabilities cap = DesiredCapabilities.chrome();
            session.setDriverInstance(new RemoteWebDriver(new URL(HTTP + SessionPrototype.getSeleniumHub() + HUB_PART_URL), cap));
        } else {
            session.setDriverInstance(new ChromeDriver());
        }
        session.getDriverInstance().manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
    }

    /**
     * Launches Firefox natively or on the remote machine according to session
     */
    protected void startFirefox() throws MalformedURLException {

        FirefoxProfile profile = null;
        try {
            profile = buildFirefoxProfile();
        } catch (Exception e) {
            fail("Failed to create Firefox profile!\n" + e.getMessage());
        }
        try {
            if (SessionPrototype.isGrid()) {

                DesiredCapabilities cap = DesiredCapabilities.firefox();
                cap.setCapability(FirefoxDriver.PROFILE, profile);

                session.setDriverInstance(new RemoteWebDriver(new URL(HTTP + SessionPrototype.getSeleniumHub() + HUB_PART_URL), cap));

            } else if (SystemUtils.IS_OS_LINUX) {

                String xPort = CommonHelper.runXvfb();
                final File firefoxPath = new File(System.getProperty(
                        "lmportal.deploy.firefox.path", "/usr/bin/firefox"));

                FirefoxBinary fb = new FirefoxBinary(firefoxPath);
                fb.setTimeout(java.util.concurrent.TimeUnit.SECONDS.toMillis(90));
                fb.setEnvironmentProperty("DISPLAY", xPort);

                session.setDriverInstance(new FirefoxDriver(fb, profile));
                session.getDriverInstance().manage().window().setSize(new Dimension(1920, 1080));

            } else {
                session.setDriverInstance(new FirefoxDriver(profile));
            }
        } catch (WebDriverException e) {
            if (++tries < 3) {
                startFirefox();
            } else {
                fail("Failed to launch browser after " + tries + " tries! " + e.getMessage());
            }
        } catch (IOException e) {
            fail("Failed to launch browser!" + e.getMessage());
        }
    }

    protected FirefoxProfile buildFirefoxProfile() throws IOException {

        FirefoxProfile profile = new FirefoxProfile();

        profile.setEnableNativeEvents(false);
        profile.setPreference("dom.successive_dialog_time_limit", 0);
        profile.setPreference("dom.popup_maximum", 200000);
        profile.setPreference("network.automatic-ntlm-auth.allow-non-fqdn", true);
        profile.setPreference("network.ntlm.send-lm-response", true);
        profile.setPreference("browser.download.dir", CommonHelper.makeDownloadsDir());
        profile.setPreference("browser.download.lastDir", CommonHelper.makeDownloadsDir());
        profile.setPreference("browser.download.folderList", 2);
        profile.setPreference("browser.download.useDownloadDir", true);
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/octet-stream");
        profile.setPreference("browser.download.manager.showWhenStarting", false);
        profile.setPreference("pdfjs.disabled", true);
        profile.setPreference("network.automatic-ntlm-auth.trusted-uris", allowedFirefoxServers());

        return profile;
    }

    /**
     * Specify domains that require basic authorization
     *
     * @return list of domains. Comma separated.
     */
    protected abstract String allowedFirefoxServers();

    protected void startIOS() throws MalformedURLException {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(CapabilityType.BROWSER_NAME, "iOS");
        capabilities.setCapability(CapabilityType.VERSION, SessionPrototype.getClientOSVersion());
        capabilities.setCapability("app", SessionPrototype.getPathToApp());
        session.setDriverInstance(new IOSDriver(new URL(SessionPrototype.getAppiumServer()), capabilities));
    }

    protected void startAndroid() throws MalformedURLException {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Android");
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, Platform.ANDROID);
        capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, SessionPrototype.getClientOSVersion());
        capabilities.setCapability(MobileCapabilityType.APP, SessionPrototype.getPathToApp());
        capabilities.setCapability(MobileCapabilityType.APP_ACTIVITY, SessionPrototype.getAppActivity());
        capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "Appium");
        capabilities.setCapability("platform", "LINUX");
        capabilities.setCapability("browserName", "Android");
        capabilities.setCapability("noReset", "true");
        session.setDriverInstance(new AndroidDriver(new URL(SessionPrototype.getAppiumServer()), capabilities));
    }

    /**
     * Close specified WebDriver instance
     *
     * @param instance session instance
     */
    public void shutDown(SessionPrototype instance) {
        instance.getDriverInstance().quit();
        instance.setDriverInstance(null);
    }

    /**
     * Closes current WebDriver instance
     */
    public void shutDown() {
        shutDown(session);
    }

    /**
     * Make user available for another test
     */
    public abstract void saveUserFromSession();

    /**
     * Clears all cookies in browser
     */
    public void clearCookies() {
        session.getDriverInstance().manage().deleteAllCookies();
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

    /**
     * Marks test as failed and stops test running with the provided message
     *
     * @param errorMessage error message to throw
     */
    protected void fail(String errorMessage) {
        Assert.fail(errorMessage);
    }

}
