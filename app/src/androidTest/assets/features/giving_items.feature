Feature: Giving items
    In order to spread joy
    As a wise being
    I should be able to give an item

Scenario: Use the "share via..." feature of the Camera app
    Given I take a picture with the Camera and share it via Karma
     Then I should see a thumbnail of my picture
      And I should see a form input field for the title
      And I should see a form input field for the location
     When I fill the title with "My test item"
      And I fill the location with "Toulouse"
      And I submit
     Then I should be on my profile

     # There are multiple ways to control or mock the camera in order to keep testing here.
     # https://code.google.com/p/robotium/wiki/RobotiumForPreInstalledApps
     # https://github.com/bryanl/FakeCamera

# Scenarios to do
# With a location and a title
# With a location and a title and a description
# With a location and a title and a description and tags
# With a location and a title and a description and tags and a category

# BDD on Android is Hell©
