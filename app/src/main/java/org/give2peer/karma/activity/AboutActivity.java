package org.give2peer.karma.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.mikepenz.materialdrawer.Drawer;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.LongClick;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.rest.RestService;
import org.give2peer.karma.Application;
import org.give2peer.karma.R;
import org.give2peer.karma.response.ErrorResponse;
import org.give2peer.karma.response.Stats;
import org.give2peer.karma.service.RestClient;
import org.give2peer.karma.service.RestClientErrorHandler;
import org.give2peer.karma.service.ErrorResponseHandler;


/**
 * The about activity.
 *
 * Spouts rubbish about the purpose of the app.
 *
 * Displays licences of vendor libraries, thanks, and various links to related content, such as :
 * - the portal website
 * - the source repository
 * - the issue tracker
 * - the authors email
 */
@EActivity(R.layout.activity_about)
public class AboutActivity extends AppCompatActivity implements ErrorResponseHandler
{
    @App
    Application app;

    static String WEBSITE_URL = "http://www.give2peer.org";
    static String SOURCE_URL  = "http://github.com/Give2Peer/g2p-client-android";
    static String REPORT_URL  = "http://github.com/Give2Peer/g2p-client-android/issues";
    static String EMAIL       = "give2peer@gmail.com";

    @ViewById
    Toolbar aboutToolbar;
    @ViewById
    TextView aboutStats;

    // STATS ///////////////////////////////////////////////////////////////////////////////////////

    // This is a WIP, we're using this activity for testing the new RestClient.

    @RestService
    RestClient restClient;

    @AfterInject
    void setupRestClient() {
        restClient.setRootUrl(app.getCurrentServer().getUrl());
        restClient.setRestErrorHandler(new RestClientErrorHandler(this));
    }

    @UiThread
    public void handleErrorResponse(ErrorResponse errorResponse) {
        app.snack(this, errorResponse.getMessage());
    }

    @Background
    @AfterViews
    void fetchStats() {
        Stats stats = restClient.getStats();
        if (null != stats) updateInterfaceWithStats(stats);
    }

    @UiThread
    void updateInterfaceWithStats(Stats stats) {
        String msg = getString(R.string.about_stats, stats.getItemsTotal(), stats.getUsersCount());
        aboutStats.setText(msg);
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
            app.toasty(R.string.error_no_email_client);
            e.printStackTrace();
        }
    }

    // SECRETS (shhhh, don't tell!) ////////////////////////////////////////////////////////////////

    @Click(R.id.aboutCopyrightLogos)
    public void aboutCopyrightLogosClick() {
        app.toast(R.string.about_copyright_logos_description);
    }

    @LongClick(R.id.aboutCopyrightLogos)
    public void aboutCopyrightLogosLongClick() {
        app.launchServerConfig(this);
    }

}