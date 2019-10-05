/*
 Copyright (c) 2017-2019, Stephen Gold
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
package jme3utilities.nifty;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.controls.ListBox;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * GUI display for lines of text, added one at a time.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MessageDisplay extends GuiScreenController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MessageDisplay.class.getName());
    // *************************************************************************
    // fieldss

    /**
     * FIFO queue of lines added prior to initialization
     */
    final private List<String> backlog = new ArrayList<>(4);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled controller.
     */
    public MessageDisplay() {
        super("message-display", "Interface/Nifty/huds/message-display.xml",
                false);

    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a line of text to this display.
     *
     * @param line text to display (not null)
     */
    public void addLine(String line) {
        Validate.nonNull(line, "line");

        if (!isInitialized()) {
            /*
             * Can't access the listbox yet, so add the line to the backlog.
             */
            backlog.add(line);
            return;
        }

        setEnabled(true);
        @SuppressWarnings("unchecked")
        ListBox<String> listBox
                = getScreen().findNiftyControl("messages", ListBox.class);
        listBox.enable();
        int rows = listBox.getDisplayItemCount();
        /*
         * Remove lines from the top of the listbox to
         * make room for the new line.
         */
        while (listBox.itemCount() >= rows) {
            listBox.removeItemByIndex(0);
        }

        listBox.addItem(line);
    }
    // *************************************************************************
    // BasicScreenController methods

    /**
     * Initialize this display prior to its first update.
     *
     * @param stateManager (not null)
     * @param application application which owns this screen (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);
        assert isInitialized();
        /*
         * Process any backlog.
         */
        for (String line : backlog) {
            addLine(line);
        }
        backlog.clear();
    }
}
