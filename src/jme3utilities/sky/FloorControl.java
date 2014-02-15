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
package jme3utilities.sky;

import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.SubtreeControl;

/**
 * A simple control to provide a visual "floor" for a scene. The floor would
 * typically be textured to look like an extension of the terrain.
 * <p>
 * The control is disabled at creation. When enabled, it attaches a floor node
 * to the controlled spatial, which must also be a node.
 * <p>
 * For best results, the control should be enabled <em>after</em> all sky
 * geometries have been attached.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class FloorControl
        extends SubtreeControl {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(FloorControl.class.getName());
    /**
     * name for the geometry
     */
    final private static String geometryName = "floor";
    /**
     * name for the geometry
     */
    final private static String nodeName = "floor node";
    // *************************************************************************
    // fields
    /**
     * the application's camera: set by constructor
     */
    final private Camera camera;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled control.
     *
     * @param camera the application's camera (not null)
     * @param material to apply to the floor (not null)
     * @param textureScale scale to apply to texture coordinates (&gt;0)
     */
    public FloorControl(Camera camera, Material material, float textureScale) {
        if (camera == null) {
            throw new NullPointerException("camera should not be null");
        }
        if (material == null) {
            throw new NullPointerException("material should not be null");
        }
        if (!(textureScale > 0f)) {
            logger.log(Level.SEVERE, "uvScale={0}", textureScale);
            throw new IllegalArgumentException("scale should be positive");
        }

        this.camera = camera;
        createSpatials(material, textureScale);

        assert !isEnabled();
    }
    // *************************************************************************
    // SimpleControl methods

    /**
     * Callback to update this control while it is enabled. (Invoked once per
     * frame.)
     *
     * @param elapsedTime since the previous update (in seconds, &ge;0)
     */
    @Override
    public void controlUpdate(float elapsedTime) {
        super.controlUpdate(elapsedTime);
        if (!isEnabled()) {
            throw new IllegalStateException("should be enabled");
        }
        if (spatial == null) {
            throw new IllegalStateException("should be added to a spatial");
        }
        if (!(elapsedTime >= 0f)) {
            logger.log(Level.SEVERE, "time={0}", elapsedTime);
            throw new IllegalArgumentException("time should not be negative");
        }
        /*
         * Translate the floor to center it below the camera.
         */
        Vector3f cameraLocation = camera.getLocation();
        MySpatial.setWorldLocation(subtree, cameraLocation);
        MySpatial.setWorldOrientation(subtree, Quaternion.IDENTITY);
        MySpatial.setWorldScale(subtree, 1f);
    }
    // *************************************************************************
    // private methods

    /**
     * Create and initialize the floor node and geometry.
     *
     * @param material to apply to the floor (not null)
     * @param textureScale scale to apply to texture coordinates (&gt;0)
     */
    private void createSpatials(Material material, float textureScale) {
        assert material != null;
        assert textureScale > 0f : textureScale;
        /*
         * Create a node to parent the floor geometry.
         */
        subtree = new Node(nodeName);
        subtree.setQueueBucket(Bucket.Sky);
        /*
         * Create and attach the floor geometry.
         */
        Quad mesh = new Quad(1f, 1f);
        mesh.scaleTextureCoordinates(new Vector2f(textureScale, textureScale));
        Geometry floor = new Geometry(geometryName, mesh);
        subtree.attachChild(floor);
        floor.setMaterial(material);

        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
        floor.setLocalRotation(rotation); // rotate from X-Y plane to X-Z plane
        float radius = camera.getFrustumFar();
        floor.setLocalScale(2f * radius);
        float drop = camera.getFrustumNear();
        floor.setLocalTranslation(-radius, -drop, radius);
    }
}