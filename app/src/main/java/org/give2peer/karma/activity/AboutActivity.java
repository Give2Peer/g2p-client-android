package org.give2peer.karma.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.mikepenz.materialdrawer.Drawer;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.rest.RestService;
import org.give2peer.karma.Application;
import org.give2peer.karma.R;
import org.give2peer.karma.response.Stats;
import org.give2peer.karma.service.RestClient;


/**
 * The about activity.
 *
 * Displays licences of vendor libraries, thanks, and various links to related content, such as :
 * - the portal website
 * - the source repository
 * - the issue tracker
 * - the authors email
 */
@EActivity(R.layout.activity_about)
public class AboutActivity extends AppCompatActivity
{
    Application app;

    static String WEBSITE_URL = "http://www.give2peer.org";
    static String SOURCE_URL  = "http://github.com/Give2Peer/g2p-client-android";
    static String REPORT_URL  = "http://github.com/Give2Peer/g2p-client-android/issues";
    static String EMAIL       = "give2peer@gmail.com";

    @ViewById
    Toolbar aboutToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (Application) getApplication();
    }

    // STATS ///////////////////////////////////////////////////////////////////////////////////////

    @RestService
    RestClient restClient;

    @Background
    @AfterViews
    void fetchStats() {
        Stats stats = restClient.getStats();
        updateInterfaceWithStats(stats);
    }

    @UiThread
    void updateInterfaceWithStats(Stats stats) {
        app.toasty(String.format("%d", stats.getItemsCount()));
    }

    // NAVIGATION DRAWER ///////////////////////////////////////////////////////////////////////////

    Drawer navigationDrawer;

    public Drawer getNavigationDrawer() {
        return navigationDrawer;
    }

    @AfterViews
    public void setUpNavigationDrawer() {
        if (null != navigationDrawer) return;
        navigationDrawer = app.setUpNavigationDrawer(this, aboutToolbar,
                Application.NAVIGATION_DRAWER_ITEM_ABOUT
        );
    }

    // UI LISTENERS ////////////////////////////////////////////////////////////////////////////////

    @Click public void aboutWebsiteButton() {
        app.openBrowser(this, WEBSITE_URL);
    }

    @Click public void aboutSourceButton() {
        app.openBrowser(this, SOURCE_URL);
    }

    @Click public void aboutReportButton() {
        app.openBrowser(this, REPORT_URL);
    }

    @Click public void aboutEmailButton() {
        // This is still a wip, and it does not work in the emulator.

        Intent emailIntent = new Intent(
                Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(EMAIL))
        );
        //emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        //emailIntent.putExtra(Intent.EXTRA_TEXT, body);

        //startActivity(emailIntent);
        //startActivity(Intent.createChooser(emailIntent, "Send an email"));

        try {
            startActivity(emailIntent);
        } catch (ActivityNotFoundException e) {
            app.toasty("No application can handle this request. Please install an email client.");
            e.printStackTrace();
        }
    }

}