package org.give2peer.give2peer.service;

import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.give2peer.give2peer.Item;
import org.give2peer.give2peer.entity.Server;
import org.give2peer.give2peer.exception.AuthorizationException;
import org.give2peer.give2peer.exception.ErrorResponseException;
import org.give2peer.give2peer.exception.MaintenanceException;
import org.give2peer.give2peer.exception.QuotaException;
import org.give2peer.give2peer.exception.UnavailableEmailException;
import org.give2peer.give2peer.exception.UnavailableUsernameException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    static int UNAVAILABLE_USERNAME = 1;
    static int BANNED_FOR_ABUSE     = 2;
    static int UNSUPPORTED_FILE     = 3;
    static int NOT_AUTHORIZED       = 4;
    static int SYSTEM_ERROR         = 5;
    static int BAD_LOCATION         = 6;
    static int UNAVAILABLE_EMAIL    = 7;
    static int EXCEEDED_QUOTA       = 8;
    static int BAD_USERNAME         = 9;

    /**
     * The `scheme`://`authority` part of the URL of the server.
     * Eg: https://g2p.give2peer.org
     */
    protected String serverUrl;

    /**
     * The G2P REST API require credentials for most of its methods
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
        serverUrl = config.getUrl();
        setCredentials(config.getUsername(), config.getPassword());

        client = new DefaultHttpClient();
    }

    // CREDENTIALS /////////////////////////////////////////////////////////////////////////////////

    public UsernamePasswordCredentials getCredentials() { return credentials; }

    public void setCredentials(UsernamePasswordCredentials creds) { credentials = creds; }

    public void setCredentials(String username, String password)
    {
        setCredentials(new UsernamePasswordCredentials(username, password));
    }

    // HTTP QUERIES : ITEMS ////////////////////////////////////////////////////////////////////////

    public ArrayList<Item> findAroundPaginated(double latitude, double longitude, int page)
            throws IOException, URISyntaxException, AuthorizationException, MaintenanceException, QuotaException
    {
        return findAround(latitude, longitude, page * ITEMS_PER_PAGE);
    }

    public ArrayList<Item> findAround(double latitude, double longitude)
            throws IOException, URISyntaxException, AuthorizationException, MaintenanceException, QuotaException
    {
        return findAround(latitude, longitude, 0);
    }

    /**
     * Returns a list of at most 64 items.
     * Pages start at 0, and hold `ITEMS_PER_PAGE` items per page.
     *
     * @param latitude
     * @param longitude
     * @param offset
     * @return
     */
    public ArrayList<Item> findAround(double latitude, double longitude, int offset)
            throws URISyntaxException, IOException, AuthorizationException, MaintenanceException, QuotaException
    {
        String route = "/item/around/" + latitude + "/" + longitude;

        BasicHttpParams params = new BasicHttpParams();
        params.setParameter("skip", offset);

        return jsonToItems(getJson(route, params));
    }


    public Item giveItem(Item item)
            throws URISyntaxException, IOException, JSONException, ErrorResponseException
    {
        String url = serverUrl + "/item";

        HttpPost request = new HttpPost();
        request.setURI(new URI(url));

        authenticate(request);

//        request.setParams(); // fixme: try it

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("location", item.getLocation()));
        pairs.add(new BasicNameValuePair("title",    item.getTitle()));
        request.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8")); // deprecated in API level 22 !

        HttpResponse response = client.execute(request);

        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
        if (response.getStatusLine().getStatusCode() < 400) {
            Log.d("G2P", "Successfully gave an item. Response : " + json);
            item.updateWithJSON(new JSONObject(json));
        } else {
            throw new ErrorResponseException(json);
        }

        return item;
    }

    /**
     * TODO: make a temp file with a smaller picture, upload it, and delete it afterwards
     *
     * @param item to add the picture to.
     * @param picture to add to the item.
     */
    public Item pictureItem(Item item, File picture)
    {
        String url = serverUrl + String.format("/item/%s/picture", item.getId().toString());
        
        try {
            HttpPost request = new HttpPost();
            request.setURI(new URI(url));

            authenticate(request);

            HttpEntity httpEntity = MultipartEntityBuilder
                    .create()
                    .addBinaryBody("picture", picture, ContentType.create("image/jpg"), picture.getName())
                    .build();
            request.setEntity(httpEntity);

            HttpResponse response = client.execute(request);

            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            if (response.getStatusLine().getStatusCode() < 400) {
                item.updateWithJSON(new JSONObject(json));
            } else {
                Log.e("G2P", "Picture Item Error : "+json);
            }
        } catch (URISyntaxException|IOException|JSONException e) {
            Log.e("G2P", e.getMessage());
            e.printStackTrace();
        }

        return item;
    }

    // HTTP QUERIES : USERS ////////////////////////////////////////////////////////////////////////

    public void register(String username, String password, String email)
            throws URISyntaxException, IOException, JSONException, ErrorResponseException,
                   UnavailableUsernameException, UnavailableEmailException
    {
        String url = serverUrl + "/register";

        HttpPost request = new HttpPost();
        request.setURI(new URI(url));

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("username", username));
        pairs.add(new BasicNameValuePair("password", password));
        pairs.add(new BasicNameValuePair("email",    email));
        request.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8")); // deprecated in API level 22 !

        HttpResponse response = client.execute(request);
        String json = EntityUtils.toString(response.getEntity(), "UTF-8");

        if (response.getStatusLine().getStatusCode() >= 400) {
            JSONObject data = new JSONObject(json);
            JSONObject error = data.getJSONObject("error");
            int errorCode = error.getInt("code");
            String errorMessage = error.optString("message");
            if (errorCode == UNAVAILABLE_USERNAME) {
                throw new UnavailableUsernameException(errorMessage);
            } else if (errorCode == UNAVAILABLE_EMAIL) {
                throw new UnavailableEmailException(errorMessage);
            } else {
                throw new ErrorResponseException(json);
            }
        }
    }

    // HTTP QUERIES : TESTS ////////////////////////////////////////////////////////////////////////

    public boolean testServer()
            throws IOException, URISyntaxException, AuthorizationException, MaintenanceException, QuotaException
    {
        String json = getJson("/ping");
        return json.equals("\"pong\"");
    }


    public boolean testLogin()
            throws IOException, URISyntaxException, AuthorizationException, MaintenanceException, QuotaException
    {
        String json = getJson("/login");
        return json.equals("\"pong\"");
    }

    // UTILS ///////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param request The request to authenticate
     */
    protected void authenticate(HttpRequest request)
    {
        try {
            BasicScheme scheme = new BasicScheme();
            Header authorizationHeader = scheme.authenticate(credentials, request);
            request.addHeader(authorizationHeader);
        } catch (AuthenticationException e) {
            Log.e("G2P", "Authentication failure !");
            Log.e("G2P", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @param route must start with `/`.
     * @return the raw server response body, which happens to be a JSON string
     */
    public String getJson(String route)
    throws URISyntaxException, IOException, AuthorizationException, MaintenanceException, QuotaException
    {
        return this.getJson(route, null);
    }

    /**
     * Get a UTF-8 encoded response from method described by `route`.
     *
     * @param route  must start with `/`.
     * @param params Optional parameters to send with the request
     * @return the raw server response body, which happens to be a JSON string
     */
    public String getJson(String route, @Nullable HttpParams params)
    throws URISyntaxException, IOException,
           AuthorizationException, QuotaException, MaintenanceException
    {
        HttpGet request = new HttpGet();
        request.setURI(new URI(serverUrl + route));
        authenticate(request);
        if (null != params) request.setParams(params);
        HttpResponse response = client.execute(request);

        // A lot of things can go wrong with each request
        int code = response.getStatusLine().getStatusCode();

        if (code >= 400) {
            if (code == 401) {
                throw new AuthorizationException();
            }
            if (code == 429) {
                throw new QuotaException(); // instead maybe we could use the JSON response ?
            }
            if (code >= 500) {
                throw new MaintenanceException();
            }
        }

        return EntityUtils.toString(response.getEntity(), "UTF-8");
    }


    /**
     * Try to parse the `json` and build the items in it.
     *
     * @param json
     * @return
     */
    protected ArrayList<Item> jsonToItems(String json)
    {
        ArrayList<Item> itemsList = new ArrayList<Item>();
        try {
            Item item;
            JSONObject jsonObject;
            JSONArray rows = new JSONArray(json);
            for (int i = 0 ; i < rows.length() ; i++) {
                jsonObject = rows.getJSONObject(i);
                item = new Item(jsonObject);
                itemsList.add(item);
            }
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data : " + e.toString());
        }

        return itemsList;
    }

}
