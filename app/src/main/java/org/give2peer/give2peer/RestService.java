package org.give2peer.give2peer;

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
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.give2peer.give2peer.entity.Server;
import org.give2peer.give2peer.exception.ErrorResponseException;
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
 * Responsibilities :
 * - Fetch items from server HTTP REST API.
 *   It fetches them synchronously, so there's also a bunch of AsyncTasks that use these methods.
 * - Keep fetched items up to date. (meh, no.)
 * - Store items locally in a cache for offline use. (don't know how, yet, and ... nope, anyway)
 */
public class RestService
{
    /**
     * The server limits the number of items sent in the response.
     * This constant is defined by the server.
     */
    static int ITEMS_PER_PAGE = 128;

    static int UNAVAILABLE_USERNAME = 1;
    static int BANNED_FOR_ABUSE     = 2;
    static int UNSUPPORTED_FILE     = 3;
    static int NOT_AUTHORIZED       = 4;
    static int SYSTEM_ERROR         = 5;
    static int BAD_LOCATION         = 6;

    /**
     * The `scheme`://`authority` part of the URL of the server.
     * Eg: https://g2p.give2peer.org
     */
    protected String serverUrl;

    protected String username;
    protected String password;
    protected UsernamePasswordCredentials credentials;

    protected HttpClient client;

    public RestService(Server config)
    {
        serverUrl = config.getUrl();
        username = config.getUsername();
        password = config.getPassword();
        credentials = new UsernamePasswordCredentials(username, password);

        client = new DefaultHttpClient();
    }

    // HTTP QUERIES ////////////////////////////////////////////////////////////////////////////////

    public ArrayList<Item> findAroundPaginated(double latitude, double longitude, int page)
            throws IOException, URISyntaxException
    {
        return findAround(latitude, longitude, page * ITEMS_PER_PAGE);
    }

    public ArrayList<Item> findAround(double latitude, double longitude)
            throws IOException, URISyntaxException
    {
        return findAround(latitude, longitude, 0);
    }

    /**
     * Returns a list of at most 32 items.
     * Pages start at 0, and hold `ITEMS_PER_PAGE` items per page.
     *
     * @param latitude
     * @param longitude
     * @param offset
     * @return
     */
    public ArrayList<Item> findAround(double latitude, double longitude, int offset)
            throws URISyntaxException, IOException
    {
        String route = "/find/" + latitude + "/" + longitude + "/" + offset;
        String json = getJson(route);

        return jsonToItems(json);
    }


    public Item giveItem(Item item)
            throws URISyntaxException, IOException, JSONException, ErrorResponseException
    {
        String url = serverUrl + "/give";

        HttpPost request = new HttpPost();
        request.setURI(new URI(url));

        authenticate(request);

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("location", item.getLocation()));
        pairs.add(new BasicNameValuePair("title",    item.getTitle()));
        request.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8")); // deprecated in API level 22 !

        HttpResponse response = client.execute(request);

        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
        if (response.getStatusLine().getStatusCode() < 400) {
            item.updateWithJSON(new JSONObject(json));
        } else {
            throw new ErrorResponseException(json);
        }

        return item;
    }


    public Item pictureItem(Item item, File picture)
    {
        String url = serverUrl + "/picture/" + item.getId().toString();
        
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


    public boolean testServer()
            throws IOException, URISyntaxException
    {
        String json = getJson("/ping");
        return json.equals("\"pong\"");
    }


    public void register(String username, String password)
            throws URISyntaxException, IOException, JSONException, ErrorResponseException,
                   UnavailableUsernameException
    {
        String url = serverUrl + "/register";

        HttpPost request = new HttpPost();
        request.setURI(new URI(url));

        authenticate(request);

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("username", username));
        pairs.add(new BasicNameValuePair("password", password));
        request.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8")); // deprecated in API level 22 !

        HttpResponse response = client.execute(request);
        String json = EntityUtils.toString(response.getEntity(), "UTF-8");

        if (response.getStatusLine().getStatusCode() > 400) {
            JSONObject data = new JSONObject(json);
            JSONObject error = data.getJSONObject("error");
            int errorCode = error.getInt("code");
            String errorMessage = error.optString("message");
            if (errorCode == UNAVAILABLE_USERNAME) {
                throw new UnavailableUsernameException(errorMessage);
            } else {
                throw new ErrorResponseException(json);
            }
        }
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
            throws URISyntaxException, IOException
    {
        String url = serverUrl + route;

        HttpGet request = new HttpGet();
        request.setURI(new URI(url));

        authenticate(request);

        HttpResponse response = client.execute(request);

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
