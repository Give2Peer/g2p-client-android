package org.give2peer.karma.service;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.http.util.ExceptionUtils;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.adapter.DateTimeTypeAdapter;
import org.give2peer.karma.exception.AuthenticationException;
import org.give2peer.karma.exception.BadConfigException;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.exception.NoInternetException;
import org.give2peer.karma.response.CreateItemResponse;
import org.give2peer.karma.response.ErrorResponse;
import org.give2peer.karma.response.FindItemsResponse;
import org.give2peer.karma.response.PictureItemResponse;
import org.give2peer.karma.response.PrivateProfileResponse;
import org.give2peer.karma.entity.Server;
import org.give2peer.karma.exception.AuthorizationException;
import org.give2peer.karma.exception.ErrorResponseException;
import org.give2peer.karma.exception.MaintenanceException;
import org.give2peer.karma.exception.QuotaException;
import org.give2peer.karma.exception.UnavailableEmailException;
import org.give2peer.karma.exception.UnavailableUsernameException;
import org.give2peer.karma.response.RegistrationResponse;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This is our REST client service.
 * This also manages the collections of Items, but should not.
 *
 * It fetches data synchronously, so there's also a bunch of AsyncTasks that use these methods.
 *
 * Responsibilities :
 * - Fetch items from server's HTTP REST API.
 * - Keep fetched items up to date. (meh, no. -- but someone has to !)
 * - Store items locally in a cache for offline use. (don't know how, yet)
 */
public class RestService
{
    /**
     * The server limits the number of items sent in the response.
     * This constant is defined by the server.
     */
    static int ITEMS_PER_PAGE = 64;

    // See the routes at http://g2p.give2peer.org
    static String ROUTE_HELLO        = "/hello";
    static String ROUTE_CHECK        = "/check";
    static String ROUTE_USER         = "/user";
    static String ROUTE_ITEM         = "/item";
    static String ROUTE_ITEM_PICTURE = "/item/{id}/picture";
    static String ROUTE_ITEMS_AROUND = "/items/around/{latitude}/{longitude}";

    static String METHOD_GET  = "GET";
    static String METHOD_POST = "POST";

    protected Server currentServer;

    /**
     * The G2P REST API require credentials for most of its methods
     *
     */
    protected UsernamePasswordCredentials credentials;

    /**
     * Dependency : HttpClient
     * Look into DIC with for example Dagger : http://square.github.io/dagger/
     */
    protected HttpClient client;

    // CONSTRUCTOR /////////////////////////////////////////////////////////////////////////////////

    public RestService(Server config)
    {
        setServer(config);

        client = new DefaultHttpClient();
    }

    public void setServer(Server config)
    {
        currentServer = config;
        setCredentials(config.getUsername(), config.getPassword());
    }

    public String makeUrl(String route)
    {
        return currentServer.getUrl() + route;
    }

    // CREDENTIALS /////////////////////////////////////////////////////////////////////////////////

    public UsernamePasswordCredentials getCredentials()           { return credentials; }

    public void setCredentials(UsernamePasswordCredentials creds) { credentials = creds; }

    public void setCredentials(String username, String password)
    {
        setCredentials(new UsernamePasswordCredentials(username, password));
    }

    // HTTP QUERIES : ITEMS ////////////////////////////////////////////////////////////////////////

    public FindItemsResponse findAroundPaginated(double latitude, double longitude, int page)
            throws AuthorizationException, MaintenanceException, AuthenticationException,
            QuotaException, BadConfigException, ErrorResponseException,
            CriticalException, NoInternetException {
        return findAround(latitude, longitude, page * ITEMS_PER_PAGE);
    }

    public FindItemsResponse findAround(double latitude, double longitude)
            throws AuthorizationException, MaintenanceException, AuthenticationException,
            QuotaException, BadConfigException, ErrorResponseException,
            CriticalException, NoInternetException {
        return findAround(latitude, longitude, 0);
    }

    /**
     * Returns a list of at most 64 items.
     * Pages start at 0, and hold `ITEMS_PER_PAGE` items per page.
     */
    public FindItemsResponse findAround(double latitude, double longitude, int offset)
            throws AuthorizationException, MaintenanceException, AuthenticationException,
            QuotaException, BadConfigException, ErrorResponseException,
            CriticalException, NoInternetException {
        String route = ROUTE_ITEMS_AROUND.replaceAll("\\{latitude\\}",  String.valueOf(latitude))
                                         .replaceAll("\\{longitude\\}", String.valueOf(longitude));

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("skip", String.valueOf(offset));

        String json = getJson(route, params);

        FindItemsResponse findItemsResponse = new FindItemsResponse();

        try {
            Gson gson = createGson();
            findItemsResponse = gson.fromJson(json, FindItemsResponse.class);
        } catch (JsonSyntaxException e) {
            String msg = "Failed to parse finding items `%s` response :\n%s";
            throw new CriticalException(String.format(msg, route, json), e);
        }

        return findItemsResponse;
    }


    public CreateItemResponse createItem(Item item)
            throws
            AuthorizationException, QuotaException, MaintenanceException,
            ErrorResponseException, NoInternetException, BadConfigException,
            CriticalException, AuthenticationException
    {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("location", item.getLocation());
        params.put("title", item.getTitle());
        params.put("description", item.getDescription());
        params.put("type", item.getType());

        String jsonResponse = postJson(ROUTE_ITEM, params);

        CreateItemResponse createItemResponse = new CreateItemResponse();

        try {
            Gson gson = createGson();
            createItemResponse = gson.fromJson(jsonResponse, CreateItemResponse.class);
        } catch (JsonSyntaxException e) {
            String msg = "Failed to parse item creation response :\n%s";
            throw new CriticalException(String.format(msg, jsonResponse), e);
        }

        return createItemResponse;
    }

    /**
     * TODO: make a temp file with a smaller picture, upload it, and delete it afterwards
     *
     * @param item to add the picture to.
     * @param picture to add to the item.
     */
    public PictureItemResponse pictureItem(Item item, File picture)
            throws
            CriticalException, AuthorizationException, QuotaException, MaintenanceException,
            NoInternetException, BadConfigException, ErrorResponseException, AuthenticationException
    {
        String route = ROUTE_ITEM_PICTURE.replaceAll("\\{id\\}", item.getId().toString());

        HashMap<String, String> params = new HashMap<String, String>();

        String json = requestJson(METHOD_POST, route, params, true, picture);

        PictureItemResponse pictureItemResponse = new PictureItemResponse();

        try {
            Gson gson = createGson();
            pictureItemResponse = gson.fromJson(json, PictureItemResponse.class);
        } catch (JsonSyntaxException e) {
            String msg = "Failed to parse picture item response :\n%s";
            throw new CriticalException(String.format(msg, json), e);
        }

        return pictureItemResponse;
    }

    // HTTP QUERIES : USERS ////////////////////////////////////////////////////////////////////////

    public RegistrationResponse preregister()
            throws
                    // see method below for more details
                    ErrorResponseException, UnavailableUsernameException, UnavailableEmailException,
                    AuthorizationException, MaintenanceException, QuotaException, CriticalException,
                    BadConfigException, NoInternetException, AuthenticationException
    {
        return register("", "", "");
    }

    public RegistrationResponse register(String username, String password, String email)
            throws
                UnavailableUsernameException, UnavailableEmailException // obvious
                , ErrorResponseException  // never if we implement everything the server responds
                , AuthorizationException  // user is not allowed to so that
                , AuthenticationException // server credentials don't check out
                , MaintenanceException    // such pro, very maintain wow
                , CriticalException       // we want to know when they happen
                , QuotaException
                , BadConfigException      // something is badly configured ! User's fault, 99.99%
                , NoInternetException

    {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        params.put("password", password);
        params.put("email", email);
        String json = postJson(ROUTE_USER, params, false);

        RegistrationResponse registrationResponse = new RegistrationResponse();

        try {
            Gson gson = createGson();
            registrationResponse = gson.fromJson(json, RegistrationResponse.class);
        } catch (JsonSyntaxException e) {
            String msg = "Failed to parse registration response :\n%s";
            throw new CriticalException(String.format(msg, json), e);
        }

        return registrationResponse;
    }

    /**
     * @throws AuthorizationException
     * @throws QuotaException
     * @throws MaintenanceException
     */
    public PrivateProfileResponse getProfile()
            throws
            AuthorizationException, AuthenticationException, QuotaException, MaintenanceException,
            NoInternetException, ErrorResponseException, BadConfigException, CriticalException
    {
        String json = getJson(ROUTE_USER);
        Log.d("G2P", "Profile json reponse :\n"+json);

        PrivateProfileResponse privateProfileResponse = new PrivateProfileResponse();

        try {
            Gson gson = createGson();
            privateProfileResponse = gson.fromJson(json, PrivateProfileResponse.class);
        } catch (JsonSyntaxException e) {
            String msg = "Failed to parse private profile response :\n%s";
            throw new CriticalException(String.format(msg, json), e);
        }

        return privateProfileResponse;
    }

    // HTTP QUERIES : TESTS ////////////////////////////////////////////////////////////////////////

    /**
     * Does not check credentials. Mostly tells us that the server URL is correct.
     * Tells us :
     * - if the server URL is correct (failed or suceeded)
     * - additional info on the server, and we might make the app say hello on create ?
     *
     * @throws AuthorizationException
     * @throws MaintenanceException
     * @throws QuotaException
     * @throws NoInternetException
     * @throws ErrorResponseException
     * @throws BadConfigException
     * @throws CriticalException
     */
    public boolean checkServer()
            throws
            AuthorizationException, AuthenticationException, MaintenanceException, QuotaException,
            NoInternetException, ErrorResponseException, BadConfigException, CriticalException {
        String json = getJson(ROUTE_HELLO, new HashMap<String, String>(), false);
        return json.equals("\"pong\"");
    }

    /**
     * Checks connection to server to a route behind the authentication firewall.
     *
     * @throws AuthorizationException
     * @throws MaintenanceException
     * @throws QuotaException
     * @throws NoInternetException
     * @throws ErrorResponseException
     * @throws BadConfigException
     * @throws CriticalException
     */
    public boolean checkServerAndAuthentication()
            throws
            AuthorizationException, AuthenticationException, MaintenanceException, QuotaException,
            NoInternetException, ErrorResponseException, BadConfigException, CriticalException {
        String json = getJson(ROUTE_CHECK);
        return json.equals("\"pong\"");
    }

    // UTILS ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Our own very puny Gson factory, to attach our DateTime adapter.
     */
    protected Gson createGson()
    {
        GsonBuilder gsonBuilder = new GsonBuilder();
        // Register a Joda time adapter for ISO8601 strings. Otherwise it expects an object.
        gsonBuilder.registerTypeAdapter(DateTime.class, new DateTimeTypeAdapter());
        // We don't need to specify the DateFormat, Joda time accepts ISO8601 in our converter.
        // gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); // ISO8601
        return gsonBuilder.create();
    }

    /**
     * Authenticate the `request` using the `credentials`.
     *
     * @param request The request to authenticate
     */
    protected void authenticate(HttpRequest request) throws CriticalException
    {
        try {
            BasicScheme scheme = new BasicScheme();
            Header authorizationHeader = scheme.authenticate(getCredentials(), request);
            request.addHeader(authorizationHeader);
        } catch (org.apache.http.auth.AuthenticationException e) {
            // This AuthenticationException is when apache fails to set HTTPAuth headers.
            // If this happens, the app is useless. So, very much critical. Never happened yet.
            // This is NOT our AuthenticationException that happens when credentials don't check out
            // This is much more dire than that: the app has failed before even asking.
            String msg = "Apache failed to set headers for `%s`.";
            throw new CriticalException(String.format(msg, request.getRequestLine().getUri()), e);
        }
    }

    public String getJson(String route)
            throws
            CriticalException, MaintenanceException, QuotaException, ErrorResponseException,
            NoInternetException, BadConfigException, AuthorizationException, AuthenticationException {
        return this.getJson(route, new HashMap<String, String>(), true);
    }

    public String getJson(String route, HashMap<String, String> parameters)
            throws
            AuthorizationException, QuotaException, MaintenanceException, ErrorResponseException,
            CriticalException, BadConfigException, NoInternetException, AuthenticationException {
        return getJson(route, parameters, true);
    }

    /**
     * Get a UTF-8 encoded response from method described by `route`.
     *
     * @param route  must start with `/`.
     * @param parameters Optional parameters to send with the request
     * @return the raw server response body, which happens to be a JSON string
     */
    public String getJson(String route, HashMap<String, String> parameters, boolean authenticated)
            throws
            AuthorizationException, QuotaException, MaintenanceException, ErrorResponseException,
            CriticalException, BadConfigException, NoInternetException, AuthenticationException {
        return requestJson(METHOD_GET, route, parameters, authenticated, null);
    }


    public String postJson(String route, HashMap<String, String> parameters) throws CriticalException, AuthorizationException, QuotaException, MaintenanceException, NoInternetException, BadConfigException, ErrorResponseException, AuthenticationException {
        return postJson(route, parameters, true);
    }


    public String postJson(String route, HashMap<String, String> parameters, boolean authenticated) throws CriticalException, AuthorizationException, QuotaException, MaintenanceException, NoInternetException, BadConfigException, ErrorResponseException, AuthenticationException {
        return requestJson(METHOD_POST, route, parameters, authenticated, null);
    }

    /**
     * This is the heart of darkness.
     * Pretty much every HTTP request to the API goes through here.
     *
     * => Retrofit, please !
     *
     * @param method
     * @param route
     * @param parameters
     * @param authenticated
     * @param picture
     * @return
     * @throws UnavailableUsernameException
     * @throws UnavailableEmailException
     * @throws ErrorResponseException
     * @throws AuthorizationException
     * @throws MaintenanceException
     * @throws CriticalException
     * @throws QuotaException
     * @throws BadConfigException
     * @throws NoInternetException
     */
    public String requestJson(String method, String route, HashMap<String, String> parameters,
                              boolean authenticated, @Nullable File picture)
            throws
            UnavailableUsernameException, UnavailableEmailException // obvious
            , ErrorResponseException // never if we implement everything the server responds
            , AuthenticationException // server credentials don't check out
            , AuthorizationException // user is not allowed to do that
            , MaintenanceException   // such pro, very maintain wow
            , CriticalException      // we want to know when they happen
            , QuotaException         // (daily) quotas were exceeded
            , BadConfigException     // something is badly configured ! User's fault, 99.99%
            , NoInternetException

    {
        String url = makeUrl(route);

        HttpResponse response = null;
        String json = null;

        try {
            HttpRequestBase request;

            if (method.equals(METHOD_POST)) {
                request = new HttpPost();
            } else if (method.equals(METHOD_GET)) {
                request = new HttpGet();
            } else {
                throw new CriticalException(String.format("Unsupported HTTP method `%s`.", method));
            }

            request.setURI(new URI(url));

            if (method.equals(METHOD_POST)) {

                HttpEntity httpEntity = null;
                if (null == picture) {
                    // Add the POST parameters to the request
                    // The following is deprecated in API level 22 !
                    List<NameValuePair> pairs = new ArrayList<NameValuePair>();
                    for (String name : parameters.keySet()) {
                        String value = parameters.get(name);
                        if ( ! value.isEmpty()) {
                            pairs.add(new BasicNameValuePair(name, value));
                        }
                    }
                    httpEntity = new UrlEncodedFormEntity(pairs, "UTF-8");
                } else {
                    // We want to upload a picture file *instead* !
                    // This is a haxxx ; but release day is in 2 days !
                    httpEntity = MultipartEntityBuilder
                            .create()
                            .addBinaryBody(
                                    "picture", picture,
                                    ContentType.create("image/jpg"),
                                    picture.getName()
                            ).build();
                }

                try {
                    ((HttpPost)request).setEntity(httpEntity);
                } catch (ClassCastException e) {
                    throw new CriticalException("Failed to cast to HttpPost.");
                }

            } else if (method.equals(METHOD_GET)) {
                // Add the GET parameters to the request
                if ( ! parameters.isEmpty()) {
                    BasicHttpParams params = new BasicHttpParams();
                    for (String key : parameters.keySet()) {
                        String value = parameters.get(key);
                        params.setParameter(key, value);
                    }
                    request.setParams(params);
                }

            } else {
                throw new CriticalException(String.format("Unsupported HTTP method `%s`.", method));
            }

            if (authenticated) authenticate(request);

            // Let's exchange some bytes !
            response = client.execute(request);

            // Get the response as a UTF-8 string
            json = EntityUtils.toString(response.getEntity(), "UTF-8");

            // Inspect the response and throw accordingly
//            inspectResponseForErrors(response);


            // A lot of things can go wrong with each request
            int code = response.getStatusLine().getStatusCode();

            if (code >= 400) {
                if (code == 401) {
                     throw new AuthenticationException(); // unsure if the response is valid JSON
                }
                if (code == 403) {
                     throw new AuthorizationException(); // we could also use the JSON response
                }
                if (code == 404) {
                    // 1. when you badly configured the server URL
                    throw new BadConfigException(currentServer);
                    // 2. when querying a deleted entity (it WILL happen someday)
//                throw new OutOfSyncException();
                    // which should not be decided here, this should simply throw a NotFoundException
                    // and let the parent decide.
                }
                if (code == 429) {
                    throw new QuotaException(); // we could also use the JSON response
                }
                if (code >= 500) {
                    throw new MaintenanceException(); // there's no json response right now I think
                }

                // ... fixme: what about other codes ?

                ErrorResponse errorResponse = null;

                try {
                    Gson gson = createGson();
                    errorResponse = gson.fromJson(json, ErrorResponse.class);
                } catch (JsonSyntaxException e) {
                    String m = "No JSON on a %d on %s :\n%s";
                    throw new CriticalException(String.format(m, code, request.getURI(), json), e);
                }

                if (null == errorResponse) {
                    // This can happen only if you change the code above, which might happen out of
                    // annoyance because the critical above may happen a lot until the server is OK.
                    throw new CriticalException("NEIN ! ... NEIN !");
                } else {
                    if (errorResponse.isBadEmail()) {
                        throw new UnavailableEmailException(errorResponse);
                    }
                    if (errorResponse.isBadUsername()) {
                        throw new UnavailableUsernameException(errorResponse);
                    }
                }

                // The default, for what we don't yet explicitely support
                throw new ErrorResponseException(errorResponse);
            }




        } catch (URISyntaxException e) {
            // That's on the user ; probably entered a wrong URI in the server config.
            throw new BadConfigException(currentServer, e);

        } catch (ClientProtocolException e) {
            // ProtocolException: Signals that an HTTP protocol violation has occurred.
            // For example a malformed status line or headers, a missing message body, etc.
            // We need to make sure our server never triggers this exception, so it's critical.
            String msg = "Protocol failed for `%s`.";
            throw new CriticalException(String.format(msg, url), e);

        } catch (UnsupportedEncodingException e) {
            // We want to know when this happens, maybe we'll have to roll out some appcompat
            String msg = "Trouble with UTF-8 on POST to `%s` with values `%s`.";
            throw new CriticalException(String.format(msg, url, parameters.toString()), e);

        } catch (IOException e) {
            // Let's assume for now that this can either be a bad server url or no internet
            throw new NoInternetException();

        } catch (Exception e) {
            // ... just propagate up other errors
            e.printStackTrace();
            throw e;

        } finally {
            if (null != response) {
                try {
                    response.getEntity().consumeContent();
                } catch (IOException e) {
                    String msg = "I never happen. Nope. Consuming response failed for `%s`.";
                    Log.e("G2P", String.format(msg, url));
                }
            }
        }

        if (null == json) {
            throw new CriticalException("Sanity check. If this happens, I am mad.");
        }

        return json;
    }

}