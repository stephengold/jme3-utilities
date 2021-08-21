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

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.Trigger;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
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
     * universal code of this hotkey: either a JME key code (from
     * {@link com.jme3.input.KeyInput}) or buttonFirst + a JME button code (from
     * {@link com.jme3.input.MouseInput}) TODO support joystick buttons
     */
    final private int universalCode;
    /**
     * map universal codes to hotkeys
     */
    final private static Map<Integer, Hotkey> byUniversalCode = new TreeMap<>();
    /**
     * map local names to hotkeys
     */
    final private static Map<String, Hotkey> byLocalName = new TreeMap<>();
    /**
     * map US names to hotkeys
     */
    final private static Map<String, Hotkey> byUsName = new TreeMap<>();
    /**
     * brief, descriptive name of this hotkey (not null, not empty) for use by
     * BindScreen and HelpUtils. On systems with Dvorak or non-US keyboards,
     * this might differ from its US name.
     */
    final private String localName;
    /**
     * brief, descriptive name of this hotkey on systems with United States
     * QWERTY keyboards (not null, not empty). This is the name InputMode uses.
     */
    final private String usName;
    /**
     * JME input trigger of this hotkey (not null)
     */
    final private Trigger trigger;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a Hotkey with the specified universal code, local name, US
     * name, and trigger.
     *
     * @param universalCode the desired universal code: either a key code (from
     * {@link com.jme3.input.KeyInput}) or buttonFirst + a button code (from
     * {@link com.jme3.input.MouseInput})
     * @param localName the desired local name (not null, not empty)
     * @param usName the desired US name (not null, not empty)
     * @param trigger the desired trigger (not null)
     */
    private Hotkey(int universalCode, String localName, String usName,
            Trigger trigger) {
        assert universalCode >= 0 : universalCode;
        assert universalCode <= lastButton : universalCode;
        assert localName != null;
        assert !localName.isEmpty();
        assert usName != null;
        assert !usName.isEmpty();
        assert trigger != null;

        this.universalCode = universalCode;
        this.localName = localName;
        this.usName = usName;
        this.trigger = trigger;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Determine the button code of this hotkey.
     *
     * @return a JME button code (from {@link com.jme3.input.MouseInput}) or -1
     * if none
     */
    public int buttonCode() {
        int buttonCode;
        if (universalCode < firstButton) {
            buttonCode = -1;
        } else {
            buttonCode = universalCode - firstButton;
        }

        assert buttonCode >= -1 : buttonCode;
        assert buttonCode <= lastButton - firstButton : buttonCode;
        return buttonCode;
    }

    /**
     * Determine the universal code of this hotkey.
     *
     * @return a universal code (&ge;0, &le;lastButton)
     */
    public int code() {
        assert universalCode >= 0 : universalCode;
        assert universalCode <= lastButton : universalCode;
        return universalCode;
    }

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
     * Find a hotkey by its button code.
     *
     * @param buttonCode a JME button code from
     * {@link com.jme3.input.MouseInput}
     * @return the pre-existing instance (or null if none)
     */
    public static Hotkey findButton(int buttonCode) {
        Validate.inRange(buttonCode, "button code",
                0, lastButton - firstButton);
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
     * Find a hotkey by its local name.
     *
     * @param localName a local name (not null, not empty)
     * @return the pre-existing instance (or null if none)
     */
    public static Hotkey findLocal(String localName) {
        Validate.nonEmpty(localName, "local name");
        Hotkey result = byLocalName.get(localName);
        return result;
    }

    /**
     * Find a hotkey by its US name.
     *
     * @param usName a US name (not null, not empty)
     * @return the pre-existing instance (or null if none)
     */
    public static Hotkey findUs(String usName) {
        Validate.nonEmpty(usName, "US name");
        Hotkey result = byUsName.get(usName);
        return result;
    }

    /**
     * Instantiate all known hotkeys.
     */
    public static void initialize() {
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
        addKey(KeyInput.KEY_A, "a");
        addKey(KeyInput.KEY_B, "b");
        addKey(KeyInput.KEY_C, "c");
        addKey(KeyInput.KEY_D, "d");
        addKey(KeyInput.KEY_E, "e");
        addKey(KeyInput.KEY_F, "f");
        addKey(KeyInput.KEY_G, "g");
        addKey(KeyInput.KEY_H, "h");
        addKey(KeyInput.KEY_I, "i");
        addKey(KeyInput.KEY_J, "j");
        addKey(KeyInput.KEY_K, "k");
        addKey(KeyInput.KEY_L, "l");
        addKey(KeyInput.KEY_M, "m");
        addKey(KeyInput.KEY_N, "n");
        addKey(KeyInput.KEY_O, "o");
        addKey(KeyInput.KEY_P, "p");
        addKey(KeyInput.KEY_Q, "q");
        addKey(KeyInput.KEY_R, "r");
        addKey(KeyInput.KEY_S, "s");
        addKey(KeyInput.KEY_T, "t");
        addKey(KeyInput.KEY_U, "u");
        addKey(KeyInput.KEY_V, "v");
        addKey(KeyInput.KEY_W, "w");
        addKey(KeyInput.KEY_X, "x");
        addKey(KeyInput.KEY_Y, "y");
        addKey(KeyInput.KEY_Z, "z");
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
        addKey(KeyInput.KEY_PRTSCR, "prtscr");
        /*
         * the numeric keypad
         */
        addKey(KeyInput.KEY_NUMLOCK, "num lock");
        addKey(KeyInput.KEY_DECIMAL, "numpad decimal");
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
        addKey(KeyInput.KEY_NUMPADCOMMA, "numpad comma");
        addKey(KeyInput.KEY_NUMPADEQUALS, "numpad equals");
        addKey(KeyInput.KEY_SUBTRACT, "numpad subtract");
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
     * Determine the JME key code of this hotkey.
     *
     * @return a JME key code (from {@link com.jme3.input.KeyInput}) or -1 if
     * none
     */
    public int keyCode() {
        int keyCode;
        if (universalCode < firstButton) {
            keyCode = universalCode;
        } else {
            keyCode = -1;
        }

        assert keyCode >= -1 : keyCode;
        assert keyCode <= KeyInput.KEY_LAST : keyCode;
        return keyCode;
    }

    /**
     * Enumerate all known hotkeys.
     *
     * @return a new list
     */
    public static List<Hotkey> listAll() {
        Collection<Hotkey> all = byLocalName.values();
        int numInstances = all.size();
        List<Hotkey> list = new ArrayList<>(numInstances);
        list.addAll(all);

        return list;
    }

    /**
     * Determine the local name of this hotkey, which is the name used by
     * BindScreen and HelpUtils.
     *
     * @return the local name (not null, not empty)
     */
    public String localName() {
        assert localName != null;
        assert !localName.isEmpty();
        return localName;
    }

    /**
     * Map this hotkey to an action string in an input manager. Overrides any
     * previous mappings for the hotkey.
     *
     * @param actionString action string (not null)
     * @param inputManager application's input manager (not null)
     */
    void map(String actionString, InputManager inputManager) {
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
    void unmap(String actionString, InputManager inputManager) {
        Validate.nonNull(actionString, "action");
        Validate.nonNull(inputManager, "manager");

        if (inputManager.hasMapping(actionString)) {
            inputManager.deleteTrigger(actionString, trigger);
        }
    }

    /**
     * Determine the US name of this hotkey, which is the name InputMode uses.
     *
     * @return the brief, descriptive name for this hotkey on systems with
     * United States QWERTY keyboards (not null, not empty)
     */
    public String usName() {
        assert usName != null;
        assert !usName.isEmpty();
        return usName;
    }
    // *************************************************************************
    // private methods

    /**
     * Add a new hotkey for a mouse button.
     *
     * @param buttonCode the JME button code (from
     * {@link com.jme3.input.MouseInput}) that isn't already assigned to a
     * hotkey
     * @param name a name not already assigned (not null, not empty)
     */
    private static void addButton(int buttonCode, String name) {
        assert buttonCode >= 0 : buttonCode;
        assert buttonCode <= lastButton - firstButton : buttonCode;
        assert name != null;
        assert !name.isEmpty();
        assert findButton(buttonCode) == null :
                "button" + buttonCode + " is already assigned to a hotkey";
        assert findLocal(name) == null;
        assert findUs(name) == null;

        int universalCode = buttonCode + firstButton;
        Trigger trigger = new MouseButtonTrigger(buttonCode);
        Hotkey instance = new Hotkey(universalCode, name, name, trigger);

        byUniversalCode.put(universalCode, instance);
        byLocalName.put(name, instance);
        byUsName.put(name, instance);
    }

    /**
     * Add a hotkey for a keyboard key.
     *
     * @param keyCode the JME key code from {@link com.jme3.input.KeyInput} that
     * isn't already assigned to a hotkey
     * @param usName the name of the key on United States QWERTY keyboards (not
     * null, not empty)
     */
    private static void addKey(int keyCode, String usName) {
        assert keyCode >= 0 : keyCode;
        assert keyCode <= KeyInput.KEY_LAST : keyCode;
        assert findKey(keyCode) == null :
                "key" + keyCode + " is already assigned to a hotkey";
        assert usName != null;
        assert !usName.isEmpty();
        /*
         * Attempt to localize the name.
         */
        String localName = usName;
        if (!usName.startsWith("numpad ")) { // not a numpad key
            String glfwName = glfwName(keyCode);

            if (glfwName != null) { // key is printable
                localName = englishName(glfwName);

                if (!localName.equals(usName)) {
                    String usQ = MyString.quote(usName);
                    String localQ = MyString.quote(localName);
                    if (localName.length() == 1) {
                        int ch = localName.charAt(0);
                        String unicodeName = Character.getName(ch);
                        localQ += String.format("    (\"\\u%04x\": %s)",
                                ch, unicodeName);
                    }
                    logger.log(Level.INFO,
                            "localizing the hotkey name for key{0}: {1} -> {2}",
                            new Object[]{keyCode, usQ, localQ});
                }
            }
        }
        /*
         * In case of a duplicate local name (such as "circumflex"), the hotkey with
         * the localized name is preferred.  If both hotkeys have localized
         * names, the new hotkey overrides the pre-existing one.
         */
        Hotkey preexistingHotkey = findLocal(localName);
        if (preexistingHotkey != null) {
            int preexistingCode = preexistingHotkey.keyCode();
            String nameQ = MyString.quote(usName);
            Object[] args = new Object[]{keyCode, preexistingCode, nameQ};
            if (!localName.equals(usName)) {
                logger.log(Level.INFO,
                        "Key{0} overrides pre-existing key{1} that was "
                        + "also named {2}.", args);

                byLocalName.remove(localName);
                byUniversalCode.remove(preexistingCode);
            } else {
                logger.log(Level.INFO,
                        "Ignore key{0} because pre-existing key{1} is "
                        + "also named {2}.", args);
                return;
            }
        }

        int universalCode = keyCode;
        Trigger trigger = new KeyTrigger(keyCode);
        Hotkey instance = new Hotkey(universalCode, localName, usName, trigger);

        byUniversalCode.put(universalCode, instance);
        byLocalName.put(localName, instance);
        byUsName.put(usName, instance);
    }

    /**
     * Transform the GLFW name of a printable keyboard key into a brief,
     * descriptive name in English. Only a few common names are handled. When a
     * name isn't handled, the GLFW name is returned. TODO handle additional
     * cases
     *
     * @param glfwKeyName a key name obtained from GLFW (not null, typically a
     * single Unicode character)
     * @return a brief, descriptive name for the hotkey (not null)
     */
    private static String englishName(String glfwKeyName) {
        assert glfwKeyName != null;

        switch (glfwKeyName) {
            case "\u00B4":
                return "acute";
            case "'":
                return "apostrophe";
            case "\\":
                return "backslash";
            case "`":
                return "backtick";
            case "\u005E":
                return "circumflex";
            case ",":
                return "comma";
            case "\u00A8":
                return "diaeresis";
            case "=":
                return "equals";
            case "\u00A1":
                return "exclaim";
            case "\u00BD":
                return "half";
            case "#":
                return "hash";
            case "[":
                return "left bracket";
            case "-":
                return "minus";
            case "\u00BA":
                return "ordinal";
            case ".":
                return "period";
            case "+":
                return "plus";
            case "]":
                return "right bracket";
            case "\u00A7":
                return "section";
            case ";":
                return "semicolon";
            case "/":
                return "slash";
            case "\u0384":
                return "tonos";
            default:
                return glfwKeyName;
        }
    }

    /**
     * Determine GLFW's layout-specific name for the specified keyboard key.
     *
     * @param jmeKeyCode a JMonkeyEngine key code
     * @return the name, or null if GLFW is unavailable OR the key is unknown to
     * GLFW OR the key isn't printable
     */
    private static String glfwName(int jmeKeyCode) {
        assert jmeKeyCode >= 0 : jmeKeyCode;
        assert jmeKeyCode <= KeyInput.KEY_LAST : jmeKeyCode;
        String result = null;

        try {
            /*
             * Translate the JME code to a GLFW code.
             */
            Class<?> glfwKeyMapClass
                    = Class.forName("com.jme3.input.lwjgl.GlfwKeyMap");
            Method method = glfwKeyMapClass.getDeclaredMethod("fromJmeKeyCode",
                    int.class);
            method.setAccessible(true);
            int glfwKeyCode = (int) method.invoke(null, jmeKeyCode);

            if (glfwKeyCode != -1) {
                /*
                 * The key is known to GLFW.
                 * Look up its name, assuming it's a printable key.
                 */
                Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
                method = glfwClass.getDeclaredMethod("glfwGetKeyName",
                        int.class, int.class);
                method.setAccessible(true);
                result = (String) method.invoke(null, glfwKeyCode, 0);
            }

        } catch (ClassNotFoundException exception) {
            // GLFW is unavailable, so ignore the exception and return null.

        } catch (IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException exception) {
            throw new RuntimeException(exception);
        }

        return result;
    }
}
