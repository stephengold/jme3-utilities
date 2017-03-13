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
package jme3utilities.nifty.bind;

import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.Hotkey;
import jme3utilities.ui.InputMode;

/**
 * An item in the hotkeys listbox of the hotkey bindings editor.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class HotkeyItem {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            HotkeyItem.class.getName());
    // *************************************************************************
    // fields
    
    /**
     * which hotkey (not null)
     */
    final private Hotkey hotkey;
    /**
     * which input mode (not null)
     */
    final private InputMode mode;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an item based on its hotkey and an input mode.
     *
     * @param hotkey (not null, set by constructor)
     * @param mode (not null, set by constructor)
     */
    HotkeyItem(Hotkey hotkey, InputMode mode) {
        assert hotkey != null;
        assert mode != null;

        this.hotkey = hotkey;
        this.mode = mode;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the hotkey.
     */
    Hotkey getHotkey() {
        return hotkey;
    }

    /**
     * Test whether the hotkey is bound.
     *
     * @return true if the hotkey is bound, else false
     */
    boolean isBound() {
        boolean result = mode.binds(hotkey);
        return result;
    }

    /**
     * Test whether this item corresponds to the specified key code.
     *
     * @param keyCode
     * @return true if it corresponds, else false
     */
    boolean isForKeyCode(int keyCode) {
        int thisCode = hotkey.keyCode();
        boolean result = thisCode == keyCode;
        return result;
    }
    // *************************************************************************
    // Object methods

    /**
     * Convert the item to a string so the listbox can display it.
     *
     * @return display string for the item (not null)
     */
    @Override
    public String toString() {
        if (mode == null) {
            return "";
        }
        String name = hotkey.name();
        String result = String.format(" [ %s ]    ... ", name);
        if (isBound()) {
            String action = mode.getActionName(hotkey);
            result += String.format("bound to %s", MyString.quote(action));
        } else {
            result += "not bound";
        }
        return result;
    }
}