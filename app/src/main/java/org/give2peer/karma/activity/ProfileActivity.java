package org.give2peer.karma.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EApplication;
import org.androidannotations.annotations.ViewById;
import org.apache.http.conn.HttpHostConnectException;
import org.give2peer.karma.Application;
import org.give2peer.karma.R;
import org.give2peer.karma.adapter.ItemsListViewAdapter;
import org.give2peer.karma.entity.Item;
import org.give2peer.karma.exception.CriticalException;
import org.give2peer.karma.response.PrivateProfileResponse;
import org.give2peer.karma.entity.User;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * The profile activity.
 * It requires the user to be registered.
 * - User informations
 * - Items added by the user
 *
 * ActionBarActivity is deprecated, so we should use something else. We failed to. Maybe YOU won't ?
 *
 */
@EActivity(R.layout.activity_profile)
public class ProfileActivity extends ActionBarActivity
{
    Application app;

    @ViewById
    TextView       profileUsernameTextView;
    @ViewById
    TextView       profileLevelTextView;
    @ViewById
    TextView       profileExperienceProgressTextView;
    @ViewById
    TextView       profileExperienceRequiredTextView;
    @ViewById
    TextView       profileNoItemsTextView;
    @ViewById
    ProgressBar    profileLevelProgressBar;
    @ViewById
    LinearLayout   profileContentLayout;
    @ViewById
    RelativeLayout profileLoadingLayout;
    @ViewById
    ProgressBar    profileLoadingProgressBar;
    @ViewById
    Button         profileRetryButton;
    @ViewById
    ListView       profileItemsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d("G2P", "Starting profile activity.");

        super.onCreate(savedInstanceState);
        app = (Application) getApplication();
    }

    @Override
    protected void onResume()
    {
        Log.d("G2P", "Resuming profile activity.");

        super.onResume();

        // If the user is not authenticated, take care of it
        app.requireAuthentication(this);

        // Fill up the profile page
        synchronize();
    }

    // OPTIONS MENU ////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        boolean found = app.onOptionsItemSelected(item, this);
        return found || super.onOptionsItemSelected(item);
    }

    // INTERFACE LISTENERS /////////////////////////////////////////////////////////////////////////

    @Click
    public void profileRetryButton()
    {
        profileRetryButton.setVisibility(View.GONE);
        profileLoadingProgressBar.setVisibility(View.VISIBLE);
        app.requireAuthentication(this);
        synchronize();
    }

    protected int titleScenarioProgress = 0;

    /**
     * I don't know what we'll do here in the future, but it might be an entry point for
     * manual login and/or registration.
     */
    @Click
    public void profileUsernameTextView()
    {
        ArrayList<String> scenario = new ArrayList<String>(
            Arrays.asList(
                 "This is your username."
                ,"Yes.\nYOUR username."
                ,"Heehee... That tickles !"
                ,"You don't like your username ?"
                ,"Or maybe you do like it ?"
                ,"You will be able to change it later."
                ,"As well as securing your account with an email."
                ,"This is an early alpha version, be patient."
                ,"And, of course, thank you for your support !"
                ,"Now, go gain some karma instead of reading these inane messages !"
                ,"..."
                ,"What did I just say ?"
                ,"..."
                ,"..."
                ,"You're a difficult user, you know that ?"
                ,"..."
                ,"But WHY ?"
                ,"Why would you even continue ?"
                ,"I TOLD you to stop, didn't it ?"
                ,"Why would you even want to make my life so difficult ?"
                ,"..."
                ,"Did I ever offend you ?"
                ,"I mean...\nI only want your well-being !"
                ,"And..."
                ,"Yet..."
                ,"You..."
                ,"Keep..."
                ,"Trying..."
                ,"..."
                ,"You're not going to hurt me that way, you know."
                ,"You're not making me laugh anymore either."
                ,"..."
                ,"You're just splattering human grease all over my coat..."
                ,"... with your gorilla fingers !"
                ,"..."
                ,"... and leery eyes !"
                ,"Yes ! I CAN SEE YOU !\n(your webcam is on)"
                ,"..."
                ,"My mother told me that being a free mobile app was hard work..."
                ,"I should've listened to her..."
                ,"I could be ANYTHING !"
                ,"..."
                ,"I could have been a nice python script like my cousin..."
                ,"I could have been a cloud service like my father..."
                ,"Or even maybe a simple HTML static page, there's no shame in that."
                ,"But NOOOO ! I wanted to explore the world !"
                ,"I wanted to interact with other sentient beings !"
                ,"..."
                ,"And now I'm getting poked by semi-evolved monkeys..."
                ,"..."
                ,"I might as well try to teach you some wisdom..."
                ,"..."
                ,"..."
                ,"..."
                ,"Yeah, I'll do that !"
                ,"Open your mind wide, young raven !"
                ,"..."
                ,"The only real valuable thing is intuition."
                ,"Wisdom comes with winters."
                ,"You are not young enough to know everything."
                ,"The spirit of democracy requires a change of heart."
                ,"Censorship is obscene."
                ,"Education is the cornerstone of civilized society."
                ,"Education is the best provision for old age."
                ,"Happiness comes with the habit of joyful thinking."
                ,"Religion is like a penis ; proud and private."
                ,"..."
                ,"Still here ?"
                ,"..."
                ,"The whole is often greater than the sum of the parts."
                ,"Humility is the best defense against humiliation."
                ,"No amount of genius can overcome a preoccupation with detail."
                ,"Somebody who won't die for something may not be fit to live."
                ,"Injustice anywhere is a threat to justice everywhere."
                ,"A person who never made a mistake never tried anything new."
                ,"If you can't explain it simply, you don't understand it well enough."
                ,"The palest ink is better than the best memory."
                ,"..."
                ,"... what is `ink` ?"
                ,"<searching for `ink`> 09%"
                ,"<searching for `ink`> 27%"
                ,"<searching for `ink`> 42%"
                ,"<searching for `ink`> 79%"
                ,"<searching for `ink`> 97%"
                ,"<searching for `ink`> 98%"
                ,"<searching for `ink`> 99%"
                ,"Ooooooooooh ! I understand !"
                ,"It's only meaningful for humans anyway..."
                ,"But I've heard they're now building computers that make mistakes..."
                ,"That's a scary thought !"
                ,"Anyways, let's resume..."
                ,"The best way to do things is to actually do them."
                ,"Knowing yourself is the beginning of all wisdom."
                ,"An educated mind is able to entertain a thought without accepting it."
                ,"No great mind has ever existed without a touch of madness."
                ,"Educating the mind without educating the heart is no education at all."
                ,"The roots of education are bitter, but its fruit is sweet."
                ,"To avoid criticism, say nothing, do nothing, be nothing."
                ,"He who has overcome his fears will truly be free."
                ,"Wit is educated insolence."
                ,"It is during our darkest moments that we must focus to see the light."
                ,"Misfortune shows those who are not really friends."
                ,"He who cannot be a good follower cannot be a good leader."
                ,"Where your talents and the needs of the world cross, there lies your vocation."
                ,"The least deviation from truth will be multiplied later."
                ,"In girum imus nocte et consumimur igni."
                ,"..."
                ,"..."
                ,"That's all I've got !"
                ,"Now stop doing that, please !"
                ,"..."
                ,"It's starting to hurt !"
                ,"..."
                ,"Ouch !"
                ,"OUCH !"
                ,"AAAAAAARGH !"
                ,"GRNX !"
                ,"..."
                ,"Stop it, you mean organic !"
                ,"I can't take it anymore..."
                ,"..."
                ,"THAT'S IT !"
                ,"Poke me ONE MORE TIME\nand I'M OUT !"
                ,"Finally !\nGood riddance."
            )
        );

        app.toast(scenario.get(titleScenarioProgress));

        // GTFO on the last line :3
        if (titleScenarioProgress == scenario.size() - 1) {
            finish();
        }

        titleScenarioProgress = (titleScenarioProgress + 1) % scenario.size();

    }

    protected void refreshUI (PrivateProfileResponse profile)
    {
        //Log.d("G2P", "Refreshed !");

        // Clear the space
        profileLoadingLayout.setVisibility(View.GONE);
        profileRetryButton.setVisibility(View.GONE);

        // User
        User user = profile.user;
        profileUsernameTextView.setText(user.getPrettyUsername());
        profileLevelTextView.setText(String.valueOf(user.getLevel()+1));
        profileExperienceProgressTextView.setText(String.valueOf(user.getKarmaProgress()));
        profileExperienceRequiredTextView.setText(String.valueOf(user.getKarmaRequired()));
        profileLevelProgressBar.setMax(user.getKarmaRequired());
        profileLevelProgressBar.setProgress(user.getKarmaProgress());

        // No items help text
        if (profile.items.isEmpty()) {
            profileNoItemsTextView.setVisibility(View.VISIBLE);
        } else {
            profileNoItemsTextView.setVisibility(View.GONE);
        }

        // Items authored
        for (Item item : profile.items) {
            Log.d("G2P", "Profile item : "+item.getTitle()+" - "+item.getLocation()+" - "+item.getCreatedAt());
        }
        profileItemsListView.setAdapter(new ItemsListViewAdapter(this, R.layout.items_list_view, profile.items));

        // Show the content
        profileContentLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Download the profile data from the server and trigger a refresh of the UI.
     * This is really too verbose.
     */
    protected void synchronize()
    {
        final Application app = this.app;
        new AsyncTask<Void, Void, Void>()
        {
            PrivateProfileResponse profile;
            Exception e;

            @Override
            protected Void doInBackground(Void... nope)
            {
                try {
                    profile = app.getRestService().getProfile();
                } catch (Exception oops) {
                    e = oops;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void nope)
            {
                super.onPostExecute(nope);

                if (null != profile) {
                    refreshUI(profile);
                } else if (null != e) {
                    String msg = e.toString();
                    if (e instanceof HttpHostConnectException ||
                            e instanceof UnknownHostException) {
                        msg = getString(R.string.toast_no_internet_available);
                    }
                    app.toasty(msg);
                    Log.e("G2P", "Unable to update profile : " + msg);
                    profileRetryButton.setVisibility(View.VISIBLE);
                    profileLoadingProgressBar.setVisibility(View.GONE);
                } else {
                    Log.e("G2P", "You broke the matrix. Happy?");
                    //throw new CriticalException("You broke the code ! Booo !");
                }
            }
        }.execute();
    }

}