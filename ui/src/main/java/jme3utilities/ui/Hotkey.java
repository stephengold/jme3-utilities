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

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.Trigger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * An immutable representation of a hotkey on a desktop system's keyboard.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Hotkey {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Hotkey.class.getName());
    // *************************************************************************
    // fields

    /**
     * this hotkey's key code: set by constructor
     */
    final private int keyCode;
    /**
     * map to look up a hotkey by its key code
     */
    final private static Map<Integer, Hotkey> instances = new TreeMap<>();
    /**
     * map to look up a hotkey by its name
     */
    final private static Map<String, Hotkey> instancesByName = new TreeMap<>();
    /**
     * descriptive name for this hotkey: set by constructor
     */
    final private String name;
    /**
     * trigger for this hotkey: set by constructor
     */
    final private Trigger trigger;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a hotkey based on its key code and name.
     *
     * @param keyCode
     * @param name (not null)
     */
    private Hotkey(int keyCode, String name) {
        assert name != null;

        this.keyCode = keyCode;
        this.name = name;
        this.trigger = new KeyTrigger(keyCode);

    }
    // *************************************************************************
    // new methods exposed

    /**
     * Find a hotkey by key code.
     *
     * @param keyCode key code
     * @return pre-existing instance (or null for an invalid key code)
     */
    public static Hotkey find(int keyCode) {
        Hotkey result = instances.get(keyCode);
        return result;
    }

    /**
     * Find a hotkey by description.
     *
     * @param description description of hotkey
     * @return pre-existing instance (or null for an invalid description)
     */
    public static Hotkey find(String description) {
        Hotkey result;
        if (description == null) {
            result = null;
        } else {
            result = instancesByName.get(description);
        }

        return result;
    }

    /**
     * Read the key code of a hotkey.
     *
     * @return key code from com.jme3.input.KeyInput (&ge;minKeyCode and
     * &le;maxKeyCode)
     */
    public int getKeyCode() {
        return keyCode;
    }

    /**
     * Read the name of a hotkey.
     *
     * @return descriptive name (not null)
     */
    public String getName() {
        return name;
    }

    /**
     * Instantiate known hotkeys.
     */
    public static void intialize() {
        /*
         * mode keys
         */
        add(KeyInput.KEY_LCONTROL, "left ctrl");
        add(KeyInput.KEY_LMENU, "left alt");
        add(KeyInput.KEY_LMETA, "left meta");
        add(KeyInput.KEY_LSHIFT, "left shift");

        add(KeyInput.KEY_RCONTROL, "right ctrl");
        add(KeyInput.KEY_RMENU, "right alt");
        add(KeyInput.KEY_RMETA, "right meta");
        add(KeyInput.KEY_RSHIFT, "right shift");

        add(KeyInput.KEY_CAPITAL, "caps lock");
        /*
         * main keyboard letters
         */
        add(KeyInput.KEY_A, "A");
        add(KeyInput.KEY_B, "B");
        add(KeyInput.KEY_C, "C");
        add(KeyInput.KEY_D, "D");
        add(KeyInput.KEY_E, "E");
        add(KeyInput.KEY_F, "F");
        add(KeyInput.KEY_G, "G");
        add(KeyInput.KEY_H, "H");
        add(KeyInput.KEY_I, "I");
        add(KeyInput.KEY_J, "J");
        add(KeyInput.KEY_K, "K");
        add(KeyInput.KEY_L, "L");
        add(KeyInput.KEY_M, "M");
        add(KeyInput.KEY_N, "N");
        add(KeyInput.KEY_O, "O");
        add(KeyInput.KEY_P, "P");
        add(KeyInput.KEY_Q, "Q");
        add(KeyInput.KEY_R, "R");
        add(KeyInput.KEY_S, "S");
        add(KeyInput.KEY_T, "T");
        add(KeyInput.KEY_U, "U");
        add(KeyInput.KEY_V, "V");
        add(KeyInput.KEY_W, "W");
        add(KeyInput.KEY_X, "X");
        add(KeyInput.KEY_Y, "Y");
        add(KeyInput.KEY_Z, "Z");
        /*
         * main keyboard digits
         */
        add(KeyInput.KEY_1, "1");
        add(KeyInput.KEY_2, "2");
        add(KeyInput.KEY_3, "3");
        add(KeyInput.KEY_4, "4");
        add(KeyInput.KEY_5, "5");
        add(KeyInput.KEY_6, "6");
        add(KeyInput.KEY_7, "7");
        add(KeyInput.KEY_8, "8");
        add(KeyInput.KEY_9, "9");
        add(KeyInput.KEY_0, "0");
        /*
         * main keyboard punctuation
         */
        add(KeyInput.KEY_GRAVE, "backtick");
        add(KeyInput.KEY_MINUS, "minus");
        add(KeyInput.KEY_EQUALS, "equals");
        add(KeyInput.KEY_LBRACKET, "left bracket");
        add(KeyInput.KEY_RBRACKET, "right bracket");
        add(KeyInput.KEY_BACKSLASH, "backslash");
        add(KeyInput.KEY_SEMICOLON, "semicolon");
        add(KeyInput.KEY_APOSTROPHE, "apostrophe");
        add(KeyInput.KEY_COMMA, "comma");
        add(KeyInput.KEY_PERIOD, "period");
        add(KeyInput.KEY_SLASH, "slash");
        /*
         * ASCII control and whitespace keys
         */
        add(KeyInput.KEY_ESCAPE, "esc");
        add(KeyInput.KEY_BACK, "backspace");
        add(KeyInput.KEY_TAB, "tab");
        add(KeyInput.KEY_RETURN, "enter");
        add(KeyInput.KEY_SPACE, "space");
        /*
         * function keys
         */
        add(KeyInput.KEY_F1, "f1");
        add(KeyInput.KEY_F2, "f2");
        add(KeyInput.KEY_F3, "f3");
        add(KeyInput.KEY_F4, "f4");
        add(KeyInput.KEY_F5, "f5");
        add(KeyInput.KEY_F6, "f6");
        add(KeyInput.KEY_F7, "f7");
        add(KeyInput.KEY_F8, "f8");
        add(KeyInput.KEY_F9, "f9");
        add(KeyInput.KEY_F10, "f10");
        add(KeyInput.KEY_F11, "f11");
        add(KeyInput.KEY_F12, "f12");
        add(KeyInput.KEY_F13, "f13");
        add(KeyInput.KEY_F14, "f14");
        add(KeyInput.KEY_F15, "f15");
        /*
         * editing and arrow keys
         */
        add(KeyInput.KEY_INSERT, "insert");
        add(KeyInput.KEY_HOME, "home");
        add(KeyInput.KEY_PGUP, "page up");
        add(KeyInput.KEY_DELETE, "delete");
        add(KeyInput.KEY_END, "end");
        add(KeyInput.KEY_PGDN, "page down");
        add(KeyInput.KEY_UP, "up arrow");
        add(KeyInput.KEY_LEFT, "left arrow");
        add(KeyInput.KEY_DOWN, "down arrow");
        add(KeyInput.KEY_RIGHT, "right arrow");
        /*
         * system keys
         */
        add(KeyInput.KEY_SYSRQ, "sys rq");
        add(KeyInput.KEY_SCROLL, "scroll lock");
        add(KeyInput.KEY_PAUSE, "pause");
        /*
         * the numeric keypad
         */
        add(KeyInput.KEY_NUMLOCK, "num lock");
        add(KeyInput.KEY_DIVIDE, "numpad divide");
        add(KeyInput.KEY_MULTIPLY, "numpad multiply");
        add(KeyInput.KEY_NUMPAD7, "numpad 7");
        add(KeyInput.KEY_NUMPAD8, "numpad 8");
        add(KeyInput.KEY_NUMPAD9, "numpad 9");
        add(KeyInput.KEY_ADD, "numpad add");
        add(KeyInput.KEY_NUMPAD4, "numpad 4");
        add(KeyInput.KEY_NUMPAD5, "numpad 5");
        add(KeyInput.KEY_NUMPAD6, "numpad 6");
        add(KeyInput.KEY_NUMPAD1, "numpad 1");
        add(KeyInput.KEY_NUMPAD2, "numpad 2");
        add(KeyInput.KEY_NUMPAD3, "numpad 3");
        add(KeyInput.KEY_NUMPADENTER, "numpad enter");
        add(KeyInput.KEY_NUMPAD0, "numpad 0");
        add(KeyInput.KEY_NUMPADCOMMA, "numpad decimal");
        add(KeyInput.KEY_NUMPADEQUALS, "numpad equals");
        /*
         * miscellaneous keys
         */
        add(KeyInput.KEY_APPS, "apps");
        add(KeyInput.KEY_AT, "at sign");
        add(KeyInput.KEY_AX, "ax");
        add(KeyInput.KEY_CIRCUMFLEX, "circumflex");
        add(KeyInput.KEY_COLON, "colon");
        add(KeyInput.KEY_CONVERT, "convert");
        add(KeyInput.KEY_KANA, "kana");
        add(KeyInput.KEY_KANJI, "kanji");
        add(KeyInput.KEY_NOCONVERT, "no convert");
        add(KeyInput.KEY_POWER, "power");
        add(KeyInput.KEY_SLEEP, "sleep");
        add(KeyInput.KEY_STOP, "stop");
        add(KeyInput.KEY_UNDERLINE, "underline");
        add(KeyInput.KEY_UNLABELED, "unlabeled");
        add(KeyInput.KEY_YEN, "yen");
    }

    /**
     * Enumerate all known hotkeys.
     *
     * @return a new list
     */
    public static List<Hotkey> listAll() {
        Collection<Hotkey> all = instancesByName.values();
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
     * Add a new hotkey.
     *
     * @param keyCode unused key code (&ge;minKeyCode and &le;maxKeyCode)
     * @param name unused hotkey name (not null)
     */
    private static void add(int keyCode, String name) {
        assert name != null;
        assert instancesByName.get(name) == null;

        Hotkey instance = new Hotkey(keyCode, name);

        instances.put(keyCode, instance);
        instancesByName.put(name, instance);
    }
}
