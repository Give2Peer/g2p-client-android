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



ROADMAP
=======

2.0.0
-----

- [ ] Action points.
- [ ] Experience points.
- [ ] Propose deletion of items submitted by someone else.


1.0.0
-----

A version for the beta community.

- [ ] Add a new Item offline, upload later.
- [ ] Require `https` servers.


0.3.0
-----

- [ ] Locally cache item thumbnail images.
- [ ] Register automatically when available.
- [ ] Delete items submitted by self.


CHANGELOG
=========

0.2.0
-----

- [x] Set up a feature suite in Gherkin
- [x] Add a Report Bug button


0.1.0
-----

- [x] List 32 items around my position, and launch maps and navigation.
- [x] Add a new Item with a location, a picture, and an optional title.
- [x] Detect my location using the GPS or WiFi.
- [x] List, add, edit and forget servers.
- [x] List, add, edit and forget locations.


TESTS AND SPECS
===============

We are big believers in behavior-driven development.

We use [Cucumber] and [Robotium] to set up a [Gherkin]-based feature suite than spans multiple
activities and tests application flows.

Simply run `app/src/androidTest/java/org/give2peer/give2peer/test/MainTest.java` in Android Studio.

[Cucumber]: https://cucumber.io
[Robotium]: https://robotium.org
[Gherkin]:  https://github.com/cucumber/cucumber/wiki/Gherkin


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

This will be yet another experiment in algorithms towards self-sustenance.

New features and maintenance will be provided by the
[FLOSS](http://en.wikipedia.org/wiki/Free_and_open-source_software#FLOSS) community
(aka: everybody), with the possible use of [bounties](https://www.bountysource.com).

Besides, all the code being open-source, anyone knowledgeable can host their own server.


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
- Ilane, for feeding us and putting up with our code-related ramblings.
- Misha, for picking the name _give2peer_, and his general enthusiasm and advice.
- Willou, for his mumbled advice.
- The ioth, because they can read between the books.
- Our families, for their unwavering support.