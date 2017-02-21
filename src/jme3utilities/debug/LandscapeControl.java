/*
 Copyright (c) 2013-2017, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.debug;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.SubtreeControl;
import jme3utilities.Validate;

/**
 * Subtree control to provide a landscape for tests and demos. The landscape
 * consists of a circular monument (resembling Stonehenge) set on a plain
 * surrounded by hills.
 * <p>
 * The controlled spatial must be a Node.
 * <p>
 * The control is disabled by default. When enabled, it attaches two nodes (one
 * for the terrain and one for the monument) to the scene graph.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LandscapeControl extends SubtreeControl {
    // *************************************************************************
    // constants

    /**
     * color for grass
     */
    final private static ColorRGBA grassColor = new ColorRGBA(
            0.65f, 0.8f, 0.2f, 1f);
    /**
     * color for stone
     */
    final private static ColorRGBA stoneColor = new ColorRGBA(
            0.8f, 0.8f, 0.6f, 1f);
    /**
     * length of the lintel stones in the monument
     */
    final private static float lintelLength = 3.2f;
    /**
     * thickness of the lintel stones in the monument
     */
    final private static float lintelThickness = 0.8f;
    /**
     * depth of the ring of stones in the monument
     */
    final private static float ringDepth = 2f;
    /**
     * diameter of the ring of stones in the monument
     */
    final private static float ringDiameter = 33f;
    /**
     * height of the upright stones in the monument
     */
    final private static float uprightHeight = 4.1f;
    /**
     * width of the upright stones in the monument
     */
    final private static float uprightWidth = 2f;
    /**
     * number of upright stones in the monument, also the number of lintels
     */
    final private static int numUprights = 30;
    /**
     * size of terrain patches (in pixels)
     */
    final private static int patchSize = 33;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            LandscapeControl.class.getName());
    /**
     * asset path of the terrain's height map
     */
    final private static String heightMapAssetPath =
            "Textures/terrain/height/basin.png";
    /**
     * local copy of Vector3f#UNIT_Y
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    // *************************************************************************
    // fields

    /**
     * application's asset manager: set by constructor
     */
    private AssetManager assetManager;
    /**
     * maximum (unscaled) height of the terrain above its base
     */
    private float terrainHeight = 0f;
    /**
     * unscaled diameter of the terrain (in pixels)
     */
    private int terrainDiameter = 0;
    /**
     * spatial which represents the terrain: set by constructor
     */
    private Spatial terrain;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor for serialization purposes only. Do not invoke
     * directly!
     */
    public LandscapeControl() {
        super();
        assetManager = null;
        terrain = null;
    }

    /**
     * Instantiate a disabled control.
     *
     * @param assetManager for loading textures and material definitions (not
     * null)
     */
    public LandscapeControl(AssetManager assetManager) {
        super();
        Validate.nonNull(assetManager, "asset manager");

        this.assetManager = assetManager;
        /*
         * Generate monument and terrain and attach them to the subtree.
         */
        Spatial monument = createMonument();
        terrain = createTerrain();

        subtree = new Node("landscape node");
        subtree.attachChild(monument);
        subtree.attachChild(terrain);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access a material identical to the one applied to the terrain.
     *
     * @return new instance
     */
    final public Material getGrass() {
        return createShadedMaterial(grassColor);
    }

    /**
     * Alter the terrain's radius and vertical relief.
     *
     * @param radius distance from center to edge (&gt;0)
     * @param baseY lowest possible Y-coordinate
     * @param peakY Y-coordinate of the peak (&gt;baseY)
     */
    public void setTerrainScale(float radius, float baseY, float peakY) {
        Validate.positive(radius, "radius");
        if (!(peakY > baseY)) {
            logger.log(Level.SEVERE, "peakY={0}, baseY={1}",
                    new Object[]{peakY, baseY});
            throw new IllegalArgumentException("peak should be above base");
        }

        float yScale = (peakY - baseY) / terrainHeight;
        float xzScale = (2f * radius) / terrainDiameter;
        Vector3f scale = new Vector3f(xzScale, yScale, xzScale);
        terrain.setLocalScale(scale);

        Vector3f center = new Vector3f(0f, baseY, 0f);
        MySpatial.setWorldLocation(terrain, center);
    }
    // *************************************************************************
    // SubtreeControl methods

    /**
     * De-serialize this instance, for example when loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule ic = importer.getCapsule(this);

        assetManager = importer.getAssetManager();
        terrainHeight = ic.readFloat("terrainHeight", 0f);
        terrainDiameter = ic.readInt("terrainDiameter", 0);
        /*
         * Infer cached reference from the subtree.
         */
        terrain = MySpatial.findChild(subtree, "terrain");
    }

    /**
     * Serialize this instance, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule oc = exporter.getCapsule(this);

        oc.write(terrainHeight, "terrainHeight", 0f);
        oc.write(terrainDiameter, "terrainDiameter", 0);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a shallow copy of this control.
     *
     * @return a new instance
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public LandscapeControl clone() throws CloneNotSupportedException {
        LandscapeControl clone = (LandscapeControl) super.clone();
        return clone;
    }
    // *************************************************************************
    // private methods

    /**
     * Create a circular monument which vaguely resembles Stonehenge.
     */
    private Node createMonument() {
        Material stoneMaterial = createShadedMaterial(stoneColor);
        Node node = new Node("monument");
        float ringRadius = ringDiameter / 2f;
        Box uprightMesh = new Box(uprightWidth / 2f, uprightHeight / 2f,
                ringDepth / 2);
        for (int index = 0; index < numUprights; index++) {
            String name = "upright" + index;
            Geometry upright = new Geometry(name, uprightMesh);
            node.attachChild(upright);

            upright.setMaterial(stoneMaterial);
            upright.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            float theta = index * FastMath.TWO_PI / numUprights;
            float x = ringRadius * FastMath.sin(theta);
            float z = ringRadius * FastMath.cos(theta);
            Vector3f location = new Vector3f(x, uprightHeight / 2f, z);
            MySpatial.setWorldLocation(upright, location);
            Quaternion rotation = new Quaternion();
            rotation.fromAngleNormalAxis(theta, yAxis);
            MySpatial.setWorldOrientation(upright, rotation);
        }

        Box lintelMesh = new Box(lintelLength / 2f, lintelThickness / 2f,
                ringDepth / 2);
        for (int index = 0; index < numUprights; index++) {
            String name = "lintel" + index;
            Geometry lintel = new Geometry(name, lintelMesh);
            node.attachChild(lintel);

            lintel.setMaterial(stoneMaterial);
            lintel.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            float theta = (2 * index + 1) * FastMath.TWO_PI / (2 * numUprights);
            float x = ringRadius * FastMath.sin(theta);
            float y = uprightHeight + lintelThickness / 2f;
            float z = ringRadius * FastMath.cos(theta);
            Vector3f location = new Vector3f(x, y, z);
            MySpatial.setWorldLocation(lintel, location);
            Quaternion rotation = new Quaternion();
            rotation.fromAngleNormalAxis(theta, yAxis);
            MySpatial.setWorldOrientation(lintel, rotation);
        }

        return node;
    }

    /**
     * Create a shaded material for the specified color.
     *
     * @param color ambient/diffuse color (not null)
     * @return new material
     */
    private Material createShadedMaterial(ColorRGBA color) {
        assert color != null;

        Material material = new Material(assetManager,
                Misc.shadedMaterialAssetPath);
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Ambient", color);
        material.setColor("Diffuse", color);

        return material;
    }

    /**
     * Load terrain from assets.
     */
    private TerrainQuad createTerrain() {
        /*
         * Create the terrain quad.
         */
        AbstractHeightMap heightMap = loadHeightMap();
        terrainDiameter = heightMap.getSize();

        int mapSize = terrainDiameter + 1; // number of samples on a side
        float[] heightArray = heightMap.getHeightMap();
        TerrainQuad quad = new TerrainQuad(
                "terrain", patchSize, mapSize, heightArray);
        Material grass = getGrass();
        quad.setMaterial(grass);
        quad.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        float[] minMaxHeights = heightMap.findMinMaxHeights();
        terrainHeight = minMaxHeights[1];
        assert terrainHeight >= 0f : terrainHeight;

        return quad;
    }

    /**
     * Load a height map asset.
     *
     * @return new instance
     */
    private AbstractHeightMap loadHeightMap() {
        Texture heightTexture = MyAsset.loadTexture(
                assetManager, heightMapAssetPath);
        Image heightImage = heightTexture.getImage();
        float heightScale = 1f;
        AbstractHeightMap heightMap = new ImageBasedHeightMap(
                heightImage, heightScale);
        heightMap.load();

        return heightMap;
    }
}
