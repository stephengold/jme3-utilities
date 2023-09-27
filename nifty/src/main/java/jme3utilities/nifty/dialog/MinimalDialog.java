/*
 Copyright (c) 2018, Stephen Gold
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
package jme3utilities.nifty.dialog;

import de.lessvoid.nifty.elements.Element;
import java.util.logging.Logger;

/**
 * Controller for a minimal dialog box.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MinimalDialog implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MinimalDialog.class.getName());
    // *************************************************************************
    // DialogController methods

    /**
     * Test whether "commit" actions are allowed.
     *
     * @param dialogElement (ignored)
     * @return true if allowed, otherwise false
     */
    @Override
    public boolean allowCommit(Element dialogElement) {
        return true;
    }

    /**
     * Construct the action-string suffix for a commit.
     *
     * @param dialogElement (ignored)
     * @return the suffix (not null)
     */
    @Override
    public String commitSuffix(Element dialogElement) {
        return "";
    }

    /**
     * Callback to update the dialog box prior to rendering. (Invoked once per
     * frame.)
     *
     * @param dialogElement (not null)
     * @param ignored time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(Element dialogElement, float ignored) {
    }
}
