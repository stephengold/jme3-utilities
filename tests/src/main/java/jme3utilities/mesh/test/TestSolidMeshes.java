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
package jme3utilities.mesh.test;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MyMesh;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.debug.Dumper;
import jme3utilities.math.MyMath;
import jme3utilities.mesh.Cone;
import jme3utilities.mesh.Dodecahedron;
import jme3utilities.mesh.DomeMesh;
import jme3utilities.mesh.Icosahedron;
import jme3utilities.mesh.Icosphere;
import jme3utilities.mesh.Octahedron;
import jme3utilities.mesh.Prism;
import jme3utilities.mesh.Tetrahedron;

/**
 * A SimpleApplication to test the Cone, DomeMesh, Icosahedron, Icosphere,
 * Octahedron, Prism, and Tetrahedron classes. Also test MyMesh.reverseNormals()
 * and MyMesh.reverseWinding().
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestSolidMeshes
        extends SimpleApplication
        implements ActionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TestSolidMeshes.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TestSolidMeshes.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * true if the normals are inward-facing
     */
    private boolean inwardNormals;
    /**
     * true if the triangles are wound to be inward-facing
     */
    private boolean inwardWinding;
    /**
     * status displayed in the upper-left corner of the GUI node
     */
    private BitmapText statusText;
    /**
     * dump debugging information to System.out
     */
    final private Dumper dumper = new Dumper();
    /**
     * all geometries in the scene
     */
    private List<Geometry> allGeometries;
    /**
     * materials to visualize meshes
     */
    private List<Material> allMaterials;
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

        TestSolidMeshes application = new TestSolidMeshes();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
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
                case "dump":
                    dumper.dump(rootNode);
                    break;

                case "flip normals":
                    flipNormals();
                    break;

                case "next material":
                    nextMaterial();
                    break;

                case "reverse winding":
                    reverseWinding();
                    break;
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
        configureCamera();
        configureDumper();
        generateMaterials();

        addGeometries();
        addLighting();
        applyMaterial(allMaterials.get(0));
        assignKeys();

        /*
         * Add the status text to the GUI.
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
     * Add geometries to the scene, without materials.
     */
    private void addGeometries() {
        boolean generateNormals, generatePyramid;
        float height, radius;
        Geometry geometry;
        int numSides, radialSamples, zSamples;
        Mesh mesh;

        inwardNormals = false;
        inwardWinding = false;

        radius = 1f;
        zSamples = 32;
        radialSamples = 32;
        mesh = new Sphere(zSamples, radialSamples, radius);
        geometry = new Geometry("sphere-original", mesh);
        rootNode.attachChild(geometry);
        geometry.move(0f, -2f, 0f);

        numSides = 40;
        height = 1f;
        generatePyramid = false;
        mesh = new Cone(numSides, radius, height, generatePyramid);
        mesh = MyMesh.addIndices(mesh);
        geometry = new Geometry("cone", mesh);
        rootNode.attachChild(geometry);
        geometry.move(0f, 0f, 0f);

        int rimSamples = 20;
        int quadrantSamples = 6;
        float topU = 0.5f;
        float topV = 0.5f;
        float uvScale = 0.4f;
        mesh = new DomeMesh(rimSamples, quadrantSamples, topU, topV, uvScale,
                inwardWinding);
        geometry = new Geometry("dome", mesh);
        rootNode.attachChild(geometry);
        geometry.move(0f, 2f, 0f);

        generateNormals = true;
        mesh = new Icosahedron(radius, generateNormals);
        geometry = new Geometry("icosahedron", mesh);
        rootNode.attachChild(geometry);
        geometry.move(0f, 4f, 0f);

        mesh = new Sphere(zSamples, radialSamples, radius);
        ((Sphere) mesh).setTextureMode(Sphere.TextureMode.Projected);
        geometry = new Geometry("sphere-projected", mesh);
        rootNode.attachChild(geometry);
        geometry.move(2f, -2f, 0f);

        int refineSteps = 3;
        mesh = new Icosphere(refineSteps, radius);
        geometry = new Geometry("icoSphere", mesh);
        rootNode.attachChild(geometry);
        geometry.move(2f, 0f, 0f);

        mesh = new Octahedron(radius, generateNormals);
        geometry = new Geometry("octahedron", mesh);
        rootNode.attachChild(geometry);
        geometry.move(2f, 2f, 0f);

        numSides = 3;
        mesh = new Prism(numSides, radius, height, generateNormals);
        geometry = new Geometry("prism", mesh);
        rootNode.attachChild(geometry);
        geometry.move(2f, 4f, 0f);

        mesh = new Sphere(zSamples, radialSamples, radius);
        ((Sphere) mesh).setTextureMode(Sphere.TextureMode.Polar);
        geometry = new Geometry("sphere-polar", mesh);
        rootNode.attachChild(geometry);
        geometry.move(4f, -2f, 0f);

        numSides = 4;
        generatePyramid = true;
        mesh = new Cone(numSides, radius, height, generatePyramid);
        geometry = new Geometry("pyramid", mesh);
        rootNode.attachChild(geometry);
        geometry.move(4f, 0f, 0f);

        mesh = new Tetrahedron(radius, generateNormals);
        geometry = new Geometry("tetrahedron", mesh);
        rootNode.attachChild(geometry);
        geometry.move(4f, 2f, 0f);

        mesh = new Dodecahedron(radius, Mesh.Mode.Triangles);
        mesh = MyMesh.expand(mesh);
        MyMesh.generateNormals(mesh);
        geometry = new Geometry("dodecahedron", mesh);
        rootNode.attachChild(geometry);
        geometry.move(4f, 4f, 0f);

        allGeometries
                = MySpatial.listSpatials(rootNode, Geometry.class, null);
    }

    /**
     * Add lighting to the scene during startup.
     */
    private void addLighting() {
        ColorRGBA ambientColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootNode.addLight(ambient);

        Vector3f direction = new Vector3f(1f, -2f, -2f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction);
        rootNode.addLight(sun);
    }

    /**
     * Apply the specified Material to all geometries in the scene.
     *
     * @param material the desired material
     */
    private void applyMaterial(Material material) {
        for (Geometry geometry : allGeometries) {
            geometry.setMaterial(material);
        }
    }

    /**
     * Map keys to actions during startup.
     */
    private void assignKeys() {
        inputManager.addMapping("dump", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addListener(this, "dump");

        inputManager.addMapping("flip normals", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addListener(this, "flip normals");

        inputManager.addMapping("next material",
                new KeyTrigger(KeyInput.KEY_N));
        inputManager.addListener(this, "next material");

        inputManager.addMapping("reverse winding",
                new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(this, "reverse winding");
    }

    /**
     * Configure the camera during startup.
     */
    private void configureCamera() {
        flyCam.setMoveSpeed(10f);

        cam.setLocation(new Vector3f(-0.9f, 6.8f, 9f));
        cam.setRotation(new Quaternion(0.026f, 0.9644f, -0.243f, 0.1f));
    }

    /**
     * Configure the Dumper during startup.
     */
    private void configureDumper() {
        dumper.setDumpTransform(true);
        //dumper.setDumpVertex(true);
    }

    /**
     * Reverse the normals of all meshes in the scene.
     */
    private void flipNormals() {
        for (Geometry geometry : allGeometries) {
            Mesh mesh = geometry.getMesh();
            MyMesh.reverseNormals(mesh);
            mesh.setDynamic();
        }
        inwardNormals = !inwardNormals;
    }

    /**
     * Initialize materials during startup.
     */
    private void generateMaterials() {
        allMaterials = new ArrayList<>(5);

        ColorRGBA green = new ColorRGBA(0f, 0.12f, 0f, 1f);
        Material mat;

        mat = MyAsset.createShadedMaterial(assetManager, green);
        mat.setName("front-only lit");
        allMaterials.add(mat);

        mat = MyAsset.createShadedMaterial(assetManager, green);
        mat.getAdditionalRenderState()
                .setFaceCullMode(RenderState.FaceCullMode.Front);
        mat.setName("back-only lit");
        allMaterials.add(mat);

        mat = MyAsset.createWireframeMaterial(assetManager, green);
        mat.setName("front-only wireframe");
        allMaterials.add(mat);

        mat = MyAsset.createWireframeMaterial(assetManager, green);
        mat.getAdditionalRenderState()
                .setFaceCullMode(RenderState.FaceCullMode.Front);
        mat.setName("back-only wireframe");
        allMaterials.add(mat);

        mat = MyAsset.createDebugMaterial(assetManager);
        allMaterials.add(mat);

        String assetPath = "Interface/Logo/Monkey.jpg";
        boolean generateMips = true;
        Texture texture
                = MyAsset.loadTexture(assetManager, assetPath, generateMips);
        mat = MyAsset.createUnshadedMaterial(assetManager, texture);
        mat.setName("unshaded texture");
        allMaterials.add(mat);
    }

    /**
     * Apply the next Material to all geometries in the scene.
     */
    private void nextMaterial() {
        Material oldMaterial = allGeometries.get(0).getMaterial();
        int oldIndex = allMaterials.indexOf(oldMaterial);
        int numMaterials = allMaterials.size();
        int newIndex = MyMath.modulo(oldIndex + 1, numMaterials);
        Material newMaterial = allMaterials.get(newIndex);

        for (Geometry geometry : allGeometries) {
            geometry.setMaterial(newMaterial);
        }
    }

    /**
     * Reverse the triangle winding of all meshes in the scene.
     */
    private void reverseWinding() {
        for (Geometry geometry : allGeometries) {
            Mesh mesh = geometry.getMesh();
            MyMesh.reverseWinding(mesh);
            mesh.setDynamic();
        }
        inwardWinding = !inwardWinding;
    }

    /**
     * Update the status text in the GUI.
     */
    private void updateStatusText() {
        String materialName = allGeometries.get(0).getMaterial().getName();
        String message = String.format(
                "material=%s  normals=%s  winding=%s",
                MyString.quote(materialName),
                inwardNormals ? "in" : "out",
                inwardWinding ? "in" : "out");
        statusText.setText(message);
    }
}
