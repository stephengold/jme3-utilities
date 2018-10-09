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
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.KinematicRagdollControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import java.util.List;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.math.MyVector3f;
import jme3utilities.ui.ActionApplication;

/**
 * Test scaling on a KinematicRagdollControl.
 */
public class TestRagdollScaling extends ActionApplication {
    // *************************************************************************
    // fields

    private Node sinbad;
    private KinematicRagdollControl ragdoll;
    private RigidBodyControl boxRbc;
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

        rootNode.scale(1.5f);
        cam.setLocation(new Vector3f(0f, 8f, 25f));

        viewPort.setBackgroundColor(ColorRGBA.Gray);
        addLighting();

        BulletAppState bulletAppState = new BulletAppState();
        bulletAppState.setDebugEnabled(true);
        stateManager.attach(bulletAppState);

        addBox();
        addSinbad();

        //sinbad.scale(2f);
        sinbad.rotate(0f, 1f, 0f);
        center(sinbad);

        PhysicsSpace ps = bulletAppState.getPhysicsSpace();
        ragdoll.setPhysicsSpace(ps);
        boxRbc.setPhysicsSpace(ps);

        AnimControl animControl = sinbad.getControl(AnimControl.class);
        AnimChannel animChannel = animControl.createChannel();
        animChannel.setAnim("Dance");
    }

    @Override
    public void moreDefaultBindings() {
        getDefaultInputMode().bind("rag", KeyInput.KEY_SPACE);
    }

    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case "rag":
                    ragdoll.setRagdollMode();
                    return;
            }
        }
        super.onAction(actionString, ongoing, tpf);
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
        ColorRGBA color = new ColorRGBA(0.3f, 0.6f, 0.3f, 1f);
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
     * Add a Sinbad model with a KinematicRagdollControl.
     */
    private void addSinbad() {
        sinbad = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        rootNode.attachChild(sinbad);
        center(sinbad);
        sinbad.scale(0.5f);

        List<Spatial> s = MySpatial.listSpatials(sinbad, Spatial.class, null);
        for (Spatial spatial : s) {
            spatial.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        }

        ragdoll = new SinbadControl();
        sinbad.addControl(ragdoll);
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
}
