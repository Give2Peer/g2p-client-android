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

The client is be able to connect to multiple servers. This will allow big organizations to
staffsource internal item transfers.


License
-------

_Do whatever you want._ (unless you're a patent troll)


Help
----

If you know Android (or want to learn), you can help in a number of ways :

- Refactor this learner's code into a beautiful codebase.
- Suggest good third-party libraries to DRY the code.
- Give advice on how to give the emulator a default `geo fix`.
- Set up or give advice for a feature suite.
  _(I don't know [which one](https://android-arsenal.com/) to choose)_
- Report issues and submit ideas.
- Save a coin (if you have some) for future donations.
- And, of course, spread the word!


Disclaimer
----------

This is my first project with Android and Java.
There's horrible code everywhere.
Please counsel.



CHANGE LOG
==========

ROADMAP
-------

- Locally cache item thumbnail images.
- Register automatically and store credentials


1.0.0-rc1
---------

- List 32 items around my position, and launch maps and navigation.
- Detect my position using the GPS.
- Add a new Item with a location, a picture, and an optional title.
- List, add, edit and forget servers.



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


THANKS
======

- My roommate, for feeding me when I forget, and putting up with my code-related ramblings.
- His brother, for picking the name give2peer, and his general enthusiasm and advice.
- Willou, for his mumbled advice.
- The ioth, because they can read between the books.
- My family, for their unwavering support.