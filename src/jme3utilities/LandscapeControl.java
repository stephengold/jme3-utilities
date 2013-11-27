// (c) Copyright 2013 Stephen Gold <sgold@sonic.net>
// Distributed under the terms of the GNU General Public License

/*
 This file is part of the JME3 Utilities Package.

 The JME3 Utilities Package is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 The JME3 Utilities Package is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

 You should have received a copy of the GNU General Public License along with
 the JME3 Utilities Package.  If not, see <http://www.gnu.org/licenses/>.
 */
package jme3utilities;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
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
import java.util.logging.Logger;

/**
 * A simple control to provide a landscape for tests and demos. The landscape
 * consists of a circular monument (resembling Stonehenge) set on a plain
 * surrounded by hills.
 *
 * The controlled spatial must be a Node.
 *
 * The control is disabled by default. When enabled, it attaches two nodes (one
 * for the terrain and one for the monument) to the controlled spatial.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class LandscapeControl
        extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * color for grass
     */
    final private static ColorRGBA grassColor =
            new ColorRGBA(0.65f, 0.8f, 0.2f, 1f);
    /**
     * color for stone
     */
    final private static ColorRGBA stoneColor =
            new ColorRGBA(0.8f, 0.8f, 0.6f, 1f);
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
    final private static Logger logger =
            Logger.getLogger(LandscapeControl.class.getName());
    /**
     * asset path of the shaded material definition
     */
    final private static String shadedMaterialAssetPath =
            "Common/MatDefs/Light/Lighting.j3md";
    /**
     * asset path of the terrain's height map
     */
    final private static String heightMapAssetPath =
            "Textures/terrain/height/basin.png";
    // *************************************************************************
    // fields
    /**
     * the application's asset manager: set by constructor
     */
    final private AssetManager assetManager;
    /**
     * the maximum (unscaled) height of the terrain above its base
     */
    private float terrainHeight = 0f;
    /**
     * the unscaled diameter of the terrain (in pixels)
     */
    private int terrainDiameter = 0;
    /**
     * spatial which represents the monument: set by constructor
     */
    final private Spatial monument;
    /**
     * spatial which represents the terrain: set by constructor
     */
    final private Spatial terrain;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled control.
     */
    public LandscapeControl(AssetManager assetManager) {
        super.setEnabled(false);

        if (assetManager == null) {
            throw new NullPointerException("manager cannot be null");
        }
        this.assetManager = assetManager;
        /*
         * Generate monument and terrain, but don't attach them yet.
         */
        monument = createMonument();
        terrain = createTerrain();

        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Alter the terrain's radius and vertical relief.
     *
     * @param radius distance from center to edge (>0)
     * @param baseY lowest possible Y-coordinate
     * @param peakY Y-coordinate of the peak (>baseY)
     */
    public void setTerrainScale(float radius, float baseY, float peakY) {
        if (radius <= 0f) {
            throw new IllegalArgumentException("radius must be positive");
        }
        if (peakY <= baseY) {
            throw new IllegalArgumentException("peak must be above base");
        }

        float yScale = (peakY - baseY) / terrainHeight;
        float xzScale = (2f * radius) / terrainDiameter;
        Vector3f scale = new Vector3f(xzScale, yScale, xzScale);
        terrain.setLocalScale(scale);

        Vector3f center = new Vector3f(0f, baseY, 0f);
        MySpatial.setWorldLocation(terrain, center);
    }
    // *************************************************************************
    // AbstractControl methods

    /**
     * Alter the visibility of this landscape. Assumes that the control has been
     * added to a node.
     *
     * @param newState if true, make this landscape visible; if false, hide this
     * landscape
     */
    @Override
    public void setEnabled(boolean newState) {
        assert spatial != null;

        Node node = (Node) spatial;
        if (enabled && !newState) {
            node.detachChild(monument);
            node.detachChild(terrain);
        } else if (!enabled && newState) {
            node.attachChild(monument);
            node.attachChild(terrain);
        }
        super.setEnabled(newState);
    }

    /**
     * Alter the controlled node.
     *
     * @param newNode which node to control (or null)
     */
    @Override
    public void setSpatial(Spatial newNode) {
        super.setSpatial(newNode);
        if (enabled && newNode != null) {
            Node node = (Node) spatial;
            node.attachChild(monument);
            node.attachChild(terrain);
        }
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
            String name = "upright" + String.valueOf(index);
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
            rotation.fromAngleAxis(theta, Vector3f.UNIT_Y);
            MySpatial.setWorldOrientation(upright, rotation);
        }

        Box lintelMesh = new Box(lintelLength / 2f, lintelThickness / 2f,
                ringDepth / 2);
        for (int index = 0; index < numUprights; index++) {
            String name = "lintel" + String.valueOf(index);
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
            rotation.fromAngleAxis(theta, Vector3f.UNIT_Y);
            MySpatial.setWorldOrientation(lintel, rotation);
        }

        return node;
    }

    /**
     * Create a shaded material for a given color.
     *
     * @param color which ambient/diffuse color (not null)
     * @return a new material
     */
    private Material createShadedMaterial(ColorRGBA color) {
        assert color != null;

        Material material = new Material(assetManager, shadedMaterialAssetPath);
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
        TerrainQuad quad =
                new TerrainQuad("terrain", patchSize, mapSize, heightArray);

        quad.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        float[] minMaxHeights = heightMap.findMinMaxHeights();
        terrainHeight = minMaxHeights[1];
        assert terrainHeight >= 0f : terrainHeight;
        /*
         * Apply a shaded material which vaguely resembles grass.
         */
        Material grass = createShadedMaterial(grassColor);
        quad.setMaterial(grass);

        return quad;
    }

    /**
     * Load a height map asset.
     *
     * @return a new instance
     */
    private AbstractHeightMap loadHeightMap() {
        Texture heightTexture = loadTexture(heightMapAssetPath, false);
        Image heightImage = heightTexture.getImage();
        float heightScale = 1f;
        AbstractHeightMap heightMap =
                new ImageBasedHeightMap(heightImage, heightScale);
        heightMap.load();
        return heightMap;
    }

    /**
     * Load a texture asset in edge-clamp mode.
     *
     * @param assetPath asset path to the texture (not null)
     * @param flipY if true, flip the texture's Y axis, else don't flip
     * @return the texture which was loaded
     */
    private Texture loadTexture(String assetPath, boolean flipY) {
        assert assetPath != null;

        TextureKey key = new TextureKey(assetPath, flipY);
        Texture texture = assetManager.loadTexture(key);
        return texture;
    }
}