Give2Peer
=========

This is the source of the Android client application [Give2Peer](http://give2peer.org).


Goal
----

Photograph and geotag lost items in public spaces (eg: a glove, a shoe, a 500€ bill), recyclable
detritus (eg: planks of wood, broken mirrors), and just plain gifts (eg: baby apparatus, attic
surplus).

The user interface should be oriented towards speed. Adding a new item should not take more than a
handful of seconds.

The client is able to connect to multiple servers.
This will allow big organizations to staffsource internal item transfers and disgorge email inboxes
by setting up their own private server.


Help
----

If you know Android (or want to learn), you can help in a number of ways :

- Refactor this learner's code into a beautiful codebase.
- Suggest good third-party libraries to DRY the code.
- Give advice on how to give the emulator a default `geo fix`.
- Set up or give advice for a feature suite.
  _(We don't know [which one](https://android-arsenal.com/tag/98?sort=rating) to choose)_
- Report issues and submit ideas.
- And, of course, use the app and spread the word!


Donate
------

_"If it's free, you're the product."_

Hosting a server costs money. We can pay for a couple years from our own pocket, but eventually the
service must be able to pay for its own running costs, or it will shut down.

You'll be the product for a while, as the server will measure your total usage for a year or two
and show you (exactly!) how much you should give to keep it afloat for another year.

Transparency is the key. If you know about good software suites / practices to that effect, we are
interested.

This will be yet another experiment in algorithms towards fiduciary self-sustenance.

New features and maintenance will be provided by the
[FLOSS](http://en.wikipedia.org/wiki/Free_and_open-source_software#FLOSS) community
(aka: everybody), with the possible use of [bounties](https://www.bountysource.com).

Besides, all the code being open-source, anyone knowledgeable can host their own server.

_"Caring profits everyone."_



ROADMAP
=======

1.0.0-beta
----------

- Locally cache item thumbnail images.
- Register automatically when available.
- Delete items submitted by self.
- Propose deletion of items submitted by someone else.



CHANGELOG
=========

1.0.0-alpha
-----------

- List 32 items around my position, and launch maps and navigation.
- Add a new Item with a location, a picture, and an optional title.
- Detect my location using the GPS.
- List, add, edit and forget servers.
- List, add, edit and forget locations.


TESTS AND SPECS
===============

We are big believers in behavior-driven development, yet this app has no test-suite whatsoever, right now.

Android's documentation uses [Espresso](http://developer.android.com/training/testing/ui-testing/espresso-testing.html).

[JBehave](http://jbehave.org/) and Robotium stand out.
([Truth](http://google.github.io/truth/) looks fine, too)
Calabash introduces Ruby code, and we don't understand how it works.

Any PR setting up a skeleton feature suite (in Gherkin) is welcome.



LIBS
====

Here's a breakdown of the third-party libs we use, or looked at.


Apache HttpComponents
---------------------

File upload requires `org.apache.httpcomponents:httpmime:4.3`
and `org.apache.httpcomponents:httpclient-android:4.3.5`.

There are duplication shenanigans with the `httpclient`, it may not even be needed.


RetroFit
--------

https://square.github.io/retrofit/

We do not use that lib right now because the default adapters choked on our JSON.
A refactorization implementing support for this (or an even better REST lib) is welcome.



OTHERS
======

License
-------

_Do whatever you want._ (unless you're a patent troll)


Disclaimer
----------

This is our first project with Android and Java.
There's terrible code everywhere. EVERYWHERE.
Please counsel.


Thanks
------

- The community, without you there'd be nothing. You know who you are.
- My roommate, for feeding me when I forget, and putting up with my code-related ramblings.
- His brother, for picking the name _give2peer_, and his general enthusiasm and advice.
- Willou, for his mumbled advice.
- The ioth, because they can read between the books.
- My family, for their unwavering support.