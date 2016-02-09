package test.java.framework.helpers;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.SessionConfig;
import com.jayway.restassured.response.ExtractableResponse;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import test.java.framework.Bindings;
import test.java.framework.SessionPrototype;
import test.java.framework.manager.SoftAssertionError;

import java.io.File;
import java.util.*;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;

public abstract class APICore extends Bindings {

    public APICore(SessionPrototype instance) {
        super(instance);
    }

//======================================================================================================================
//Properties

    private ExtractableResponse<Response> lastResponse = null;
    private String lastRequest = null;
    private Map<String, String> lastRequestParams = null;
    private String phpSessionId = null;
    private static String sessionKey = "session.id";

//======================================================================================================================
//Methods

    public ExtractableResponse<Response> getLastResponse() {
        return lastResponse;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setPhpSessionId(String phpSessionId) {
        this.phpSessionId = phpSessionId;
    }

    public String getLastResponseBody() {
        return lastResponse.response().body().asString();
    }

    /**
     * Clear last response value; set new request url and params
     *
     * @param url    request url
     * @param params request parameters
     */
    protected void updateLastRequest(String url, Map<String, String> params) {
        lastResponse = null;
        lastRequest = url;
        lastRequestParams = params;
    }

    /**
     * GET HTTP request
     *
     * @param url fully qualified URL to request
     * @return response
     * @throws Exception any errors by rest assured lib
     */
    protected static ExtractableResponse<Response> get(String url) throws Exception {
        return given().get(url).then().extract();
    }

    /**
     * GET HTTP request
     *
     * @param url    fully qualified URL to request
     * @param params parameters to send with get request
     * @return response
     * @throws Exception any errors by rest assured lib
     */
    protected ExtractableResponse<Response> get(String url, Map<String, String> params) throws Exception {
        RequestSpecification given = request(params);
        return given.get(url).then().extract();
    }

    /**
     * POST HTTP request
     *
     * @param url    fully qualified URL to request
     * @param params parameters to send with post request
     * @return response
     * @throws Exception any errors by rest assured lib
     */
    protected ExtractableResponse<Response> post(String url, Map<String, String> params) throws Exception {
        RequestSpecification given = request(params);
        return given.post(url).then().extract();
    }

    /**
     * POST HTTP request with multiPart data (files)
     *
     * @param url        fully qualified URL to request
     * @param params     parameters to send with post request
     * @param fileParams files to send with post request
     * @return response
     * @throws Exception any errors by rest assured lib
     */
    protected ExtractableResponse<Response> post(String url, Map<String, String> params, Map<String, File> fileParams) throws Exception {
        RequestSpecification given = request(params);
        for (Map.Entry<String, File> param : fileParams.entrySet()) {
            given.multiPart(param.getKey(), param.getValue());
        }
        return given.post(url).then().extract();
    }

    /**
     * Build up API query with parameters and/or cookies
     *
     * @param params parameters
     * @return RequestSpecification
     */
    protected RequestSpecification request(Map<String, String> params) throws Exception {

        //Set session id name if session id was provided
        if (params.containsKey(sessionKey) && phpSessionId != null) {
            RestAssured.config = newConfig().sessionConfig(new SessionConfig().sessionIdName(phpSessionId)).encoderConfig(encoderConfig().defaultContentCharset("UTF-8"));
        }
        RequestSpecification given = given();

        //Add provided parameters to the request
        for (Map.Entry<String, String> param : params.entrySet()) {
            //Re-use session, only if id was provided
            if (param.getKey().equals(sessionKey)) {
                given.sessionId(param.getValue());
                continue;
            }
            if (param.getKey().equals("X-Requested-With")) {
                given.header("X-Requested-With", "XMLHttpRequest");
                continue;
            }
            given.formParams(param.getKey(), param.getValue());
        }
        return given;
    }

    /**
     * Sends GET request
     *
     * @param url fully qualified URL
     */
    protected void sendGetRequest(String url) {
        updateLastRequest(url, new HashMap<>());
        try {
            lastResponse = get(url);
            verifyLastStatusCode(200);
        } catch (Throwable e) {
            fail(String.format("API request failed: %s\n\nOriginal request: %s", e.getMessage(), url), false);
        }
    }

    /**
     * Sends GET request
     *
     * @param url    fully qualified URL
     * @param params options to post
     */
    protected void sendGetRequest(String url, Map<String, String> params) {
        updateLastRequest(url, params);
        try {
            lastResponse = get(url, params);
            verifyLastStatusCode(200);
        } catch (Throwable e) {
            fail(String.format("API request failed: %s\n\nOriginal request: %s\n\nParams: %s\n", e.getMessage(), url, params), false);
        }
    }

    /**
     * Sends POST request
     *
     * @param url    fully qualified URL
     * @param params options to post
     */
    protected void sendPostRequest(String url, Map<String, String> params) {
        updateLastRequest(url, params);
        try {
            lastResponse = post(url, params);
            verifyLastStatusCode(200);
        } catch (Throwable e) {
            String errMessage = "API request failed: %s\n\nOriginal request: %s\n\nParams: %s\n";
            fail(String.format(errMessage, e.getMessage(), url, params), false);
        }
    }

    /**
     * Sends POST request
     *
     * @param url        fully qualified URL
     * @param params     options to post
     * @param fileParams files to post
     */
    protected void sendPostRequest(String url, Map<String, String> params, Map<String, File> fileParams) {
        updateLastRequest(url, params);
        try {
            lastResponse = post(url, params, fileParams);
            verifyLastStatusCode(200);
        } catch (Throwable e) {
            Map<String, String> fileData = new HashMap<>();
            for (Map.Entry<String, File> entry : fileParams.entrySet()) {
                fileData.put(entry.getKey(), entry.getValue().getName());
            }
            String errMessage = "API request failed: %s\n\nOriginal request: %s\n\nParams: %s\n with file names: %s\n";
            fail(String.format(errMessage, e.getMessage(), url, params, fileData), false);
        }
    }

    /**
     * Checks last response for the provided response code
     *
     * @param status expected status code
     */
    public void verifyLastStatusCode(int status) {

        if (lastResponse == null) {
            fail("API request was not sent or response is not saved", false);
        }

        int actualStatus = lastResponse.statusCode();
        if (actualStatus != status) {
            String body = "Response body is not available\n";
            try {
                body = lastResponse.body().jsonPath().prettify();
            } catch (Throwable e) {
                body = body.concat(e.getMessage());
            }
            fail(String.format("Status code is not %s: %s\n\n%s", status, actualStatus, body), false);
        }
    }

    /**
     * Sends GET API request to Alice server
     *
     * @param url API_URL endpoint (without domain name)
     */
    public void sendGetAPIRequest(String url) {
        String request = prepareRequestUrl(url);
        sendGetRequest(request);
    }

    /**
     * Sends GET API request to Alice server
     *
     * @param url    API_URL endpoint (without domain name)
     * @param params options to post
     */
    public void sendGetAPIRequest(String url, Map<String, String> params) {
        updateLastRequest(url, params);
        String request = prepareRequestUrl(url);
        sendGetRequest(request, params);
    }

    /**
     * Sends GET API request to Alice server
     *
     * @param url    API_URL endpoint (without domain name)
     * @param params options to post
     */
    public void sendGetAPIRequestUsingLastSession(String url, Map<String, String> params) {
        Map<String, String> paramsToSend = prepareSessionParameters(params);
        sendGetAPIRequest(url, paramsToSend);
    }

    /**
     * Sends POST API request to Alice server
     *
     * @param url    API_URL endpoint (without domain name)
     * @param params options to post
     */
    public void sendPostAPIRequest(String url, Map<String, String> params) {
        updateLastRequest(url, params);
        String request = prepareRequestUrl(url);
        sendPostRequest(request, params);
    }

    /**
     * Sends POST API request to Alice server
     *
     * @param url        API_URL endpoint (without domain name)
     * @param params     options to post
     * @param fileParams files to post
     */
    public void sendPostAPIRequest(String url, Map<String, String> params, Map<String, File> fileParams) {
        updateLastRequest(url, params);
        String request = prepareRequestUrl(url);
        sendPostRequest(request, params, fileParams);
    }

    /**
     * Sends POST API request to Alice server
     *
     * @param url        API_URL endpoint (without domain name)
     * @param params     options to post
     * @param fileParams file to post
     */
    public void sendPostAPIRequestUsingLastSession(String url, Map<String, String> params, Map<String, File> fileParams) {
        Map<String, String> paramsToSend = prepareSessionParameters(params);
        sendPostAPIRequest(url, paramsToSend, fileParams);
    }

    /**
     * Sends POST API request to Alice server
     *
     * @param url    API_URL endpoint (without domain name)
     * @param params options to post
     */
    public void sendPostAPIRequestUsingLastSession(String url, Map<String, String> params) {
        Map<String, String> paramsToSend = prepareSessionParameters(params);
        sendPostAPIRequest(url, paramsToSend);
    }

    /**
     * hack for HTTPS on all pages for BD
     *
     * @param partUrl part url after domain name or full url if does not belong to carmudi
     * @return fully qualified url to send API request to
     */
    protected String prepareRequestUrl(String partUrl) {
        String fullUrl;
        if (partUrl.startsWith("http") || partUrl.startsWith("https")) {
            fullUrl = partUrl;
        } else {
            fullUrl = addDomainToURL(partUrl);
        }
        return fullUrl;
    }

    protected abstract String addDomainToURL(String partUrl);

    protected Map<String, String> prepareSessionParameters(Map<String, String> params) {

        Map<String, String> paramsToSend = new HashMap<>();
        paramsToSend.putAll(params);

        //Use existing session, if present
        if (isLastResponseKeyPresent(sessionKey)) {
            Map<String, String> paramsWithSession = getLastResponseValues(sessionKey);
            paramsToSend.putAll(paramsWithSession);
        }
        return paramsToSend;
    }

    protected Map<String, Object> prepareSessionParametersWithObject(HashMap<String, List<String>> params) {
        Map<String, Object> paramsToSend = new HashMap<>();
        paramsToSend.putAll(params);

        //Use existing session, if present
        if (isLastResponseKeyPresent(sessionKey)) {
            Map<String, String> paramsWithSession = getLastResponseValues(sessionKey);
            paramsToSend.putAll(paramsWithSession);
        }
        return paramsToSend;
    }

    /**
     * Parses a path containing foo{name:key}.bar
     * <p>
     * If path is accessible via array e.g. foo[23].bar instead of direct foo.key.bar,
     * use the following path foo{key}.bar, where key is the value of the name attribute
     *
     * @param path path containing foo{name:key}.bar
     * @return restassured formatted json path
     */
    protected String parsePath(String path) {
        if (path.contains("{")) {
            //get names of the nodes
            String[] startPath = path.split("\\{");
            String tempPath = startPath[0] + ".name";
            TreeMap<String, String> keys = getLastResponseValues(tempPath);
            List<String> keyValues = new ArrayList<>(keys.values());

            //Save attribute key
            String[] endPath = startPath[1].split("\\}");
            String key = endPath[0];

            //Get position of the lookup key
            int keyIndex = keyValues.indexOf(key);

            path = String.format("%s[%s]%s", startPath[0], keyIndex, endPath[1]);
        }
        return path;
    }

    /**
     * Verifies last API response
     *
     * @param toCompare data to compare
     */
    public void verifyLastResponse(Map<String, String> toCompare) {
        if (lastResponse == null) {
            error("No saved response to verify!", false);
        } else {
            String errMsg = "";

            for (Map.Entry<String, String> row : toCompare.entrySet()) {
                try {
                    String valueFromResponse = lastResponse.path(row.getKey()).toString();
                    verifyEquals(valueFromResponse, row.getValue(),
                            row.getKey() + " value is incorrect!");

                } catch (Throwable e) {
                    errMsg += handleResponseException(e, row.getKey());
                }
            }
            if (!errMsg.isEmpty()) {
                error(errMsg, false);
            }
        }
    }

    /**
     * Returns map of <key, value> from the last response for the provided array of fields
     *
     * @param paths list of keys to get values for
     * @return ordered map of <key, value> from the last response
     */
    public TreeMap<String, String> getLastResponseValues(String... paths) {
        TreeMap<String, String> values = new TreeMap<>();
        String errMsg = "";

        for (String singlePath : paths) {
            try {
                String parsedPath = parsePath(singlePath);
                Object response = lastResponse.path(parsedPath);

                //if node is a collection, add all
                if (response instanceof Collection) {
                    int i = 0;
                    for (Object value : (List) response) {
                        values.put(String.format("%s[%s]", parsedPath, i++), value.toString());
                    }
                } else if (response instanceof Map) {
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) response).entrySet()) {
                        values.put(entry.getKey().toString(), entry.getValue().toString());
                    }
                } else if (response == null && parsedPath.contains("is_agent")) {
                    values.put(parsedPath, "0");

                } else if (response != null) {
                    values.put(parsedPath, response.toString());
                }

            } catch (Throwable e) {
                errMsg += handleResponseException(e, singlePath);
            }
        }
        if (!errMsg.isEmpty()) {
            error(errMsg, false);
        }
        return values;
    }

    protected String handleResponseException(Throwable exception, String key) {

        String errMsg = String.format("Original request: %s\nParameters: %s", lastRequest, lastRequestParams);
        String finalMsg;

        if (exception instanceof SoftAssertionError) {
            finalMsg = String.format(
                    "%s\nKey: %s\n\n%s\n\nResponse body: %s\n",
                    exception.getMessage(), key, errMsg, getLastResponseBody()
            );
        } else if (exception instanceof NullPointerException) {
            finalMsg = String.format(
                    "Key '%s' not found in the response!\n\n%s\n\nResponse body:\n\n%s",
                    key, errMsg, getLastResponseBody()
            );
        } else {
            finalMsg = String.format(
                    "Error on key: %s\n%s\n\n%s\n\nResponse body:\n\n%s",
                    key, exception.getMessage(), errMsg, getLastResponseBody()
            );
        }
        return finalMsg;
    }

    /**
     * Returns value from the last response for the provided key
     *
     * @param field key to get values
     * @return value from the last response
     */
    public String getLastResponseValue(String field) {
        return getLastResponseValues(field).get(field);
    }

    /**
     * Verify if response contains specific key
     *
     * @param key key to to verify
     * @return value from the last response
     */
    public boolean isLastResponseKeyPresent(String key) {
        try {
            getLastResponseValue(key);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

}
