/*
 Copyright (c) 2019-2020, Stephen Gold
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

import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.system.JmeSystem;
import com.jme3.system.MockJmeSystemDelegate;
import com.jme3.texture.plugins.AWTLoader;
import jme3utilities.Misc;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.debug.BoundsVisualizer;
import jme3utilities.debug.SkeletonVisualizer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cloning/saving/loading various abstract controls.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestCloneControls {
    // *************************************************************************
    // fields

    /**
     * AssetManager required by the BinaryImporter
     */
    final private static AssetManager assetManager = new DesktopAssetManager();
    // *************************************************************************
    // new methods exposed

    /**
     * Test cloning/saving/loading various abstract controls.
     */
    @Test
    public void testClone() {
        JmeSystem.setSystemDelegate(new MockJmeSystemDelegate());
        assetManager.registerLoader(AWTLoader.class, "jpg", "png");
        assetManager.registerLoader(J3MLoader.class, "j3m", "j3md");
        assetManager.registerLocator(null, ClasspathLocator.class);
        Node node = new Node("node");

        AxesVisualizer axes = new AxesVisualizer(assetManager, 1f);
        node.addControl(axes);
        setParameters(axes, 0f);
        verifyParameters(axes, 0f);
        AxesVisualizer axesClone = (AxesVisualizer) Misc.deepCopy(axes);
        cloneTest(axes, axesClone);

        BoundsVisualizer bounds = new BoundsVisualizer(assetManager);
        node.addControl(bounds);
        setParameters(bounds, 0f);
        verifyParameters(bounds, 0f);
        BoundsVisualizer boundsClone = (BoundsVisualizer) Misc.deepCopy(bounds);
        cloneTest(bounds, boundsClone);

        SkeletonControl subject = null; // TODO more interesting subject
        SkeletonVisualizer skeleton
                = new SkeletonVisualizer(assetManager, subject);
        node.addControl(skeleton);
        setParameters(skeleton, 0f);
        verifyParameters(skeleton, 0f);
        SkeletonVisualizer skeletonClone
                = (SkeletonVisualizer) Misc.deepCopy(skeleton);
        cloneTest(skeleton, skeletonClone);
    }
    // *************************************************************************
    // private methods

    private static void cloneTest(AbstractControl control,
            AbstractControl controlClone) {
        assert controlClone != control;

        verifyParameters(control, 0f);
        verifyParameters(controlClone, 0f);

        setParameters(control, 0.3f);
        verifyParameters(control, 0.3f);
        verifyParameters(controlClone, 0f);

        setParameters(controlClone, 0.6f);
        verifyParameters(control, 0.3f);
        verifyParameters(controlClone, 0.6f);

        AbstractControl controlCopy
                = BinaryExporter.saveAndLoad(assetManager, control);
        verifyParameters(controlCopy, 0.3f);

        AbstractControl controlCopyClone
                = BinaryExporter.saveAndLoad(assetManager, controlClone);
        verifyParameters(controlCopyClone, 0.6f);
    }

    /**
     * Modify Control parameters based on the specified key value.
     *
     * @param control the Control to modify (not null)
     * @param b the key value
     */
    private static void setParameters(AbstractControl control, float b) {
        boolean flag = (b > 0.15f && b < 0.45f);
        control.setEnabled(flag);

        if (control instanceof AxesVisualizer) {
            setAxes((AxesVisualizer) control, b);
        } else if (control instanceof BoundsVisualizer) {
            setBounds((BoundsVisualizer) control, b);
        } else if (control instanceof SkeletonVisualizer) {
            setSkeleton((SkeletonVisualizer) control, b);
        } else {
            throw new IllegalArgumentException(control.getClass().getName());
        }
    }

    private static void setAxes(AxesVisualizer axes, float b) {
        int index = Math.round(b / 0.3f);
        boolean flag = (b > 0.15f && b < 0.45f);

        axes.setAxisLength(0.01f + b);
        axes.setDepthTest(!flag);
        axes.setLineWidth(0.02f + b);
        axes.setNumAxes(1 + index);
    }

    private static void setBounds(BoundsVisualizer bounds, float b) {
        boolean flag = (b > 0.15f && b < 0.45f);
        ColorRGBA color
                = new ColorRGBA(0.11f + b, 0.012f + b, 0.013f + b, 0.014f + b);

        bounds.setColor(color);
        bounds.setDepthTest(!flag);
        bounds.setLineWidth(0.02f + b);
    }

    private static void setSkeleton(SkeletonVisualizer skeleton, float b) {
        ColorRGBA color
                = new ColorRGBA(0.11f + b, 0.012f + b, 0.013f + b, 0.014f + b);

        skeleton.setColor(color);
        skeleton.setLineWidth(0.02f + b);
    }

    /**
     * Verify that all Control parameters have their expected values for the
     * specified key value.
     *
     * @param control the Control to verify (not null, unaffected)
     * @param b the key value
     */
    private static void verifyParameters(AbstractControl control, float b) {
        assert control != null;
        boolean flag = (b > 0.15f && b < 0.45f);
        Assert.assertEquals(flag, control.isEnabled());

        if (control instanceof AxesVisualizer) {
            verifyAxes((AxesVisualizer) control, b);
        } else if (control instanceof BoundsVisualizer) {
            verifyBounds((BoundsVisualizer) control, b);
        } else if (control instanceof SkeletonVisualizer) {
            verifySkeleton((SkeletonVisualizer) control, b);
        } else {
            throw new IllegalArgumentException(control.getClass().getName());
        }
    }

    private static void verifyAxes(AxesVisualizer axes, float b) {
        int index = Math.round(b / 0.3f);
        boolean flag = (b > 0.15f && b < 0.45f);

        Assert.assertEquals(0.01f + b, axes.axisLength(), 0f);
        Assert.assertEquals(!flag, axes.isDepthTest());
        Assert.assertEquals(0.02f + b, axes.lineWidth(), 0f);
        Assert.assertEquals(1 + index, axes.numAxes());
    }

    private static void verifyBounds(BoundsVisualizer bounds, float b) {
        ColorRGBA color
                = new ColorRGBA(0.11f + b, 0.012f + b, 0.013f + b, 0.014f + b);
        boolean flag = (b > 0.15f && b < 0.45f);

        Assert.assertEquals(color, bounds.copyLineColor(null));
        Assert.assertEquals(!flag, bounds.isDepthTest());
        Assert.assertEquals(0.02f + b, bounds.lineWidth(), 0f);
    }

    private static void verifySkeleton(SkeletonVisualizer skeleton, float b) {
        ColorRGBA color
                = new ColorRGBA(0.11f + b, 0.012f + b, 0.013f + b, 0.014f + b);

        Assert.assertEquals(color, skeleton.copyLineColor(null));
        Assert.assertEquals(0.02f + b, skeleton.lineWidth(), 0f);
    }
}
