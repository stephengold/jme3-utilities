# release notes for the SkyControl library and related tests

## Version 0.9.5 releases on TBD

Notable changes:
 + split off 2 general-purpose packages to new "jme3-utilites-heart" library

## Version 0.9.4 released on 14 May 2017

Notable changes:
 + fix a logic bug which set the main light to the wrong color

## Version 0.9.3 released on 5 May 2017

Notable changes:
 + add utility methods
 + use latest jCommander in tests
 + exclude assets from sources JAR

## Version 0.9.2 released on 9 April 2017

Notable changes:
 + fix a de-serialization bug that affected SkyControl and FloorControl
 + move all "virally licensed" assets out of the SkyControl library
 + build using Gradle instead of Ant
 + rename the {get/set}Cloud{Rate/YOffset} methods in the API

## Version 0.9.1 released on 8 March 2017

Major features added:
 + SkyControl is now serializable.
 + SkyControl no longer restricts the moon to the ecliptic.
 + TestSkyControl now includes a hotkey editor.
 + TestSkyControl no longer disables flyCam.

Other notable changes:
 + SkyControl puts moving stars on quads instead of domes - fewer triangles!
 + improved compatibility with jMonkeyEngine 3.1
 + App states are now based on NamedAppState instead of AbstractAppState.
 + moved noise/polygon/spline classes (not used by SkyControl) out of library

## Version 0.9.0 released on 21 January 2017

This was the initial baseline release.