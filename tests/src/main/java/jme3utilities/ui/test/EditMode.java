/*
 Copyright (c) 2020, Stephen Gold
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
package jme3utilities.ui.test;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.InputMode;

/**
 * A simple InputMode for entering strings of letters and spaces, used in the
 * TestTwoModes example.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class EditMode extends InputMode {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(EditMode.class.getName());
    // *************************************************************************
    // fields

    /**
     * text that's being edited
     */
    final private StringBuilder builder = new StringBuilder(80);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     */
    EditMode() {
        super("edit");
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Obtain the current text for display.
     *
     * @return a new String
     */
    String getText() {
        return builder.toString();
    }
    // *************************************************************************
    // InputMode methods

    /**
     * Add all hotkey bindings for this mode.
     */
    @Override
    protected void defaultBindings() {
        char[] tmpArray = new char[1];
        for (char c = 'A'; c <= 'Z'; ++c) {
            tmpArray[0] = c;
            String letter = new String(tmpArray);
            bind("appendLetter " + letter, letter);
        }

        bind("appendSpace", KeyInput.KEY_SPACE);
        bind("backspace", KeyInput.KEY_BACK, KeyInput.KEY_DELETE);
        bind("commit", KeyInput.KEY_RETURN, KeyInput.KEY_TAB);
        bind("signal shift", KeyInput.KEY_LSHIFT, KeyInput.KEY_RSHIFT);
        bind(SimpleApplication.INPUT_MAPPING_EXIT, KeyInput.KEY_ESCAPE);
        /*
         * Ask the application build to the help node for this mode.
         */
        ((TestTwoModes) actionApplication).buildEditHelp(this);
    }

    /**
     * Process an action from the keyboard or mouse.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        logger.log(Level.INFO, "Got action {0} ongoing={1}", new Object[]{
            MyString.quote(actionString), ongoing
        });

        if (ongoing) {
            if (actionString.startsWith("appendLetter ")) {
                String arg = MyString.remainder(actionString, "appendLetter ");
                if (!signals.test("shift")) {
                    arg = arg.toLowerCase();
                }
                builder.append(arg);
                return;

            } else if (actionString.equals("appendSpace")) {
                builder.append(" ");
                return;

            } else if (actionString.equals("backspace")) {
                int length = builder.length();
                if (length > 0) {
                    builder.setLength(length - 1);
                }
                return;

            } else if (actionString.equals("commit")) {
                ((TestTwoModes) actionApplication).switchToDefault();
                return;
            }
        }
        /*
         * Forward all other actions to the application for processing.
         */
        actionApplication.onAction(actionString, ongoing, tpf);
    }
}
