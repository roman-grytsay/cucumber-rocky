package test.java.framework;

import org.openqa.selenium.WebDriver;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.ResourceBundle;

public abstract class SessionPrototype {

//======================================================================================================================
//Config file properties (aka default)

    private static Boolean debug = false;
    private static Integer usersCount = 1;
    private static Boolean grid = false;
    private static Boolean mobile = false;
    private static Client client;

    private static String projectPath;

    private static String seleniumHub;

    private static String appiumServer;
    private static String pathToApp;
    private static String clientOSVersion;
    private static String appActivity;
    private static String appPackage;

    private static String language;

    private static ResourceBundle webLanguageRB;
    private static ResourceBundle mobileLanguageRB;

//======================================================================================================================
//Session properties

    private WebDriver driver;
    private User user = null;

//======================================================================================================================
//Package local getters/setters

    WebDriver setDriverInstance(WebDriver driver) {
        return this.driver = driver;
    }

    WebDriver getDriverInstance() {
        return this.driver;
    }

//======================================================================================================================
//Config file getters/setters

    /**
     * Set language of the application to verify messages in specified language
     *
     * @param language 2 letter language code, e.g. "en" for English
     */
    public static void setLanguage(String language) {
        SessionPrototype.language = language;

        //For indonesian Bahasa language, (ISO language code id). Java uses outdated ISO code: in
        String localeLang = language.equals("ba") || language.equals("id") ? "in" : language;
        Locale locale = new Locale(localeLang);

        // Resource bundles based on property files
        try {
            webLanguageRB = ResourceBundle.getBundle("translations.web.language", locale);
            mobileLanguageRB = ResourceBundle.getBundle("translations.mobile.language", locale);
        } catch (java.util.MissingResourceException ignored) {
            if (debug) {
                System.out.println("No translations file for " + localeLang);
            }
        }
    }

    public static String getLanguage() {
        return language;
    }

    public static String getAppiumServer() {
        return appiumServer;
    }

    public static void setAppiumServer(String appiumServer) {
        SessionPrototype.appiumServer = appiumServer;
    }

    public static String getPathToApp() {
        return pathToApp;
    }

    public static void setPathToApp(String pathToApp) {
        SessionPrototype.pathToApp = pathToApp;
    }

    public static String getClientOSVersion() {
        return clientOSVersion;
    }

    public static void setClientOSVersion(String clientOSVersion) {
        SessionPrototype.clientOSVersion = clientOSVersion;
    }

    public static String getAppActivity() {
        return appActivity;
    }

    public static void setAppActivity(String appActivity) {
        SessionPrototype.appActivity = appActivity;
    }

    public static void setAppPackage(String appPackage) {
        SessionPrototype.appPackage = appPackage;
    }

    public static String getAppPackage() {
        return appPackage;
    }

    public static String setProjectPath(String projectPath) {
        return SessionPrototype.projectPath = projectPath;
    }

    public static String getProjectPath() {
        return SessionPrototype.projectPath;
    }

    public static String setSeleniumHub(String seleniumHub) {
        return SessionPrototype.seleniumHub = seleniumHub;
    }

    public static String getSeleniumHub() {
        return seleniumHub;
    }

    public static Boolean setGrid(Boolean grid) {
        return SessionPrototype.grid = grid;
    }

    public static Boolean isGrid() {
        return SessionPrototype.grid;
    }

    public static Boolean setMobile(Boolean mobile) {
        return SessionPrototype.mobile = mobile;
    }

    public static Boolean isMobile() {
        return SessionPrototype.mobile;
    }

    public static Integer setUsersCount(Integer usersCount) {
        return SessionPrototype.usersCount = usersCount;
    }

    public static Integer getUsersCount() {
        return usersCount;
    }

    public static Boolean setDebug(Boolean isDebugMode) {
        return debug = isDebugMode;
    }

    public static Boolean isDebug() {
        return debug;
    }

    public static Client getClient() {
        return SessionPrototype.client;
    }

    public static Client setClient(String clientName) throws IllegalArgumentException {
        client = Client.valueOf(clientName.toUpperCase());
        switch (client) {
            case IOS:
                setMobile(true);
                break;
            case ANDROID:
                setMobile(true);
                break;
            default:
                setMobile(false);
        }
        return SessionPrototype.client;
    }

    public static String getTranslation(LangPrototype key) {
        if (webLanguageRB == null) return null;
        try {
            return new String(webLanguageRB.getString(key.name()).getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println(e.getMessage());
            return webLanguageRB.getString(key.name());
        }
    }

    public static String getTranslationMobile(LangPrototype key) {
        if (mobileLanguageRB == null) return null;
        return mobileLanguageRB.getString(key.name());
    }

//======================================================================================================================
//Session getters/setters

    public User setUser(User user) {
        return this.user = user;
    }

    public User getUser() {
        return this.user;
    }

    public boolean isDriverReady() {
        return this.driver != null;
    }

}
