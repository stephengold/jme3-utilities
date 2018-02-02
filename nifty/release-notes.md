# release notes for the jme3-utilities-nifty library and related tests

## Version 0.7.9 released on 2 February 2018

 + More flexible policies for the placement of submenus.
 + Disable warnings while loading the screen layout in BasicScreenController.
 + Don't pre-validate the screen layout in BasicScreenController.
 + Add readColorBank() methods to the GuiScreenController and
   GuiWindowController classes.
 + Incorporate the fix for Nifty issue #384.
 + Remove the useControls and useStyles directives from the layouts for
   simple popups.

## Version 0.7.8 released on 25 January 2018

 + Bypass the jme3-niftygui library to avoid its dependency on
   nifty-style-black.
 + Base on heart library v2.0 to avoid its dependency on jme3-bullet.

## Version 0.7.7 released on 10 January 2018

Add disableCheckBox() methods to the GuiScreenController and
 GuiWindowController classes.

## Version 0.7.6 released on 4 January 2018

 + Remove disableSlider() and enableSlider() methods from GuiSceneController.
 + Fix an assertion error triggered by clicking a mouse button in "bind" screen.
 + In "bind" screen, selecting a bound hotkey using a keypress will now select
   its action as well.
 + Add a readVectorBank() method to the GuiWindowController class.

## Version 0.7.5 released on 3 January 2018

 + Remove one of the PopScreenController.showPopupMenu() methods.
 + Add allowNull option to float/integer/long dialog controllers.
 + Privatize activateDialog() and activatePopupMenu() in PopScreenController.
 + Fix the logic error that caused FloatDialog to accept "NaN" as input.
 + Add the GuiWindowController class.
 + Add dialog controllers for booleans, doubles, and vectors.
 + Add enable/disable methods for named sliders to GuiScreenController.
 + Implement positioning policies for submenus.
 + Setting a button's text to "" will now hide the button.

## Version 0.7.4 released on 19 December 2017

Replace GuiScreenController.setButtonLabel() with setButtonText(), with
 new semantics.

## Version 0.7.3for32 released on 5 December 2017

1st release to target JME 3.2

## Version 0.7.2 released on 7 November 2017

API changes to GuiScreenController:
 + Add SliderTransform arguments to readSlider() and setSlider() methods.
 + Remove updateSlider() and updateLogSlider() methods.
 + Add readVectorBank() method.

Move dialog controllers to new package 'jme3utilities.nifty.dialog'.

## Version 0.7.1 released on 20 September 2017

 + Use commas to join indices selected by a multi-select dialog box.
 + Remove test message from PopScreenController.showMultiSelectDialog().
 + Allow a dialog box to display another dialog box.

## Version 0.7.0 released on 19 September 2017

 + Require a list or array to invoke PopScreenController.showPopupMenu().
 + Add multi-select dialog box.
 + Standardized the BSD license texts.

## Version 0.6.10 released on 5 September 2017

 + Align infobox body text with the top of its panel.
 + Accommodate infobox body text up to 400 lines long.

## Version 0.6.9 released on 3 September 2017

 + Added 2 more dialog controllers: LongDialog and TextEntryDialog.
 + Widened text-entry dialog boxes by 100 pixels.
 + In popup menus, use the main "0" key to select the 10th item.
 + Reduced display darkening for popup menus, from 50% to 37%.

## Version 0.6.8 released on 16 July 2017

+ Use wrap="true" for labels in dialog boxes and infoboxes.
+ Made the GUI render order an initialization option.

## Version 0.6.7 released on 3 July 2017

Render NiftyGUI after JME's guiNode.

## Version 0.6.6 released on 25 June 2017

Publicize constructors for dialog controllers.

## Version 0.6.5 released on 24 June 2017

+ Added simpler API for PopScreenController.showTextEntryDialog() method.
+ Add 2 dialog controllers: FloatDialog and IntegerDialog.

## Version 0.6.4 released on 20 May 2017

+ The library now depends on jme3-utilities-heart instead of SkyControl.
+ Register each window controller with its screen controller.
+ Added some methods, raised the visibility of others.

## Version 0.6.3 released on 10 May 2017

+ Added a simple confirmation dialog box.
+ Added DialogController class for customizing dialog boxes.
+ Added a feedback section to text-entry dialog box.
+ Bound the F5 key in both menu-input mode and dialog-input mode.

## Version 0.6.2 released on 2 May 2017

Public access to updateSliderStatus() in GuiScreenController class.

## Version 0.6.1 released on 27 April 2017

Added informational dialog boxes and changed APIs, renaming showDialog() and
 showPopup() methods.

## Version 0.6.0 released on 15 April 2017

Added WindowController class and revamped APIs (especially GuiScreenController)
 making most methods public and non-static.

## Version 0.5.2 released on 13 April 2017

Improved sizing of popup menus.

## Version 0.5.1 released on 12 April 2017

Fixed assertion error when activating BindScreen from a popup.

## Version 0.5.0 released on 9 April 2017

This was the initial baseline release.