# release log for the jme3-utilities-ui library and related tests

## Version 0.9.4 released on TBD

Based on jMonkeyEngine version 3.4.0-stable and Heart version 6.4.4.

## Version 0.9.3+for34 released on 23 April 2021

Based on jMonkeyEngine version 3.4.0-beta1 and Heart version 6.4.3+for34.

## Version 0.9.2 released on 9 February 2021

 + Published to MavenCentral instead of JCenter.
 + Based on version 6.4.2 of the Heart Library.

## Version 0.9.1 released on 23 November 2020

 + Added combo hints to help screens.
 + Allowed spaces in signal names (should still be discouraged).
 + Added the `listCombos()` method to the `InputMode` class.
 + Added `countSignals()`, `isPositive()`, and `signalName()` methods
   to the `Combo` class.
 + Clarified the `TestCombo` application.
 + Reimplemented `Signals` as a subclass of `SignalTracker`.
 + Based on version 6.2.0 of the Heart Library.

## Version 0.9.0 released on 16 August 2020

 + Redesigned the API of the `InputMode` class:
   + Renamed the `findHotkeys()`, `getActionName()`, `getConfigPath()`,
     and `getShortName()` methods.
   + Privatized the `suspend()` and `resume()` methods.
   + Protected the `activate()` and `deactivate()` methods.
   + Extended the `bind(String, int)` method to accept multiple keycodes.
 + Redesigned the API of the `Hotkey` class:
   + De-publicized the `map()` and `unmap()` methods.
   + Renamed the `getButtonCode()`, `getCode()`, `getKeyCode()`, and `getName()`
     methods.
 + Other changes to the library API:
   + Deleted the `exists()` method from the `Signals` class.
   + Privatized the `mapActions()` method from the `HelpUtils` class.
 + Implemented combos and combo actions, including a `Combo` class.
 + Implemented a suspend/resume stack and added `suspendAndActivate()`
   and `resumeLifo()` methods to the `InputMode` class.
 + Added the `bindSignal()` convenience methods to the `InputMode` class.
 + Added the `TestCaos`, `TestCombo`, `TestCursors`,
   and `TestTwoModes` applications.
 + Based on version 6.0.0 of the Heart Library.

## Version 0.8.3 released on 27 April 2020

 + Updated the `DisplaySettings` class to allow BPP, MSAA, and gamma-correction
   changes to be applied via a restart, since JME issue #801 is fixed.
 + Added a `getApplication()` method to the `DisplaySettings` class.
 + Based on version 5.3.0 of the Heart Library.

## Version 0.8.2 released on 1 April 2020

Based on version 5.2.1 of the Heart Library.

## Version 0.8.1for33 released on 4 February 2020

 + Changed the Maven groupId from "jme3utilities" to "com.github.stephengold"
 + Based on version 5.0 of the Heart Library.

## Version 0.8.0for33 released on 24 January 2020

 + Moved the `PropertiesLoader` class to the jme3-utilities-heart
   library. (API change)
 + Finalized `simpleInitApp()` in the `ActionApplication` class. (API change)
 + Bufix: corrected the name for `KEY_NUMPADCOMMA` in the `Hotkey` class.
 + Added names for `KEY_PRTSCR`, `KEY_DECIMAL`, and `KEY_SUBTRACT`
   to the `HotKey` class.
 + Added screenshot support to the `ActionApplication` class.
 + Added black backgrounds to all help nodes.
 + Added the `TestHotKeys` application to the tests sub-project.
 + Added a help node to the `TestFlyCam` application.
 + Added a `dumpAll()` method to the `Locators` class, for debugging.
 + Added "green.cur" cursor texture to the built-in assets.
 + Based on version 4.4 of the jme3-utilities-heart library.

## Version 0.7.10for33 released on 4 January 2020

 + Publicized the `HelpUtils` class.
 + Changed `HelpUtils` to sort displayed hints by action.
 + Based on version 4.3 of the jme3-utilities-heart library and the NEW
   version 3.3.0-beta1 of the jme3-core library.

## Version 0.7.9for33 released on 8 December 2019

 + Added the `HelpUtils` class.
 + Implemented `unbind()` by keycode in the `InputMode` class.
 + Based on version 4.2 of the jme3-utilities-heart library and
   version v3.3.0-beta1 of the jme3-core library, which was later deleted!

## Version 0.7.8for33 released on 23 September 2019

Based on version 4.0 of the jme3-utilities-heart library and
version 3.3.0-alpha5 of the jme3-core library.

## Version 0.7.7for33 released on 25 August 2019

Based on version 3.0 of the jme3-utilities-heart library.

## Version 0.7.6for33 released on 7 August 2019

Based on version 2.31 of the jme3-utilities-heart library.

## Version 0.7.4for33 released on 5 July 2019

 + Privatized the `shortName` field and publicized the
   `signalActionPrefix` field of the `InputMode` class.
 + Based on version 2.29 of the jme3-utilities-heart library and
   version 3.3.0-alpha2 of the jme3-core library.

## Version 0.7.3 released on 7 June 2019

 + Changed `ActionApplication` to feed a de-scaled `tpf` to `flyCam`.
 + Based on version 2.28.1 of the jme3-utilities-heart library.

## Version 0.7.2 released on 19 March 2019

Based on version 2.23 of the jme3-utilities-heart library.

## Version 0.7.1 released on 10 March 2019

 + Added a `CameraOrbitAppState` class based on the one in MinieExamples.
 + Based on version 2.21 of the jme3-utilities-heart library.

## Version 0.7.0 released on 13 January 2019

 + Added a `DisplaySizeLimits` class.
 + Added a `DisplaySettings` class based on the one in Maud.

## Version 0.6.8 released on 9 January 2019

 + Added an `ActionApplication.getSettings()` method.
 + Based on version 2.18 of the jme3-utilities-heart library.

## Version 0.6.7 released on 28 December 2018

 + Removed the deprecated `ActionApplication.getWrittenAssetDirPath()` method.
 + Based on version 2.17 of the jme3-utilities-heart library.

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