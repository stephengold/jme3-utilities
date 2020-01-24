/*
 Copyright (c) 2020, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.debug.test;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MyMesh;
import jme3utilities.MyString;
import jme3utilities.debug.BoundsVisualizer;
import jme3utilities.debug.Dumper;
import jme3utilities.debug.SphereMeshes;
import jme3utilities.math.MyVector3f;
import jme3utilities.mesh.Cone;

/**
 * A SimpleApplication to test the BoundsVisualizer class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestBoundsVisualizer
        extends SimpleApplication
        implements ActionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TestBoundsVisualizer.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TestBoundsVisualizer.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * status displayed in the upper-left corner of the GUI node
     */
    private BitmapText statusText;
    /**
     * control under test
     */
    private BoundsVisualizer visualizer;
    /**
     * dump debugging information to System.out
     */
    final private Dumper dumper = new Dumper();
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the application.
     *
     * @param unused array of command-line arguments
     */
    public static void main(String[] unused) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);

        TestBoundsVisualizer application = new TestBoundsVisualizer();
        /*
         * Customize the window's title bar.
         */
        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        settings.setTitle(applicationName);

        settings.setGammaCorrection(true);
        settings.setSamples(4); // anti-aliasing
        settings.setVSync(true);
        application.setSettings(settings);

        application.start();
        /*
         * ... and onward to TestSolids.simpleInitApp()!
         */
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action from the InputManager.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case "billboard off":
                    visualizer.disableBillboarding();
                    return;
                case "billboard X":
                    visualizer.enableBillboarding(cam, MyVector3f.xAxis);
                    return;
                case "billboard Y":
                    visualizer.enableBillboarding(cam, MyVector3f.yAxis);
                    return;
                case "billboard Z":
                    visualizer.enableBillboarding(cam, MyVector3f.zAxis);
                    return;

                case "dump":
                    dumper.dump(rootNode);
                    return;

                case "toggle bounds":
                    toggleBounds();
                    return;
                case "toggle depthTest":
                    toggleDepthTest();
                    return;
            }
            if (actionString.startsWith("sphere ")) {
                String name = actionString.split(" ")[1];
                SphereMeshes sphereType = SphereMeshes.valueOf(name);
                visualizer.setSphereType(sphereType);
                return;
            }
            if (actionString.startsWith("width ")) {
                int width = Integer.parseInt(actionString.split(" ")[1]);
                visualizer.setLineWidth(width);
            }
        }
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize the application.
     */
    @Override
    public void simpleInitApp() {
        assignKeys();
        configureCamera();
        configureDumper();
        /*
         * Create a wireframe pyramid and attach it to the scene.
         */
        int numSides = 4;
        float radius = 2f;
        float height = 1f;
        boolean generatePyramid = true;
        Mesh mesh = new Cone(numSides, radius, height, generatePyramid);
        mesh = MyMesh.addIndices(mesh);
        Spatial pyramid = new Geometry("pyramid", mesh);
        rootNode.attachChild(pyramid);

        Material material = MyAsset.createWireframeMaterial(assetManager,
                ColorRGBA.Green);
        pyramid.setMaterial(material);
        /*
         * Add a bounds visualizer for the pyramid.
         */
        visualizer = new BoundsVisualizer(assetManager);
        rootNode.addControl(visualizer);
        visualizer.enableBillboarding(cam, MyVector3f.yAxis);
        visualizer.setSubject(pyramid);
        visualizer.setEnabled(true);
        /*
         * Create status text and attach it to the GUI.
         */
        statusText = new BitmapText(guiFont, false);
        statusText.setLocalTranslation(0f, cam.getHeight(), 0f);
        guiNode.attachChild(statusText);
    }

    /**
     * Callback invoked once per frame.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        updateStatusText();
    }
    // *************************************************************************
    // private methods

    /**
     * Map keys to actions during startup.
     */
    private void assignKeys() {
        inputManager.addMapping("billboard off", new KeyTrigger(KeyInput.KEY_N));
        inputManager.addListener(this, "billboard off");

        inputManager.addMapping("billboard X", new KeyTrigger(KeyInput.KEY_X));
        inputManager.addListener(this, "billboard X");

        inputManager.addMapping("billboard Y", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addListener(this, "billboard Y");

        inputManager.addMapping("billboard Z", new KeyTrigger(KeyInput.KEY_V));
        inputManager.addListener(this, "billboard Z");

        inputManager.addMapping("dump", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addListener(this, "dump");

        inputManager.addMapping("sphere Icosphere",
                new KeyTrigger(KeyInput.KEY_F2));
        inputManager.addListener(this, "sphere Icosphere");

        inputManager.addMapping("sphere LoopMesh",
                new KeyTrigger(KeyInput.KEY_F3));
        inputManager.addListener(this, "sphere LoopMesh");

        inputManager.addMapping("sphere PoleSphere",
                new KeyTrigger(KeyInput.KEY_F1));
        inputManager.addListener(this, "sphere PoleSphere");

        inputManager.addMapping("sphere WireSphere",
                new KeyTrigger(KeyInput.KEY_F4));
        inputManager.addListener(this, "sphere WireSphere");

        inputManager.addMapping("toggle bounds",
                new KeyTrigger(KeyInput.KEY_B));
        inputManager.addListener(this, "toggle bounds");

        inputManager.addMapping("toggle depthTest",
                new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(this, "toggle depthTest");

        inputManager.addMapping("width 1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addListener(this, "width 1");

        inputManager.addMapping("width 2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addListener(this, "width 2");

        inputManager.addMapping("width 3", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addListener(this, "width 3");

        inputManager.addMapping("width 4", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addListener(this, "width 4");

        inputManager.addMapping("width 5", new KeyTrigger(KeyInput.KEY_5));
        inputManager.addListener(this, "width 5");

        inputManager.addMapping("width 6", new KeyTrigger(KeyInput.KEY_6));
        inputManager.addListener(this, "width 6");
    }

    /**
     * Configure the Camera during startup.
     */
    private void configureCamera() {
        flyCam.setMoveSpeed(10f);
        cam.setLocation(new Vector3f(10.5f, 3f, 8.3f));
        cam.setRotation(new Quaternion(-0.049f, 0.90316f, -0.1082f, -0.4126f));
    }

    /**
     * Configure the Dumper during startup.
     */
    private void configureDumper() {
        dumper.setDumpTransform(true);
        //dumper.setDumpVertex(true);
    }

    /**
     * Toggle the subject's bounds between AABB and sphere.
     */
    private void toggleBounds() {
        Geometry subject = (Geometry) visualizer.getSubject();
        Mesh mesh = subject.getMesh();
        if (mesh.getBound() instanceof BoundingBox) {
            mesh.setBound(new BoundingSphere());

        } else {
            mesh.setBound(new BoundingBox());
        }
        subject.updateModelBound();
    }

    /**
     * Toggle the depth-test setting.
     */
    private void toggleDepthTest() {
        boolean state = visualizer.isDepthTest();
        visualizer.setDepthTest(!state);
    }

    /**
     * Update the status text in the GUI.
     */
    private void updateStatusText() {
        BoundingVolume bound = visualizer.getSubject().getWorldBound();
        BoundingVolume.Type boundsType = bound.getType();
        String message = "bounds=" + boundsType;

        if (boundsType == BoundingVolume.Type.Sphere) {
            int axisIndex = visualizer.billboardAxis();
            String axis;
            if (axisIndex == -1) {
                axis = "none";
            } else {
                axis = MyString.axisName(axisIndex);
            }
            SphereMeshes sphere = visualizer.sphereType();
            message += String.format("  sphere=%s  axis=%s", sphere, axis);
        }

        boolean depth = visualizer.isDepthTest();
        float width = visualizer.lineWidth();
        message += String.format("  depth=%s  width=%.0f", depth, width);

        statusText.setText(message);
    }
}
