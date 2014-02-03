/*
 Copyright (c) 2014, Stephen Gold
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
package jme3utilities.sky.test;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;

/**
 * A simple application which loads a cubical star map generated by Alex
 * Peterson's Spacescape tool: http://alexcpeterson.com/spacescape
 * <p>
 * This application also uses assets from the jme3-test-data library.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class CubeMapExample
        extends SimpleApplication {
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] args) {
        CubeMapExample app = new CubeMapExample();
        app.start();
        /*
         * ... and onward to CubeMapExample.simpleInitApp()!
         */
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize the application.
     */
    @Override
    public void simpleInitApp() {
        initializeCamera();
        initializeLandscape();
        initializeLights();
        initializeSky();
    }
    // *************************************************************************
    // private methods

    /**
     * Create a cubical star map.
     *
     * @param name name of the star map (not null)
     */
    private Spatial createStarMap(String name) {
        assert name != null;
        /*
         * Load the cube map textures.
         */
        Texture right = loadFace(name, "right1");
        Texture left = loadFace(name, "left2");
        Texture top = loadFace(name, "top3");
        Texture bottom = loadFace(name, "bottom4");
        Texture front = loadFace(name, "front5");
        Texture back = loadFace(name, "back6");
        /*
         * Create the map.
         */
        Vector3f normalScale = Vector3f.UNIT_XYZ;
        int sphereRadius = 10;
        Spatial starMap = SkyFactory.createSky(assetManager, right, left, back,
                front, top, bottom, normalScale, sphereRadius);
        return starMap;
    }

    /**
     * Configure the camera, including flyCam.
     */
    private void initializeCamera() {
        cam.setLocation(new Vector3f(177f, 17f, 326f));
        Vector3f direction = new Vector3f(31f, -7f, -95f).normalize();
        Vector3f up = Vector3f.UNIT_Y.clone();
        cam.lookAtDirection(direction, up);

        flyCam.setDragToRotate(true);
        flyCam.setRotationSpeed(2f);
        flyCam.setMoveSpeed(20f);
        flyCam.setUpVector(up);
        flyCam.setZoomSpeed(20f);
    }

    /**
     * Create, configure, add, and enable the landscape.
     */
    private void initializeLandscape() {
        /*
         * textures
         */
        Texture alphaMap = assetManager.loadTexture(
                "Textures/Terrain/splat/alphamap.png");
        Texture dirt = loadSplatTexture("dirt.jpg");
        Texture dirtNormal = loadSplatTexture("dirt_normal.png");
        Texture grass = loadSplatTexture("grass.jpg");
        Texture grassNormal = loadSplatTexture("grass_normal.jpg");
        Texture heights = assetManager.loadTexture(
                "Textures/Terrain/splat/mountains512.png");
        Texture road = loadSplatTexture("road.jpg");
        Texture roadNormal = loadSplatTexture("road_normal.png");
        /*
         * material
         */
        Material terrainMaterial = new Material(assetManager,
                "Common/MatDefs/Terrain/TerrainLighting.j3md");
        terrainMaterial.setBoolean("useTriPlanarMapping", false);
        terrainMaterial.setBoolean("WardIso", true);
        terrainMaterial.setFloat("DiffuseMap_0_scale", 64);
        terrainMaterial.setFloat("DiffuseMap_1_scale", 16);
        terrainMaterial.setFloat("DiffuseMap_2_scale", 128);
        terrainMaterial.setTexture("AlphaMap", alphaMap);
        terrainMaterial.setTexture("DiffuseMap", grass);
        terrainMaterial.setTexture("DiffuseMap_1", dirt);
        terrainMaterial.setTexture("DiffuseMap_2", road);
        terrainMaterial.setTexture("NormalMap", grassNormal);
        terrainMaterial.setTexture("NormalMap_1", dirtNormal);
        terrainMaterial.setTexture("NormalMap_2", roadNormal);
        /*
         * spatials
         */
        Image image = heights.getImage();
        ImageBasedHeightMap heightMap = new ImageBasedHeightMap(image);
        heightMap.load();
        float[] heightArray = heightMap.getHeightMap();
        TerrainQuad terrain = new TerrainQuad("terrain", 65, 513, heightArray);
        rootNode.attachChild(terrain);
        terrain.setLocalScale(2f, 0.25f, 2f);
        terrain.setMaterial(terrainMaterial);
    }

    /**
     * Create, configure, and add light sources.
     */
    private void initializeLights() {
        DirectionalLight mainLight = new DirectionalLight();
        Vector3f lightDirection = new Vector3f(-2f, -5f, 4f).normalize();
        mainLight.setColor(ColorRGBA.White.mult(1f));
        mainLight.setDirection(lightDirection);
        mainLight.setName("main");
        rootNode.addLight(mainLight);

        AmbientLight ambientLight = new AmbientLight();
        ambientLight.setColor(ColorRGBA.White.mult(0.2f));
        ambientLight.setName("ambient");
        rootNode.addLight(ambientLight);
    }

    /**
     * Create and attach the sky.
     */
    private void initializeSky() {
        Spatial starMap = createStarMap("purple-nebula-complex");
        rootNode.attachChild(starMap);
    }

    /**
     * Load the texture asset for a specific face of a cubical star map.
     *
     * @param mapName name of the star map folder (not null)
     * @param faceName which face (not null, e.g. "top3")
     */
    private Texture loadFace(String mapName, String faceName) {
        assert mapName != null;
        assert faceName != null;

        String path = String.format("Textures/skies/star-maps/%s/%s_%s.png",
                mapName, mapName, faceName);
        Texture result = assetManager.loadTexture(path);
        return result;
    }

    /**
     * Load an inverted splat texture asset in "repeat" mode.
     *
     * @param fileName (not null)
     */
    private Texture loadSplatTexture(String fileName) {
        assert fileName != null;

        String path = String.format("Textures/Terrain/splat/%s", fileName);
        Texture result = assetManager.loadTexture(path);
        result.setWrap(Texture.WrapMode.Repeat);

        return result;
    }
}