Karma
=====

This is the source of the Android client application [Karma](http://give2peer.org).


Goal
----

**Photograph** and **geotag lost items** in public spaces (eg: a glove, a shoe, ~~a 500€ bill~~),
recyclable **MOOP** (eg: planks of wood), and just plain **gifts**.

_MOOP: Matter Out Of Place._


### Examples

Karma aims to help in the following situations, for example :

- _"I need wood scraps to fix that chair."_
- _"I need worms to kickstart my compost."_
- _"I lost my left glove last night !"_
- _"Who wants the old computer taking up space in my office ?"_


### Features

- Adding a new item requires less than a handful of seconds.
- Displays items on a map around your position.
- Supports 99,9% of android devices.
- Supports multiple servers so that you can make your own.


### Open-source

Free, libre, open-source software is in most aspects the best kind of software.
Karma is and always will be _free_ as both in _free beer_ and _free speech_.


Google
------

If you're Google and you read this, please help us make that happen :

_"Ok google, please add these old shoes to my MOOP bag."_

It's easier said than done, of course.

Then again, Google should make a lost&found/ownership app altogether. Any big player should.


Server
------

This is made to be a client to the [Give2Peer REST API](http://g2p.give2peer.org/),
run by another FLOSS [server](https://github.com/Give2Peer/g2p-server-symfony) written in PHP,
and [heavily featured](https://github.com/Give2Peer/g2p-server-symfony/tree/master/features#what).


Roadmap
=======

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
- [ ] Feature: share added item on facebook.
- [ ] Feature: share added item on twitter.
- [ ] Feature: share added item on google+.
- [ ] Feature: show items around as a list from closest to furthest.
- [ ] Feature: communities.
- [ ] Feature: a logging utility that we can more easily disable for production.
- [ ] BugHunt: fail gracefully when item image type or size is unsupported.
- [ ] L10N:    español.
- [ ] Feature: list notifications in the profile.


2.0.0
-----

A version for the open beta community.

- [ ] Feature: thank someone for an item.
- [ ] Cleanup: remove all deprecated code.
- [ ] Feature: add my email to my user account.
- [ ] Feature: log in with an existing user account.
- [ ] Feature: request new credentials by email.


1.7.0
-----

- [ ] Feature: all-time world leaderboard.
- [ ] Feature: I18N (Interplanetarization) for left-to-right languages.
- [ ] Feature: L10N en français.


1.6.0
-----

- [ ] Refacto: clear up a bunch of internal things left to do
- [x] BugHunt: issue #2: item images are mishandled.
- [x] Feature: configure the drop animation of the map pins
- [x] Feature: a proper Settings activity
- [x] Config:  move servers configuration out of harm's way
- [x] Feature: an About activity.
- [x] Feature: delete items I authored.
- [x] BugHunt: google play services crashes when internet is unavailable on API 10.
- [x] Feature: report an abusive item, since level 1.


CHANGELOG
=========


1.5.0 (05-07-2016)
------------------

- [x] Design:  custom map marker icons, version GIMP noob
- [x] Feature: an activity for the details of an item.
- [x] Feature: provide a description to items.
- [x] Feature: try to fetch the user's location if not done already when showing the new item map.
- [x] Feature: iconified radio buttons for item type.
- [x] Design:  swindle, paint and hack some icons for item types.


1.4.0 (22-06-2016)
------------------

- [x] Feature: a navigation drawer. \o/
- [x] Feature: re-enable taking a picture from the app.
- [x] Setting: move the map's region drawer FAB to the bottom center.
- [x] Feature: display a count of available characters when relevant.
- [x] BugHunt: automatic geolocation when adding an item.
- [x] Design:  a splash of color.
- [x] Feature: thumbnail image in the map's item marker's info window.


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




OTHERS
======


Help
----

If you know Android and/or Java, we need some technical expertise :

- Refactor this learner's code into a beautiful codebase.
- Suggest good third-party libraries to DRY the code.
- Give advice on how to handle the release `keystore` of an open-source project.
- Enrich and give advice for the feature suite.
- Write or recommend good code guidelines for Android / Java.

If not, you can still help in a number of ways :

- Write a Usage Licence Agreement that users will have to accept on registration.
- Write the text that will be shown in the Google Play store.
- Translate the app into your language. (we'll release at first with english, french and spanish)
- Design images, banners, logos, trophies, videos, everything.
- Report issues and submit ideas.
- And, of course, use the app and spread the word!


Donate
------

Hosting a server costs money. We can pay for a couple years from our own pocket, but eventually the
service must be able to pay for its own running costs, or it will shut down.

This will be yet another experiment in self-sustaining algorithms.

A donation process will be set up someday.


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