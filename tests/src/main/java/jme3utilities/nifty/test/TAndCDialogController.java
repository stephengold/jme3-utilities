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
package jme3utilities.nifty.test;

import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.nifty.dialog.DialogController;

/**
 * Controller for a text-and-check dialog box used to input a boolean and some
 * non-empty text.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class TAndCDialogController implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TAndCDialogController.class.getName());
    // *************************************************************************
    // fields

    /**
     * description of the commit action (not null, not empty, should fit the
     * button -- about 8 or 9 characters)
     */
    final private String buttonText;
    /**
     * label text for the checkbox (not null, not empty)
     */
    final private String checkboxLabel;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param buttonText commit-button text (not null, not empty)
     * @param checkboxLabel checkbox label text (not null, not empty)
     */
    TAndCDialogController(String buttonText, String checkboxLabel) {
        Validate.nonEmpty(buttonText, "button label");
        Validate.nonEmpty(checkboxLabel, "checkbox label");

        this.buttonText = buttonText;
        this.checkboxLabel = "  " + checkboxLabel;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Determine the feedback message for the specified input text.
     *
     * @param inputText (not null)
     * @return the message (not null)
     */
    protected String feedback(String inputText) {
        String message;
        if (inputText.isEmpty()) {
            message = "must not be empty";
        } else {
            message = "";
        }
        return message;
    }

    /**
     * Read the checkbox status.
     *
     * @param dialogElement (not null)
     * @return a text string (not null)
     */
    protected boolean isChecked(Element dialogElement) {
        CheckBox checkBox = dialogElement.findNiftyControl("#dialogcheck",
                CheckBox.class);
        boolean result = checkBox.isChecked();
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
     * Test whether "commit" actions are allowed.
     *
     * @param dialogElement (not null)
     * @return true if allowed, otherwise false
     */
    @Override
    public boolean allowCommit(Element dialogElement) {
        Validate.nonNull(dialogElement, "dialog element");

        String text = text(dialogElement);
        String feedback = feedback(text);
        boolean result = feedback.isEmpty();

        return result;
    }

    /**
     * Construct the action-string suffix for a commit.
     *
     * @param dialogElement (not null)
     * @return the suffix (not null)
     */
    @Override
    public String commitSuffix(Element dialogElement) {
        Validate.nonNull(dialogElement, "dialog element");

        boolean isChecked = isChecked(dialogElement);
        String text = text(dialogElement);
        String result = Boolean.toString(isChecked) + " " + text;

        return result;
    }

    /**
     * Update this dialog box prior to rendering. (Invoked once per frame.)
     *
     * @param dialogElement (not null)
     * @param unused time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(Element dialogElement, float unused) {
        Validate.nonNull(dialogElement, "dialog element");

        String text = text(dialogElement);
        String feedback = feedback(text);
        Button commitButton
                = dialogElement.findNiftyControl("#commit", Button.class);

        Element element = dialogElement.findElementById("#feedback");
        TextRenderer renderer = element.getRenderer(TextRenderer.class);
        renderer.setText(feedback);

        element = dialogElement.findElementById("#checklabel");
        renderer = element.getRenderer(TextRenderer.class);
        renderer.setText(checkboxLabel);

        if (feedback.isEmpty()) {
            commitButton.setText(buttonText);
            commitButton.getElement().show();

        } else {
            commitButton.setText("");
            commitButton.getElement().hide();
        }
    }
}
