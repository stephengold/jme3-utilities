/*
 Copyright (c) 2013-2021, Stephen Gold
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

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyString;
import jme3utilities.UncachedKey;
import jme3utilities.Validate;

/**
 * An ActionAppState to implement a configurable input mode. At most one mode is
 * active at a time.
 * <p>
 * Modes may be temporarily suspended, in which the case the underlying app
 * state remains enabled even though the mode is considered inactive.
 * <p>
 * The active mode maps hotkeys to actions and controls the appearance of the
 * mouse pointer/cursor. Hotkeys can be mapped to signal actions, which cause
 * multiple keys (shift or control keys, for exampled) to share a common modal
 * function.
 * <p>
 * When a hotkey binding is live in the input manager, the hotkey is said to be
 * "mapped". When an input mode is inactive, its hotkey bindings persist,
 * allowing them to be altered, loaded, and saved even though they are unmapped.
 * <p>
 * Hotkeys are bound to action names. The input manager, however, stores "action
 * strings". For non-signal actions, the action string is identical to the
 * action name. For an action which updates a signal, the action name consists
 * of "signal " followed by the name of the signal. In that case, a space and
 * decimal keycode are appended to the action name generate a unique action
 * string for each signal source.
 * <p>
 * Input modes are disabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
abstract public class InputMode
        extends ActionAppState
        implements ActionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * highest-numbered universal code + 1
     */
    final private static int numCodes = KeyInput.KEY_LAST + 4;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(InputMode.class.getName());
    /**
     * action-string prefix for a combo action
     */
    final public static String comboActionPrefix = "combo ";
    /**
     * action-string prefix for a signal action
     */
    final public static String signalActionPrefix = "signal ";
    // *************************************************************************
    // fields

    /**
     * true if the mode is suspended (enabled but temporarily deactivated)
     */
    private boolean isSuspended = false;
    /**
     * true if initialize() should activate (and enable) this mode
     */
    private boolean startEnabled = false;
    /**
     * keep track of the currently active mode (null means there's none)
     */
    private static InputMode activeMode = null;
    /**
     * appearance of the mouse pointer/cursor in this mode (null means hidden)
     */
    private JmeCursor cursor = null;
    /**
     * map combos to action names for each universal code
     */
    @SuppressWarnings("unchecked")
    final private Map<Combo, String>[] comboBindings = new Map[numCodes];
    /**
     * map from short names to initialized input modes
     */
    final private static Map<String, InputMode> modes = new TreeMap<>();
    /**
     * bindings from hotkeys to action names: needed because the InputManager
     * doesn't provide access to its mappings and also so that the hotkey
     * bindings editor can examine hotkey bindings while this mode is disabled
     */
    private Properties hotkeyBindings = new Properties();
    /**
     * all known action names, bound and unbound
     */
    final private Set<String> actionNames = new TreeSet<>();
    /**
     * LIFO stack of suspended input modes
     */
    final private static Stack<InputMode> suspendedModes = new Stack<>();
    /**
     * path to configuration asset for loading and saving hotkey bindings (or
     * null if not loadable/savable): set by #setConfigPath()
     */
    private String configAssetPath = null;
    /**
     * terse name for this mode: set by constructor
     */
    final private String shortName;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     *
     * @param shortName terse name for the mode (not null)
     */
    public InputMode(String shortName) {
        super(false);

        Validate.nonNull(shortName, "name");
        this.shortName = shortName;

        for (int code = 0; code < numCodes; ++code) {
            comboBindings[code] = new HashMap<>(8);
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add the specified action name without binding anything to it.
     *
     * @param name action name (not null)
     */
    public void addActionName(String name) {
        Validate.nonNull(name, "name");
        actionNames.add(name);
    }

    /**
     * Bind the named action to the specified Combo. Any existing binding for
     * the Combo is removed.
     *
     * @param actionName name of the action (not null)
     * @param combo which Combo to bind (not null)
     */
    public void bind(String actionName, Combo combo) {
        Validate.nonNull(actionName, "action name");
        Validate.nonNull(combo, "hotkey");

        int triggerCode = combo.triggerCode();
        comboBindings[triggerCode].put(combo, actionName);

        addActionName(actionName);
    }

    /**
     * Bind the named action to the specified hotkey, but don't map it yet. Any
     * existing binding for the hotkey is removed.
     *
     * @param actionName name of the action (not null)
     * @param hotkey which hotkey to bind (not null)
     */
    public void bind(String actionName, Hotkey hotkey) {
        Validate.nonNull(actionName, "action name");
        Validate.nonNull(hotkey, "hotkey");

        String description = hotkey.name();
        hotkeyBindings.put(description, actionName);
        addActionName(actionName);
    }

    /**
     * Bind the named action to the specified key codes, but don't map it yet.
     * Any existing bindings for those keys are removed.
     *
     * @param actionName the name of the action (not null)
     * @param keyCodes key codes from {@link com.jme3.input.KeyInput}
     */
    public void bind(String actionName, int... keyCodes) {
        Validate.nonNull(actionName, "action name");

        for (int keyCode : keyCodes) {
            Hotkey hotkey = Hotkey.findKey(keyCode);
            bind(actionName, hotkey);
        }
    }

    /**
     * Bind the named action to the named hotkey, but don't map it yet. Any
     * existing binding for the hotkey is removed.
     *
     * @param actionName the name of the action (not null)
     * @param hotkeyName the hotkey's name (not null)
     */
    public void bind(String actionName, String hotkeyName) {
        Validate.nonNull(actionName, "action name");
        Validate.nonNull(hotkeyName, "hotkey name");

        assert Hotkey.find(hotkeyName) != null : hotkeyName;
        hotkeyBindings.put(hotkeyName, actionName);
        addActionName(actionName);
    }

    /**
     * Test whether this input mode binds the specified hotkey.
     *
     * @param hotkey hotkey to test (not null)
     * @return true if bound, otherwise false
     */
    public boolean binds(Hotkey hotkey) {
        Validate.nonNull(hotkey, "hotkey");

        String actionName = findActionName(hotkey);
        return actionName != null;
    }

    /**
     * Bind the named signal to the specified key codes, but don't map it yet.
     * Any existing bindings for those keys are removed.
     *
     * @param signalName the name of the signal (not null)
     * @param keyCodes key codes from {@link com.jme3.input.KeyInput}
     */
    public void bindSignal(String signalName, int... keyCodes) {
        Validate.nonNull(signalName, "action name");

        String actionName = InputMode.signalActionPrefix + signalName;
        bind(actionName, keyCodes);
    }

    /**
     * Determine the path to the configuration asset.
     *
     * @return current asset path (or null if the bindings are not
     * loadable/savable)
     */
    public String configPath() {
        String path = configAssetPath;
        return path;
    }

    /**
     * Look up the action bound to a hotkey.
     *
     * @param hotkey (not null)
     * @return hotkey's action name, or null if the hotkey isn't bound
     */
    public String findActionName(Hotkey hotkey) {
        String keyName = hotkey.name();
        String actionName = hotkeyBindings.getProperty(keyName);

        return actionName;
    }

    /**
     * Find an initialized mode by its short name.
     *
     * @param shortName (not null)
     * @return pre-existing instance (or null if none)
     */
    public static InputMode findMode(String shortName) {
        Validate.nonNull(shortName, "name");
        InputMode result = modes.get(shortName);
        return result;
    }

    /**
     * Access the active mode, if any.
     *
     * @return the pre-existing instance (or null if none active)
     */
    public static InputMode getActiveMode() {
        return activeMode;
    }

    /**
     * List all known action names.
     *
     * @return a new list
     */
    public List<String> listActionNames() {
        List<String> result = new ArrayList<>(actionNames);
        return result;
    }

    /**
     * Enumerate all combos bound to the named action.
     *
     * @param actionName the action name (not null)
     * @return a new collection of combos
     */
    public Collection<Combo> listCombos(String actionName) {
        Validate.nonNull(actionName, "name");

        Collection<Combo> result = new HashSet<>(32);
        for (int code = 0; code < numCodes; ++code) {
            Map<Combo, String> map = comboBindings[code];
            for (Map.Entry<Combo, String> entry : map.entrySet()) {
                String action = entry.getValue();
                if (action.equals(actionName)) {
                    Combo combo = entry.getKey();
                    result.add(combo);
                }
            }
        }

        return result;
    }

    /**
     * Enumerate all hotkeys bound to a named action.
     *
     * @param actionName name of action (not null)
     * @return a new collection of names in lexicographic order
     */
    public Collection<String> listHotkeys(String actionName) {
        Validate.nonNull(actionName, "name");

        Collection<String> result = new TreeSet<>();
        for (String keyName : hotkeyBindings.stringPropertyNames()) {
            String property = hotkeyBindings.getProperty(keyName);
            /*
             * Note that action string comparisons are sensitive to both
             * case and whitespace.
             */
            if (property.equals(actionName)) {
                result.add(keyName);
            }
        }

        return result;
    }

    /**
     * Load a set of hotkey bindings from the configuration asset.
     */
    public void loadBindings() {
        String assetPath = configPath();
        if (assetPath == null) {
            logger.log(Level.WARNING,
                    "bindings not loaded: config path not set");
            return;
        }

        try {
            loadBindings(assetPath);

        } catch (FileNotFoundException exception) {
            logger.log(Level.SEVERE, "Didn''t find any hotkey bindings at {0}.",
                    MyString.quote(assetPath));

        } catch (IOException exception) {
            logger.log(Level.SEVERE,
                    "Input exception while loading hotkey bindings from {0}!",
                    MyString.quote(assetPath));
            throw new RuntimeException(exception);
        }
    }

    /**
     * Process a "combo" action.
     *
     * @param code the universal code of the action (&ge;0)
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    void processCombos(int code, float tpf) {
        Map<Combo, String> map = comboBindings[code];
        for (Map.Entry<Combo, String> entry : map.entrySet()) {
            Combo combo = entry.getKey();
            if (combo.testAll(signals)) {
                String actionString = entry.getValue();
                boolean ongoing = true;
                onAction(actionString, ongoing, tpf);
            }
        }
    }

    /**
     * Disable the active input mode and resume the most recently suspended
     * mode.
     */
    public static void resumeLifo() {
        int numSuspended = suspendedModes.size();
        assert numSuspended > 0 : numSuspended;

        InputMode active = InputMode.getActiveMode();
        if (active != null) {
            active.setEnabled(false);
        }

        InputMode mostRecent = suspendedModes.pop();
        mostRecent.resume();
    }

    /**
     * Save all hotkey bindings to the configuration asset.
     */
    public void saveBindings() {
        assert isInitialized();

        String assetPath = configPath();
        if (assetPath == null) {
            logger.log(Level.WARNING,
                    "Hotkey bindings not saved: config path not set");
            return;
        }

        try {
            saveBindings(assetPath);
        } catch (IOException exception) {
            logger.log(Level.SEVERE,
                    "Output exception while saving hotkey bindings to {0}!",
                    MyString.quote(assetPath));
            throw new RuntimeException(exception);
        }
    }

    /**
     * Alter the path to the configuration asset.
     *
     * @param assetPath desired asset path (or null to make the bindings not
     * loadable/savable)
     */
    public void setConfigPath(String assetPath) {
        configAssetPath = assetPath;
    }

    /**
     * Alter the mouse cursor for this uninitialized mode.
     *
     * @param newCursor new cursor, or null to hide the cursor when active
     */
    public void setCursor(JmeCursor newCursor) {
        assert !isInitialized();
        cursor = newCursor;
    }

    /**
     * Determine the short-form name for this mode.
     *
     * @return name (not null)
     */
    public String shortName() {
        assert shortName != null;
        return shortName;
    }

    /**
     * Save and suspend the active input mode (if any) and activate the
     * specified mode.
     *
     * @param newMode the desired input mode, or null for none
     */
    public static void suspendAndActivate(InputMode newMode) {
        InputMode oldMode = InputMode.getActiveMode();
        if (oldMode != null) {
            oldMode.suspend();
            suspendedModes.push(oldMode);
        }

        if (newMode != null) {
            newMode.setEnabled(true);
        }
    }

    /**
     * Unbind the specified hotkey.
     *
     * @param hotkey (not null)
     */
    public void unbind(Hotkey hotkey) {
        assert isInitialized();

        String hotkeyName = hotkey.name();
        hotkeyBindings.remove(hotkeyName);
    }

    /**
     * Unbind the specified keyboard key.
     *
     * @param keyCode the key code
     */
    public void unbind(int keyCode) {
        Validate.inRange(keyCode, "key code", 0, KeyInput.KEY_LAST);

        Hotkey hotkey = Hotkey.findKey(keyCode);
        unbind(hotkey);
    }
    // *************************************************************************
    // new protected methods

    /**
     * Activate this mode.
     */
    protected void activate() {
        setActiveMode(this);

        if (cursor == null) {
            inputManager.setCursorVisible(false);
        } else {
            inputManager.setMouseCursor(cursor);
            inputManager.setCursorVisible(true);
        }
        /*
         * Map all bound hotkeys to their actions.
         */
        for (String keyName : hotkeyBindings.stringPropertyNames()) {
            String actionName = hotkeyBindings.getProperty(keyName);
            Hotkey hotkey = Hotkey.find(keyName);
            mapActionName(actionName, hotkey);
        }
        /*
         * Map all bound combos to their actions.
         */
        for (int code = 0; code < numCodes; ++code) {
            Map<Combo, String> map = comboBindings[code];
            if (!map.isEmpty()) {
                String actionName = comboActionPrefix + Integer.toString(code);
                Hotkey hotkey = Hotkey.find(code);
                mapNonsignalHotkey(actionName, hotkey);
            }
        }
    }

    /**
     * Deactivate this mode.
     */
    protected void deactivate() {
        setActiveMode(null);
        inputManager.setCursorVisible(false);
        /*
         * Unmap all Hotkey actions.
         */
        for (String keyName : hotkeyBindings.stringPropertyNames()) {
            String actionName = hotkeyBindings.getProperty(keyName);
            Hotkey hotkey = Hotkey.find(keyName);
            unmapHotkey(actionName, hotkey);
        }
        /*
         * Unmap all Combo actions.
         */
        for (int code = 0; code < numCodes; ++code) {
            Map<Combo, String> map = comboBindings[code];
            if (!map.isEmpty()) {
                String actionString
                        = comboActionPrefix + Integer.toString(code);
                Hotkey hotkey = Hotkey.find(code);
                hotkey.unmap(actionString, inputManager);
            }
        }

        inputManager.removeListener(this);
    }

    /**
     * Add the default hotkey bindings. The bindings to be used if no custom
     * bindings are found.
     */
    abstract protected void defaultBindings();
    // *************************************************************************
    // ActionAppState methods

    /**
     * Initialize this (disabled) mode prior to its first update.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);

        InputMode prior = modes.put(shortName, this);
        assert prior == null : shortName;
        /*
         * Load the initial hotkey bindings.
         */
        initializeHotkeyBindings();

        ActionApplication aa = (ActionApplication) application;
        if (this == aa.getDefaultInputMode()) {
            /*
             * Give the application an opportunity to override the
             * initial bindings.
             */
            aa.moreDefaultBindings();
        }

        setEnabled(startEnabled);
    }

    /**
     * Enable or disable this mode.
     *
     * @param newState true to enable, false to disable
     */
    @Override
    public void setEnabled(boolean newState) {
        logger.log(Level.FINE, "mode={0} newState={1}", new Object[]{
            shortName, newState
        });
        assert !isSuspended;
        if (!isInitialized()) {
            startEnabled = newState;
            return;
        }

        if (!isEnabled() && newState) {
            activate();
        } else if (isEnabled() && !newState) {
            assert activeMode == this : activeMode;
            deactivate();
        }

        super.setEnabled(newState);
    }
    // *************************************************************************
    // Object methods

    /**
     * Represent this input mode as a text string.
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        String status;
        if (isInitialized()) {
            if (isEnabled()) {
                status = isSuspended ? "suspended" : "active";
            } else {
                status = "disabled";
            }
        } else {
            status = String.format("uninitialized, startEnabled=%s",
                    startEnabled);
        }
        String result = String.format("%s (%s)", shortName, status);

        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Count how many hotkeys are bound to a named action.
     *
     * @param actionName (not null)
     * @return count (&ge;0)
     */
    private int countBindings(String actionName) {
        assert actionName != null;

        int count = 0;
        for (String keyString : hotkeyBindings.stringPropertyNames()) {
            String name = hotkeyBindings.getProperty(keyString);
            /*
             * Note: action name comparisons are sensitive to both
             * case and whitespace.
             */
            if (name.equals(actionName)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Initialize the hotkey bindings.
     */
    private void initializeHotkeyBindings() {
        if (configAssetPath == null) {
            defaultBindings();
            return;
        }
        /*
         * Attempt to load custom hotkey bindings from the configuration asset.
         * If that fails, load the default bindings for this mode.
         */
        String path = configPath();
        try {
            loadBindings(path);

        } catch (AssetNotFoundException exception) {
            logger.log(Level.INFO, "Didn''t find hotkey bindings at {0}.",
                    MyString.quote(path));

            hotkeyBindings.clear();
            defaultBindings();

        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Input exception while loading hotkey "
                    + "bindings from {0}!",
                    MyString.quote(path));

            hotkeyBindings.clear();
            defaultBindings();
        }
    }

    /**
     * Load hotkey bindings from a configuration asset.
     *
     * @param assetPath asset path (not null)
     */
    private void loadBindings(String assetPath)
            throws AssetNotFoundException, IOException {
        assert assetPath != null;

        logger.log(Level.INFO, "Loading hotkey bindings from asset {0}.",
                MyString.quote(assetPath));

        UncachedKey key = new UncachedKey(assetPath);
        hotkeyBindings = (Properties) assetManager.loadAsset(key);

        for (String keyString : hotkeyBindings.stringPropertyNames()) {
            String actionName = hotkeyBindings.getProperty(keyString);
            Hotkey hotkey = Hotkey.find(keyString);
            if (hotkey == null) {
                logger.log(Level.WARNING, "Skipped unknown hotkey {0} in {1}",
                        new Object[]{
                            MyString.quote(keyString),
                            MyString.quote(assetPath)
                        });
                hotkeyBindings.remove(keyString);
            } else {
                bind(actionName, hotkey);
            }
        }
    }

    /**
     * Map a hotkey to an action name. Overrides any previous mappings for the
     * hotkey.
     *
     * @param actionName the action name (not null)
     * @param hotkey (not null)
     */
    private void mapActionName(String actionName, Hotkey hotkey) {
        assert hotkey != null;

        if (actionName.startsWith(signalActionPrefix)) {
            mapSignalHotkey(actionName, hotkey);
        } else {
            mapNonsignalHotkey(actionName, hotkey);
        }
    }

    /**
     * Map a hotkey to a non-signal action.
     *
     * @param actionName name of the non-signal action (not null)
     * @param hotkey (not null)
     */
    private void mapNonsignalHotkey(String actionName, Hotkey hotkey) {
        assert actionName != null;
        assert hotkey != null;

        inputManager.addListener(this, actionName);
        /*
         * For a non-signal action, the action string is simply the name.
         * Add the mapping to the input manager.
         */
        hotkey.map(actionName, inputManager);
    }

    /**
     * Map a hotkey to a signal action.
     *
     * @param actionName name of the signal action (not null)
     * @param hotkey (not null)
     */
    private void mapSignalHotkey(String actionName, Hotkey hotkey) {
        assert hotkey != null;
        /*
         * The second word of the action name is the signal name.
         */
        String[] words = actionName.split("\\s+");
        assert words.length > 1 : MyString.quote(actionName);
        assert "signal".equals(words[0]);
        String signalName = words[1];
        signals.add(signalName);
        /*
         * Append the decimal keyCode to ensure a unique action string.
         */
        String actionString = signalActionString(actionName, hotkey);
        int count = countBindings(actionString);
        boolean isUnique = (count == 0);
        assert isUnique : count;
        inputManager.addListener(signals, actionString);
        /*
         * Add the mapping to the input manager.
         */
        hotkey.map(actionString, inputManager);
    }

    /**
     * Reactivate this (enabled) mode after a suspension.
     */
    private void resume() {
        assert isEnabled();
        assert isSuspended;

        activate();
        isSuspended = false;
    }

    /**
     * Save hotkey bindings to an output stream.
     *
     * @param stream output stream to save to (not null)
     */
    private void saveBindings(OutputStream stream) throws IOException {
        assert stream != null;

        String comment = String.format("custom hotkey bindings for %s mode",
                shortName);
        hotkeyBindings.storeToXML(stream, comment);
    }

    /**
     * Save hotkey bindings to a configuration asset.
     *
     * @param assetPath asset path (not null)
     */
    private void saveBindings(String assetPath) throws IOException {
        assert assetPath != null;

        logger.log(Level.INFO, "Saving hotkey bindings to asset {0}.",
                MyString.quote(assetPath));

        FileOutputStream stream = null;
        String filePath = ActionApplication.filePath(assetPath);
        try {
            File file = new File(filePath);
            File parentDirectory = file.getParentFile();
            if (parentDirectory != null && !parentDirectory.exists()) {
                boolean success = parentDirectory.mkdirs();
                if (!success) {
                    String parentPath = Heart.fixedPath(parentDirectory);
                    String msg = String.format(
                            "Unable to create folder %s for hotkey bindings",
                            MyString.quote(parentPath));
                    throw new IOException(msg);
                }
            }
            stream = new FileOutputStream(filePath);
            saveBindings(stream);
        } catch (IOException exception) {
            throw exception;
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * Alter the static reference to the active InputMode. At most one InputMode
     * is active at a time.
     *
     * @param mode the desired InputMode (or null if none)
     */
    private static void setActiveMode(InputMode mode) {
        if (mode != null && activeMode != null) {
            String message = String.format(
                    "tried to activate %s input mode while %s mode was active",
                    MyString.quote(mode.shortName),
                    MyString.quote(activeMode.shortName));
            throw new IllegalStateException(message);
        }

        activeMode = mode;
    }

    /**
     * Generate an action string for a signal action.
     *
     * @param actionName name of the action (not null)
     * @param hotkey triggering hotkey (not null)
     */
    private String signalActionString(String actionName, Hotkey hotkey) {
        assert actionName != null;

        int keyCode = hotkey.keyCode();
        String actionString = String.format("%s %d", actionName, keyCode);

        return actionString;
    }

    /**
     * Temporarily deactivate this enabled mode without disabling its app state.
     */
    private void suspend() {
        assert isEnabled();
        assert !isSuspended;

        deactivate();
        isSuspended = true;
    }

    /**
     * Delete a hotkey mapping from the input manager (if it exists).
     *
     * @param actionName name of the mapped action (not null)
     * @param hotkey (not null)
     */
    private void unmapHotkey(String actionName, Hotkey hotkey) {
        assert hotkey != null;
        /*
         * Reconstruct the action string.
         */
        String actionString;
        if (actionName.startsWith(signalActionPrefix)) {
            actionString = signalActionString(actionName, hotkey);
        } else {
            actionString = actionName;
        }
        /*
         * Delete the mapping, if it exists.
         */
        hotkey.unmap(actionString, inputManager);
    }
}
