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