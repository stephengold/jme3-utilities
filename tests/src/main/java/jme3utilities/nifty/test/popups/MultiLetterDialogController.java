/*
 Copyright (c) 2021, Stephen Gold
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
package jme3utilities.nifty.test.popups;

import java.util.List;
import java.util.logging.Logger;
import jme3utilities.nifty.dialog.MultiSelectDialog;

/**
 * Controller for a multi-select dialog box used in TestPopups to select 2 or
 * more letters.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class MultiLetterDialogController extends MultiSelectDialog<LetterItem> {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MultiLetterDialogController.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller with the specified list of items.
     *
     * @param commitDescription the commit-button text (not null, not empty)
     * @param itemList the items to select from, in the displayed order (not
     * null, alias created)
     */
    MultiLetterDialogController(String commitDescription,
            List<LetterItem> itemList) {
        super(commitDescription, itemList);
    }
    // *************************************************************************
    // MultiSelectDialog methods

    /**
     * Determine the feedback message for the specified list of indices.
     *
     * @param indexList the indices of all selected items (in arbitrary order,
     * not null, unaffected)
     * @return the message (not null)
     */
    @Override
    protected String feedback(List<Integer> indexList) {
        String result;
        if (indexList.isEmpty()) {
            result = "no items selected";
        } else if (indexList.size() < 2) {
            result = "not enough items selected";
        } else {
            result = "";
        }

        return result;
    }
}
