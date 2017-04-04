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

import java.util.Collection;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.InputMode;

/**
 * An item in the actions listbox of the hotkey bindings editor.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class ActionItem {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ActionItem.class.getName());
    // *************************************************************************
    // fields

    /**
     * which input mode (not null, set by constructor)
     */
    final private InputMode mode;
    /**
     * the action name (not null, set by constructor)
     */
    final private String actionName;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an item from an action name and an input mode.
     *
     * @param actionName (not null)
     * @param mode (not null)
     */
    ActionItem(String actionName, InputMode mode) {
        assert actionName != null;
        assert mode != null;

        this.actionName = actionName;
        this.mode = mode;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the action name.
     */
    String getActionName() {
        return actionName;
    }
    // *************************************************************************
    // Object methods

    /**
     * Convert the item to text for listbox display.
     *
     * @return display text for the item
     */
    @Override
    public String toString() {
        String result = String.format("  %s", MyString.quote(actionName));
        Collection<String> names = mode.findHotkeys(actionName);
        if (!names.isEmpty()) {
            result += String.format(":     %s", formatList(names));
        }
        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Format a collection of hotkey names for display.
     *
     * @param names (not null)
     */
    private static String formatList(Collection<String> names) {
        StringBuilder result = new StringBuilder(30);
        boolean firstFlag = true;
        for (String keyName : names) {
            if (!firstFlag) {
                result.append(" and ");
            }
            result.append(String.format("[ %s ]", keyName));
            firstFlag = false;
        }
        return result.toString();
    }
}
