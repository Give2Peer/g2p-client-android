Feature: Giving items
    In order to spread joy
    As a wise being
    I should be able to give an item

Scenario: With a location
    Given I start the main activity
     Then I should see a button named "Give"
     When I click on the button named "Give"
     Then I should be on the new item activity
     # There are multiple ways to control or mock the camera in order to keep testing here.
     # https://code.google.com/p/robotium/wiki/RobotiumForPreInstalledApps
     # https://github.com/bryanl/FakeCamera

# Scenarios to do
# With a location and a title
# With a location and a title and a description
# With a location and a title and a description and tags
# With a location and a title and a description and tags and a category

# Assertions ideas
# When I find the items around the same location
# -> We can do this by clicking the buttons
# Then there should be an item with the following properties:
# """
# title: Test
# ...
# """
# -> We'll need serious inside-the-box here, no ? Parsing the interface seems too much
