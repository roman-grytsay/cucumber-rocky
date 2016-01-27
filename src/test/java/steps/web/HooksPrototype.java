package test.java.steps.web;

import test.java.framework.ManagerPrototype;
import test.java.framework.SessionPrototype;

public abstract class HooksPrototype {

    protected boolean noDriver = false;
    protected boolean noUser = false;
    protected SessionPrototype session;

    protected abstract ManagerPrototype getManager();

    public void beforeNoSession() {
        noDriver = true;
    }

    public void beforeNoUser() {
        noUser = true;
    }

    public void beforeScenario() {

        //Create new session or re-use existing one
        SessionPrototype currentSession = ManagerPrototype.getCurrentSession();
        if (currentSession == null) {
            session = getManager().setUpNoBrowser();
        } else {
            session = currentSession;
            getManager().setSession(session);
        }

        //Make sure user is assigned to the session unless tagged with @no-user
        if (session.getUser() == null && !noUser) {
            getManager().setupSessionUser();
        }

        //Make sure user returned to storage if tagged with @no-user
        if (noUser) {
            getManager().saveUserFromSession();
        }

        //Make sure webdriver session always exists, unless tagged with @no-driver
        try {
            if (!noDriver) {
                if (session.isDriverReady()) {
                    getManager().clearCookies();
                } else {
                    getManager().startDriver();
                }
            }
        } catch (org.openqa.selenium.remote.UnreachableBrowserException |
                org.openqa.selenium.remote.SessionNotFoundException e) {

            try {
                shutdown();
            } catch (Throwable ignored) {
            }
            getManager().startDriver();
        }
    }

    public void shutdown() {
        getManager().shutDown();
    }

}