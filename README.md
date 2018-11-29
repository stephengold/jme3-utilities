# Jme3-utilities Project

The Jme3-utilities Project contains Java packages and assets, developed for
sgold's jMonkeyEngine projects, which might prove useful in similar projects.
It contains 10 sub-projects:

 1. heart: the "jme3-utilities-heart" library of core classes
 2. SkyControl: the "SkyControl" library for sky simulation
 3. moon-ccbysa: assets for a realistic Moon in SkyControl
 4. debug: the "jme3-utilities-debug" library of debugging aids
 5. tests: demos, examples, and test software
 6. textures: generate textures used by jme3-utilities-debug and SkyControl
 7. ui: the "jme3-utilities-ui" library for building user interfaces
 8. nifty: the "jme3-utilities-nifty" library for using NiftyGUI user
    interfaces with jMonkeyEngine
 9. wes: the "Wes" library for animation editing and retargeting
 10. x: the "jme3-utilities-x" library of experimental software

The "Minie" library, formerly a sub-project, is now a separate project
at [GitHub](https://github.com/stephengold/Minie).

Summary of SkyControl features:

 + sun, moon, stars, horizon haze, and up to 6 cloud layers
 + compatible with static backgrounds such as cube maps
 + high resolution textures are provided -- or customize with your own textures
 + compatible with effects such as SimpleWater, shadows, bloom, and cartoon edges
 + continuous and reversible motion and blending of cloud layers
 + option to foreshorten clouds near the horizon
 + continuous and reversible motion of sun, moon, and stars based on time of day
 + updater to synchronize lighting and shadows with sun, moon, and clouds
 + continuous scaling of sun, moon, and clouds
 + option for continuously variable phase of the moon
 + demonstration apps and online tutorial provided
 + complete source code provided under FreeBSD license

## Contents of this document

 + [Downloads](#downloads)
 + [Conventions](#conventions)
 + [History](#history)
 + [How to install the SDK and the Jme3-utilities Project](#install)
 + [How to add SkyControl to an existing game](#addsky)
 + [External links](#links)
 + [Acknowledgments](#acks)

<a name="downloads"/>

### Downloads

Recent releases can be downloaded from
[GitHub](https://github.com/stephengold/jme3-utilities/releases).

Maven artifacts are available from
[JFrog Bintray](https://bintray.com/stephengold/jme3utilities).

<a name="conventions"/>

### Conventions

Package names generally begin with "jme3utilities".

The source code is compatible with JDK 7.

World coordinate system:

 + the +X axis points toward the northern horizon
 + the +Y axis points up (toward the zenith)
 + the +Z axis points toward the eastern horizon

<a name="history"/>

### History

Since September 2015, the Jme3-utilities Project has been hosted at
[GitHub](https://github.com/stephengold/jme3-utilities).

From November 2013 to September 2015, it was hosted at
[Google Code](http://code.google.com/p/jme3-utilities).

Old (2014) versions of the Jme3-utilities Project can still be found in
[the jMonkeyEngine-Contributions Project](https://github.com/jMonkeyEngine-Contributions/SkyControl).

<a name="install"/>

## How to install the SDK and the Jme3-utilities Project

### jMonkeyEngine3 (jME3) Software Development Kit (SDK)

The "master" branch of the jme3-utilities repository targets
Version 3.2.1 of jMonkeyEngine.  You are welcome to use the Engine
without also using the SDK, but I use the SDK, and the following
installation instructions assume you will too.

The hardware and software requirements of the SDK are documented on
[the JME wiki](https://jmonkeyengine.github.io/wiki/jme3/requirements.html).

 1. Download a jMonkeyEngine 3.2 SDK from
    [GitHub](https://github.com/jMonkeyEngine/sdk/releases).
 2. Install the SDK, which includes:
    + the engine itself,
    + an integrated development environment (IDE) based on NetBeans,
    + various plugins, and
    + the Blender 3D application.
 3. To open the project in the SDK (or NetBeans), you will need the "Gradle
    Support" plugin.  Download and install it before proceeding.

### Source files

Clone the jme3-utilities repository using Git:

 1. Open the Clone wizard in the IDE:
     + Menu bar -> "Team" -> "Remote" -> "Clone..."
 2. For "Repository URL:" specify
    "https://github.com/stephengold/jme3-utilities.git" (without the quotes).
 3. Clear the "User:" and "Password:" text boxes.
 4. For "Clone into:" specify a writable folder (on a local filesystem)
    which doesn't already contain "jme3-utilities".
 5. Click on the "Next >" button.
 6. Make sure the "master" remote branch is checked.
 7. Click on the "Next >" button again.
 8. Make sure the Checkout Branch is set to "master".
 9. Make sure the "Scan for NetBeans Projects after Clone" box is checked.
10. Click on the "Finish" button.
11. When the "Clone Complete" dialog appears, click on the "Open Project..."
    button.
12. Expand the root project node to reveal the 10 sub-projects.
13. To start with, I recommend opening 4 sub-projects:  "heart", "SkyControl",
    "tests", and "textures".  Select them using control-click, then click on the
    "Open" button.
14. There will be errors in the "textures" sub-project.  However, the build
    task should resolve them.

### External file

 1. Download "bsc5.dat.gz" (the ASCII catalog, version 5 of the Yale Bright Star
    Catalog) from [Harvard](http://tdc-www.harvard.edu/catalogs/bsc5.html).
 2. Extract the file "bsc5.dat" to the "src/main/resources" folder of the
    "textures" sub-project.

### Build the project

 1. In the "Projects" window, right-click on the "tests" sub-project to
    select it.
 2. Select "Build".

<a name="addsky"/>

## How to add SkyControl to an existing game

SkyControl is a reusable sky simulation for jMonkeyEngine games.

Adding it to an existing JME3 project should be a simple 6-step process:

 1. Add jme3-utilities JARs to the classpath.
 2. Disable any existing sky which might interfere with SkyControl.
 3. Add a SkyControl instance to some node in the scene graph.
 4. Configure the SkyControl instance.
 5. Enable the SkyControl instance.
 6. Test and tune as necessary.

To demonstrate this process, we'll apply it to BasicGame and then to
CubeMapExample, a more sophisticated application. I encourage you to
follow along in your development environment.

### BasicGame example

You'll need:

 + A development system with the JME3 SDK installed.
 + A clean build of the jme3-utilities JARs, either downloaded from
   [GitHub](https://github.com/stephengold/jme3-utilities/releases) or built yourself.

Instantiate a BasicGame Project:

 1. Open the "New Project" wizard in the IDE:
    + Menu bar -> "File" -> "New Project..."
 2. Under "Categories:" select "JME3".
 3. Under "Projects:" select "BasicGame".
 4. Click on the "Next >" button.
 5. For "Project Location:" specify a writable folder (on a local filesystem)
    which doesn't already contain "BasicGame".
 6. Click on the "Finish" button.

If you're unfamiliar with BasicGame, you may wish to run it (and/or examine
the source code) to see how it works before modifying it.

 + To rotate the camera, move the mouse.
 + The W/A/S/D/Q/Z keys translate (move) the camera.
 + To exit, press the Esc key.

#### Add the SkyControl JARs to the project

In the following instructions, "0.0.0" indicates a version number.

Open the project's properties in the IDE:

 1. Right-click on the BasicGame project (not its assets) in the "Projects"
    window.
 2. Select "Properties to open the "Project Properties" dialog.
 3. Under "Categories:" select "Libraries".
 4. Click on the "Compile" tab.
 5. Add the "jme3-utilities-heart" class JAR:
    + Click on the "Add JAR/Folder" button.
    + Navigate to the "jme3-utilities" project folder.
    + Open the "heart" sub-project folder.
    + Navigate to the "build/libs" folder.
    + Select the "jme3-utilities-heart-0.0.0.jar" file.
    + Click on the "Open" button.
 6. (optional) Add JARs for javadoc and sources:
    + Click on the "Edit" button.
    + Click on the "Browse..." button to the right of "Javadoc:"
    + Select the "jme3-utilities-heart-0.0.0-javadoc.jar" file.
    + Click on the "Open" button.
    + Click on the "Browse..." button to the right of "Sources:"
    + Select the "jme3-utilities-heart-0.0.0-sources.jar" file.
    + Click on the "Open" button again.
    + Click on the "OK" button to close the "Edit Jar Reference dialog.
 7. Add the "SkyControl" class JAR:
    + Click on the "Add JAR/Folder" button.
    + Navigate to the "jme3-utilities" project folder.
    + Open the "SkyControl" sub-project folder.
    + Navigate to the "build/libs" folder.
    + Select the "SkyControl-0.0.0.jar" file.
    + Click on the "Open" button.
 8. (optional) Add JARs for javadoc and sources:
    + Click on the "Edit" button.
    + Click on the "Browse..." button to the right of "Javadoc:"
    + Select the "SkyControl-0.0.0-javadoc.jar" file.
    + Click on the "Open" button.
    + Click on the "Browse..." button to the right of "Sources:"
    + Select the "SkyControl-0.0.0-sources.jar" file.
    + Click on the "Open" button again.
    + Click on the "OK" button to close the "Edit Jar Reference dialog.
 9. (optional) Add the "moon-ccbysa" class JAR:
    + Click on the "Add JAR/Folder" button.
    + Navigate to the "jme3-utilities" project folder.
    + Open the "moon-cc-by-sa" sub-project folder.
    + Navigate to the "build/libs" folder.
    + Select the "moon-ccbysa-0.0.0.jar" file.
    + Click on the "Open" button.
10. Click on the "OK" button to exit the "Project Properties" dialog.

#### Disable existing sky

Since BasicGame has no sky -- just the default (black)
viewport background -- there is nothing to disable.

#### Add a SkyControl instance to the scene graph

The scene graph of BasicGame has only one node, the root node. The root node is
typically a good place to add SkyControl.

 1. Open the "Main.java" source file in the IDE:
    + In the "Projects" window, expand the BasicGame project node.
    + Expand the "Source Packages" node under the BasicGame project.
    + Expand the "mygame" package under "Source Packages".
    + Select "Main.java" file under the "mygame" package.
    + Double-click to open the file.

 2. In the import section of "Main.java", add the following code:

        import jme3utilities.sky.SkyControl;
        import jme3utilities.sky.StarsOption;

 3. Scroll down to the simpleInitApp() method and insert the following code just
    before the final close-brace:

        SkyControl sc = new SkyControl(assetManager, cam, 0.9f, StarsOption.Cube, true);
        rootNode.addControl(sc);

The parameters of the constructor are documented in the Javadoc for the
SkyControl class.

#### Configure the SkyControl instance

By default, SkyControl simulates midnight on March 21st in Wiltshire, England,
with no clouds and a full moon. Instead, let's configure 6 a.m. on February 10th
in Sunnyvale, California with dense clouds:

 1. In the import section of "Main.java", add the following code:

        import com.jme3.math.FastMath;
        import java.util.Calendar;
        import jme3utilities.sky.SunAndStars;

 2. In simpleInitApp(), insert the following code just before the final
    close brace:

        SunAndStars sns = sc.getSunAndStars();
        sns.setHour(6f);
        sns.setObserverLatitude(37.4046f * FastMath.DEG_TO_RAD);
        sns.setSolarLongitude(Calendar.FEBRUARY, 10);
        sc.setCloudiness(1f);

Other configuration methods are documented in the Javadocs for the SkyControl,
SkyControlCore, SunAndStars, CloudLayer, and Updater classes.

#### Enable the SkyControl instance

If you run the modified BasicGame at this point, you'll find no visible change.
Unlike most JME3 controls, SkyControl instantiates in a disabled state. In order
to see the sky, you must enable the control:

 1. In simpleInitApp(), insert the following code just before the final
    close-brace:

        sc.setEnabled(true);

#### Test

To test the modified BasicGame, right-click in the "Main.java" editor window and
select "Run File (Shift+F6)".


### CubeMapExample

Sometimes you'll want to combine SkyControl with other sky elements. The
Jme3-utilities Project includes CubeMapExample, a slightly more sophisticated
application than BasicGame. CubeMapExample includes a cube-mapped sky, lit
terrain, and multiple light sources. In this section, you'll see how SkyControl
can be used to add sun, moon, and clouds to CubeMapExample.

In the "Projects" window of the IDE, expand the "tests" sub-project node
and the "jme3utilities.sky.test" source package.  Open the "CubeMapExample.java"
file to examine the code and run it.

 + To rotate the camera, drag with the left mouse button.
 + The W/A/S/D/Q/Z keys translate (move) the camera.
 + To exit, press the Esc key.

Since CubeMapExample is part of the Jme3-utilities Project, there's no need
to add anything to the classpath.  And since we plan to use the existing cube
map, there's nothing to remove.

#### Add a SkyControl instance to the scene graph

Both SkyFactory and SkyControl add geometries to Bucket.Sky. In the absence of a
custom GeometryComparator, geometries in this bucket are rendered in scene graph
order. Since the cube map is opaque, we want to SkyControl to add its geometries
after the sky cube in the scene graph.

 1. In the import section of "CubeMapExample.java", add the following code:

        import jme3utilities.sky.SkyControl;
        import jme3utilities.sky.StarsOption;

 2. Scroll down to the initializeSky() method and insert the following code just
    before the final close-brace:

        SkyControl sc = new SkyControl(assetManager, cam, 0.8f, StarsOption.TopDome, true);
        rootNode.addControl(sc);

For now, we've turned star motion off, since that simplifies things. The lower
value (0.8) for cloud flattening will make it easier to compensate for the
scene's low horizon.

#### Configure the SkyControl instance

 1. It's important to disable the SkyControl's built-in star maps so that the
    cube map's stars will be visible at night:

        sc.clearStarMaps();

 2. Adjust the cloudiness so that clouds will be visible:

        sc.setCloudiness(0.8f);

#### Enable the SkyControl instance

In initializeSky(), insert the following code just before the final close-brace:

        sc.setEnabled(true);

#### Tuning

At this point, the application should run successfully. (Look!  A cloud!)
However, for the best result, it needs some tuning.

 1. To lower the edge of the cloud dome so that it's hidden by the terrain, add
    the following code:

        sc.setCloudsYOffset(0.4f);

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

The uneven shading of the level terrain is due to sunlight coming in at a low
angle. Since it's noon, the easiest way to raise the sun's elevation is to
decrease the observer's latitude.  Also, the terrain is too dark.
Try, for instance:

    sc.getSunAndStars().setObserverLatitude(0.2f);
    sc.getUpdater().setMainMultiplier(2f);

The sun looks like a boring white disc in the southern sky.
For a more dazzling sun, apply a bloom filter to the viewport:

    import com.jme3.post.filters.BloomFilter;
    import jme3utilities.Misc;
...

    BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
    bloom.setBlurScale(2.5f);
    bloom.setExposurePower(1f);
    int numSamples = settings.getSamples();
    Misc.getFpp(viewPort, assetManager, numSamples).addFilter(bloom);
    sc.getUpdater().addBloomFilter(bloom);

#### Adding star motion

To add star motion, it's not sufficient simply to change the control's
constructor from

    SkyControl sc = new SkyControl(assetManager, cam, 0.8f, StarsOption.TopDome, true);
to

    SkyControl sc = new SkyControl(assetManager, cam, 0.8f, StarsOption.Cube, true);

It's also necessary to rotate the cube map to match SkyControl's notions of time
and space.  To achieve this, override the application's simpleUpdate() method:

    @Override
    public void simpleUpdate(float unused) {
        Spatial starMap = rootNode.getChild("Sky");
        SkyControl sc = rootNode.getControl(SkyControl.class);
        sc.getSunAndStars().orientEquatorialSky(starMap, true);
    }

To simulate the passage of time, we need state and a mechanism to update it.
The jme3utilities.TimeOfDay appstate fills this need.

 1. Import the class:

        import jme3utilities.TimeOfDay;

 2. Declare a field in CubeMapExample:

        TimeOfDay timeOfDay;

 3. Initialize and attach it in simpleInitApp():

        timeOfDay = new TimeOfDay(19f);
        stateManager.attach(timeOfDay);
        timeOfDay.setRate(1000f);

    All that remains is to update the control.

 4. Add 2 lines to simpleUpdate(), right before the invocation of
    orientExternalSky():

        float hour = timeOfDay.hour();
        sc.getSunAndStars().setHour(hour);

### Next steps

To see how some of the other features of SkyControl
can be used, you may wish to study "TestSkyControl.java" in the
"tests" sub-project.  The easiest way to run TestSkyControl is to right-click
on the "tests" sub-project and select "Tasks" -> "run" -> "runTestSkyControl".

<a name="links"/>

## External links

  + November 2013 [SkyControl demo video](https://www.youtube.com/watch?v=FsJRM6tr3oQ)
  + January 2014 [SkyControl update video](https://www.youtube.com/watch?v=gE4wxgBIkaw)
  + A [maze game](https://github.com/stephengold/jme3-maze) that uses the Jme3-utilities libraries.
  + A [flight simulation game](https://github.com/ZoltanTheHun/SkyHussars) that uses SkyControl.

<a name="acks"/>

## Acknowledgments

Like most projects, the Jme3-utilities Project builds on the work of many who
have gone before.  I therefore acknowledge the following
artists and software developers:

+ Cris (aka "t0neg0d") for creating SkyDome (which provided both an inspiration
  and a starting point for SkyControl) and also for encouraging me to run with
  it ... thank you yet again!
+ Paul Speed, for helpful insights which got me unstuck during debugging
+ Rémy Bouquet (aka "nehon") for creating the BVH Retarget Project (parts of
  which are incorporated into the Wes library) and also for
  many helpful insights
+ Alexandr Brui (aka "javasabr") for a solving a problem with the
  de-serialization of SkyControl
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
+ users who found and reported bugs in later versions:
    + Anton Starastsin (aka "Antonystar")
+ the creators of (and contributors to) the following software:
    + Adobe Photoshop Elements
    + the Ant and Gradle build tools
    + the Blender 3-D animation suite
    + the FindBugs source-code analyzer
    + Gimp, the GNU Image Manipulation Program
    + the Git and Subversion revision-control systems
    + the Google Chrome web browser
    + Guava core libraries for Java
    + the Java compiler, standard doclet, and runtime environment
    + the JCommander Java framework
    + jMonkeyEngine and the jME3 Software Development Kit
    + LWJGL, the Lightweight Java Game Library
    + the Markdown document conversion tool
    + Microsoft Windows
    + the NetBeans integrated development environment
    + the Nifty graphical user interface library
    + Open Broadcaster Software Studio
    + the PMD source-code analyzer
    + the RealWorld Cursor Editor
    + Alex Peterson's Spacescape tool
    + the WinMerge differencing and merging tool

Many of SkyControl's assets were based on the works of others who licensed their
works under liberal terms or contributed them to the public domain.
For this I thank:

+ Cris (aka "t0neg0d")
+ Jacques Descloitres, MODIS Rapid Response Team, NASA/GSFC
+ Tom Ruen

I am grateful to JFrog, Google, and Github for providing free hosting for the
Jme3-utilities Project and many other open-source projects.

I'm also grateful to Quinn (for lending me one of her microphones) and finally
my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know so I can
correct the situation: sgold@sonic.net