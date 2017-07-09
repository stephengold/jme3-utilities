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

import com.jme3.app.state.ScreenshotAppState;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.FogFilter;
import com.jme3.post.filters.PosterizationFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.PointLightShadowFilter;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowFilter;
import com.jme3.shadow.SpotLightShadowRenderer;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.water.ReflectionProcessor;
import com.jme3.water.SimpleWaterProcessor;
import com.jme3.water.SimpleWaterProcessor.RefractionProcessor;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MyControl;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * Dump portions of a jME3 scene graph for debugging. TODO rename Dumper.dump()
 * <p>
 * {@link #printSubtree(com.jme3.scene.Spatial)} is the usual interface to this
 * class. The level of detail can be configured dynamically.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Printer {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Printer.class.getName());
    // *************************************************************************
    // fields

    /**
     * enable printing of render queue bucket assignments
     */
    private boolean printBucketFlag = false;
    /**
     * enable printing of cull hints
     */
    private boolean printCullFlag = false;
    /**
     * enable printing of shadow modes
     */
    private boolean printShadowFlag = false;
    /**
     * enable printing of location and scaling
     */
    private boolean printTransformFlag = false;
    /**
     * enable printing of user data
     */
    private boolean printUserFlag = true;
    /**
     * stream to use for output: set by constructor
     */
    final private PrintStream stream;
    /**
     * indentation for each level of a dump
     */
    private String indentIncrement = "  ";
    /**
     * separator text between names
     */
    private String nameSeparator = ",";
    // *************************************************************************
    // constructors

    /**
     * Instantiate a printer which will use System.out for output.
     */
    public Printer() {
        stream = System.out;
    }

    /**
     * Instantiate a printer which will use the specified output stream.
     *
     * @param printStream (not null)
     */
    public Printer(PrintStream printStream) {
        Validate.nonNull(printStream, "print stream");

        stream = printStream;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Generate a textual description of a spatial's controls.
     *
     * @param spatial spatial being described (not null)
     * @param enabled if true, describe only the enabled controls; if false,
     * describe only the disabled controls
     * @return description (not null)
     */
    public String describeControls(Spatial spatial, boolean enabled) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;
        int count = spatial.getNumControls();
        for (int i = 0; i < count; i++) {
            Object object = spatial.getControl(i);
            boolean isEnabled = isControlEnabled(object);
            if (isEnabled == enabled) {
                if (addSeparators) {
                    result.append(nameSeparator);
                } else {
                    addSeparators = true;
                }
                String description = describeControl(object);
                result.append(description);
            }
        }
        return result.toString();
    }

    /**
     * Generate a textual description of a viewport's scene processors.
     *
     * @param viewPort view port being described (not null)
     * @return description (not null)
     */
    public String describeProcessors(ViewPort viewPort) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;
        List<SceneProcessor> pList = viewPort.getProcessors();
        int count = pList.size();
        for (int i = 0; i < count; i++) {
            SceneProcessor processor = pList.get(i);
            if (addSeparators) {
                result.append(nameSeparator);
            } else {
                addSeparators = true;
            }
            String description = describe(processor);
            result.append(description);
        }

        return result.toString();
    }

    /**
     * Generate a textual description of a filter post-processor's filters.
     *
     * @param fpp processor being described (not null)
     * @return description (not null)
     */
    public String describeFilters(FilterPostProcessor fpp) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;

        Iterator<Filter> iterator = fpp.getFilterIterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }

        iterator = fpp.getFilterIterator();
        for (int i = 0; i < count; i++) {
            Filter filter = iterator.next();
            if (addSeparators) {
                result.append(nameSeparator);
            } else {
                addSeparators = true;
            }
            String description = describe(filter);
            result.append(description);
        }

        return result.toString();
    }

    /**
     * Dump the specified list of scenes.
     *
     * @param sceneList the root nodes of the scenes to dump (not null)
     * @param indent (not null)
     */
    public void print(List<Spatial> sceneList, String indent) {
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
            printSubtree(scene, indent + indentIncrement);
        }
    }

    /**
     * Dump the specified render manager.
     *
     * @param renderManager which render manager to dump (not null)
     */
    public void print(RenderManager renderManager) {
        List<ViewPort> pres = renderManager.getPreViews();
        int numPres = pres.size();
        List<ViewPort> mains = renderManager.getMainViews();
        int numMains = mains.size();
        List<ViewPort> posts = renderManager.getPostViews();
        int numPosts = posts.size();

        stream.printf("%nrender manager with %d preView%s, %d mainView%s, and ",
                numPres, (numPres == 1) ? "" : "s",
                numMains, (numMains == 1) ? "" : "s");
        stream.printf("%s postView%s%n", numPosts, (numPosts == 1) ? "" : "s");

        for (int index = 0; index < numPres; index++) {
            stream.printf("preView[%d]:%n", index);
            print(pres.get(index), indentIncrement);
        }
        for (int index = 0; index < numMains; index++) {
            stream.printf("mainView[%d]:%n", index);
            print(mains.get(index), indentIncrement);
        }
        for (int index = 0; index < numPosts; index++) {
            stream.printf("postView[%d]:%n", index);
            print(posts.get(index), indentIncrement);
        }
    }

    /**
     * Dump the specified view port.
     *
     * @param viewPort the view port to dump (not null)
     * @param indent (not null)
     */
    public void print(ViewPort viewPort, String indent) {
        Validate.nonNull(indent, "indent");

        stream.print(indent);
        String name = viewPort.getName();
        stream.printf("view port %s ", MyString.quote(name));
        if (viewPort.isEnabled()) {
            stream.println("enabled");
            stream.print(indent);

            Camera cam = viewPort.getCamera();
            String desc = cam.toString();
            desc = desc.replace("\n", " ");
            desc = desc.replace(", ", " ");
            stream.println(desc);
            stream.print(indent);

            float l = cam.getViewPortLeft();
            float r = cam.getViewPortRight();
            float b = cam.getViewPortBottom();
            float t = cam.getViewPortTop();
            stream.printf("x[%.2f %.2f] y[%.2f %.2f] ", l, r, b, t);

            float aspectRatio = MyCamera.aspectRatio(cam);
            stream.printf("%.3f:1 ", aspectRatio);

            if (!viewPort.isClearDepth()) {
                stream.print("NO");
            }
            stream.print("clDepth,");

            if (!viewPort.isClearColor()) {
                stream.print("NO");
            }
            stream.print("clColor,");

            if (!viewPort.isClearStencil()) {
                stream.print("NO");
            }
            stream.print("clStencil ");

            if (viewPort.isClearColor()) {
                ColorRGBA backColor = viewPort.getBackgroundColor();
                stream.printf("back%s ", backColor.toString());
            }

            stream.printf("procs=(%s) with ", describeProcessors(viewPort));
            List<Spatial> scenes = viewPort.getScenes();
            print(scenes, indent);

        } else {
            stream.println("disabled");
        }
    }

    /**
     * Dump the render queue bucket to which a spatial is assigned.
     *
     * @param spatial spatial being described (not null)
     */
    public void printBucket(Spatial spatial) {
        /*
         * Print its local assignment.
         */
        Bucket bucket = spatial.getLocalQueueBucket();
        stream.printf(" bucket=%s", bucket.toString());
        if (bucket == Bucket.Inherit) {
            /*
             * Print its effective assignment.
             */
            bucket = spatial.getQueueBucket();
            stream.printf("/%s", bucket.toString());
        }
    }

    /**
     * List the controls associated with a spatial.
     *
     * @param spatial spatial being described (not null)
     */
    public void printControls(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");
        /*
         * List its enabled controls first.
         */
        String description = describeControls(spatial, true);
        if (description.length() > 0) {
            stream.printf(" %s", description);
        }
        /*
         * List its disabled controls last, in parentheses.
         */
        description = describeControls(spatial, false);
        if (description.length() > 0) {
            stream.printf(" (%s)", description);
        }
    }

    /**
     * Dump the view frustum culling hints associated with a spatial.
     *
     * @param spatial spatial being described (not null)
     */
    public void printCullHints(Spatial spatial) {
        /*
         * Print its local cull hint.
         */
        CullHint mode = spatial.getLocalCullHint();
        stream.printf(" cull=%s", mode.toString());
        if (mode == CullHint.Inherit) {
            /*
             * Print its effective cull hint.
             */
            mode = spatial.getCullHint();
            stream.printf("/%s", mode.toString());
        }
    }

    /**
     * List the lights associated with a spatial.
     *
     * @param spatial spatial being described (not null)
     */
    public void printLights(Spatial spatial) {
        LightList lights = spatial.getLocalLightList();
        for (Light light : lights) {
            String name = light.getName();
            stream.printf(" L(%s)", name); // TODO quotes not parens
        }
    }

    /**
     * Print the world location of a spatial.
     *
     * @param spatial spatial being described (not null)
     */
    public void printLocation(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");

        Vector3f location = MySpatial.getWorldLocation(spatial);
        if (!MyVector3f.isZero(location)) {
            stream.printf(" loc=[%.3f, %.3f, %.3f]",
                    location.x, location.y, location.z);
        }
    }

    /**
     * Print the world orientation of a spatial.
     *
     * @param spatial spatial being described (not null)
     */
    public void printOrientation(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");

        Quaternion orientation = MySpatial.getWorldOrientation(spatial);
        if (!orientation.isIdentity()) {
            stream.printf(" orient=%s", orientation.toString());
        }
    }

    /**
     * Print the world scale of a spatial.
     *
     * @param spatial spatial being described (not null)
     */
    public void printScale(Spatial spatial) {
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
     * Print the shadow modes associated with a spatial.
     *
     * @param spatial spatial being described (not null)
     */
    public void printShadowModes(Spatial spatial) {
        /*
         * Print its local shadow mode.
         */
        ShadowMode mode = spatial.getLocalShadowMode();
        stream.printf(" shad=%s", mode.toString());
        if (mode == ShadowMode.Inherit) {
            /*
             * Print its effective shadow mode.
             */
            mode = spatial.getShadowMode();
            stream.printf("/%s", mode.toString());
        }
    }

    /**
     * Dump a subtree of the scene graph.
     *
     * @param spatial root of the subtree (or null)
     */
    public void printSubtree(Spatial spatial) {
        printSubtree(spatial, "");
        stream.flush();
    }

    /**
     * Dump a subtree of the scene graph. Note: recursive!
     *
     * @param spatial root of the subtree (or null)
     * @param indent (not null)
     */
    public void printSubtree(Spatial spatial, String indent) {
        Validate.nonNull(indent, "indent");

        if (spatial == null) {
            return;
        }
        stream.print(indent);

        int elementCount = spatial.getTriangleCount();
        stream.printf("%c[%d] ", describeType(spatial), elementCount);

        String name = spatial.getName();
        if (name == null) {
            stream.print("(no name)");
        } else {
            stream.print(MyString.quote(spatial.getName()));
        }
        /*
         * Print the spatial's controls and lights
         */
        printControls(spatial);
        printLights(spatial);
        if (printTransformFlag) {
            printLocation(spatial);
            printOrientation(spatial);
            printScale(spatial);
        }
        if (printUserFlag) {
            printUserData(spatial);
        }
        if (printBucketFlag) {
            printBucket(spatial);
        }
        if (printShadowFlag) {
            printShadowModes(spatial);
        }
        if (printCullFlag) {
            printCullHints(spatial);
        }
        stream.println();
        /*
         * If the spatial is a node (but not a terrain node),
         * print its children with incremented indentation.
         */
        if (spatial instanceof Node && !(spatial instanceof TerrainQuad)) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                printSubtree(child, indent + indentIncrement);
            }
        }
    }

    /**
     * Print the user data associated with a spatial.
     *
     * @param spatial spatial being described (not null)
     */
    public void printUserData(Spatial spatial) {
        Collection<String> keys = spatial.getUserDataKeys();
        for (String key : keys) {
            Object value = spatial.getUserData(key);
            String valueString = MyString.escape(value.toString());
            if (value instanceof String) {
                valueString = MyString.quote(valueString);
            }
            stream.printf(" %s=%s", key, valueString);
        }
    }

    /**
     * Configure the control name separator.
     *
     * @param newValue (not null)
     * @return this instance for chaining
     */
    public Printer setControlNameSeparator(String newValue) {
        Validate.nonNull(newValue, "separator");
        nameSeparator = newValue;
        return this;
    }

    /**
     * Configure the indent increment.
     *
     * @param newValue (not null)
     * @return this instance for chaining
     */
    public Printer setIndentIncrement(String newValue) {
        Validate.nonNull(newValue, "increment");
        indentIncrement = newValue;
        return this;
    }

    /**
     * Configure printing of render queue bucket assignments.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Printer setPrintBucket(boolean newValue) {
        printBucketFlag = newValue;
        return this;
    }

    /**
     * Configure printing of cull hints.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Printer setPrintCull(boolean newValue) {
        printCullFlag = newValue;
        return this;
    }

    /**
     * Configure printing of shadow modes.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Printer setPrintShadow(boolean newValue) {
        printShadowFlag = newValue;
        return this;
    }

    /**
     * Configure printing of location and scaling.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Printer setPrintTransform(boolean newValue) {
        printTransformFlag = newValue;
        return this;
    }

    /**
     * Configure printing of user data.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Printer setPrintUser(boolean newValue) {
        printUserFlag = newValue;
        return this;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Generate a textual description of a filter.
     *
     * @param filter filter to describe (unaffected)
     * @return description (not null, not empty)
     */
    protected String describe(Filter filter) {
        String result;
        if (filter instanceof BloomFilter) {
            result = "Bloom";
        } else if (filter instanceof DepthOfFieldFilter) {
            result = "DOF";
        } else if (filter instanceof DirectionalLightShadowFilter) {
            result = "DShadow";
        } else if (filter instanceof FogFilter) {
            result = "Fog";
        } else if (filter instanceof PointLightShadowFilter) {
            result = "PShadow";
        } else if (filter instanceof PosterizationFilter) {
            result = "Posterize";
        } else if (filter instanceof SpotLightShadowFilter) {
            result = "SShadow";
        } else if (filter == null) {
            result = "null";
        } else {
            result = filter.getClass().getSimpleName();
            if (result.isEmpty()) {
                result = "?";
            }
        }

        return result;
    }

    /**
     * Generate a textual description of a scene processor.
     *
     * @param processor processor to describe (unaffected)
     * @return description (not null, not empty)
     */
    protected String describe(SceneProcessor processor) {
        String result;
        if (processor instanceof DirectionalLightShadowRenderer) {
            result = "DShadow";
        } else if (processor instanceof FilterPostProcessor) {
            FilterPostProcessor fpp = (FilterPostProcessor) processor;
            String desc = describeFilters(fpp);
            result = String.format("filters<%s>", desc);
        } else if (processor instanceof PointLightShadowRenderer) {
            result = "PShadow";
        } else if (processor instanceof ReflectionProcessor) {
            result = "Reflect";
        } else if (processor instanceof RefractionProcessor) {
            result = "Refract";
        } else if (processor instanceof ScreenshotAppState) {
            result = "Screenshot";
        } else if (processor instanceof SimpleWaterProcessor) {
            result = "SimpleWater";
        } else if (processor instanceof SpotLightShadowRenderer) {
            result = "SShadow";
        } else if (processor == null) {
            result = "null";
        } else {
            result = processor.getClass().getSimpleName();
            if (result.isEmpty()) {
                result = "?";
            }
        }

        return result;
    }

    /**
     * Generate a textual description of a scene-graph control.
     *
     * @param control (not null)
     * @return description (not null)
     */
    protected String describeControl(Object control) {
        Validate.nonNull(control, "control");

        String result = MyControl.describe(control);
        return result;
    }

    /**
     * Generate a single-character description of a spatial.
     *
     * @param spatial spatial being described
     * @return mnemonic character
     */
    protected static char describeType(Spatial spatial) {
        char result = MySpatial.describeType(spatial);
        return result;
    }

    /**
     * Test whether a scene-graph control is enabled.
     *
     * @param control control to test (not null)
     * @return true if the control is enabled, otherwise false
     */
    protected boolean isControlEnabled(Object control) {
        boolean result = MyControl.isEnabled(control);

        return result;
    }
}
