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
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.adapter.DateTimeTypeAdapter;
import org.give2peer.karma.exception.AlreadyDoneException;
import org.give2peer.karma.exception.AuthenticationException;
import org.give2peer.karma.exception.BadConfigException;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.exception.LevelTooLowException;
import org.give2peer.karma.exception.NoInternetException;
import org.give2peer.karma.response.CheckResponse;
import org.give2peer.karma.response.CreateItemResponse;
import org.give2peer.karma.response.DeleteItemResponse;
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
import org.give2peer.karma.response.ReportItemResponse;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This WAS our REST client service.
 * It fetches data synchronously, so you need to wrap it in an AsyncTask.
 *
 * It is now unused but in a hidden activity to configure servers ; too complex to refactor now.
 * Never mind this class, see the RestClient instead.
 *
 * Responsibilities :
 * - Fetch data from server's HTTP REST API.
 *
 * /!\ DEPRECATION NOTICE
 *     There are a lot of deprecated classes used in this service, such as apache HTTP components.
 *     Use the RestClient instead. We're working to migrate existing routes. @Goutte
 */
@Deprecated
public class RestService
{
    // See the routes at http://g2p.give2peer.org
    static String ROUTE_CHECK        = "/check";

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

//        client = new DefaultHttpClient();
        client = getHttpClient();
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

    // HTTP CLIENT SINGLETON ///////////////////////////////////////////////////////////////////////

    /**
     * I don't know where to start... This is BAAAAAAAAD.
     * We accept all certificates, and are very much vulnerable to a MITM attack. :(
     * But I can't figure out how to handle certificates the right way
     *
     *
     * @return the http client
     */
    public synchronized HttpClient getHttpClient()
    {
        if (client != null)
            return client;

        SSLConnectionSocketFactory sslSF = null;
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    // We could match StringUtils.bytesToHex(cert.getTBSCertificate()) against
                    // String tbs = "30820188020900B36C4DED88CD2BF6300D06092A864886F70D010105050030123110300E0603550403130771726F6B2E6D65301E170D3133303533303032333634335A170D3233303532383032333634335A30123110300E0603550403130771726F6B2E6D6530820122300D06092A864886F70D01010105000382010F003082010A0282010100D05CF4CD16510597A92750D20643D691C05BE51EF892407C2C2434A93659FE6BB11C5E6B039E3E670AE541252970AB1DB4014EC9F5A26C9500FE43D85A8053CD2EA507D6B9BDCD02510A9612EF4BFD145A2465C24061F0EE482935821CA75C06A8931CCD9F7B797A0D05B0ACF6FDA8409972646304B0ADA5034E55FFEB03CF2E410C4016B6D06CBBCCD5CB5B1BFC65A9B4798B751027AEDF2D7CE219E3655DF257BF84958DE417A886BA5E1E54C9F0CBF7B7ED205B9F7AA7E2D963643B20B880D80B4A5BD0AAB9604B99F21AA49602932F2F4DF3A013351E2D919D812C70011D8BAE799D39AAFF59EC269ECD64B0D0591DD215897BCA5FBB0C8F8FC42EEBDE2F0203010001";
                    // but when our certificate expires it would break the app !
                    // And what about the certificates of other servers ?
                    // for (X509Certificate cert : chain) {
                    //     Log.d("G2P", String.format("Certificate %s", cert.toString()));
                    // }
                    return true;
                }
            });
            sslSF = new SSLConnectionSocketFactory(
                    builder.build(),
                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER
            );
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new CriticalException(e);
        }

        client = HttpClients.custom().setSSLSocketFactory(sslSF).build();

        return client;
    }





    /**
     * Checks connection to server to a route behind the authentication firewall.
     *
     * This is the only method we're still using in deprecated activities, like the
     * server configuration.
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
            NoInternetException, ErrorResponseException, BadConfigException, CriticalException, AlreadyDoneException, LevelTooLowException {
        Gson gson = createGson();
        CheckResponse cr = gson.fromJson(getJson(ROUTE_CHECK), CheckResponse.class);
        return cr.isOk();
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
            NoInternetException, BadConfigException, AuthorizationException, AuthenticationException, LevelTooLowException, AlreadyDoneException {
        return getJson(route, new HashMap<String, String>(), true);
    }

    public String getJson(String route, HashMap<String, String> parameters)
            throws
            AuthorizationException, QuotaException, MaintenanceException, ErrorResponseException,
            CriticalException, BadConfigException, NoInternetException, AuthenticationException, LevelTooLowException, AlreadyDoneException {
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
            CriticalException, BadConfigException, NoInternetException, AuthenticationException, AlreadyDoneException, LevelTooLowException {
        return requestJson(METHOD_GET, route, parameters, authenticated, null);
    }


    public String postJson(String route, HashMap<String, String> parameters) throws CriticalException, AuthorizationException, QuotaException, MaintenanceException, NoInternetException, BadConfigException, ErrorResponseException, AuthenticationException, LevelTooLowException, AlreadyDoneException {
        return postJson(route, parameters, true);
    }


    public String postJson(String route) throws CriticalException, AuthorizationException, QuotaException, MaintenanceException, NoInternetException, BadConfigException, ErrorResponseException, AuthenticationException, LevelTooLowException, AlreadyDoneException {
        return postJson(route, new HashMap<String, String>(), true);
    }


    /**
     * @param route
     * @param parameters
     * @param authenticated
     * @return The JSON as string.
     * @throws CriticalException
     * @throws AuthorizationException
     * @throws QuotaException
     * @throws MaintenanceException
     * @throws NoInternetException
     * @throws BadConfigException
     * @throws ErrorResponseException
     * @throws AuthenticationException
     */
    public String postJson(String route, HashMap<String, String> parameters, boolean authenticated) throws CriticalException, AuthorizationException, QuotaException, MaintenanceException, NoInternetException, BadConfigException, ErrorResponseException, AuthenticationException, AlreadyDoneException, LevelTooLowException {
        return requestJson(METHOD_POST, route, parameters, authenticated, null);
    }

    /**
     * This is the heart of darkness.
     * Pretty much every HTTP request to the API goes through here.
     *
     * => Retrofit, please ! See the wip in RestClient.
     *
     * @param method
     * @param route
     * @param parameters
     * @param authenticated
     * @param picture
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
            , NoInternetException, LevelTooLowException, AlreadyDoneException {
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

            URIBuilder uriBuilder = new URIBuilder(url);

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

                    for (String key : parameters.keySet()) {
                        String value = parameters.get(key);
                        uriBuilder.addParameter(key, value);
                    }
                }

            } else {
                throw new CriticalException(String.format("Unsupported HTTP method `%s`.", method));
            }

            request.setURI(uriBuilder.build());

            if (authenticated) authenticate(request);

            // Let's exchange some bytes !
            response = client.execute(request);

            // Get the response as a UTF-8 string
            // Not sure why it's deprecated.
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
                }
//                else {
//
//                    if (errorResponse.isBadEmail()) {
//                        throw new UnavailableEmailException(errorResponse);
//                    }
//                    if (errorResponse.isBadUsername()) {
//                        throw new UnavailableUsernameException(errorResponse);
//                    }
//                    if (errorResponse.isAlreadyDone()) {
//                        throw new AlreadyDoneException();
//                    }
//                    if (errorResponse.isLevelTooLow()) {
//                        throw new LevelTooLowException();
//                    }
//
//                }

                // The default, we'll toast or snackbar the error message with it I guess.
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
//            e.printStackTrace();
            throw new NoInternetException(e);

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