// (c) Copyright 2013 Stephen Gold <sgold@sonic.net>
// Distributed under the terms of the GNU General Public License

/*
 This file is part of the JME3 Utilities Package.

 The JME3 Utilities Package is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 The JME3 Utilities Package is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

 You should have received a copy of the GNU General Public License along with
 the JME3 Utilities Package.  If not, see <http://www.gnu.org/licenses/>.
 */
package jme3utilities;

import com.jme3.app.state.AbstractAppState;
import java.util.logging.Logger;

/**
 * An app state for tracking the time of day in a game.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class TimeOfDay
        extends AbstractAppState {
    // *************************************************************************
    // constants

    /**
     * duration of a full day (in hours)
     */
    final public static int hoursPerDay = 24;
    /**
     * number of minutes in an hour
     */
    final public static int minutesPerHour = 60;
    /**
     * number of seconds in a minute
     */
    final public static int secondsPerMinute = 60;
    /**
     * number of seconds in an hour
     */
    final public static int secondsPerHour = secondsPerMinute * minutesPerHour;
    /**
     * number of seconds in a day
     */
    final public static int secondsPerDay = secondsPerHour * hoursPerDay;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(TimeOfDay.class.getName());
    // *************************************************************************
    // fields
    /**
     * simulated time of day (seconds since midnight, <86400, >=0)
     *
     * The simulated time is stored in double precision because it is
     * incremented by a small amount for each frame.
     */
    private double timeOfDay = 0.0;
    /**
     * simulation rate relative to real time
     */
    private float rate = 1f;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new simulation clock, specifying the start time.
     *
     * @param startHour hours since midnight (<24, >=0)
     */
    public TimeOfDay(float startHour) {
        if (startHour < 0f || startHour >= hoursPerDay) {
            throw new IllegalArgumentException("hour must be between 0 and 24");
        }

        timeOfDay = startHour * secondsPerHour;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the simulated time of day.
     *
     * @return hours since midnight (<24, >=0)
     */
    public float getHour() {
        float result = (float) timeOfDay / secondsPerHour;
        if (result == hoursPerDay) {
            result = 0f;
        }

        assert result >= 0f : result;
        assert result <= hoursPerDay : result;
        return result;
    }

    /**
     * Read the simulated time of day.
     *
     * @return seconds since midnight (<86400, >=0)
     */
    public int getSecond() {
        int result = (int) Math.round(timeOfDay);
        if (result == secondsPerDay) {
            result = 0;
        }

        assert result >= 0 : result;
        assert result < secondsPerDay : result;
        return result;
    }

    /**
     * Write the simulation rate.
     *
     * @param newRate simulation rate relative to real time
     */
    public void setRate(float newRate) {
        rate = newRate;
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Update the time of day.
     *
     * @param interval real seconds elapsed since the previous update (>=0)
     */
    @Override
    public void update(float interval) {
        double simulatedSeconds = rate * interval;
        timeOfDay = timeOfDay + simulatedSeconds;
        timeOfDay = MyMath.modulo(timeOfDay, secondsPerDay);
    }
    // *************************************************************************
    // Object methods

    /**
     * Format the time of day as text.
     */
    @Override
    public String toString() {
        int second = getSecond();
        int ss = second % secondsPerMinute;
        int minute = second / secondsPerMinute;
        int mm = minute % minutesPerHour;
        int hh = minute / minutesPerHour;
        String result = String.format("%02d:%02d:%02d", hh, mm, ss);

        return result;
    }
}