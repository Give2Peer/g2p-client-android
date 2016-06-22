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