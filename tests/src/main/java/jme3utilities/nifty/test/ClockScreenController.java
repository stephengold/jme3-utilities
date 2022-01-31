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
package jme3utilities.nifty.test;

import java.util.Calendar;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.GuiScreenController;

/**
 * GUI screen controller used by ClockDemo.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ClockScreenController extends GuiScreenController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ClockScreenController.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled controller which will be enabled during
     * initialization.
     */
    public ClockScreenController() {
        super("ClockDemo/clockScreen",
                "Interface/Nifty/screens/ClockDemo/clockScreen.xml",
                true);
    }
    // *************************************************************************
    // SimpleAppState methods

    /**
     * Callback to update this display. (Invoked once per frame.)
     *
     * @param elapsedTime time since the previous update (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        if (!isEnabled()) {
            return;
        }

        Calendar rightNow = Calendar.getInstance();
        int hours = rightNow.get(Calendar.HOUR);
        if (hours == 0) {
            hours = 12;
        }
        int minutes = rightNow.get(Calendar.MINUTE);
        int seconds = rightNow.get(Calendar.SECOND);
        /*
         * Update the labels which show status.
         */
        String timeString = String.format("%d:%02d:%02d",
                hours, minutes, seconds);
        setStatusText("time1", timeString);

        if (MyMath.isOdd(seconds)) {
            setStatusText("time2", "odd");
        } else {
            setStatusText("time2", "even");
        }

        Calendar midnight = (Calendar) rightNow.clone();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        long msSinceMidnight = rightNow.getTimeInMillis() - midnight.getTimeInMillis();
        double secondsSinceMidnight = msSinceMidnight / 1000.0;

        String text3 = String.format("%.3f", secondsSinceMidnight);
        setStatusText("time3", text3);
    }
}
