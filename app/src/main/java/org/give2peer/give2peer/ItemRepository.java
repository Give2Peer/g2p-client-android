package org.give2peer.give2peer;

import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This manages the collections of Items.
 *
 * Responsibilities :
 * - Fetch items from server HTTP REST API.
 * - Keep fetched items up to date.
 * - Store items locally in a cache for offline use. (don't know how, yet)
 */
public class ItemRepository
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

    ItemRepository(String serverUrl, String username, String password)
    {
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
        credentials = new UsernamePasswordCredentials(username, password);

        client = new DefaultHttpClient();

        items = new HashMap<Integer, Item>();
    }

    protected ArrayList<Item> findAroundPaginated(double latitude, double longitude, int page)
    {
        return findAround(latitude, longitude, page * ITEMS_PER_PAGE);
    }

    protected ArrayList<Item> findAround(double latitude, double longitude)
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
    protected ArrayList<Item> findAround(double latitude, double longitude, int offset)
    {

        String url = serverUrl + "/find/" + latitude + "/" + longitude + "/" + offset;

        ArrayList<Item> itemsList = new ArrayList<Item>();

        try {
            HttpGet request = new HttpGet();
            request.setURI(new URI(url));

            BasicScheme scheme = new BasicScheme();
            Header authorizationHeader = scheme.authenticate(credentials, request);
            request.addHeader(authorizationHeader);

            HttpResponse response = client.execute(request);

            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            itemsList = jsonToItems(json);

        } catch (URISyntaxException|AuthenticationException|IOException e) {
            Log.e("Item", e.getMessage());
            e.printStackTrace();
        }

        return itemsList;
    }


    protected void giveItem(Item item)
    {
        String url = serverUrl + "/give";

        try {
            HttpPost request = new HttpPost();
            request.setURI(new URI(url));

            BasicScheme scheme = new BasicScheme();
            Header authorizationHeader = scheme.authenticate(credentials, request);
            request.addHeader(authorizationHeader);

            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("location", item.getLocation()));
            pairs.add(new BasicNameValuePair("title", item.getTitle()));
            request.setEntity(new UrlEncodedFormEntity(pairs));

            HttpResponse response = client.execute(request);

            Log.i("G2P", "Reponse Entity : "+response.getEntity().toString());
            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            Log.i("G2P", "Reponse JSON : "+json);
//            itemsList = jsonToItems(json);

        } catch (URISyntaxException|AuthenticationException|IOException e) {
            Log.e("Item", e.getMessage());
            e.printStackTrace();
        }
    }

    // UTILS ///////////////////////////////////////////////////////////////////////////////////////

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
        // try parse the string to a JSON object
        try {
            Item item;
            JSONObject jsonObject;
            JSONArray rows = new JSONArray(json);
            for (int i = 0 ; i < rows.length() ; i++) {
                jsonObject = rows.getJSONObject(i);
                item = new Item(jsonObject);
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
