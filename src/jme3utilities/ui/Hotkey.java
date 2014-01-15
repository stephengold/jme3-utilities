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

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * A hotkey on the system's keyboard.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Hotkey {
    // *************************************************************************
    // constants

    /**
     * largest valid key code (per com.jme3.input.KeyInput, >= minKeyCode)
     */
    final private static int maxKeyCode = KeyInput.KEY_SLEEP;
    /**
     * smallest valid key code (per com.jme3.input.KeyInput, >= 0)
     */
    final private static int minKeyCode = KeyInput.KEY_ESCAPE;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Hotkey.class.getName());
    // *************************************************************************
    // fields
    /**
     * the hotkey's key code (per com.jme3.input.KeyInput, must be between
     * minKeyCode and maxKeyCode): set by constructor
     */
    final private int keyCode;
    /**
     * array to look up the hotkey by its key code
     */
    private static Hotkey[] instances = new Hotkey[maxKeyCode + 1];
    /**
     * map to look up the hotkey by its name
     */
    final private static Map<String, Hotkey> instancesByName = new TreeMap<>();
    /**
     * the hotkey's name: set by constructor
     */
    final private String name;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a hotkey based on its key code and name.
     *
     * @param keyCode (in the range minKeyCode to maxKeyCode)
     * @param name (not null)
     */
    private Hotkey(int keyCode, String name) {
        assert keyCode >= minKeyCode;
        assert keyCode <= maxKeyCode;
        assert name != null;

        this.keyCode = keyCode;
        this.name = name;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Collect all the valid hotkeys.
     *
     * @return a new collection
     */
    public static Collection<Hotkey> getAll() {
        Collection<Hotkey> collection = instancesByName.values();
        return collection;
    }

    /**
     * Get the hotkey for a given key code.
     *
     * @param keyCode which hotkey
     * @return the pre-existing instance (or null for an invalid key code)
     */
    public static Hotkey getInstance(int keyCode) {
        if (keyCode < minKeyCode || keyCode > maxKeyCode) {
            return null;
        }
        Hotkey result = instances[keyCode];
        return result;
    }

    /**
     * Find the hotkey with a given name.
     *
     * @param name which hotkey
     * @return the pre-existing instance (or null for an invalid name)
     */
    public static Hotkey getInstance(String name) {
        if (name == null) {
            return null;
        }
        Hotkey result = instancesByName.get(name);
        return result;
    }

    /**
     * Instantiate all hotkeys.
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
     * Read the key code of a hotkey.
     */
    public int keyCode() {
        return keyCode;
    }

    /**
     * Map this hotkey to an action string in an input manager. Overrides any
     * previous mappings for the hotkey.
     *
     * @param actionString the action string (not null)
     * @param inputManager the input manager (not null)
     */
    public void map(String actionString, InputManager inputManager) {
        assert actionString != null;

        KeyTrigger trigger = new KeyTrigger(keyCode);
        inputManager.addMapping(actionString, trigger);
    }

    /**
     * Read the name of a hotkey.
     */
    public String name() {
        return name;
    }
    // *************************************************************************
    // private methods

    /**
     * Add a new hotkey.
     *
     * @param keyCode an unused key code (between minKeyCode and maxKeyCode)
     * @param name an unused hotkey name (not null)
     */
    private static void add(int keyCode, String name) {
        assert keyCode >= minKeyCode;
        assert keyCode <= maxKeyCode;
        assert instances[keyCode] == null : keyCode;
        assert name != null;
        assert instancesByName.get(name) == null;

        Hotkey instance = new Hotkey(keyCode, name);

        instances[keyCode] = instance;
        instancesByName.put(name, instance);
    }
}