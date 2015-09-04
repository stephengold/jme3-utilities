## JMonkeyEngine (JME3) Software Development Kit (SDK) ##

  * Download the jMonkeyEngine SDK from http://hub.jmonkeyengine.org/downloads
  * Install the SDK, which includes:
    * the engine itself,
    * an integrated development environment (IDE) based on NetBeans, and
    * the Blender 3D application.

The hardware and software requirements of the SDK are documented at http://hub.jmonkeyengine.org/wiki/doku.php/jme3:requirements

## Source files ##

Check out the JME3 Utilities Package source files using Subversion:
  * Open the Checkout wizard in the IDE:
    * Menu bar -> "Team" -> "Subversion" -> "Checkout..."
  * For "Repository URL:" specify "https://jme3-utilities.googlecode.com/svn" (without the quotes).
  * Clear the "User:" and "Password:" text boxes.
  * Click on the "Next >" button.
  * For "Repository Folder(s):" specify "trunk" (without the quotes).
  * For "Local Folder:" specify a writable folder on a local filesystem or a non-existent subfolder of such a folder.
  * Make sure the "Skip ... and checkout only its content" box is checked.
  * Make sure the "Scan for NetBeans Projects after Checkout" box is checked.
  * Click on the "Finish" button.
  * When asked whether to open the project, click the "Open Project" button.
  * There will be problems due to a missing JAR file.  Do not attempt to resolve them yet!

## External files ##

  * Download the JCommander JAR file from "http://mvnrepository.com/artifact/com.beust/jcommander" to the "jars" folder of the new project.
  * If you plan to generate your own star maps, download the Yale Catalogue of Bright Stars from "http://www-kpno.kpno.noao.edu/Info/Caches/Catalogs/BSC5/catalog5.html" to the "assets/Textures/skies" folder of the new project.

## Project configuration ##

  * (optional) Rename the project:
    * Right-click on the jme3-utilities project in the "Projects" window.
    * Select "Rename...".
    * Type a new name in the text box.
    * Click on the "Rename" button.
  * Add the downloaded JAR to the project:
    * Open the project's properties in the IDE:
      * Right-click on the new project (not its assets) in the "Projects" window.
      * Select "Properties".
    * Under "Categories:" select "Libraries".
    * Click on the "OK" button.
  * Resolve any remaining project problems.
  * Build the project's JARs:
    * Right-click on the new project in the "Projects" window.
    * Select "Clean and Build".
  * (optional) Generate the project's javadoc:
    * Right-click on the new project in the "Projects" window.
    * Select "Generate Javadoc".

Note:  The source files are in JDK 7 format, but the IDE creates new JME3 projects in JDK 5 format.