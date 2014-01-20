/*
 Copyright (c) 2013-2014, Stephen Gold
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
package jme3utilities.ui;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;

/**
 * An app state to implement a configurable input mode. At most one mode is
 * enabled at a time.
 * <p>
 * An enabled mode maps hotkeys to actions and controls the appearance of the
 * mouse pointer/cursor. Some hotkeys (such as the shift keys) can be associated
 * with signals, which allow multiple keys to share a common modal function.
 * <p>
 * When a hotkey binding is live in the input manager, the hotkey is said to be
 * "mapped". When an input mode is disabled, its hotkey bindings persist, but
 * its mappings don't, at which time the bindings can be reconfigured, loaded,
 * and saved.
 * <p>
 * Hotkeys are bound to action names. The input manager, however, stores "action
 * strings". For non-signal actions, the action string is identical to the
 * action name. For an action which updates a signal, the action name consists
 * of "signal " followed by the name of the signal. In that case, a space and
 * decimal hotkey code are appended to the action name generate a unique action
 * string for each signal source.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
abstract public class InputMode
        extends AbstractAppState
        implements ActionListener {
    // *************************************************************************
    // constants

    /**
     * list of initialized modes
     */
    final private static ArrayList<InputMode> modes = new ArrayList<>();
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(InputMode.class.getName());
    // *************************************************************************
    // fields
    /**
     * true if initialize() should enable this mode
     */
    private boolean startEnabled = false;
    /**
     * the application instance which owns this mode
     */
    protected GuiApplication application = null;
    /**
     * keep track of the currently enabled mode (null means there's none)
     */
    private static InputMode enabledMode = null;
    /**
     * appearance of the mouse pointer/cursor in this mode (null means hidden)
     */
    private JmeCursor cursor = null;
    /**
     * bindings from hotkeys to action names: needed because the InputManager
     * doesn't provide access to its mappings and also so that the bind screen
     * can look up hotkey bindings while this mode is disabled
     */
    final private Properties hotkeyBindings = new Properties();
    /**
     * file name for loading and saving the custom hotkey bindings (or null if
     * the bindings are not customizable): set by setSaveFileName()
     */
    private String customBindingsFileName = null;
    /**
     * terse name for this mode: set by constructor
     */
    final protected String shortName;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     *
     * @param shortName terse name for the mode (not null)
     */
    public InputMode(String shortName) {
        assert shortName != null;
        this.shortName = shortName;

        super.setEnabled(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Bind a given action to a hotkey, but don't map it yet.
     * <p>
     * Each hotkey can bind to at most one action, but the same action can be
     * shared by multiple hotkeys. Signals are used to track the state of mode
     * keys (such as the shift keys).
     *
     * @param actionName which action (not null)
     * @param keyCode the hotkey's keycode
     */
    public void bind(String actionName, int keyCode) {
        assert actionName != null;

        Hotkey hotkey = Hotkey.getInstance(keyCode);
        assert hotkey != null : keyCode;
        bind(actionName, hotkey);
    }

    /**
     * Bind a given action to a hotkey, but don't map it yet.
     * <p>
     * Each hotkey can bind to at most one action, but the same action can be
     * shared by multiple hotkeys. Signals are used to track the state of mode
     * keys (such as the shift keys).
     *
     * @param actionName which action (not null)
     * @param hotkey which hotkey (not null)
     */
    public void bind(String actionName, Hotkey hotkey) {
        assert actionName != null;

        String hotkeyName = hotkey.name();
        /*
         * Add to the bindings.  Remove any old binding of this hotkey.
         */
        hotkeyBindings.put(hotkeyName, actionName);
    }

    /**
     * Test whether this input mode binds the specified hotkey.
     *
     * @param hotkey which hotkey (not null)
     * @return true if bound, otherwise false
     */
    public boolean binds(Hotkey hotkey) {
        assert hotkey != null;

        String actionName = getActionName(hotkey);
        return actionName != null;
    }

    /**
     * Find the names of all hotkeys bound to a particular action.
     *
     * @param actionName which action (not null)
     * @return a new collection of hotkey names
     */
    public Collection<String> findHotkeys(String actionName) {
        assert actionName != null;

        Collection<String> result = new TreeSet<>();
        for (String keyName : hotkeyBindings.stringPropertyNames()) {
            String property = hotkeyBindings.getProperty(keyName);
            /*
             * Note that ction string comparisons are sensitive to both
             * case and whitespace.
             */
            if (property.equals(actionName)) {
                result.add(keyName);
            }
        }
        return result;
    }

    /**
     * Find an initialized mode by its short name.
     *
     * @param shortName (not null)
     * @return the pre-existing instance (or null if none)
     */
    public static InputMode findMode(String shortName) {
        if (shortName == null) {
            throw new NullPointerException("name cannot be null");
        }
        for (InputMode mode : modes) {
            if (mode.shortName.equals(shortName)) {
                return mode;
            }
        }
        return null;
    }

    /**
     * Look up the action bound to a hotkey.
     *
     * @param hotkey (not null)
     * @return the hotkey's action name, or null if the hotkey isn't bound
     */
    public String getActionName(Hotkey hotkey) {
        String keyName = hotkey.name();
        String actionName = hotkeyBindings.getProperty(keyName);

        return actionName;
    }

    /**
     * Access the enabled mode, if any.
     *
     * @return the pre-existing instance (or null if none)
     */
    public static InputMode getEnabledMode() {
        return enabledMode;
    }

    /**
     * Read the short-form name for this mode.
     */
    public String getShortName() {
        assert shortName != null;
        return shortName;
    }

    /**
     * Load a set of hotkey bindings from a file.
     */
    public void loadBindings() {
        if (customBindingsFileName == null) {
            logger.log(Level.WARNING, "bindings not loaded: file name not set");
            return;
        }
        String path = getSavePath();
        try {
            loadHotkeyFile(path);

        } catch (FileNotFoundException exception) {
            logger.log(Level.SEVERE, "Didn''t find any hotkey bindings at {0}.",
                    MyString.quote(path));

        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Input exception while loading hotkey "
                    + "bindings from the file {0}!",
                    MyString.quote(path));
        }
    }

    /**
     * Save the hotkey bindings to a file.
     */
    public void saveBindings()
            throws IOException {
        assert initialized;

        if (customBindingsFileName == null) {
            logger.log(Level.WARNING, "bindings not saved: file name not set");
            return;
        }
        String path = getSavePath();
        logger.log(Level.INFO, "Save hotkey bindings to XML file at {0}.",
                MyString.quote(path));

        try (FileOutputStream stream = new FileOutputStream(path)) {
            String comment = String.format(
                    "custom hotkey bindings for %s mode",
                    shortName);

            hotkeyBindings.storeToXML(stream, comment);
        }
    }

    /**
     * Alter the mouse cursor for this uninitialized mode.
     *
     * @param newCursor or null to hide the cursor when enabled
     */
    public void setCursor(JmeCursor newCursor) {
        assert !initialized;
        cursor = newCursor;
    }

    /**
     * Alter the file name for this mode's custom hotkey bindings.
     *
     * @param newFileName file name and extension (or null to make the bindings
     * not loadable or savable)
     */
    public void setSaveFileName(String newFileName) {
        customBindingsFileName = newFileName;
    }

    /**
     * Unbind a given hotkey.
     *
     * @param hotkey (not null)
     */
    public void unbind(Hotkey hotkey) {
        assert initialized;

        String hotkeyName = hotkey.name();
        assert hotkeyBindings.containsKey(hotkeyName) : hotkeyName;
        /*
         * Remove the binding.
         */
        hotkeyBindings.remove(hotkeyName);
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Initialize this (disabled) mode after it gets attached.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        assert !isInitialized();
        assert !isEnabled();
        super.initialize(stateManager, application);

        if (!(application instanceof GuiApplication)) {
            throw new IllegalArgumentException(
                    "application should be a GuiApplication");
        }
        this.application = (GuiApplication) application;

        boolean changed = modes.add(this);
        assert changed;
        /*
         * Load the intitial hotkey bindings.
         */
        initializeHotkeyBindings();

        assert isInitialized();
        setEnabled(startEnabled);
    }

    /**
     * Enable or disable this mode.
     *
     * @param newState true to enable, false to disable
     */
    @Override
    public void setEnabled(boolean newState) {
        if (!isInitialized()) {
            startEnabled = newState;
            return;
        }
        logger.log(Level.INFO, "mode={0} newState={1}", new Object[]{
            shortName, newState
        });

        InputManager inputManager = application.getInputManager();
        if (!isEnabled() && newState) {
            setEnabledMode(this);

            if (cursor == null) {
                inputManager.setCursorVisible(false);
            } else {
                inputManager.setMouseCursor(cursor);
                inputManager.setCursorVisible(true);
            }
            mapBoundHotkeys();

        } else if (isEnabled() && !newState) {
            assert enabledMode == this : enabledMode;
            setEnabledMode(null);

            inputManager.setCursorVisible(false);
            unmapBoundHotkeys();
        }
        super.setEnabled(newState);
    }
    // *************************************************************************
    // Object methods

    /**
     * Format the mode as a string, for debugging.
     */
    @Override
    public String toString() {
        String status;
        if (isInitialized()) {
            status = isEnabled() ? "enabled" : "disabled";
        } else {
            status = String.format("uninitialized, startEnabled=%s",
                    startEnabled);
        }
        String result = String.format("%s (%s)", shortName, status);
        return result;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Add the default hotkey bindings. These bindings will be used if no custom
     * bindings are found.
     */
    abstract protected void defaultBindings();
    // *************************************************************************
    // private methods

    /**
     * Count how many hotkeys are bound to a particular action name.
     *
     * @param actionName (not null)
     * @return count (>=0)
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
     * Generate the filesystem path to the user's customized hotkey bindings.
     *
     * @return a new string
     */
    private String getSavePath() {
        String path = Misc.getUserPath(customBindingsFileName);
        return path;
    }

    /**
     * Initialize the hotkey bindings.
     */
    private void initializeHotkeyBindings() {
        if (customBindingsFileName == null) {
            defaultBindings();
            return;
        }
        /*
         * Attempt to load custom hotkey bindings from a file.  If that fails,
         * load the default bindings for this mode.
         */
        String path = getSavePath();
        try {
            loadHotkeyFile(path);

        } catch (FileNotFoundException exception) {
            logger.log(Level.INFO, "Didn''t find any hotkey bindings at {0}.",
                    MyString.quote(path));
            hotkeyBindings.clear();
            defaultBindings();

        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Input exception while loading hotkey "
                    + "bindings from the file {0}!",
                    MyString.quote(path));
            hotkeyBindings.clear();
            defaultBindings();
        }
    }

    /**
     * Load hotkey bindings from an XML input stream.
     *
     * @param stream which input stream (not null)
     */
    private void loadBindings(InputStream stream)
            throws IOException {
        assert stream != null;

        hotkeyBindings.clear();
        hotkeyBindings.loadFromXML(stream);
        stream.close();
        for (String keyString : hotkeyBindings.stringPropertyNames()) {
            String actionName = hotkeyBindings.getProperty(keyString);
            Hotkey hotkey = Hotkey.getInstance(keyString);
            bind(actionName, hotkey);
        }
    }

    /**
     * Load hotkey bindings from an XML file.
     *
     * @param filePath file system path to the XML file (not null)
     */
    private void loadHotkeyFile(String filePath)
            throws FileNotFoundException, IOException {
        assert filePath != null;
        logger.log(Level.INFO,
                "Loading hotkey bindings from an XML file at {0}.",
                MyString.quote(filePath));

        FileInputStream stream;
        try {
            stream = new FileInputStream(filePath);
        } catch (FileNotFoundException exception) {
            throw exception;
        }
        loadBindings(stream);
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

        String[] words = actionName.split("\\s+");
        assert words.length > 0 : MyString.quote(actionName);
        String verb = words[0];
        if ("signal".equals(verb)) {
            mapSignalHotkey(actionName, hotkey);
        } else {
            mapNonsignalHotkey(actionName, hotkey);
        }
    }

    /**
     * Map a hotkey to an action string in the input manager. Overrides any
     * previous mapping for the hotkey.
     *
     * @param actionString the action string (not null)
     * @param hotkey (not null)
     */
    private void mapActionString(String actionString, Hotkey hotkey) {
        assert actionString != null;

        InputManager inputManager = application.getInputManager();
        hotkey.map(actionString, inputManager);
    }

    /**
     * Map all bound keys to their actions.
     */
    private void mapBoundHotkeys() {
        for (String keyName : hotkeyBindings.stringPropertyNames()) {
            String actionName = hotkeyBindings.getProperty(keyName);
            Hotkey hotkey = Hotkey.getInstance(keyName);
            assert hotkey != null : keyName;
            mapActionName(actionName, hotkey);
        }
    }

    /**
     * Map a hotkey to a non-signal action.
     *
     * @param actionName the name of the non-signal action (not null)
     * @param hotkey (not null)
     */
    private void mapNonsignalHotkey(String actionName, Hotkey hotkey) {
        assert actionName != null;
        assert hotkey != null;

        InputManager inputManager = application.getInputManager();
        inputManager.addListener(this, actionName);
        /*
         * For a non-signal action, the action string is simply the name.
         * Add the mapping to the input manager.
         */
        mapActionString(actionName, hotkey);
    }

    /**
     * Map a hotkey to a signal action.
     *
     * @param actionName the name of the signal action (not null)
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
        Signals signals = application.getSignals();
        signals.add(signalName);
        /*
         * Append the decimal keyCode ensure a unique action string.
         */
        String actionString = signalActionString(actionName, hotkey);
        int count = countBindings(actionString);
        boolean isUnique = (count == 0);
        assert isUnique : count;
        InputManager inputManager = application.getInputManager();
        inputManager.addListener(signals, actionString);
        /*
         * Add the mapping to the input manager.
         */
        mapActionString(actionString, hotkey);
    }

    /**
     * Alter the reference to the currently-enabled input mode. At most one mode
     * is enabled at a time.
     *
     * @param mode (or null if none)
     */
    private static void setEnabledMode(InputMode mode) {
        if (mode != null && enabledMode != null) {
            String message = String.format(
                    "tried to enable %s input mode while %s mode was active",
                    MyString.quote(mode.shortName),
                    MyString.quote(enabledMode.shortName));
            throw new IllegalStateException(message);
        }

        enabledMode = mode;
    }

    /**
     * Generate an action string for a signal action.
     *
     * @param actionName the name of the action (not null)
     * @param hotkey the triggering hotkey (not null)
     */
    private String signalActionString(String actionName, Hotkey hotkey) {
        assert actionName != null;

        int keyCode = hotkey.keyCode();
        String actionString = String.format("%s %d", actionName, keyCode);

        return actionString;
    }

    /**
     * Delete all hotkey mappings associated with this input mode.
     */
    private void unmapBoundHotkeys() {
        for (String keyName : hotkeyBindings.stringPropertyNames()) {
            String actionName = hotkeyBindings.getProperty(keyName);
            Hotkey hotkey = Hotkey.getInstance(keyName);
            assert hotkey != null : keyName;
            unmapHotkey(actionName, hotkey);
        }
    }

    /**
     * Delete a hotkey mapping from the input manager.
     *
     * @param actionName the name of the mapped action (not null)
     * @param hotkey (not null)
     */
    private void unmapHotkey(String actionName, Hotkey hotkey) {
        assert hotkey != null;
        /*
         * Reconstruct the action string.
         */
        String[] words = actionName.split("\\s+");
        assert words.length > 0 : MyString.quote(actionName);
        String verb = words[0];
        String actionString;
        if ("signal".equals(verb)) {
            actionString = signalActionString(actionName, hotkey);
        } else {
            actionString = actionName;
        }
        /*
         * Delete the mapping.
         */
        InputManager inputManager = application.getInputManager();
        if (inputManager.hasMapping(actionString)) {
            inputManager.deleteMapping(actionString);
        }
    }
}