/*
 Copyright (c) 2017-2022, Stephen Gold
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

import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.Validate;

/**
 * Controller for a text-entry dialog box that accepts arbitrary text.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TextEntryDialog implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TextEntryDialog.class.getName());
    /**
     * pattern for matching the word "null"
     */
    final private static Pattern nullPattern
            = Pattern.compile("\\s*null\\s*");
    // *************************************************************************
    // fields

    /**
     * description of the commit action (not null, not empty, should fit the
     * button -- about 8 or 9 characters)
     */
    final private String commitDescription;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param description the commit-button text (not null, not empty)
     */
    public TextEntryDialog(String description) {
        Validate.nonEmpty(description, "description");
        commitDescription = description;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Determine the feedback message for the specified input text. Meant to be
     * overridden.
     *
     * @param inputText (not null)
     * @return the message (not null)
     */
    protected String feedback(String inputText) {
        return "";
    }

    /**
     * Test whether the specified string matches nullPattern.
     *
     * @param lcText text string (not null, assumed to be in lower case)
     * @return true for match, otherwise false
     */
    protected static boolean matchesNull(CharSequence lcText) {
        Validate.nonNull(lcText, "lc text");

        Matcher matcher = nullPattern.matcher(lcText);
        boolean result = matcher.matches();

        return result;
    }

    /**
     * Read the text field.
     *
     * @param dialogElement (not null)
     * @return a text string (not null)
     */
    protected String text(Element dialogElement) {
        TextField textField
                = dialogElement.findNiftyControl("#textfield", TextField.class);
        String text = textField.getRealText();

        assert text != null;
        return text;
    }
    // *************************************************************************
    // DialogController methods

    /**
     * Test whether "commit" actions are allowed for the current selection.
     *
     * @param dialogElement (not null)
     * @return true if allowed, otherwise false
     */
    @Override
    public boolean allowCommit(Element dialogElement) {
        Validate.nonNull(dialogElement, "dialog element");

        String text = text(dialogElement);
        String feedback = feedback(text);
        boolean allow = feedback.isEmpty();

        return allow;
    }

    /**
     * Construct the action-string suffix for a commit.
     *
     * @param dialogElement (not null)
     * @return the commit suffix (not null)
     */
    @Override
    public String commitSuffix(Element dialogElement) {
        Validate.nonNull(dialogElement, "dialog element");
        String suffix = text(dialogElement);
        return suffix;
    }

    /**
     * Update this dialog box prior to rendering. (Invoked once per frame.)
     *
     * @param dialogElement (not null)
     * @param ignored time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(Element dialogElement, float ignored) {
        Validate.nonNull(dialogElement, "dialog element");

        String text = text(dialogElement);
        String feedbackMessage = feedback(text);

        String commitLabel;
        if (feedbackMessage.isEmpty()) {
            commitLabel = commitDescription;
        } else {
            commitLabel = "";
        }

        Button commitButton
                = dialogElement.findNiftyControl("#commit", Button.class);
        commitButton.setText(commitLabel);

        boolean makeButtonVisible = !commitLabel.isEmpty();
        commitButton.getElement().setVisible(makeButtonVisible);

        Element feedbackElement = dialogElement.findElementById("#feedback");
        TextRenderer renderer = feedbackElement.getRenderer(TextRenderer.class);
        renderer.setText(feedbackMessage);
    }
}
