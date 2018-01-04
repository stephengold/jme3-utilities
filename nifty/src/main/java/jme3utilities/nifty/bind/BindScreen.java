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
package jme3utilities.nifty.bind;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.screen.Screen;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.ui.Hotkey;
import jme3utilities.ui.InputMode;

/**
 * Screen controller for the hotkey bindings editor. Use activate() to activate
 * this screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BindScreen
        extends GuiScreenController
        implements ActionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BindScreen.class.getName());
    /**
     * short name for this screen
     */
    final public static String name = "bind";
    // *************************************************************************
    // fields

    /**
     * input mode to edit: set by {@link #activate(jme3utilities.ui.InputMode)}
     */
    private InputMode subjectMode = null;
    /**
     * input mode for this screen controller: set by constructor
     */
    final private BindInputMode inputMode;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled screen controller.
     */
    public BindScreen() {
        super(name, "Interface/Nifty/screens/bind.xml", false);

        inputMode = new BindInputMode(this);
        setListener(inputMode);
        influence(inputMode);
        inputMode.influence(this);

        assert !isEnabled();
        assert !isInitialized();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Activate this screen to edit the specified input mode.
     *
     * @param mode which input mode (not null, alias created)
     */
    public void activate(InputMode mode) {
        Validate.nonNull(mode, "mode");

        assert !isEnabled();
        assert isInitialized();
        assert subjectMode == null : subjectMode;
        assert mode.isEnabled();

        closeAllPopups();
        subjectMode = mode;
        subjectMode.setEnabled(false);
        setEnabled(true);
    }

    /**
     * Deactivate this screen and enable the input mode that was just edited.
     */
    void deactivate() {
        assert isEnabled();
        assert subjectMode != null;
        assert !subjectMode.isEnabled();

        setEnabled(false);
        subjectMode.setEnabled(true);
        subjectMode = null;
    }

    /**
     * Test whether a hotkey is exempt from re-binding.
     *
     * @param hotkey (not null)
     * @return false if subject to editing, true if exempt
     */
    static boolean isExempt(Hotkey hotkey) {
        assert hotkey != null;

        int keyCode = hotkey.getKeyCode();
        /*
         * Windows uses these hotkeys:
         *  KEY_LMETA to open the Windows menu
         *  KEY_NUMLOCK to toggle keypad key definitions
         * In addition, the Nifty GUI uses these hotkeys:
         *  KEY_DOWN, KEY_TAB, and KEY_UP for navigation
         *  KEY_RETURN for selection
         *  KEY_SPACE to reset the GUI
         * And com.jme3.app.state.ScreenshotAppState uses:
         *  KEY_SYSRQ to save a screenshot
         * Avoid re-binding those hotkeys.
         */
        switch (keyCode) {
            case KeyInput.KEY_LMETA:
            case KeyInput.KEY_NUMLOCK:
            case KeyInput.KEY_DOWN:
            case KeyInput.KEY_TAB:
            case KeyInput.KEY_UP:
            case KeyInput.KEY_RETURN:
            case KeyInput.KEY_SPACE:
            case KeyInput.KEY_SYSRQ:
                return true;
        }
        return false;
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action from the GUI or keyboard.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param ignored time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float ignored) {
        /*
         * Ignore actions that are not ongoing.
         */
        if (!ongoing) {
            return;
        }
        logger.log(Level.INFO, "Got action {0}", MyString.quote(actionString));

        if (actionString.equals("close")) {
            deactivate();
            return;
        }
        /*
         * Split the action string into words.
         */
        String[] words = actionString.split("\\s+");
        assert words.length > 0 : words;
        String verb = words[0];
        switch (verb) {
            case "bind":
                if (words.length == 1) {
                    bindSelected();
                    return;
                }
                break;

            case "load":
                if (words.length == 1) {
                    subjectMode.loadBindings();
                    return;
                }
                break;

            case "return":
                if (words.length == 1) {
                    deactivate();
                    return;
                }
                break;

            case "save":
                if (words.length == 1) {
                    subjectMode.saveBindings();
                    return;
                }
                break;

            case "select":
                if (words.length == 2) {
                    int keyCode = Integer.parseInt(words[1]);
                    selectHotkey(keyCode);
                    return;
                }
                break;

            case "unbind":
                if (words.length == 1) {
                    unbindSelectedHotkey();
                    return;
                }
                break;
        }

        logger.log(Level.WARNING, "Action {0} was not handled.",
                MyString.quote(actionString));
    }
    // *************************************************************************
    // GuiScreenController methods

    /**
     * Callback that Nifty invokes when the screen is enabled.
     *
     * @param nifty (not null)
     * @param screen (not null)
     */
    @Override
    public void bind(Nifty nifty, Screen screen) {
        assert nifty != null;
        assert screen != null;
        super.bind(nifty, screen);
        /*
         * Populate the action list box.
         */
        ListBox<ActionItem> actionBox = getActionBox();
        actionBox.clear();
        List<String> actionNames = subjectMode.listActionNames();
        Collections.sort(actionNames);
        for (String actionName : actionNames) {
            ActionItem item = new ActionItem(actionName, subjectMode);
            actionBox.addItem(item);
        }
        /*
         * Populate the hotkey list box, putting bound keys before unbound ones.
         */
        ListBox<HotkeyItem> hotkeyBox = getHotkeyBox();
        hotkeyBox.clear();
        populateHotkeyBox(true);
        populateHotkeyBox(false);
    }

    /**
     * Initialize this screen.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        assert !isInitialized();
        assert !isEnabled();

        inputMode.initialize(stateManager, application);
        super.initialize(stateManager, application);
    }

    /**
     * Callback to update this screen prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param simInterval time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float simInterval) {
        assert isEnabled();
        super.update(simInterval);

        Screen screen = getScreen();
        if (!screen.isBound()) {
            /*
             * Avoid Nifty exceptions and warnings regarding unbound controls.
             */
            return;
        }

        getActionBox().refresh();
        getHotkeyBox().refresh();
        updateButtonLabels();

        String modeStatus = String.format(
                "Edit hotkey bindings for %s input mode",
                MyString.quote(subjectMode.getShortName()));
        setStatusText("modeStatus", modeStatus);

        String configPath = subjectMode.getConfigPath();
        if (configPath == null) {
            setStatusText("configStatus", "");
        } else {
            String configStatus = String.format("Path for load/save is %s",
                    MyString.quote(configPath));
            setStatusText("configStatus", configStatus);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Bind the selected action to the selected hotkey.
     */
    private void bindSelected() {
        Hotkey hotkey = getSelectedHotkey();
        assert hotkey != null;
        String actionName = getSelectedAction();
        assert actionName != null;
        subjectMode.bind(actionName, hotkey);
    }

    /**
     * Find the list-box item for the specified action.
     *
     * @param actionName the name of the action to find (not null)
     * @return the pre-existing item, or null if none found
     */
    private ActionItem findActionItem(String actionName) {
        assert actionName != null;

        ListBox<ActionItem> actionBox = getActionBox();
        List<ActionItem> list = actionBox.getItems();
        for (ActionItem item : list) {
            String name = item.getActionName();
            if (actionName.equals(name)) {
                return item;
            }
        }

        return null;
    }

    /**
     * Find the hotkey list-box item for the specified universal code.
     *
     * @param code universal code (&ge;0)
     * @return the pre-existing item, or null if none found
     */
    private HotkeyItem findHotkeyItem(int code) {
        ListBox<HotkeyItem> listBox = getHotkeyBox();
        List<HotkeyItem> list = listBox.getItems();
        for (HotkeyItem item : list) {
            if (item.isForCode(code)) {
                return item;
            }
        }

        return null;
    }

    /**
     * Access the list box for selecting actions.
     *
     * @return the pre-existing instance (not null)
     */
    private ListBox<ActionItem> getActionBox() {
        Screen screen = getScreen();
        @SuppressWarnings("unchecked")
        ListBox<ActionItem> listBox
                = screen.findNiftyControl("actionList", ListBox.class);

        assert listBox != null;
        return listBox;
    }

    /**
     * Access the list box for selecting hotkeys.
     *
     * @return the pre-existing instance (not null)
     */
    private ListBox<HotkeyItem> getHotkeyBox() {
        Screen screen = getScreen();
        @SuppressWarnings("unchecked")
        ListBox<HotkeyItem> listBox
                = screen.findNiftyControl("hotkeyList", ListBox.class);

        assert listBox != null;
        return listBox;
    }

    /**
     * Read the selected action.
     *
     * @return the name of the action, or null if none selected
     */
    private String getSelectedAction() {
        ListBox<ActionItem> listBox = getActionBox();
        List<ActionItem> selection = listBox.getSelection();
        if (selection.isEmpty()) {
            return null;
        }
        ActionItem item = selection.get(0);
        String actionName = item.getActionName();

        return actionName;
    }

    /**
     * Access the selected hotkey.
     *
     * @return the selected hotkey, or null if none selected
     */
    private Hotkey getSelectedHotkey() {
        ListBox<HotkeyItem> listBox = getHotkeyBox();
        List<HotkeyItem> selection = listBox.getSelection();
        if (selection.isEmpty()) {
            return null;
        }
        HotkeyItem item = selection.get(0);
        Hotkey hotkey = item.getHotkey();

        return hotkey;
    }

    /**
     * Test whether an action is selected.
     *
     * @return true if an action is selected, false if none is selected
     */
    private boolean isActionSelected() {
        ListBox<ActionItem> listBox = getActionBox();
        List<ActionItem> selection = listBox.getSelection();

        boolean result = !selection.isEmpty();
        return result;
    }

    /**
     * Test whether a bound hotkey is selected.
     *
     * @return true if any bound hotkey is selected, false if no hotkeys are
     * selected or none of the selected hotkeys are bound
     */
    private boolean isBoundHotkeySelected() {
        ListBox<HotkeyItem> listBox = getHotkeyBox();
        List<HotkeyItem> selection = listBox.getSelection();
        for (HotkeyItem item : selection) {
            if (item.isBound()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Test whether a hotkey is selected.
     *
     * @return true if a hotkey is selected, false if none are selected
     */
    private boolean isHotkeySelected() {
        ListBox<HotkeyItem> listBox = getHotkeyBox();
        List<HotkeyItem> selection = listBox.getSelection();
        boolean result = !selection.isEmpty();

        return result;
    }

    /**
     * Populate the hotkey list box.
     *
     * @param boundFlag if true, add bound hotkeys only; otherwise add unbound
     * hotkeys only
     */
    private void populateHotkeyBox(boolean boundFlag) {
        ListBox<HotkeyItem> listBox = getHotkeyBox();
        Collection<Hotkey> allHotkeys = Hotkey.listAll();
        for (Hotkey hotkey : allHotkeys) {
            if (isExempt(hotkey)) {
                continue;
            }
            if (subjectMode.binds(hotkey) == boundFlag) {
                HotkeyItem item = new HotkeyItem(hotkey, subjectMode);
                listBox.addItem(item);
            }
        }
    }

    /**
     * Select the hotkey with the specified code. If the hotkey is bound, also
     * select the action it's bound to.
     *
     * @param code universal code of the hotkey to select (&ge;0)
     */
    private void selectHotkey(int code) {
        HotkeyItem item = findHotkeyItem(code);
        assert item != null;

        ListBox<HotkeyItem> listBox = getHotkeyBox();
        listBox.setFocusItem(item);
        listBox.selectItem(item);

        Hotkey hotkey = item.getHotkey();
        if (subjectMode.binds(hotkey)) {
            String actionName = subjectMode.getActionName(hotkey);
            ActionItem actionItem = findActionItem(actionName);
            ListBox<ActionItem> actionBox = getActionBox();
            actionBox.selectItem(actionItem);
        }
    }

    /**
     * Unbind the selected hotkey.
     */
    private void unbindSelectedHotkey() {
        Hotkey hotkey = getSelectedHotkey();
        assert hotkey != null;
        subjectMode.unbind(hotkey);
    }

    /**
     * Update the labels of the Nifty buttons.
     */
    private void updateButtonLabels() {
        String bindLabel = "";
        if (isHotkeySelected()
                && isActionSelected()
                && !isBoundHotkeySelected()) {
            bindLabel = "Bind";
        }
        setButtonText("bind", bindLabel);

        String returnLabel = String.format("Return to %s mode",
                MyString.quote(subjectMode.getShortName()));
        setButtonText("return", returnLabel);

        String unbindLabel = "";
        if (isBoundHotkeySelected()) {
            unbindLabel = "Unbind selected hotkey";
        }
        setButtonText("unbind", unbindLabel);

        String loadLabel = "";
        String saveLabel = "";
        if (subjectMode.getConfigPath() != null) {
            loadLabel = "Load from file";
            saveLabel = "Save to file";
        }
        setButtonText("loadConfig", loadLabel);
        setButtonText("saveConfig", saveLabel);
    }
}
