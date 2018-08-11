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
package jme3utilities.ui;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.Trigger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A named, immutable, simple trigger for actions.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Hotkey {
    // *************************************************************************
    // constants and loggers

    /**
     * universal code for the first mouse button
     */
    final private static int firstButton = KeyInput.KEY_LAST + 1;
    /**
     * universal code for the last mouse button
     */
    final private static int lastButton = KeyInput.KEY_LAST + 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Hotkey.class.getName());
    // *************************************************************************
    // fields

    /**
     * a universal code for this hotkey: either a key code (from
     * {@link com.jme3.input.KeyInput}) or buttonFirst + a button code (from
     * {@link com.jme3.input.MouseInput})
     */
    final private int code;
    /**
     * map to look up a hotkey by its description
     */
    final private static Map<String, Hotkey> byName = new TreeMap<>();
    /**
     * map to look up a hotkey by its universal code
     */
    final private static Map<Integer, Hotkey> byUniversalCode = new TreeMap<>();
    /**
     * descriptive name for this hotkey (not null, not empty)
     */
    final private String name;
    /**
     * trigger for this hotkey (not null)
     */
    final private Trigger trigger;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a hotkey with the specified universal code, name, and
     * trigger.
     *
     * @param code a universal code: either a key code (from
     * {@link com.jme3.input.KeyInput}) or buttonFirst + a button code (from
     * {@link com.jme3.input.MouseInput})
     * @param name descriptive name (not null, not empty)
     * @param trigger (not null)
     */
    private Hotkey(int code, String name, Trigger trigger) {
        assert code >= 0 : code;
        assert code <= lastButton : code;
        assert name != null;
        assert !name.isEmpty();
        assert trigger != null;

        this.code = code;
        this.name = name;
        this.trigger = trigger;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Find a hotkey by its universal code.
     *
     * @param code a universal code: either a key code (from
     * {@link com.jme3.input.KeyInput}) or buttonFirst + a button code (from
     * {@link com.jme3.input.MouseInput})
     * @return the pre-existing instance (or null if none)
     */
    public static Hotkey find(int code) {
        Validate.inRange(code, "code", 0, lastButton);
        Hotkey result = byUniversalCode.get(code);
        return result;
    }

    /**
     * Find a hotkey by its name.
     *
     * @param name the descriptive name (not null, not empty)
     * @return the pre-existing instance (or null if none)
     */
    public static Hotkey find(String name) {
        Validate.nonEmpty(name, "name");
        Hotkey result = byName.get(name);
        return result;
    }

    /**
     * Find a hotkey by its button code.
     *
     * @param buttonCode a button code from {@link com.jme3.input.MouseInput}
     * @return the pre-existing instance (or null if none)
     */
    public static Hotkey findButton(int buttonCode) {
        Validate.inRange(buttonCode, "button code", 0, lastButton - firstButton);
        Hotkey result = find(firstButton + buttonCode);
        return result;
    }

    /**
     * Find a hotkey by its key code.
     *
     * @param keyCode a key code from {@link com.jme3.input.KeyInput}
     * @return the pre-existing instance (or null if none)
     */
    public static Hotkey findKey(int keyCode) {
        Validate.inRange(keyCode, "key code", 0, KeyInput.KEY_LAST);
        Hotkey result = find(keyCode);
        return result;
    }

    /**
     * Read the universal code of this hotkey.
     *
     * @return a universal code
     */
    public int getCode() {
        assert code >= 0 : code;
        assert code <= lastButton : code;
        return code;
    }

    /**
     * Read the button code of this hotkey.
     *
     * @return a button code (from {@link com.jme3.input.MouseInput}) or -1 if
     * none
     */
    public int getButtonCode() {
        int buttonCode;
        if (code < firstButton) {
            buttonCode = -1;
        } else {
            buttonCode = code - firstButton;
        }

        assert buttonCode >= -1 : buttonCode;
        assert buttonCode <= lastButton - firstButton : buttonCode;
        return buttonCode;
    }

    /**
     * Read the key code of this hotkey.
     *
     * @return a key code (from {@link com.jme3.input.KeyInput}) or -1 if none
     */
    public int getKeyCode() {
        int keyCode;
        if (code < firstButton) {
            keyCode = code;
        } else {
            keyCode = -1;
        }

        assert keyCode >= -1 : keyCode;
        assert keyCode <= KeyInput.KEY_LAST : keyCode;
        return keyCode;
    }

    /**
     * Read the name of this hotkey.
     *
     * @return descriptive name (not null, not empty)
     */
    public String getName() {
        assert name != null;
        assert !name.isEmpty();
        return name;
    }

    /**
     * Instantiate known hotkeys.
     */
    public static void intialize() {
        /*
         * mouse buttons
         */
        addButton(MouseInput.BUTTON_LEFT, "LMB");
        addButton(MouseInput.BUTTON_MIDDLE, "MMB");
        addButton(MouseInput.BUTTON_RIGHT, "RMB");
        /*
         * mode keys
         */
        addKey(KeyInput.KEY_LCONTROL, "left ctrl");
        addKey(KeyInput.KEY_LMENU, "left alt");
        addKey(KeyInput.KEY_LMETA, "left meta");
        addKey(KeyInput.KEY_LSHIFT, "left shift");

        addKey(KeyInput.KEY_RCONTROL, "right ctrl");
        addKey(KeyInput.KEY_RMENU, "right alt");
        addKey(KeyInput.KEY_RMETA, "right meta");
        addKey(KeyInput.KEY_RSHIFT, "right shift");

        addKey(KeyInput.KEY_CAPITAL, "caps lock");
        /*
         * main keyboard letters
         */
        addKey(KeyInput.KEY_A, "A");
        addKey(KeyInput.KEY_B, "B");
        addKey(KeyInput.KEY_C, "C");
        addKey(KeyInput.KEY_D, "D");
        addKey(KeyInput.KEY_E, "E");
        addKey(KeyInput.KEY_F, "F");
        addKey(KeyInput.KEY_G, "G");
        addKey(KeyInput.KEY_H, "H");
        addKey(KeyInput.KEY_I, "I");
        addKey(KeyInput.KEY_J, "J");
        addKey(KeyInput.KEY_K, "K");
        addKey(KeyInput.KEY_L, "L");
        addKey(KeyInput.KEY_M, "M");
        addKey(KeyInput.KEY_N, "N");
        addKey(KeyInput.KEY_O, "O");
        addKey(KeyInput.KEY_P, "P");
        addKey(KeyInput.KEY_Q, "Q");
        addKey(KeyInput.KEY_R, "R");
        addKey(KeyInput.KEY_S, "S");
        addKey(KeyInput.KEY_T, "T");
        addKey(KeyInput.KEY_U, "U");
        addKey(KeyInput.KEY_V, "V");
        addKey(KeyInput.KEY_W, "W");
        addKey(KeyInput.KEY_X, "X");
        addKey(KeyInput.KEY_Y, "Y");
        addKey(KeyInput.KEY_Z, "Z");
        /*
         * main keyboard digits
         */
        addKey(KeyInput.KEY_1, "1");
        addKey(KeyInput.KEY_2, "2");
        addKey(KeyInput.KEY_3, "3");
        addKey(KeyInput.KEY_4, "4");
        addKey(KeyInput.KEY_5, "5");
        addKey(KeyInput.KEY_6, "6");
        addKey(KeyInput.KEY_7, "7");
        addKey(KeyInput.KEY_8, "8");
        addKey(KeyInput.KEY_9, "9");
        addKey(KeyInput.KEY_0, "0");
        /*
         * main keyboard punctuation
         */
        addKey(KeyInput.KEY_GRAVE, "backtick");
        addKey(KeyInput.KEY_MINUS, "minus");
        addKey(KeyInput.KEY_EQUALS, "equals");
        addKey(KeyInput.KEY_LBRACKET, "left bracket");
        addKey(KeyInput.KEY_RBRACKET, "right bracket");
        addKey(KeyInput.KEY_BACKSLASH, "backslash");
        addKey(KeyInput.KEY_SEMICOLON, "semicolon");
        addKey(KeyInput.KEY_APOSTROPHE, "apostrophe");
        addKey(KeyInput.KEY_COMMA, "comma");
        addKey(KeyInput.KEY_PERIOD, "period");
        addKey(KeyInput.KEY_SLASH, "slash");
        /*
         * ASCII control and whitespace keys
         */
        addKey(KeyInput.KEY_ESCAPE, "esc");
        addKey(KeyInput.KEY_BACK, "backspace");
        addKey(KeyInput.KEY_TAB, "tab");
        addKey(KeyInput.KEY_RETURN, "enter");
        addKey(KeyInput.KEY_SPACE, "space");
        /*
         * function keys
         */
        addKey(KeyInput.KEY_F1, "f1");
        addKey(KeyInput.KEY_F2, "f2");
        addKey(KeyInput.KEY_F3, "f3");
        addKey(KeyInput.KEY_F4, "f4");
        addKey(KeyInput.KEY_F5, "f5");
        addKey(KeyInput.KEY_F6, "f6");
        addKey(KeyInput.KEY_F7, "f7");
        addKey(KeyInput.KEY_F8, "f8");
        addKey(KeyInput.KEY_F9, "f9");
        addKey(KeyInput.KEY_F10, "f10");
        addKey(KeyInput.KEY_F11, "f11");
        addKey(KeyInput.KEY_F12, "f12");
        addKey(KeyInput.KEY_F13, "f13");
        addKey(KeyInput.KEY_F14, "f14");
        addKey(KeyInput.KEY_F15, "f15");
        /*
         * editing and arrow keys
         */
        addKey(KeyInput.KEY_INSERT, "insert");
        addKey(KeyInput.KEY_HOME, "home");
        addKey(KeyInput.KEY_PGUP, "page up");
        addKey(KeyInput.KEY_DELETE, "delete");
        addKey(KeyInput.KEY_END, "end");
        addKey(KeyInput.KEY_PGDN, "page down");
        addKey(KeyInput.KEY_UP, "up arrow");
        addKey(KeyInput.KEY_LEFT, "left arrow");
        addKey(KeyInput.KEY_DOWN, "down arrow");
        addKey(KeyInput.KEY_RIGHT, "right arrow");
        /*
         * system keys
         */
        addKey(KeyInput.KEY_SYSRQ, "sys rq");
        addKey(KeyInput.KEY_SCROLL, "scroll lock");
        addKey(KeyInput.KEY_PAUSE, "pause");
        /*
         * the numeric keypad
         */
        addKey(KeyInput.KEY_NUMLOCK, "num lock");
        addKey(KeyInput.KEY_DIVIDE, "numpad divide");
        addKey(KeyInput.KEY_MULTIPLY, "numpad multiply");
        addKey(KeyInput.KEY_NUMPAD7, "numpad 7");
        addKey(KeyInput.KEY_NUMPAD8, "numpad 8");
        addKey(KeyInput.KEY_NUMPAD9, "numpad 9");
        addKey(KeyInput.KEY_ADD, "numpad add");
        addKey(KeyInput.KEY_NUMPAD4, "numpad 4");
        addKey(KeyInput.KEY_NUMPAD5, "numpad 5");
        addKey(KeyInput.KEY_NUMPAD6, "numpad 6");
        addKey(KeyInput.KEY_NUMPAD1, "numpad 1");
        addKey(KeyInput.KEY_NUMPAD2, "numpad 2");
        addKey(KeyInput.KEY_NUMPAD3, "numpad 3");
        addKey(KeyInput.KEY_NUMPADENTER, "numpad enter");
        addKey(KeyInput.KEY_NUMPAD0, "numpad 0");
        addKey(KeyInput.KEY_NUMPADCOMMA, "numpad decimal");
        addKey(KeyInput.KEY_NUMPADEQUALS, "numpad equals");
        /*
         * miscellaneous keys
         */
        addKey(KeyInput.KEY_APPS, "apps");
        addKey(KeyInput.KEY_AT, "at sign");
        addKey(KeyInput.KEY_AX, "ax");
        addKey(KeyInput.KEY_CIRCUMFLEX, "circumflex");
        addKey(KeyInput.KEY_COLON, "colon");
        addKey(KeyInput.KEY_CONVERT, "convert");
        addKey(KeyInput.KEY_KANA, "kana");
        addKey(KeyInput.KEY_KANJI, "kanji");
        addKey(KeyInput.KEY_NOCONVERT, "no convert");
        addKey(KeyInput.KEY_POWER, "power");
        addKey(KeyInput.KEY_SLEEP, "sleep");
        addKey(KeyInput.KEY_STOP, "stop");
        addKey(KeyInput.KEY_UNDERLINE, "underline");
        addKey(KeyInput.KEY_UNLABELED, "unlabeled");
        addKey(KeyInput.KEY_YEN, "yen");
    }

    /**
     * Enumerate all known hotkeys.
     *
     * @return a new list
     */
    public static List<Hotkey> listAll() {
        Collection<Hotkey> all = byName.values();
        int numInstances = all.size();
        List<Hotkey> list = new ArrayList<>(numInstances);
        list.addAll(all);

        return list;
    }

    /**
     * Map this hotkey to an action string in an input manager. Overrides any
     * previous mappings for the hotkey.
     *
     * @param actionString action string (not null)
     * @param inputManager application's input manager (not null)
     */
    public void map(String actionString, InputManager inputManager) {
        Validate.nonNull(actionString, "action");
        Validate.nonNull(inputManager, "manager");

        inputManager.addMapping(actionString, trigger);
    }

    /**
     * Unmap this hotkey in the specified input manager.
     *
     * @param actionString action string (not null)
     * @param inputManager which input manager (not null)
     */
    public void unmap(String actionString, InputManager inputManager) {
        Validate.nonNull(actionString, "action");
        Validate.nonNull(inputManager, "manager");

        if (inputManager.hasMapping(actionString)) {
            inputManager.deleteTrigger(actionString, trigger);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add a new hotkey for a mouse button.
     *
     * @param buttonCode an button code not already in use (from
     * {@link com.jme3.input.MouseInput})
     * @param name a descriptive name not already in use (not null, not empty)
     */
    private static void addButton(int buttonCode, String name) {
        assert buttonCode >= 0 : buttonCode;
        assert buttonCode <= lastButton - firstButton : buttonCode;
        assert name != null;
        assert !name.isEmpty();
        assert findButton(buttonCode) == null;
        assert find(name) == null;

        int universalCode = buttonCode + firstButton;
        Trigger trigger = new MouseButtonTrigger(buttonCode);
        Hotkey instance = new Hotkey(universalCode, name, trigger);

        byUniversalCode.put(universalCode, instance);
        byName.put(name, instance);
    }

    /**
     * Add a new hotkey for a keyboard key.
     *
     * @param keyCode an unused key code from {@link com.jme3.input.KeyInput}
     * @param name a descriptive name not already in use (not null, not empty)
     */
    private static void addKey(int keyCode, String name) {
        assert keyCode >= 0 : keyCode;
        assert keyCode <= KeyInput.KEY_LAST : keyCode;
        assert name != null;
        assert !name.isEmpty();
        assert findKey(keyCode) == null;
        assert find(name) == null;

        int universalCode = keyCode;
        Trigger trigger = new KeyTrigger(keyCode);
        Hotkey instance = new Hotkey(universalCode, name, trigger);

        byUniversalCode.put(universalCode, instance);
        byName.put(name, instance);
    }
}
