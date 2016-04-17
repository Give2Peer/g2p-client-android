Karma
=====

This is the source of the Android client application [Karma](http://give2peer.org).

It is made to be a client to the [Give2Peer REST API](http://g2p.give2peer.org/),
run by another FLOSS [server](https://github.com/Give2Peer/g2p-server-symfony) written in PHP.


Goal
----

Photograph and geotag lost items in public spaces (eg: a glove, a shoe, a 500€ bill), recyclable
detritus (eg: planks of wood, broken mirrors), and just plain gifts (eg: child toys, attic surplus).

The user interface should be oriented towards speed. Adding a new item should not take more than a
handful of seconds.

The client should be able to connect to multiple servers.
This may allow big organizations to staffsource internal item transfers and disgorge email inboxes
by setting up their own private server. "Who wants the old computer taking up space in my office ?"



ROADMAP
=======

We plan to make at least two Android clients. One (this one, the first one) should support old
phones (since API 11, Gingerbread), and the other should be coded properly, with partials, sliding
menus, and overall material design suitable for phones, glasses, watches and tablets.

There might even be a third (very light) version, using a `WebView` to load an HTML-based web app.


2.0.0
-----

A version with a more limited hardware support, since API 21.
This version leverages the full power of Material design, and provides tablet layouts as well as
phone layouts.

Two concurrent versions will probably be made for Android at this point, one fully native and the other as a webapp within a `WebView`.
The second one will probably be cheap to code as we'll need an HTML webapp for FirefoxOs anyways.

- [ ] Design a launcher icon.
- [ ] Statistics
- [ ] Propose deletion of items submitted by someone else. (tricky)


1.0.0
-----

A version for everybody, working under API 10.
This version (and all of 1.x.x) does not have a proper layout for tablets, and is ugly,
but it has a very extensive hardware support, and will be kept published as fallback for
users that are below API 21.

- [ ] Provide a lifespan to Items when adding them (allowed lifespans depend on the user level)
- [ ] Require `https` servers.
- [ ] Add french translation.
- [ ] Add spanish translation.
- [ ] Add a new Item offline, upload later.
- [ ] Warn the user that the app is unavailable during server maintenance.
- [ ] Provide a means to easily send a bug report to developers.


0.4.0
-----

A version for the beta community, released on Google Play.

- [x] Karma (experience) points.
- [x] Move to the versioned API.
- [x] Implement a Floating Action Button.
- [x] Action quotas depending on user level.
- [ ] List items submitted by self in the Profile.
- [ ] Edit items submitted by self from the Profile.
- [ ] Delete items submitted by self from the Profile.
- [ ] Automatic pre-registration.



CHANGELOG
=========

0.3.0
-----

- [x] Locally cache item thumbnail images.
- [x] Design a temporary basic launcher icon.
- [x] Start application by displaying items around the user on a map.
- [x] Improve automatic location detection using Google API.
- [x] Refactor to support API 10 (and onwards).
- [x] Add a new item using the "share" feature of the camera and gallery.
- [x] Registration activity.


0.2.0
-----

- [x] Set up a feature suite in Gherkin.
- [x] Add a Report Bug button.


0.1.0
-----

- [x] List 32 items around my position, and launch maps and navigation.
- [x] Add a new Item with a location, a picture, and an optional title.
- [x] Detect my location using the GPS or WiFi.
- [x] List, add, edit and forget servers.
- [x] List, add, edit and forget locations.


CAVEATS
=======

_They are plenty, in this project._

Notably, you need at least Gradle `2.12` to be able to build,
at least until `sugar` makes a release.


TROUBLESHOOTING
===============

`peer not authenticated` : install Gradle `2.12` and configure Android Studio to use it.

Sometimes, Android Studio will _forget_ which gradle version to use.


TESTS AND SPECS
===============

We are big believers in behavior-driven development.

We (tried to) use [Cucumber] and [Robotium] to set up a [Gherkin]-based feature suite than spans
multiple activities and tests application flows.

Simply run `app/src/androidTest/java/org/give2peer/give2peer/test/MainTest.java` in Android Studio.

Sadly, our current feature suite does not pass because _steps are too hard to implement_.
The mandatory usage of the Camera or Gallery picker when adding a new Item is tricky.
Robotium simply cannot control another application without some serious and delicate apk re-signing
(which [requires rooting](https://code.google.com/p/robotium/wiki/RobotiumForPreInstalledApps)), or
[clever mocking](https://github.com/bryanl/FakeCamera).

Mocking sounds nice, but we never could manage to set it up properly.
And never mind about automatizing the setup of mocks on the emulator !

Advice would be warmly welcome !

We really want a feature suite for the native Android app. It would make this project great.
We'll make one for the HTML/JS web app in the future, 'cause it is easy, and the Gherkin should be
almost the same.


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

There are duplication shenanigans with the `httpclient`.


KeyValue Spinner
----------------

https://github.com/delight-im/Android-KeyValueSpinner

This is pretty useful, and should probably be part of Android's core, somehow.
That lib is added as a JAR file. This is bad. It should be added through gradle.
It is not used anymore, since the recent refactorization, but will probably be again in the future.


RetroFit
--------

https://square.github.io/retrofit/

We do not use that lib right now because the default adapters choked on our JSON.
A refactorization implementing support for this (or an even better REST lib) is welcome.



OTHERS
======


Help
----

If you know Android and/or Java, we need some technical expertise :

- Refactor this learner's code into a beautiful codebase.
- Suggest good third-party libraries to DRY the code.
- Give advice on how to give the emulator a default `geo fix`.
- Give advice on how to handle the release `keystore` of an open-source project.
- Enrich and give advice for the feature suite.
- Write good code guidelines for Android / Java.

If not, you can still help in a number of ways :

- Write a Usage Licence Agreement that users will have to accept on registration.
- Write the text that will be shown in the Google Play store.
- Translate the app into your language. (we'll release at first with english, french and spanish)
- Design images, banners, logos, trophies, videos, everything.
- Report issues and submit ideas.
- And, of course, use the app and spread the word!


Donate
------

_"When it's free, you're the product."_

Hosting a server costs money. We can pay for a couple years from our own pocket, but eventually the
service must be able to pay for its own running costs, or it will shut down.

You'll be the product for a while, as the server will measure your total usage for a year
and show you (exactly!) how much you should give to keep it afloat for another year.

Of course, all these statistics will be published publicly.

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

This is our very first project with both Android and Java.
There's terrible code everywhere. **EVERYWHERE.**
Please counsel.


Thanks
------

- The community, without you there'd be nothing. You know who you are.
- Ilane, for feeding us and putting up with our code-related ramblings.
- Misha, for picking the name _give2peer_, and his general enthusiasm and advice.
- Willou, for his mumbled advice.
- Kiouze, for his awesome æsthetics.
- The ioth, because they can read between the books.
- Our other friends, for not killing us when we pester them about this.
- Our families, for their unwavering support.