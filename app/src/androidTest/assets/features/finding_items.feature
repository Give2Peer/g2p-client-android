Feature: Finding items
    In order to find items I need
    As a gatherer
    I should be able to list items around my position

# This scenario is obsolete o.O
# TDD is hell on Android T_T
Scenario: List without filters
    Given I start the main activity
     Then I should see a button named "Find"
     When I click on the button named "Find"
     Then I should be on the list around activity
      And I should see the view itemsGridView
      And the grid itemsGridView should have 32 elements