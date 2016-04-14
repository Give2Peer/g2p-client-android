Feature: Finding items on a map
    In order to find items I lost or need
    As a user
    I want to see the items around me on a map


# Note : these are ideal features, not practical ones.
#        They simply don't pass, because I have NO IDEA on how to implement most of the steps.
#        I'm not even sure it can be done. I gave it a good try, and I suspect that fine-tuned
#        mocking may do the trick, but I simply don't have the skills to mock properly, and
#        all mocks should be in reusable libraries ; maybe the ocean will provide, someday ?

# Issue : this requires a testing server filled with fixtures, (say bdd.give2peer.org, tbd)
#         and the server should provide an API to reset its database.
#         Then, one step could call this API.
#         ...
#         Actually, this is probably much more doable than most of the steps described in this file.


Background:
    Given the app is configured to use the server bdd.give2peer.org
      And the server bdd.give2peer.org has been refreshed


Scenario: Launch the app with the GPS enabled and Internet available
    Given the GPS is enabled
          # Maybe replace Toulouse by coordinates ? Or do it in the steps, whatever.
          # Or just don't specify where we are in Gherkin, it's not really relevant.
      And I am in Toulouse
      And Internet is available
     When I launch the app
      And I wait for the app to finish initializing
     Then I should see a map around my position
          # We just have to make sure that the testing server fixtures include some items.
      And there should be at least 2 items on the map
      And there should be at most 64 items on the map
     When I click on the closest item
     Then I should see its name
      And I should see its distance to me
     When I click on its name
     Then I should see more information about the item in a modal window
