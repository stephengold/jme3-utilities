# release notes for the jme3-utilities-ui library and related tests

## Version 0.6.6 released on 28 November 2018

 + Fixed a bug where `FLYCAM_LOWER` mapped to the R key instead of the Z key.
 + Added an `ActionApplication.speed()` method.
 + Renamed `ActionApplication.getWrittenAssetDirPath()`
   to `writtenAssetDirPath()`.
 + Improved argument validation in the `Signals` class.
 + Based on version 2.14 of the jme3-utilities-heart library.

## Version 0.6.5 released on 23 September 2018

 + Renamed `UiVersion.getVersionShort()` to `versionShort()`. (API change)
 + Based on version 2.10 of the jme3-utilities-heart library.

## Version 0.6.4 released on 17 August 2018

Based on version 2.6 of the jme3-utilities-heart library.

## Version 0.6.3 released on 24 July 2018

Based on version 2.5 of the jme3-utilities-heart library.

## Version 0.6.2 released on 2 February 2018

Based on version 2.2 of the jme3-utilities-heart library.

## Version 0.6.1 released on 25 January 2018

Based on heart library v2.0 to make this library physics-independent.

## Version 0.6.0for32 released on 5 December 2017

 + 1st release to target JME 3.2
 + Replaced `PropertiesKey` with `UncachedKey`.
 + Simplified `InputMode` by removing the unused `stream` variable.

## Version 0.5.9 released on 13 September 2017

 + Changed semantics of the` Locators.register(List)` method.
 + Tried to avoid registering duplicate locators.
 + Added support for HttpZip and Url locators.
 + Standardized the BSD license texts.

## Version 0.5.8 released on 11 August 2017

 + Refined the API of `Locators` class.
 + Implemented save/restore for `Locators`.

## Version 0.5.7 released on 9 August 2017

 Added support for Zip locators.

## Version 0.5.6 released on 7 August 2017

+ Protected `flycamNames` from external modification.
+ Improved handling of filesystem exceptions.
+ Added several registration methods to the Locators class.

## Version 0.5.5 released on 16 July 2017

Publicized `InputMode.activate()` and `InputMode.deactivate()` so they can be
overriden.

## Version 0.5.4 released on 15 July 2017

+ Added a Locators class to manage asset locators.
+ Added getWrittenAssetDirPath() to the ActionApplication class.
+ Made ActionApplication.filePath() return an absolute pathname.

## Version 0.5.3 released on 20 May 2017

+ The library now depends on jme3-utilities-heart instead of SkyControl.
+ Added didntHandle() callback to the ActionApplication class.

## Version 0.5.2 released on 16 May 2017

Bugfix: When disabling an InputMode, remove the listener.

## Version 0.5.1 released on 10 May 2017

Extended the Hotkey class to include mouse buttons, redesigned the API.

## Version 0.5.0 released on 9 April 2017

This was the initial baseline release.