package org.give2peer.give2peer.test;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;

import com.robotium.solo.Condition;
import com.robotium.solo.Solo;

import org.give2peer.give2peer.activity.MainActivity;

import cucumber.api.CucumberOptions;
import cucumber.api.PendingException;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

/**
 * This was HELL to figure out how to set up. But now it works ! :)
 *
 * We use Cucumber to parse the features written in Gherkin, and Robotium to test through our
 * multiple activities.
 *
 * This is both the feature runner _and_ the step definitions. Not sure how to decouple those.
 *
 * Run this class using Android Studio.
 */

@CucumberOptions(features = "features")
public class MainTest extends ActivityInstrumentationTestCase2<MainActivity>
{
    private Solo solo;

    public MainTest() {
        super(MainActivity.class);
    }

    @Before
    public void setup() {
        Log.i("G2P", "setup()");
        solo = new Solo(getInstrumentation(), getActivity());
    }

    @After
    public void teardown() {
        Log.i("G2P", "teardown()");
    }

    @Given("I start the activity ([^ ]+)$")
    public void iStartTheActivity(String activityClassName) {
        Log.i("G2P", "RUN STEP 1");
        // todo: start provided activity

        // Sanity checks
        assertNotNull("Robotium's Solo is set up.", solo);
        assertNotNull("Instrumentation has been injected", getInstrumentation());
        solo.assertCurrentActivity("Current activity is " + activityClassName, activityClassName);
    }

    @Then("^I should see the view ([^ ]+)$")
    public void iShouldSeeTheView(String viewId) {
        assertNotNull(solo.getView(viewId));
    }

    @Then("^I should see a button named \"([^ ]+)\"$")
    public void iShouldSeeTheButton(String buttonName) {
        assertNotNull(solo.getButton(buttonName));
    }

    @When("^I click on the button named \"([^ ]+)\"$")
    public void iClickOnTheButtonNamed(String buttonName) {
        solo.clickOnButton(buttonName);
    }

    @Then("I should be on the activity ([^ ]+)$")
    public void iShouldBenOnTheActivity(String activityClassName) {
        solo.assertCurrentActivity("Current activity is " + activityClassName, activityClassName);
    }

    @Then("the grid ([^ ]+) should have (\\d+) elements$")
    public void theGridShouldHaveElements(String viewId, int howMany) {
        final GridView v = (GridView) solo.getView(viewId);
        // Wait for the grid to be filled and shown by the async task
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return v.isEnabled() && v.getVisibility() != View.GONE;
            }
        }, 20*1000);
        assertEquals(howMany, v.getCount());
    }

}
