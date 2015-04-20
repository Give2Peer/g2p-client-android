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
import org.give2peer.give2peer.entity.ServerConfiguration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This manages the collections of Items. It doubles as a REST service.
 *
 * Responsibilities :
 * - Fetch items from server HTTP REST API.
 *   It fetches them synchronously, so there's also a bunch of AsyncTasks that use these methods.
 * - Keep fetched items up to date.
 * - Store items locally in a cache for offline use. (don't know how, yet)
 */
public class RestService
{
    /**
     * The server limits the number of items sent in the response.
     * This constant is defined by the server.
     */
    static int ITEMS_PER_PAGE = 32;

    /**
     * The `scheme`://`authority` part of the URL of the server.
     * Eg: https://g2p.give2peer.org
     */
    protected String serverUrl;

    protected String username;
    protected String password;
    protected UsernamePasswordCredentials credentials;

    protected Map<Integer, Item> items;

    protected HttpClient client;

    RestService(ServerConfiguration config)
    {
        serverUrl = config.getUrl();
        username = config.getUsername();
        password = config.getPassword();
        credentials = new UsernamePasswordCredentials(username, password);

        client = new DefaultHttpClient();

        items = new HashMap<Integer, Item>();
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
        String url = serverUrl + "/find/" + latitude + "/" + longitude + "/" + offset;

        ArrayList<Item> itemsList = new ArrayList<Item>();

        HttpGet request = new HttpGet();
        request.setURI(new URI(url));

        authenticate(request);

        HttpResponse response = client.execute(request);

        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
        itemsList = jsonToItems(json);

        return itemsList;
    }


    public Item giveItem(Item item)
    {
        String url = serverUrl + "/give";

        try {
            HttpPost request = new HttpPost();
            request.setURI(new URI(url));

            authenticate(request);

            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("location", item.getLocation()));
            pairs.add(new BasicNameValuePair("title", item.getTitle()));
            request.setEntity(new UrlEncodedFormEntity(pairs));

            HttpResponse response = client.execute(request);

            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            if (response.getStatusLine().getStatusCode() < 400) {
                item.updateWithJSON(new JSONObject(json));
            } else {
                Log.e("G2P", "Give Item Error : "+json);
            }
        } catch (URISyntaxException|IOException|JSONException e) {
            Log.e("G2P", e.getMessage());
            e.printStackTrace();
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
     * Try to parse the `json` and build the items in it.
     * Updates the internal `Map` of `Item`s with the new data.
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
                // todo: move this memoization to upper function, it should not be here.
                if (items.containsKey(item.getId())) {
                    item = items.get(item.getId());
                    item.updateWithJSON(jsonObject);
                } else {
                    items.put(item.getId(), item);
                }
                itemsList.add(item);
            }
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data : " + e.toString());
        }

        return itemsList;
    }

}
