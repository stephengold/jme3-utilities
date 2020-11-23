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
package jme3utilities.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.mesh.RoundedRectangle;

/**
 * Utility methods to generate hotkey clues for action-oriented applications.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelpUtils {
    // *************************************************************************
    // constants and loggers

    /**
     * color of the background
     */
    final private static ColorRGBA backgroundColor
            = new ColorRGBA(0f, 0f, 0f, 1f);
    /**
     * foreground color for highlighted text
     */
    final private static ColorRGBA highlightForegroundColor
            = new ColorRGBA(1f, 1f, 0f, 1f);
    /**
     * padding added to all 4 sides of the background (in pixels)
     */
    final private static float padding = 5f;
    /**
     * Z coordinate for the background
     */
    final private static float zBackground = -1f;
    /**
     * Z coordinate for text
     */
    final private static float zText = 0f;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(HelpUtils.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private HelpUtils() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Build a Node to describe the hotkey mappings of the specified InputMode
     * within the specified bounds.
     *
     * @param inputMode (not null, unaffected)
     * @param bounds (not null, unaffected)
     * @param font (not null, unaffected)
     * @param space amount of extra space between hotkey descriptions (in
     * pixels)
     * @return a new orphan Node, suitable for attachment to the GUI node
     */
    public static Node buildNode(InputMode inputMode, Rectangle bounds,
            BitmapFont font, float space) {
        Validate.nonNull(inputMode, "input mode");
        Validate.nonNull(bounds, "bounds");
        Validate.nonNull(font, "font");

        Map<String, String> actionToList = mapActions(inputMode);

        Node result = new Node("help node");
        float x = bounds.x;
        float y = bounds.y;
        float maxX = x + 1;
        float minY = y - 1;

        for (Map.Entry<String, String> entry : actionToList.entrySet()) {
            BitmapText spatial = new BitmapText(font);
            result.attachChild(spatial);
            spatial.setSize(font.getCharSet().getRenderedSize());

            String actionName = entry.getKey();
            String hotkeyList = entry.getValue();
            String string = actionName + ": " + hotkeyList;
            spatial.setText(string);
            float textWidth = spatial.getLineWidth();
            if (x > bounds.x
                    && x + textWidth > bounds.x + bounds.width) {
                // start a new line of text
                y -= spatial.getHeight();
                x = bounds.x;
            }
            spatial.setLocalTranslation(x, y, zText);
            maxX = Math.max(maxX, x + textWidth);
            minY = Math.min(minY, y - spatial.getHeight());
            x += textWidth + space;

            if (actionName.equals("toggle help")) {
                spatial.setColor(highlightForegroundColor);
            }
        }

        Geometry backgroundGeometry = buildBackground(bounds, maxX, minY);
        result.attachChild(backgroundGeometry);

        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Beautify the specified action name.
     *
     * @param actionName the action name (not null)
     * @return the beautified name (not null)
     */
    private static String beautify(String actionName) {
        Validate.nonNull(actionName, "action name");

        String result = actionName;
        if (result.startsWith(InputMode.signalActionPrefix)) {
            result = MyString.remainder(result, InputMode.signalActionPrefix);
        }

        if (result.startsWith("SIMPLEAPP_")) {
            String suffix = MyString.remainder(result, "SIMPLEAPP_");
            result = MyString.firstToLower(suffix);
            if (result.equals("hideStats")) {
                result = "toggle stats";
            }
        } else if (result.startsWith("FLYCAM_")) {
            String suffix = MyString.remainder(result, "FLYCAM_");
            result = "camera " + MyString.firstToLower(suffix);
        }

        return result;
    }

    /**
     * Compactly describe the named hotkey. TODO re-order methods
     *
     * @param hotkeyName the hotkey name (not null)
     * @return the compressed name (not null)
     */
    private static String compress(String hotkeyName) {
        Validate.nonNull(hotkeyName, "hotkey name");

        String result = hotkeyName;
        if (result.endsWith(" arrow")) {
            result = MyString.removeSuffix(result, " arrow");
        }
        result = result.replace("numpad ", "num");

        return result;
    }

    /**
     * Generate a background geometry for the help node.
     *
     * @param bounds (in screen coordinates, not null, unaffected)
     * @param maxX the highest screen X of the text
     * @param minY the lowest screen Y of the text
     * @return a new Geometry, suitable for attachment to the help node
     */
    private static Geometry buildBackground(Rectangle bounds, float maxX,
            float minY) {
        float x1 = bounds.x - padding;
        float x2 = maxX + padding;
        float y1 = minY - padding;
        float y2 = bounds.y + padding;
        float zNorm = 1f;
        Mesh backgroundMesh
                = new RoundedRectangle(x1, x2, y1, y2, padding, zNorm);

        Geometry result = new Geometry("help background", backgroundMesh);
        result.setLocalTranslation(0f, 0f, zBackground);

        AssetManager assetManager = Locators.getAssetManager();
        Material backgroundMaterial
                = MyAsset.createUnshadedMaterial(assetManager, backgroundColor);
        result.setMaterial(backgroundMaterial);

        return result;
    }

    /**
     * Compactly describe the specified Combo using beautified hotkey names.
     * Compare with Combo.toString().
     *
     * @param combo the Combo to describe (not null)
     * @return a textual description (not null)
     */
    private static String describe(Combo combo) {
        StringBuilder result = new StringBuilder(40);

        int numSignals = combo.countSignals();
        for (int signalIndex = 0; signalIndex < numSignals; ++signalIndex) {
            boolean positiveFlag = combo.isPositive(signalIndex);
            if (!positiveFlag) {
                result.append("no");
            }

            String signalName = combo.signalName(signalIndex);
            result.append(signalName);
            result.append('+');
        }

        int code = combo.triggerCode();
        Hotkey hotkey = Hotkey.find(code);
        String hotkeyName = hotkey.name();
        hotkeyName = compress(hotkeyName);
        result.append(hotkeyName);

        return result.toString();
    }

    /**
     * For the specified InputMode, construct a Map from beautified action names
     * to comma-separated, compressed hotkey names.
     *
     * @param inputMode (not null, unaffected)
     * @return a new String-to-String Map
     */
    private static Map<String, String> mapActions(InputMode inputMode) {
        List<String> actionNames = inputMode.listActionNames();
        Map<String, String> actionsToHots = new TreeMap<>();

        for (String actionName : actionNames) {
            String action = beautify(actionName);

            Collection<String> hotkeyNames = inputMode.listHotkeys(actionName);
            for (String hotkeyName : hotkeyNames) {
                String description = compress(hotkeyName);
                if (actionsToHots.containsKey(action)) {
                    String oldList = actionsToHots.get(action);
                    String newList = oldList + "/" + description;
                    actionsToHots.put(action, newList);
                } else {
                    actionsToHots.put(action, description);
                }
            }

            Collection<Combo> combos = inputMode.listCombos(actionName);
            for (Combo combo : combos) {
                String description = describe(combo);
                if (actionsToHots.containsKey(action)) {
                    String oldList = actionsToHots.get(action);
                    String newList = oldList + "/" + description;
                    actionsToHots.put(action, newList);
                } else {
                    actionsToHots.put(action, description);
                }
            }
        }

        return actionsToHots;
    }
}
