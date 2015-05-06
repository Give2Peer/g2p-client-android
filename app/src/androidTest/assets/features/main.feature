Feature: Finding items
    In order to find items
    As a gatherer
    I should be able to list items around my position

Scenario: Using dummy server
    Given I start the activity MainActivity
     Then I should see a button named "Find"
     When I click on the button named "Find"
     Then I should be on the activity ListAroundActivity
      And I should see the view itemsGridView
      And the grid itemsGridView should have 32 elements