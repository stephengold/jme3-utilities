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
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(HotkeyItem.class.getName());
    // *************************************************************************
    // fields

    /**
     * hotkey represented by this item (not null)
     */
    final private Hotkey hotkey;
    /**
     * input mode for binding (not null)
     */
    final private InputMode mode;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an item for the specified hotkey and input mode.
     *
     * @param hotkey which hotkey (not null, alias created)
     * @param mode input mode for binding (not null, alias created)
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
     * Test whether the hotkey is bound in the input mode being edited.
     *
     * @return true if the hotkey is bound, else false
     */
    boolean isBound() {
        boolean result = mode.binds(hotkey);
        return result;
    }

    /**
     * Test whether this item corresponds to the specified universal code.
     *
     * @param code universal code (&ge;0)
     * @return true if it corresponds, else false
     */
    boolean isForCode(int code) {
        int thisCode = hotkey.code();
        if (thisCode == code) {
            return true;
        } else {
            return false;
        }
    }
    // *************************************************************************
    // Object methods

    /**
     * Represent this item as a text string so Nifty can display it in the list
     * box.
     *
     * @return a descriptive string of text (not null)
     */
    @Override
    public String toString() {
        if (mode == null) {
            return "";
        }
        String name = hotkey.localName();
        String result = String.format(" [ %s ]    ... ", name);
        if (isBound()) {
            String action = mode.findActionName(hotkey);
            result += String.format("bound to %s", MyString.quote(action));
        } else {
            result += "not bound";
        }

        return result;
    }
}
