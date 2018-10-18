/*
 Copyright (c) 2018, Stephen Gold
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
package jme3utilities.minie.test;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.ModelKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.minie.PhysicsDumper;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.Signals;

/**
 * Test scaling and load/save of a DynamicAnimControl.
 */
public class TestRagdollScaling extends ActionApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(TestRagdollScaling.class.getName());
    /**
     * asset path for loading and saving
     */
    final private static String saveAssetPath = "Models/TestRagdollScaling.j3o";
    // *************************************************************************
    // fields

    private AnimChannel animChannel = null;
    private BulletAppState bulletAppState;
    private DynamicAnimControl dac;
    private int scaleIndex = 0;
    private Node model;
    private PhysicsSpace physicsSpace;
    private RigidBodyControl boxRbc;
    private SkeletonVisualizer sv;
    private String animationName = null;
    private Transform resetTransform;
    // *************************************************************************
    // new methods exposed

    public static void main(String[] args) {
        TestRagdollScaling app = new TestRagdollScaling();
        app.start();
    }
    // *************************************************************************
    // ActionApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void actionInitializeApplication() {
        flyCam.setEnabled(false); // TODO FlyByCamera broken in action apps?

        cam.setLocation(new Vector3f(0f, 1.5f, 4f));
        viewPort.setBackgroundColor(ColorRGBA.Gray);
        addLighting();

        bulletAppState = new BulletAppState();
        bulletAppState.setDebugEnabled(true);
        stateManager.attach(bulletAppState);
        physicsSpace = bulletAppState.getPhysicsSpace();

        CollisionShape.setDefaultMargin(0.01f); // 1 cm
        addBox();
        addModel();
        /*
         * Add and configure the model's controls.
         */
        AnimControl animControl = model.getControl(AnimControl.class);
        if (animationName != null) {
            animChannel = animControl.createChannel();
            animChannel.setAnim(animationName);
            animChannel.setSpeed(1f);
        }

        List<SkeletonControl> scList
                = MySpatial.listControls(model, SkeletonControl.class, null);
        SkeletonControl sc = scList.get(0);
        sv = new SkeletonVisualizer(assetManager, sc);
        sv.setLineColor(ColorRGBA.Yellow);
        rootNode.addControl(sv);
        sv.setEnabled(true);

        PhysicsSpace ps = bulletAppState.getPhysicsSpace();
        dac.setPhysicsSpace(ps);
        boxRbc.setPhysicsSpace(ps);
    }

    /**
     * Add new hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.bind("amputate left elbow", KeyInput.KEY_A);
        dim.bind("blend all to kinematic", KeyInput.KEY_K);
        dim.bind("dump physicsSpace", KeyInput.KEY_O);
        dim.bind("dump scene", KeyInput.KEY_P);
        dim.bind("freeze upper body", KeyInput.KEY_F);
        dim.bind("go bind pose", KeyInput.KEY_B);
        dim.bind("go floating", KeyInput.KEY_0);
        dim.bind("go limp", KeyInput.KEY_SPACE);
        dim.bind("limp left elbow", KeyInput.KEY_COMMA);
        dim.bind("load", KeyInput.KEY_L);
        dim.bind("next scale", KeyInput.KEY_1);
        dim.bind("raise leftHand", KeyInput.KEY_LSHIFT);
        dim.bind("raise rightHand", KeyInput.KEY_RSHIFT);
        dim.bind("reset model transform", KeyInput.KEY_DOWN);
        dim.bind("signal rotateLeft", KeyInput.KEY_LEFT);
        dim.bind("signal rotateRight", KeyInput.KEY_RIGHT);
        dim.bind("save", KeyInput.KEY_S);
        dim.bind("toggle animation", KeyInput.KEY_PERIOD);
        dim.bind("toggle physics debug", KeyInput.KEY_SLASH);
        dim.bind("toggle skeleton", KeyInput.KEY_V);
    }

    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case "amputate left elbow":
                    dac.amputateHierarchy("Ulna.L", 2f);
                    return;
                case "blend all to kinematic":
                    dac.blendToKinematicMode(2f, null);
                    return;
                case "dump physicsSpace":
                    dumpPhysicsSpace();
                    return;
                case "dump scene":
                    dumpScene();
                    return;
                case "freeze upper body":
                    dac.freezeHierarchy("Chest");
                    return;
                case "go bind pose":
                    dac.bindHierarchy(DynamicAnimControl.torsoName, 2f);
                    return;
                case "go floating":
                    dac.setDynamicHierarchy(DynamicAnimControl.torsoName,
                            Vector3f.ZERO);
                    return;
                case "go limp":
                    dac.setRagdollMode();
                    return;
                case "limp left elbow":
                    Vector3f ragdollGravity = dac.gravity(null);
                    dac.setDynamic("Ulna.L", ragdollGravity);
                    return;
                case "load":
                    load();
                    return;
                case "next scale":
                    nextScale();
                    return;
                case "raise leftHand":
                    dac.setDynamicHierarchy("Clavicle.L",
                            new Vector3f(0f, 50f, 0f));
                    return;
                case "raise rightHand":
                    dac.setDynamicHierarchy("Clavicle.R",
                            new Vector3f(0f, 50f, 0f));
                    return;
                case "reset model transform":
                    model.setLocalTransform(resetTransform);
                    return;
                case "save":
                    save();
                    return;
                case "toggle animation":
                    toggleAnimation();
                    return;
                case "toggle skeleton":
                    toggleSkeleton();
                    return;
                case "toggle physics debug":
                    togglePhysicsDebug();
                    return;
            }
        }
        super.onAction(actionString, ongoing, tpf);
    }

    /**
     * Callback invoked once per render pass.
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        Signals signals = getSignals();
        if (signals.test("rotateRight")) {
            model.rotate(0f, tpf, 0f);
        }
        if (signals.test("rotateLeft")) {
            model.rotate(0f, -tpf, 0f);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add a large static box to serve as a platform.
     */
    private void addBox() {
        float halfExtent = 50f; // mesh units
        Mesh boxMesh = new Box(halfExtent, halfExtent, halfExtent);
        Geometry box = new Geometry("box", boxMesh);
        rootNode.attachChild(box);

        box.move(0f, -halfExtent, 0f);
        ColorRGBA color = new ColorRGBA(0.1f, 0.4f, 0.1f, 1f);
        Material material = MyAsset.createShadedMaterial(assetManager, color);
        box.setMaterial(material);
        box.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        Vector3f hes = new Vector3f(halfExtent, halfExtent, halfExtent);
        Vector3f worldScale = box.getWorldScale();
        hes.multLocal(worldScale);
        BoxCollisionShape shape = new BoxCollisionShape(hes);
        float mass = PhysicsRigidBody.massForStatic;
        boxRbc = new RigidBodyControl(shape, mass);
        boxRbc.setKinematic(true);

        box.addControl(boxRbc);
    }

    /**
     * Add lighting to the scene.
     */
    private void addLighting() {
        ColorRGBA ambientColor = new ColorRGBA(0.7f, 0.7f, 0.7f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootNode.addLight(ambient);

        Vector3f direction = new Vector3f(1f, -2f, -1f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction);
        rootNode.addLight(sun);

        DirectionalLightShadowRenderer dlsr
                = new DirectionalLightShadowRenderer(assetManager, 4096, 3);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.7f);
        viewPort.addProcessor(dlsr);
    }

    /**
     * Add a model to the scene.
     */
    private void addModel() {
        //addJaime();
        loadSinbad();

        rootNode.attachChild(model);
        setHeight(model, 2f);
        center(model);
        resetTransform = model.getLocalTransform().clone();
        model.addControl(dac);
    }

    /**
     * Translate a model's center so that it rests on the X-Z plane, directly
     * above the origin.
     */
    private void center(Spatial model) {
        Vector3f[] minMax = MySpatial.findMinMaxCoords(model);
        Vector3f center = MyVector3f.midpoint(minMax[0], minMax[1]);
        Vector3f offset = new Vector3f(center.x, minMax[0].y, center.z);

        Vector3f location = model.getWorldTranslation();
        location.subtractLocal(offset);
        MySpatial.setWorldLocation(model, location);
    }

    /**
     * Process a "dump physicsSpace" action.
     */
    private void dumpPhysicsSpace() {
        PhysicsDumper dumper = new PhysicsDumper();
        dumper.dump(physicsSpace);
    }

    /**
     * Process a "dump scene" action.
     */
    private void dumpScene() {
        PhysicsDumper dumper = new PhysicsDumper();
        //dumper.setDumpBucket(true);
        //dumper.setDumpCull(true);
        //dumper.setDumpOverride(true);
        //dumper.setDumpShadow(true);
        dumper.setDumpTransform(true);
        //dumper.setDumpUser(true);
        dumper.dump(rootNode);
    }

    /**
     * Load the saved model from the J3O file.
     */
    private void load() {
        /*
         * Remove any copy from the asset manager's cache.
         */
        ModelKey key = new ModelKey(saveAssetPath);
        assetManager.deleteFromCache(key);

        Spatial loadedScene;
        try {
            loadedScene = assetManager.loadAsset(key);
        } catch (AssetNotFoundException e) {
            logger.log(Level.SEVERE, "Didn''t find asset {0}",
                    MyString.quote(saveAssetPath));
            return;
        }
        logger.log(Level.INFO, "Loaded {0} from asset {1}", new Object[]{
            MyString.quote(loadedScene.getName()),
            MyString.quote(saveAssetPath)
        });
        Node loadedNode = (Node) loadedScene;
        // TODO
    }

    /**
     * Load a Jaime model.
     */
    private void loadJaime() {
        model = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        dac = new DynamicAnimControl();
        animationName = "Punches";
    }

    /**
     * Load a Sinbad model.
     */
    private void loadSinbad() {
        model = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        List<Spatial> s = MySpatial.listSpatials(model, Spatial.class, null);
        for (Spatial spatial : s) {
            spatial.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        }

        dac = new SinbadControl();
        animationName = "Dance";
    }

    /**
     * Cycle through different model scales.
     */
    private void nextScale() {
        scaleIndex = MyMath.modulo(scaleIndex + 1, 3);
        logger.log(Level.SEVERE, "scaleIndex = {0}", scaleIndex);

        float height = 2f;
        switch (scaleIndex) {
            case 0:
                height = 2f;
                break;
            case 1:
                height = 1f;
                break;
            case 2:
                height = 3f;
                break;
        }

        setHeight(model, height);
        center(model);
        dac.rebuild();
    }

    /**
     * Save the model to a J3O file.
     */
    private void save() {
        String filePath = ActionApplication.filePath(saveAssetPath);
        File file = new File(filePath);
        BinaryExporter exporter = BinaryExporter.getInstance();

        try {
            exporter.save(model, file);
        } catch (IOException exception) {
            logger.log(Level.SEVERE,
                    "Output exception while saving {0} to file {1}",
                    new Object[]{
                        MyString.quote(model.getName()),
                        MyString.quote(filePath)
                    });
            return;
        }
        logger.log(Level.INFO, "Saved {0} to file {1}", new Object[]{
            MyString.quote(model.getName()),
            MyString.quote(filePath)
        });
    }

    /**
     * Scale the specified model uniformly so that it has the specified height.
     *
     * @param model (not null, modified)
     * @param height (in world units)
     */
    private void setHeight(Spatial model, float height) {
        Vector3f[] minMax = MySpatial.findMinMaxCoords(model);
        float oldHeight = minMax[1].y - minMax[0].y;

        model.scale(height / oldHeight);
    }

    /**
     * Toggle the animation: paused/running.
     */
    private void toggleAnimation() {
        if (animChannel != null) {
            float rate = animChannel.getSpeed();
            animChannel.setSpeed(1f - rate);
        }
    }

    /**
     * Toggle the physics-debug visualization on/off.
     */
    private void togglePhysicsDebug() {
        boolean enabled = bulletAppState.isDebugEnabled();
        bulletAppState.setDebugEnabled(!enabled);
    }

    /**
     * Toggle the skeleton visualizer on/off.
     */
    private void toggleSkeleton() {
        boolean enabled = sv.isEnabled();
        sv.setEnabled(!enabled);
    }
}
