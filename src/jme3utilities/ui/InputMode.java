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
package jme3utilities.ui;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.controls.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Simple app state to implement a configurable input mode. At most one mode is
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
 * @author Stephen Gold sgold@sonic.net
 */
abstract public class InputMode
        extends ActionAppState
        implements ActionListener {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            InputMode.class.getName());
    // *************************************************************************
    // fields

    /**
     * list of initialized modes
     */
    final private static ArrayList<InputMode> modes = new ArrayList<>(3);
    /**
     * true if initialize() should enable this mode
     */
    private boolean startEnabled = false;
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
     * doesn't provide access to its mappings and also so that the hotkey
     * bindings editor can examine hotkey bindings while this mode is disabled
     */
    final private Properties hotkeyBindings = new Properties();
    /**
     * all known action names, bound and unbound
     */
    final private Set<String> actionNames = new TreeSet<>();
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
        Validate.nonNull(shortName, "name");
        this.shortName = shortName;
        super.setEnabled(false);
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
     * Bind the named action to a hotkey, but don't map it yet.
     * <p>
     * Each hotkey can bind to at most one action, but the same action can be
     * shared by multiple hotkeys. Signals are used to track the state of mode
     * keys (such as the shift keys).
     *
     * @param actionName name of the action (not null)
     * @param keyCode the hotkey's keycode
     */
    public void bind(String actionName, int keyCode) {
        Validate.nonNull(actionName, "name");

        Hotkey hotkey = Hotkey.getInstance(keyCode);
        assert hotkey != null : keyCode;
        bind(actionName, hotkey);
    }

    /**
     * Bind the named action to a hotkey, but don't map it yet.
     * <p>
     * Each hotkey can bind to at most one action, but the same action can be
     * shared by multiple hotkeys. Signals are used to track the state of mode
     * keys (such as the shift keys).
     *
     * @param actionName name of the action (not null)
     * @param hotkey hotkey to bind to (not null)
     */
    public void bind(String actionName, Hotkey hotkey) {
        Validate.nonNull(actionName, "action name");
        Validate.nonNull(hotkey, "hotkey");

        String hotkeyName = hotkey.name();
        /*
         * Add to the bindings.  Remove any old binding of this hotkey.
         */
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

        String actionName = getActionName(hotkey);
        return actionName != null;
    }

    /**
     * Find the names of all hotkeys bound to a named action.
     *
     * @param actionName name of action (not null)
     * @return new collection of hotkey names in lexicographic order
     */
    public Collection<String> findHotkeys(String actionName) {
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
     * Find an initialized mode by its short name.
     *
     * @param shortName (not null)
     * @return pre-existing instance (or null if none)
     */
    public static InputMode findMode(String shortName) {
        Validate.nonNull(shortName, "name");

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
     * @return hotkey's action name, or null if the hotkey isn't bound
     */
    public String getActionName(Hotkey hotkey) {
        String keyName = hotkey.name();
        String actionName = hotkeyBindings.getProperty(keyName);

        return actionName;
    }

    /**
     * Access the enabled mode, if any.
     *
     * @return pre-existing instance (or null if none)
     */
    public static InputMode getEnabledMode() {
        return enabledMode;
    }

    /**
     * Read the short-form name for this mode.
     *
     * @return name (not null)
     */
    public String getShortName() {
        assert shortName != null;
        return shortName;
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
     * Load a set of hotkey bindings from the properties file.
     */
    public void loadBindings() {
        if (customBindingsFileName == null) {
            logger.log(Level.WARNING, "bindings not loaded: file name not set");
            return;
        }

        String path = getSavePath();
        try {
            loadBindings(path);

        } catch (FileNotFoundException exception) {
            logger.log(Level.SEVERE, "Didn''t find any hotkey bindings at {0}.",
                    MyString.quote(path));

        } catch (IOException exception) {
            logger.log(Level.SEVERE,
                    "Input exception while loading hotkey bindings from {0}!",
                    MyString.quote(path));
            throw new RuntimeException(exception);
        }
    }

    /**
     * Save all hotkey bindings to the properties file.
     */
    public void saveBindings() {
        assert isInitialized();

        if (customBindingsFileName == null) {
            logger.log(Level.WARNING, "bindings not saved: file name not set");
            return;
        }

        String path = getSavePath();

        FileOutputStream stream = null;
        try {
            saveBindings(path);

        } catch (IOException exception) {
            logger.log(Level.SEVERE,
                    "Output exception while saving hotkey bindings to {0}!",
                    MyString.quote(path));
            throw new RuntimeException(exception);
            
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Alter the mouse cursor for this uninitialized mode.
     *
     * @param newCursor new cursor, or null to hide the cursor when enabled
     */
    public void setCursor(JmeCursor newCursor) {
        assert !isInitialized();
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
     * Unbind the specified hotkey.
     *
     * @param hotkey (not null)
     */
    public void unbind(Hotkey hotkey) {
        assert isInitialized();

        String hotkeyName = hotkey.name();
        if (hotkeyBindings.containsKey(hotkeyName)) {
            /*
             * Remove the binding.
             */
            hotkeyBindings.remove(hotkeyName);
        }
    }
    // *************************************************************************
    // AbstractAppState methods

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
    // ActionAppState methods

    /**
     * Initialize this (disabled) mode prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);

        boolean changed = modes.add(this);
        assert changed;
        /*
         * Load the intitial hotkey bindings.
         */
        initializeHotkeyBindings();

        assert isInitialized();
        setEnabled(startEnabled);
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
     * Read the filesystem path to the user's customized hotkey bindings.
     *
     * @return path string (may be null)
     */
    private String getSavePath() {
        String path = customBindingsFileName;
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
            loadBindings(path);

        } catch (FileNotFoundException exception) {
            logger.log(Level.INFO, "Didn''t find any hotkey bindings at {0}.",
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
     * Load hotkey bindings from an input stream.
     *
     * @param stream input stream to load from (not null)
     */
    private void loadBindings(InputStream stream)
            throws IOException {
        assert stream != null;

        hotkeyBindings.clear();
        hotkeyBindings.loadFromXML(stream);

        for (String keyString : hotkeyBindings.stringPropertyNames()) {
            String actionName = hotkeyBindings.getProperty(keyString);
            Hotkey hotkey = Hotkey.getInstance(keyString);
            bind(actionName, hotkey);
        }
    }

    /**
     * Load hotkey bindings from a properties file.
     *
     * @param filePath filesystem path (not null)
     */
    private void loadBindings(String filePath)
            throws FileNotFoundException, IOException {
        assert filePath != null;

        logger.log(Level.INFO,
                "Loading hotkey bindings from properties file at {0}.",
                MyString.quote(filePath));

        FileInputStream stream = null;
        try {
            stream = new FileInputStream(filePath);
            loadBindings(stream);
            
        } finally {
            if (stream != null) {
                stream.close();
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
     * @param actionString action string to map (not null)
     * @param hotkey (not null)
     */
    private void mapActionString(String actionString, Hotkey hotkey) {
        assert actionString != null;
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
        mapActionString(actionName, hotkey);
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
         * Append the decimal keyCode ensure a unique action string.
         */
        String actionString = signalActionString(actionName, hotkey);
        int count = countBindings(actionString);
        boolean isUnique = (count == 0);
        assert isUnique : count;
        inputManager.addListener(signals, actionString);
        /*
         * Add the mapping to the input manager.
         */
        mapActionString(actionString, hotkey);
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
     * Save hotkey bindings to a properties file.
     *
     * @param filePath filesystem path (not null)
     */
    private void saveBindings(String filePath) throws IOException {
        assert filePath != null;

        logger.log(Level.INFO,
                "Saving hotkey bindings to properties file at {0}.",
                MyString.quote(filePath));

        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(filePath);
            saveBindings(stream);
        } catch (FileNotFoundException exception) {
            throw exception;
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
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
     * @param actionName name of the mapped action (not null)
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
        if (inputManager.hasMapping(actionString)) {
            inputManager.deleteMapping(actionString);
        }
    }
}
