# Introduction #

SkyControl is a reusable sky simulation for jMonkeyEngine games.  Adding it to an existing JME3 project should be a simple six-step process:
  1. Add the JME3 Utilities Package JARs to the project.
  1. Disable any existing sky which might interfere with SkyControl.
  1. Add a SkyControl instance to some node in the scene graph.
  1. Configure the SkyControl instance.
  1. Enable the SkyControl instance.
  1. Test and tune as necessary.

To demonstrate this process, we'll apply it to BasicGame and then to CubeMapExample, a more sophisticated application.  I encourage you to follow along in your development environment.

# BasicGame #

## Prerequisites ##

You'll need:
  * A development machine with Java and the JME3 SDK installed.
  * A clean install of the JME3 Utilities Package.
  * A clean instance of the BasicGame project.

Instructions installing the SDK and the JME3 Utilities Package can be found at https://code.google.com/p/jme3-utilities/wiki/Build

To instantiate the BasicGame Project:
  * Open the "New Project" wizard in the IDE:
    * Menu bar -> "File" -> "New Project..."
  * Under "Categories:" select "JME3".
  * Under "Projects:" select "BasicGame".
  * Click on the "Next >" button.
  * For "Project Location:" specify a new folder on a local filesystem.
  * Click on the "Finish" button.

If you're unfamiliar with BasicGame, you may wish to run it (and/or examine the source code) to see how it works before modifying it.

## Add the JME3 Utilities Package JARs to the project ##

  * Open the project's properties in the IDE:
    * Right-click on the BasicGame project (not its assets) in the "Projects" window.
    * Select "Properties".
  * Under "Categories:" select "Libraries".
  * Click on the "Compile" tab.
  * If the SkyControl plugin is installed, then the SkyControl library can provide the required JARs:
    * Click on the "Add Library..." button.
    * Select the "SkyControl" library.
    * Click on the "Add Library" button.
    * Click on the "OK" button.
  * If the SkyControl plugin is not installed, then two JARs are required, one for classes and one for assets:
    * Click on the "Add JAR/Folder" button.
    * Navigate to the folder where you installed the JME3 Utilities Package and then to the "dist" folder therein.
    * Select the "jME3-utilities.jar" file.
    * Click on the "Open" button.
    * Click on the "Add JAR/Folder" button again.
    * Navigate to the "lib" folder.
    * Select the "jME3-utilities-assets.jar" file.
    * Click on the "Open" button again.
    * Click on the "OK" button.

## Disable any existing sky simulation ##

Since BasicGame comes without any sky simulation -- just the default (black) viewport background -- there is nothing to remove.

## Add a SkyControl instance ##

  * Open the "Main.java" source file in the IDE:
    * In the "Projects" window, expand the BasicGame project node.
    * Expand the "Source Packages" node under the BasicGame project.
    * Expand the "mygame" package under "Source Packages".
    * Select "Main.java" file under the "mygame" package.
    * Double-click to open the file.

The scene graph of BasicGame has only one node, the root node.  The root node is typically a good place to add SkyControl.

  * In the import section of "Main.java", add the following code:
```
   import jme3utilities.sky.SkyControl;
```
  * Scroll down to the simpleInitApp() method and insert the following code just before the final close-brace:
```
   SkyControl sc = new SkyControl(assetManager, cam, 0.9f, true, true);
   rootNode.addControl(sc);
```

The parameters of the constructor are documented in the JavaDoc for the SkyControl class.

## Configure the SkyControl instance ##

By default, SkyControl simulates midnight on March 21st in Wiltshire, England, with no clouds and a full moon.  Instead, let's configure 6 a.m. on February 10th in Sunnyvale, California with dense clouds:
  * In the import section of "Main.java", add the following code:
```
   import com.jme3.math.FastMath;
   import java.util.Calendar;
```
  * In simpleInitApp(), insert the following code just before the final close brace:
```
   sc.getSunAndStars().setHour(6f);
   sc.getSunAndStars().setObserverLatitude(37.4046f * FastMath.DEG_TO_RAD);
   sc.getSunAndStars().setSolarLongitude(Calendar.FEBRUARY, 10);
   sc.setCloudiness(1f);
```

Other configuration methods are documented in the JavaDoc for the SkyControl, SunAndStars, and Updater classes.

## Enable the SkyControl instance ##

Unlike most JME3 controls, SkyControl instantiates in a disabled state. In order to see the sky, you must enable the control:
  * In simpleInitApp(), insert the following code just before the final close-brace:
```
   sc.setEnabled(true);
```

## Test ##

To test the modified BasicGame, right-click in the "Main.java" editor window and select "Run File (Shift+F6)".

# CubeMapExample #

Sometimes you'll want to combine SkyControl with other sky elements.  The JME3 Utilities Packages includes CubeMapExample, a slightly more sophisticated application than BasicGame.  CubeMapExample includes a cube-mapped sky, lit terrain, and multiple light sources.  In this section, you'll see how SkyControl can be used to add sun, moon, and clouds to CubeMapExample.

## Add a SkyControl instance ##

  * In the Projects window of the IDE, navigate to the "jme3utilities.sky.test" source package.
  * Open the "CubeMapExample.java" file and study the code.

Since CubeMapExample is part of the JME3 Utilities Package, there are no libraries to add.  And since we plan to use the existing cube map, there's nothing to remove.

Both SkyFactory and SkyControl add geometries to `Bucket.Sky`. In the absence of a custom [GeometryComparator](http://hub.jmonkeyengine.org/javadoc/com/jme3/renderer/queue/GeometryComparator.html), geometries in this bucket are rendered in scene graph order. Since the cube map is opaque, we want to SkyControl to add its geometries _after_ the sky cube in the scene graph.

  * In the import section of "CubeMapExample.java", add the following code:
```
   import jme3utilities.sky.SkyControl;
```
  * Scroll down to the initializeSky() method and insert the following code just before the final close-brace:
```
        SkyControl sc = new SkyControl(assetManager, cam, 0.8f, false, true);
        rootNode.addControl(sc);
```

For now, we'll turn star motion off, since that simplifies things.  The lower value (0.8) for cloud flattening will make it easier to compensate for the scene's low horizon.

## Configure the SkyControl instance ##

  * It's very important to disable the control's built-in star maps so that the cube map's stars will be visible at night:
```
        sc.clearStarMaps();
```
  * Adjust the cloudiness so that clouds will be visible:
```
        sc.setCloudiness(0.8f);
```

## Enable the SkyControl instance ##

  * In initializeSky(), insert the following code just before the final close-brace:
```
        sc.setEnabled(true);
```

## Test and tune ##

At this point, the application should run successfully. However, for the best possible result, it needs some tuning.

  * To lower the edge of the cloud dome so that it's hidden by the terrain, add the following code:
```
        sc.setCloudYOffset(0.4f);
```
  * To see the scene in daylight, add this:
```
        sc.getSunAndStars().setHour(12f);
```
  * To synchronize the lights with SkyControl, add this to the import section:
```
import com.jme3.light.Light;
```
> and this to the end of the initializeSky() method:
```
        for (Light light : rootNode.getLocalLightList()) {
            if (light.getName().equals("ambient")) {
                sc.getUpdater().setAmbientLight((AmbientLight) light);
            } else if (light.getName().equals("main")) {
                sc.getUpdater().setMainLight((DirectionalLight) light);
            }
        }
```
The uneven shading of the level terrain is due to sunlight coming in at a low angle.  Since it's noon, the easiest way to raise the sun's elevation is to decrease the observer's latitude.
  * Try, for instance:
```
        sc.getSunAndStars().setObserverLatitude(0.2f);
```
The sun looks like a boring white disc in the southern sky.
  * For a more impressive sun, apply a bloom filter to it:
```
import com.jme3.post.filters.BloomFilter;
import jme3utilities.Misc;
```
> ...
```
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBlurScale(2.5f);
        bloom.setExposurePower(1f);
        Misc.getFpp(viewPort, assetManager).addFilter(bloom);
        sc.getUpdater().addBloomFilter(bloom);
```

## Adding star motion ##

To add star motion, it's not sufficient simply to change the control's constructor from
```
        SkyControl sc = new SkyControl(assetManager, cam, 0.8f, false, true);
```
to
```
        SkyControl sc = new SkyControl(assetManager, cam, 0.8f, true, true);
```

It's also necessary to rotate the cube map to match SkyControl's notions of time and space.  To achieve this, override the application's simpleUpdate() method:
```
    @Override
    public void simpleUpdate(float unused) {
        Spatial starMap = rootNode.getChild("Sky");
        SkyControl sc = rootNode.getControl(SkyControl.class);
        sc.getSunAndStars().orientExternalSky(starMap);
    }
```

To simulate the passage of time, we need some state and a mechanism to update it.  The jme3utilities.TimeOfDay class is an app state which addresses this need.

  * Import the class:
```
import jme3utilities.TimeOfDay;
```
  * Declare a field in CubeMapExample:
```
    TimeOfDay timeOfDay;
```
  * Initialize and attach it in simpleInitApp():
```
        timeOfDay = new TimeOfDay(19f);
        stateManager.attach(timeOfDay);
        timeOfDay.setRate(1000f);
```

All that remains is to update the control.  Add two lines to simpleUpdate(), right before the invocation of orientExternalSky():
```
        float hour = timeOfDay.getHour();
        sc.getSunAndStars().setHour(hour);
```

# Next steps #

For a demonstration of the more advanced features of SkyControl,
you may wish to study the [TestSkyControl](https://code.google.com/p/jme3-utilities/source/browse/trunk/src/jme3utilities/sky/test/TestSkyControl.java) class in the "jme3utilities.sky.test" package.