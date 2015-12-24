# jme3-utilities
Automatically exported from code.google.com/p/jme3-utilities

Java packages, developed for sgold's jMonkeyEngine games, which might prove useful in similar projects.
Includes SkyControl for sky simulation.

The source files are in JDK 7 format with CRLF line endings.

Summary of SkyControl features:
 + sun, moon, stars, horizon haze, and up to six cloud layers
 + compatible with static backgrounds such as cube maps
 + high resolution textures are provided -- or customize with your own textures
 + compatible with effects such as SimpleWater, shadows, and bloom
 + continuous and reversible motion and blending of cloud layers
 + option to foreshorten clouds near the horizon
 + continuous and reversible motion of sun, moon, and stars based on time of day
 + updater to synchronize lighting and shadows with sun, moon, and clouds
 + continuous scaling of sun, moon, and clouds
 + option for continuously variable phase of the moon
 + demonstration apps and online tutorial provided
 + complete source code provided under FreeBSD license


=================================================
A.0 INSTALL THE SDK AND THE jme3utilities PACKAGE
=================================================

A.1 JMonkeyEngine (JME3) Software Development Kit (SDK)

 1. Download the jMonkeyEngine SDK from http://hub.jmonkeyengine.org/downloads
 2. Install the SDK, which includes:
   + the engine itself,
   + an integrated development environment (IDE) based on NetBeans, and
   + the Blender 3D application.

The hardware and software requirements of the SDK are documented at 
http://hub.jmonkeyengine.org/wiki/doku.php/jme3:requirements

A.2 Source files

[This section needs to be written for Git.]

There will be problems due to a missing JAR file. Do not attempt to resolve them yet!

A.3 External files

 1. Download the JCommander JAR file from 
      "http://mvnrepository.com/artifact/com.beust/jcommander" 
    to the "jars" folder of the new project.
    
 2. If you plan to generate your own star maps, download the Yale Catalogue of Bright Stars from 
      "http://www-kpno.kpno.noao.edu/Info/Caches/Catalogs/BSC5/catalog5.html"
    to the "assets/Textures/skies" folder of the new project.
    
A.4 Project configuration

 1. (optional) Rename the project:
  1.1 Right-click on the jme3-utilities project in the "Projects" window.
  1.2 Select "Rename...".
  1.3 Type a new name in the text box.
  1.4 Click on the "Rename" button.
 2. Add the downloaded JAR to the project:
  2.1 Open the project's properties in the IDE:
  2.2 Right-click on the new project (not its assets) in the "Projects" window.
  2.3 Select "Properties".
  2.4 Under "Categories:" select "Libraries".
  2.5 Click on the "OK" button.
 3. Resolve any remaining project problems.
 4. Build the project's JARs:
  4.1 Right-click on the new project in the "Projects" window.
  4.2 Select "Clean and Build".
 5. (optional) Generate the project's javadoc:
  5.1 Right-click on the new project in the "Projects" window.
  5.2 Select "Generate Javadoc".

Note: The source files are in JDK 7 format, but the IDE creates new JME3 projects in JDK 5 format.


=========================================
B.0 ADDING SkyControl TO AN EXISTING GAME
=========================================

SkyControl is a reusable sky simulation for jMonkeyEngine games.

Adding it to an existing JME3 project should be a simple six-step process:
 1. Add the JME3 Utilities Package JARs to the project.
 2. Disable any existing sky which might interfere with SkyControl.
 3. Add a SkyControl instance to some node in the scene graph.
 4. Configure the SkyControl instance.
 5. Enable the SkyControl instance.
 6. Test and tune as necessary.

To demonstrate this process, we'll apply it to BasicGame and then to 
CubeMapExample, a more sophisticated application. I encourage you to 
follow along in your development environment.

B.1 BasicGame

You'll need:
 + A development machine with Java and the JME3 SDK installed.
 + A clean install of the JME3 Utilities Package.
 + A clean instance of the BasicGame project.

To instantiate the BasicGame Project:
 1. Open the "New Project" wizard in the IDE:
 2. Menu bar -> "File" -> "New Project..."
 3. Under "Categories:" select "JME3".
 4. Under "Projects:" select "BasicGame".
 5. Click on the "Next >" button.
 6. For "Project Location:" specify a new folder on a local filesystem.
 7. Click on the "Finish" button.

If you're unfamiliar with BasicGame, you may wish to run it (and/or examine 
the source code) to see how it works before modifying it.

B.1.1 Add the JME3 Utilities Package JARs to the project

Open the project's properties in the IDE:
 1. Right-click on the BasicGame project (not its assets) in the "Projects" window.
 2. Select "Properties".
 3. Under "Categories:" select "Libraries".
 4. Click on the "Compile" tab.
 5. If the SkyControl plugin is installed, then the SkyControl library can provide the required JARs:
   5.1 Click on the "Add Library..." button.
   5.2 Select the "SkyControl" library.
   5.3 Click on the "Add Library" button.
   5.4 Click on the "OK" button.
 6. If the SkyControl plugin is not installed, then two JARs are required, one for classes and one for assets:
   6.1 Click on the "Add JAR/Folder" button.
   6.2 Navigate to the folder where you installed the JME3 Utilities Package and then to the "dist" folder therein.
   6.3 Select the "jME3-utilities.jar" file.
   6.4 Click on the "Open" button.
   6.5 Click on the "Add JAR/Folder" button again.
   6.6 Navigate to the "lib" folder.
   6.7 Select the "jME3-utilities-assets.jar" file.
   6.8 Click on the "Open" button again.
   6.9 Click on the "OK" button.
   
B.1.2 Disable any existing sky simulation

Since BasicGame comes without any sky simulation -- just the default (black) 
viewport background -- there is nothing to remove.

B.1.3 Add a SkyControl instance

 1. Open the "Main.java" source file in the IDE:
   1.1 In the "Projects" window, expand the BasicGame project node.
   1.2 Expand the "Source Packages" node under the BasicGame project.
   1.3 Expand the "mygame" package under "Source Packages".
   1.4 Select "Main.java" file under the "mygame" package.
   1.5 Double-click to open the file.

The scene graph of BasicGame has only one node, the root node. The root node is typically a good place to add SkyControl.

 2. In the import section of "Main.java", add the following code:
   import jme3utilities.sky.SkyControl;
   
 3. Scroll down to the simpleInitApp() method and insert the following code just before the final close-brace:
   SkyControl sc = new SkyControl(assetManager, cam, 0.9f, true, true);
   rootNode.addControl(sc);

The parameters of the constructor are documented in the JavaDoc for the SkyControl class.

B.1.4 Configure the SkyControl instance

By default, SkyControl simulates midnight on March 21st in Wiltshire, England, with no 
clouds and a full moon. Instead, let's configure 6 a.m. on February 10th in Sunnyvale, 
California with dense clouds:

 1. In the import section of "Main.java", add the following code:
   import com.jme3.math.FastMath;
   import java.util.Calendar;
   
 2. In simpleInitApp(), insert the following code just before the final close brace:
   sc.getSunAndStars().setHour(6f);
   sc.getSunAndStars().setObserverLatitude(37.4046f * FastMath.DEG_TO_RAD);
   sc.getSunAndStars().setSolarLongitude(Calendar.FEBRUARY, 10);
   sc.setCloudiness(1f);
   
Other configuration methods are documented in the JavaDoc for the SkyControl, SunAndStars, and Updater classes.

B.1.5 Enable the SkyControl instance

Unlike most JME3 controls, SkyControl instantiates in a disabled state. In order to see the sky, you 
must enable the control:

 1. In simpleInitApp(), insert the following code just before the final close-brace:
   sc.setEnabled(true);
   
B.1.6 Test

To test the modified BasicGame, right-click in the "Main.java" editor window and select "Run File (Shift+F6)".


B.2 CubeMapExample

Sometimes you'll want to combine SkyControl with other sky elements. The JME3 Utilities Packages includes 
CubeMapExample, a slightly more sophisticated application than BasicGame. CubeMapExample includes a 
cube-mapped sky, lit terrain, and multiple light sources. In this section, you'll see how SkyControl can 
be used to add sun, moon, and clouds to CubeMapExample.

In the Projects window of the IDE, navigate to the "jme3utilities.sky.test" source package.
Open the "CubeMapExample.java" file and study the code.

Since CubeMapExample is part of the JME3 Utilities Package, there are no libraries to add.
And since we plan to use the existing cube map, there's nothing to remove.

B.2.1 Add a SkyControl instance

Both SkyFactory and SkyControl add geometries to Bucket.Sky. In the absence of a custom GeometryComparator, 
geometries in this bucket are rendered in scene graph order. Since the cube map is opaque, we want to 
SkyControl to add its geometries after the sky cube in the scene graph.

 1. In the import section of "CubeMapExample.java", add the following code:
   import jme3utilities.sky.SkyControl;
   
 2. Scroll down to the initializeSky() method and insert the following code just before the final close-brace:
        SkyControl sc = new SkyControl(assetManager, cam, 0.8f, false, true);
        rootNode.addControl(sc);
        
For now, we'll turn star motion off, since that simplifies things. The lower value (0.8) for cloud
flattening will make it easier to compensate for the scene's low horizon.

B.2.2 Configure the SkyControl instance

 1. It's very important to disable the control's built-in star maps so that the cube map's stars 
    will be visible at night:
        sc.clearStarMaps();
        
 2. Adjust the cloudiness so that clouds will be visible:
        sc.setCloudiness(0.8f);
        
B.2.3 Enable the SkyControl instance

 1. In initializeSky(), insert the following code just before the final close-brace:
        sc.setEnabled(true);
        
B.2.4 Tuning

At this point, the application should run successfully. However, for the best possible result,
it needs some tuning.

 1. To lower the edge of the cloud dome so that it's hidden by the terrain, add the following code:
        sc.setCloudYOffset(0.4f);
 2. To see the scene in daylight, add this:
        sc.getSunAndStars().setHour(12f);
 3. To synchronize the lights with SkyControl, add this to the import section:
      import com.jme3.light.Light;
    and this to the end of the initializeSky() method:
        for (Light light : rootNode.getLocalLightList()) {
            if (light.getName().equals("ambient")) {
                sc.getUpdater().setAmbientLight((AmbientLight) light);
            } else if (light.getName().equals("main")) {
                sc.getUpdater().setMainLight((DirectionalLight) light);
            }
        }
        
The uneven shading of the level terrain is due to sunlight coming in at a low angle. Since it's 
noon, the easiest way to raise the sun's elevation is to decrease the observer's latitude.
Try, for instance:
        sc.getSunAndStars().setObserverLatitude(0.2f);

The sun looks like a boring white disc in the southern sky.
For a more impressive sun, apply a bloom filter to it:
    import com.jme3.post.filters.BloomFilter;
    import jme3utilities.Misc;
...
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBlurScale(2.5f);
        bloom.setExposurePower(1f);
        Misc.getFpp(viewPort, assetManager).addFilter(bloom);
        sc.getUpdater().addBloomFilter(bloom);
        
B.2.5 Adding star motion

To add star motion, it's not sufficient simply to change the control's constructor from
        SkyControl sc = new SkyControl(assetManager, cam, 0.8f, false, true);
to
        SkyControl sc = new SkyControl(assetManager, cam, 0.8f, true, true);
It's also necessary to rotate the cube map to match SkyControl's notions of time and space.
To achieve this, override the application's simpleUpdate() method:
    @Override
    public void simpleUpdate(float unused) {
        Spatial starMap = rootNode.getChild("Sky");
        SkyControl sc = rootNode.getControl(SkyControl.class);
        sc.getSunAndStars().orientExternalSky(starMap);
    }

To simulate the passage of time, we need some state and a mechanism to update it.
The jme3utilities.TimeOfDay class is an app state which addresses this need.

 1. Import the class:
     import jme3utilities.TimeOfDay;
     
 2. Declare a field in CubeMapExample:
    TimeOfDay timeOfDay;
    
 3. Initialize and attach it in simpleInitApp():
        timeOfDay = new TimeOfDay(19f);
        stateManager.attach(timeOfDay);
        timeOfDay.setRate(1000f);

All that remains is to update the control.

 4. Add two lines to simpleUpdate(), right before the invocation of orientExternalSky():
        float hour = timeOfDay.getHour();
        sc.getSunAndStars().setHour(hour);
        
B.3 Next steps

For a demonstration of the more advanced features of SkyControl, you may wish to study 
the TestSkyControl class in the "jme3utilities.sky.test" package.

===================
C.0 ACKNOWLEDGMENTS
===================

Like most projects, the jme3-utilities project builds upon the work of many who have gone before.

I therefore acknowledge the following software developers:
+ Cris (aka "t0neg0d") for creating SkyDome (which provided both an inspiration and a starting point 
  for SkyControl) and also for encouraging me to run with it ... thank you yet again!
+ Paul Speed, for helpful insights which got me unstuck during debugging
+ the brave souls who volunteered to be alpha testers for SkyControl, including:
  + Davis Rollman
  + "Lockhead"
  + Jonatan Dahl
  + Mindaugas (aka "eraslt")
  + Thomas Kluge
  + "pixelapp"
  + Roger (aka "stenb")
+ the beta testers for SkyControl, including:
  + "madjack"
  + Benjamin D.
  + "Fissll"
  + Davis Rollman
+ the creators of (and contributors to) the following software:
  + Adobe Photoshop Elements
  + the Blender 3D animation suite
  + the FindBugs source code analyzer
  + Gimp, the GNU Image Manipulation Program
  + the Google Chrome web browser
  + Guava core libraries for Java
  + the Java compiler, standard doclet, and runtime environment
  + the JCommander Java framework
  + jMonkeyEngine and the jME3 Software Development Kit
  + LWJGL, the Lightweight Java Game Library
  + Microsoft Windows
  + the NetBeans integrated development environment
  + the Nifty GUI
  + Open Broadcaster Software
  + the PMD source code analyzer
  + the RealWorld Cursor Editor
  + Alex Peterson's Spacescape tool
  + the Subversion revision control system
  + the WinMerge differencing and merging tool

Many of SkyControl's assets were based on the works of others who licensed their 
works under liberal terms or contributed them to the public domain. For this I thank:
  + Cris (aka "t0neg0d")
  + Jacques Descloitres, MODIS Rapid Response Team, NASA/GSFC
  + Tom Ruen

I am grateful to Google Code and Github for providing free hosting for the jme3-utilities
project and many other open-source projects.

I'm also grateful to Julian (for lending me one of his microphones) and finally my dear
Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know so I can correct the situation.
