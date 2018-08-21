/*
 Copyright (c) 2013-2018, Stephen Gold
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
package jme3utilities.debug;

import com.jme3.light.LightList;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;

/**
 * Dump portions of a jME3 scene graph for debugging.
 * <p>
 * {@link #dump(com.jme3.scene.Spatial)} is the usual interface to this class.
 * The level of detail can be configured dynamically.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Dumper {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Dumper.class.getName());
    // *************************************************************************
    // fields

    /**
     * enable dumping of render-queue bucket assignments
     */
    private boolean dumpBucketFlag = false;
    /**
     * enable dumping of cull hints
     */
    private boolean dumpCullFlag = false;
    /**
     * enable dumping of material-parameter overrides
     */
    private boolean dumpOverrideFlag = false;
    /**
     * enable dumping of shadow modes
     */
    private boolean dumpShadowFlag = false;
    /**
     * enable dumping of location, rotation, and scaling
     */
    private boolean dumpTransformFlag = false;
    /**
     * enable dumping of user data
     */
    private boolean dumpUserFlag = true;
    /**
     * describer for JME objects
     */
    private Describer describer;
    /**
     * stream to use for output: set by constructor
     */
    final protected PrintStream stream;
    /**
     * indentation for each level of a dump
     */
    private String indentIncrement = "  ";
    // *************************************************************************
    // constructors

    /**
     * Instantiate a dumper that will use System.out for output.
     */
    public Dumper() {
        describer = new Describer();
        stream = System.out;
    }

    /**
     * Instantiate a dumper that will use the specified output stream.
     *
     * @param printStream output stream (not null)
     */
    public Dumper(PrintStream printStream) {
        Validate.nonNull(printStream, "print stream");

        describer = new Describer();
        stream = printStream;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Dump the specified list of scenes.
     *
     * @param sceneList the root nodes of the scenes to dump (not null,
     * unaffected)
     * @param indent (not null)
     */
    public void dump(List<Spatial> sceneList, String indent) {
        Validate.nonNull(indent, "indent");

        int numScenes = sceneList.size();
        if (numScenes == 0) {
            stream.print("no scenes");
        } else if (numScenes == 1) {
            stream.print("one scene:");
        } else {
            stream.printf("%d scenes:", numScenes);
        }
        stream.println();

        for (Spatial scene : sceneList) {
            dump(scene, indent + indentIncrement);
        }
    }

    /**
     * Dump the specified render manager.
     *
     * @param renderManager which render manager to dump (not null, unaffected)
     */
    public void dump(RenderManager renderManager) {
        List<ViewPort> pres = renderManager.getPreViews();
        int numPres = pres.size();
        List<ViewPort> mains = renderManager.getMainViews();
        int numMains = mains.size();
        List<ViewPort> posts = renderManager.getPostViews();
        int numPosts = posts.size();

        stream.printf("%nrender manager with %d preView%s, %d mainView%s, and ",
                numPres, (numPres == 1) ? "" : "s",
                numMains, (numMains == 1) ? "" : "s");
        stream.printf("%d postView%s%n", numPosts, (numPosts == 1) ? "" : "s");

        for (int index = 0; index < numPres; index++) {
            stream.printf("preView[%d]:%n", index);
            dump(pres.get(index), indentIncrement);
        }
        for (int index = 0; index < numMains; index++) {
            stream.printf("mainView[%d]:%n", index);
            dump(mains.get(index), indentIncrement);
        }
        for (int index = 0; index < numPosts; index++) {
            stream.printf("postView[%d]:%n", index);
            dump(posts.get(index), indentIncrement);
        }
    }

    /**
     * Dump a subtree of the scene graph.
     *
     * @param spatial root of the subtree (may be null, unaffected)
     */
    public void dump(Spatial spatial) {
        dump(spatial, "");
        stream.flush();
    }

    /**
     * Dump a subtree of the scene graph. Note: recursive!
     *
     * @param spatial root of the subtree (may be null, unaffected)
     * @param indent (not null)
     */
    public void dump(Spatial spatial, String indent) {
        Validate.nonNull(indent, "indent");

        if (spatial == null) {
            return;
        }
        stream.print(indent);

        int elementCount = spatial.getTriangleCount();
        stream.printf("%c[%d] ", describer.describeType(spatial), elementCount);

        String name = spatial.getName();
        if (name == null) {
            stream.print("(no name)");
        } else {
            stream.print(MyString.quote(spatial.getName()));
        }

        String description = describer.describeControls(spatial);
        if (!description.isEmpty()) {
            stream.printf(" %s", description);
        }

        LightList lights = spatial.getLocalLightList();
        description = describer.describe(lights);
        if (!description.isEmpty()) {
            stream.printf(" %s", description);
        }

        if (dumpTransformFlag) {
            dumpLocation(spatial);
            dumpOrientation(spatial);
            dumpScale(spatial);
        }

        if (dumpUserFlag) {
            description = describer.describeUserData(spatial);
            if (!description.isEmpty()) {
                stream.printf(" %s", description);
            }
        }

        if (dumpBucketFlag) {
            description = describer.describeBucket(spatial);
            stream.printf(" %s", description);
        }
        if (dumpShadowFlag) {
            description = describer.describeShadow(spatial);
            stream.printf(" %s", description);
        }
        if (dumpCullFlag) {
            description = describer.describeCull(spatial);
            stream.printf(" %s", description);
        }
        if (dumpOverrideFlag) {
            dumpOverrides(spatial);
        }

        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            Material material = geometry.getMaterial();
            description = describer.describe(material);
            if (!description.isEmpty()) {
                stream.println();
                stream.print(indent);
                stream.printf(" material %s", description);
            }
            Mesh mesh = geometry.getMesh();
            description = describer.describe(mesh);
            stream.println();
            stream.print(indent);
            stream.printf(" %s", description);
        }
        stream.println();
        /*
         * If the spatial is a node (but not a terrain node),
         * dump its children with incremented indentation.
         */
        if (spatial instanceof Node && !(spatial instanceof TerrainQuad)) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                dump(child, indent + indentIncrement);
            }
        }
    }

    /**
     * Dump the specified view port.
     *
     * @param viewPort the view port to dump (not null, unaffected)
     * @param indent (not null)
     */
    public void dump(ViewPort viewPort, String indent) {
        Validate.nonNull(indent, "indent");

        stream.print(indent);
        String name = viewPort.getName();
        stream.printf("view port %s ", MyString.quote(name));
        if (viewPort.isEnabled()) {
            stream.print("enabled ");

            String desc = describer.describeFlags(viewPort);
            stream.print(desc);
            if (viewPort.isClearColor()) {
                ColorRGBA backColor = viewPort.getBackgroundColor();
                stream.printf(" bg%s", backColor.toString());
            }
            String descP = describer.describeProcessors(viewPort);
            stream.printf(" procs=(%s)%n", descP);

            stream.print(indent);
            stream.print(" ");
            Camera camera = viewPort.getCamera();
            desc = describer.describe(camera);
            stream.print(desc);
            if (camera != null) {
                String desc2 = describer.describeMore(camera);
                stream.printf("%n%s  %s", indent, desc2);
            }

            stream.printf("%n%s with ", indent);
            List<Spatial> scenes = viewPort.getScenes();
            dump(scenes, indent);

        } else {
            stream.println("disabled");
        }
    }

    /**
     * Dump the world location of a spatial.
     *
     * @param spatial spatial being described (not null, unaffected)
     */
    public void dumpLocation(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");

        Vector3f location = MySpatial.getWorldLocation(spatial);
        if (!MyVector3f.isZero(location)) {
            stream.printf(" loc=[%.3f, %.3f, %.3f]",
                    location.x, location.y, location.z);
        }
    }

    /**
     * Dump the world orientation of a spatial.
     *
     * @param spatial spatial being described (not null, unaffected)
     */
    public void dumpOrientation(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");

        Quaternion orientation = MySpatial.getWorldOrientation(spatial);
        if (!MyQuaternion.isRotationIdentity(orientation)) {
            stream.printf(" orient=%s", orientation.toString());
        }
    }

    /**
     * Dump the material-parameter overrides of a spatial.
     *
     * @param spatial spatial being described (not null, unaffected)
     */
    public void dumpOverrides(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");

        stream.print(" mpo=(");
        boolean addSeparators = false;
        String listSeparator = describer.getListSeparator();

        List<MatParamOverride> list = spatial.getLocalMatParamOverrides();
        for (MatParamOverride override : list) {
            if (addSeparators) {
                stream.print(listSeparator);
            } else {
                addSeparators = true;
            }
            String description = describer.describe(override);
            stream.print(description);
        }
        stream.print(")");
    }

    /**
     * Dump the world scale of a spatial.
     *
     * @param spatial spatial being described (not null, unaffected)
     */
    public void dumpScale(Spatial spatial) {
        Vector3f scale = spatial.getWorldScale();
        if (scale.x != scale.y || scale.y != scale.z) {
            stream.printf(" scale=%s", scale.toString());
        } else if (scale.x != 1f) {
            /*
             * uniform scaling
             */
            String valueString = Float.toString(scale.x);
            stream.printf(" scale=%s", valueString);
        }
    }

    /**
     * Access the describer used by this dumper.
     *
     * @return the pre-existing instance (not null)
     */
    public Describer getDescriber() {
        assert describer != null;
        return describer;
    }

    /**
     * Alter which describer to use.
     *
     * @param newDescriber (not null, alias created)
     * @return this instance for chaining
     */
    public Dumper setDescriber(Describer newDescriber) {
        Validate.nonNull(newDescriber, "new describer");
        describer = newDescriber;
        return this;
    }

    /**
     * Configure dumping of render-queue bucket assignments.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Dumper setDumpBucket(boolean newValue) {
        dumpBucketFlag = newValue;
        return this;
    }

    /**
     * Configure dumping of cull hints.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Dumper setDumpCull(boolean newValue) {
        dumpCullFlag = newValue;
        return this;
    }

    /**
     * Configure dumping of material-parameter overrides.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Dumper setDumpOverride(boolean newValue) {
        dumpOverrideFlag = newValue;
        return this;
    }

    /**
     * Configure dumping of shadow modes.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Dumper setDumpShadow(boolean newValue) {
        dumpShadowFlag = newValue;
        return this;
    }

    /**
     * Configure dumping of location and scaling.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Dumper setDumpTransform(boolean newValue) {
        dumpTransformFlag = newValue;
        return this;
    }

    /**
     * Configure dumping of user data.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Dumper setDumpUser(boolean newValue) {
        dumpUserFlag = newValue;
        return this;
    }

    /**
     * Configure the indent increment.
     *
     * @param newValue (not null)
     * @return this instance for chaining
     */
    public Dumper setIndentIncrement(String newValue) {
        Validate.nonNull(newValue, "increment");
        indentIncrement = newValue;
        return this;
    }
}
