Karma
=====

This is the source of the Android client application [Karma ∝](http://give2peer.org).

It is made to be a client to the [Give2Peer REST API](http://g2p.give2peer.org/),
run by another FLOSS [server](https://github.com/Give2Peer/g2p-server-symfony) written in PHP,
and [heavily featured](https://github.com/Give2Peer/g2p-server-symfony/tree/master/features#what).




Goal
----

**Photograph** and **geotag lost items** in public spaces (eg: a glove, a shoe, ~~a 500€ bill~~),
recyclable **MOOP** (eg: planks of wood), and just plain **gifts**.

_MOOP: Matter Out Of Place._

Adding a new item should not take more than a handful of seconds.

The app should display items on a map around your position.

The app should be able to **connect to multiple servers**.
Big organizations should be able to use it privately for their internal item transfers.

Karma aims to help in the following situations, for example :

- _"I need wood scraps to fix that chair."_
- _"I need worms to kickstart my compost."_
- _"I lost my left glove last night."_
- _"Who wants the old computer taking up space in my office ?"_



Google
------

If you're Google and you read this, please help us make that happen :

_"Ok google, please add these old shoes to my MOOP bag."_

Snaps picture, prefills fields, I'm done in a matter of seconds.
It's easier said than done, of course.

Then again, Google should make a lost&found/ownership app altogether. Any big player should.


ROADMAP
=======

We plan to make at least two Android clients. One (this one, the first one) should support old
phones (since API 10, Gingerbread), and the other should be coded properly, with partials, sliding
menus, and overall material design suitable for phones, glasses, watches and tablets.

There might even be a third (very light) version, using a `WebView` to load an HTML-based web app.


Overview of the major versions
------------------------------

They will be handled in different git repositories.


### Karma ∝ (Karma Unfinity)

This is the initial version, the version of this git repository.
It is messy, buggy, with little to no tablet layout support.

It should be made bug-free, but at some point we'll need to feature-freeze it.

It should work on almost all Android devices, but should only be offered as fallback when Karma ∞ is
unavailable.


### Karma ∞ (Karma Infinity)

Will be in repo `g2p-client-android-modern`.

Another native app, but well-coded this time.
A version with support for more modern hardware only, since API 21. (probably even higher)
This version leverages the full power of Material design, and provides tablet layouts as well as
phone layouts.
It will be kickstarted during a hackathon sometime this year or next, if enough people are interested.
Write hackathon@give2peer.org to be added to the hackathon mailing list.

Karma ∞, when released, should become the mainstream app.
(unless KarmaWeb proves more efficient, but why would it ?)
Karma ∝(the app in this repository) will be considered as fallback for older devices.


### Karma @ (Karma Web)

Will be in repo `g2p-client-android-web`.

Another (concurrent) app named "Karma Web" (or something) will possibly be made for Android at this
point, as an HTML webapp within a `WebView`. Favorably in [Dart], if it can be done.

The native Android code should handle receiving picture intents, but the rest should be mostly config.
We're very interested in benchmarking these apps against each other.


Future
------

A version for open beta-testing, working as early as API 10 (Android 3.0 Gingerbread).

All list items followed by parenthesis are optional, and the parenthesis should explain why.

Ideas
-----

In no particular order, these ideas have no release milestone set yet. Some may even never yield.

- [ ] Feature: MOOP bags, to regroup items and handle another recycling bin at home.
- [ ] Feature: display basic statistics of the app.
- [ ] Feature: suggest deletion of items submitted by someone else. (tricky)
- [ ] Feature: provide a lifespan to items I tag. (lifespans depend on the user karmic level)
- [ ] Feature: add a new Item offline, upload later. (lots of work)
- [ ] Feature: easily send a bug report to developers on caught error. (we use the OS)
- [ ] Feature: warn the user that the app is unavailable during server maintenance. (to be tested)
- [ ] Feature: some form of OAuth2 as fallback for Maps, as we are subjected to quotas.
- [ ] Feature: edit items I authored in my profile.
- [ ] Feature: change my username when level 1, against some (9?) karma points.
- [ ] Feature: add my facebook to my user account.
- [ ] Feature: add my google+ to my user account.
- [ ] Feature: add my twitter to my user account.
- [ ] Feature: show items around as a list from closest to furthest.
- [ ] Feature: communities.
- [ ] Feature: a logging utility that we can more easily disable for production.
- [ ] BugHunt: fail gracefully when item image type or size is unsupported.
- [ ] L10N:    español.


2.0.0
-----

A version for the beta community.

- [ ] Feature: report an offensive item.



1.7.0
-----

- [ ] Feature: full I18N (Interplanetarization)
- [ ] L10N:    français.
- [ ] BugHunt: issue #2.


1.6.0
-----

- [ ] Feature: delete items I authored.
- [ ] Feature: list notifications in the profile.
- [ ] Feature: thank someone for an item.


1.5.0
-----

- [ ] Feature: all-time world leaderboard.
- [ ] Feature: add my email to my user account.
- [ ] Feature: log in with an existing user account.
- [ ] Feature: request new credentials by email.
- [ ] Feature: an activity for the details of an item.
- [ ] Feature: provide a description to items.


1.4.0
-----

- [x] Feature: a navigation drawer. \o/
- [x] Feature: re-enable taking a picture from the app.
- [x] Setting: move the map's region drawer FAB to the bottom center.
- [x] Feature: display a count of available characters when relevant.
- [x] BugHunt: automatic geolocation when adding an item.
- [x] Design:  a splash of color.



CHANGELOG
=========

1.3.0 (12-06-2016)
------------------

- [x] BugHunt: android M and external storage permissions.
- [x] BugHunt: disable SSL for item thumbnails.
- [x] Feature: show new item location on a small map.
- [x] Feature: support `png` images for items.
- [x] Feature: support `gif` images for items.
- [x] Feature: support `webp` images for items.
- [x] Refacto: Use a third-party `LocationManager`.


1.2.0 (30-05-2016)
------------------

- [x] Setting: enable `https` protocol, like a monkey.
- [x] BugHunt:  map loader keeps turning after onboarding activity.
- [x] BugHunt:  make sure temporary picture files are removed.
- [x] Feature: suggest enabling the GPS.
- [x] Feature: parallax the new item picture.


1.1.0 (11-05-2016)
------------------

Still in closed alpha...

- [x] Setting: allow bigger pictures.
- [x] Feature: rotate pictures when submitting a new item.
- [x] Feature: a simple onboarding activity.


1.0.1 (03-05-2016)
------------------

- [x] BugHunt: switching from map to profile and map again launches a new instance of the map.
- [x] BugHunt: item popup window of the wrong size on high dpi screens.
- [x] BugHunt: issue #1 : items sometimes disappear from the map.
- [x] BugHunt: issue #2 : uploading a picture fails silently.


1.0.0 (01-05-2016)
------------------

A version for the alpha community, released privately on Google Play.

This major version is available by invitation only.

- [x] Feature: display item details in a crude popup window.
- [x] BugHunt: support for images shared through browser downloads and others.
- [x] Design:  a launcher icon. _(Kiouze FTW)_


0.4.0 (28-04-2016)
------------------

A marathon of code !

- [x] Feature: karma points.
- [x] Feature: display karma points in the profile.
- [x] Feature: item tagging quotas depending on user's karmic level.
- [x] Setting: move to the versioned API `v1/`.
- [x] Design:  Floating Action Button.
- [x] Feature: list items I tagged in my profile.
- [x] BugHunt:  crash upon manual registration.
- [x] Feature: gain one karma point when launching the map, once per day.
- [x] Feature: automatic pre-registration.


0.3.0
-----

- [x] Feature: locally cache item thumbnail images.
- [x] Design:  a temporary basic launcher icon.
- [x] Feature: display items around my position on a map.
- [x] Feature: automatic location detection using Google Services API.
- [x] Refacto: support for API 10 with appcompat and lucky charms.
- [x] Feature: register manually on the registration activity.


0.2.0
-----

- [x] Setting: set up a feature suite in Gherkin. (now oh-so broken ._.)
- [x] Feature: add a new item using the "share" feature of the camera or gallery.
- [x] Feature: send a picture along with an item.
- [x] Feature: display the profile after adding a new item.


0.1.0
-----

- [x] Feature: guess my location using the GPS or WiFi.
- [x] Feature: list, add, edit and forget servers.
- [x] Feature: list, add, edit and forget locations.
- [x] Feature: open github.com from the menus to manually report a bug.


CAVEATS
=======

This project has a bunch of caveats. We're working to reduce their impact and numbers, but...


Gradle
------

You need at least Gradle `2.12` to be able to build, at least until `sugar` makes a release.
This is to be able to use jitpack.io in gradle, because `introduction` needs it.
Just grab it, unpack it alongside the other, and configure Android Studio.

Otherwise, you'll get a `peer not authenticated` error.


API 10
------

Polyfills, shims, appcompat... You know. Hard to implement, hell to maintain, feature blockers.

This version will have no tablet layout, because there are no fragments (that we know of).

I'm getting scolded hard for choosing API 10. And with reason;
It's kind of like trying to make a responsive website for IE6.


Settings
--------

We hacked in and around the Settings to handle Servers, and now they're a mess.
Server configurations should have proper dedicated CRUD activities, instead.



TROUBLESHOOTING
===============

`peer not authenticated` : this is because of the caveat above.
install Gradle `2.12` and configure Android Studio to use it.

Note: sometimes, Android Studio will _forget_ which gradle version to use and revert to `2.10` ?


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

These libs were removed from SDK 23, but we still use them.


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
I suspect this would not be the case anymore, so a refactorization implementing support for this
(or an even better REST lib) would be welcome.


Real-time Database Inspection
-----------------------------

Not used right now, but may be very useful some day.

http://www.idescout.com/wiki/index.php/Main/ConnectToAndroidDbs


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

You'll be the product for a while, as the server will measure its costs for a year and show
you (exactly!) how much life expectancy (in minutes) you add to the service when you donate.

Of course, all these statistics will be published publicly.

Transparency is the key. If you know about good software suites / practices to that effect, we are
interested.

This will be yet another experiment in self-sustaining algorithms.

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


Credits
-------

- The community, without you there'd be nothing. You know who you are.
- Ilane, for feeding us and putting up with our code-related ramblings.
- Misha, for picking the name _give2peer_, and his general enthusiasm and advice.
- Willou, for his mumbled advice and ruthless testing.
- Kiouze, for his awesome æsthetics.
- The ioth, because they can read between the books.
- Our other friends, for not killing us when we pester them about this.
- Our families, for their unwavering support.
- And of course everyone that told us that this was useless and could not be done.
- And you, because you read this whole inane page up to the end !



[MOOP]: http://burningman.org/event/preparation/leaving-no-trace/moop/
[Dart]: https://www.dartlang.org/