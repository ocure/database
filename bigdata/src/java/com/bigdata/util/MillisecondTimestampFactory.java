/**

Copyright (C) SYSTAP, LLC 2006-2007.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on Oct 26, 2006
 */

package com.bigdata.util;

import org.apache.log4j.Logger;

/**
 * A timestamp factory based on {@link System#currentTimeMillis()}. Timestamps
 * reported by this factory are guarenteed to be distinct and strictly
 * increasing during the life cycle of the JVM. No guarentee is made if across
 * JVMs or system reboots. A means is available to inform the factory of the
 * earliest timestamp that it may serve. This may be used on restart to ensure
 * that time goes forward or when handing off from one timestamp service to
 * another.
 * <p>
 * Note: Time as reported by {@link System#currentTimeMillis()} can do crazy
 * things, including going backwards - presumably because of an error somewhere
 * in the time management stack (observed on Fedora core 6 with Sun JDK
 * 1.6.0_03). In these cases a warning is logged and the timestamp factory
 * begins to assign up one long integers instead. If time catches up, then
 * another warning is logged and the factory again begins to report timestamps
 * based on {@link System#currentTimeMillis()}. Note that when the factory is
 * using a one-up assignment it may appear to have a resolution finer than one
 * millisecond.
 * <p>
 * Note: method on this class are <code>synchronized</code> to ensure that
 * concurrent callers receive distinct timestamps. Likewise, the methods on this
 * class are <code>static</code> to ensure that assigned timestamps are global
 * for a JVM.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class MillisecondTimestampFactory {

    protected static final Logger log = Logger
            .getLogger(MillisecondTimestampFactory.class);

    /**
     * The initial lower bound for the assigned timestamps.
     */
    private static long _lastTimestamp = System.currentTimeMillis();

    /**
     * Initially the factory will use times as reported by
     * {@link System#currentTimeMillis()}.
     */
    private static boolean _autoIncMode = false;

    /**
     * Return <code>true</code> if the timestamp factory is in auto-increment
     * mode.
     */
    synchronized static public boolean isAutoIncMode() {

        return _autoIncMode;

    }

    /**
     * No public constructor.
     */
    private MillisecondTimestampFactory() {

    }

    /**
     * Sets the lower bound for the generated timestamps. This method may be
     * used to safely synchronize a failover timestamp service with a primary.
     * <strong>Extreme care should be used with this method as it can force the
     * factory to return timestamps out of order.</strong>
     * 
     * @param lowerBound
     *            The lower bound.
     * 
     * @throws IllegalArgumentException
     *             if the given timestamp is non-positive.
     */
    synchronized static public void setLowerBound(long lowerBound) {

        assertPositive(lowerBound);

        if (_lastTimestamp < lowerBound) {

            log.warn("Timestamp factory is being set to an earlier time!");

        } else {

            log.info("Advancing: lowerBound=" + lowerBound);

        }

        _lastTimestamp = lowerBound;

        _autoIncMode = false;

    }

    /**
     * This is a paranoia check in case the timestamp overflows. The timestamp
     * value 0L and negative timestamps all have special interpretations for
     * bigdata so this factory MUST NOT assign a non-positive timestamp.
     * 
     * @param t
     *            A timestamp.
     * 
     * @throws IllegalStateException
     *             if <i>t</i> is non-positive.
     */
    private static void assertPositive(long t) {

        if (t <= 0L) {

            throw new IllegalStateException("Timestamp is non-positive: " + t);

        }

    }

    /**
     * Generates a timestamp based on {@link System#currentTimeMillis()} that is
     * guaranteed to be distinct from the last timestamp generated by this
     * method during the life cycle of the JVM. No guarantee is made if across
     * JVMs or system reboots. However, a means is available to inform the
     * factory of the earliest timestamp that it may serve.
     * <p>
     * <p>
     * 
     * @return A timestamp with no more millisecond resolution.
     * 
     * @see System#currentTimeMillis()
     */
    synchronized static public long nextMillis() {

        //        final long lastTimestamp = lastTimestamp;

        // current time.
        long timestamp = System.currentTimeMillis();
        ;

        if (_autoIncMode) {

            if (timestamp < _lastTimestamp) {

                timestamp = _lastTimestamp + 1;

                // overflow and paranoia test.
                assertPositive(timestamp);

                // timestamp is Ok, so save it.
                _lastTimestamp = timestamp;

                // return the assigned timestamp value.
                return timestamp;

            }

            _autoIncMode = false;

            log
                    .warn("Leaving auto-increment mode: time is going forward again: lastTimestamp="
                            + _lastTimestamp + ", millisTime=" + timestamp);

            // fall through.

        }

        assert !_autoIncMode;

        //        // spin looking for a distinct timestamp.
        //        for(int i=0; i<1000 && timestamp == lastMillisTime; i++) {
        //
        //            timestamp = System.currentTimeMillis();
        //
        //        }

        // if not distinct, then sleep waiting for a distinct timestamp.
        while (timestamp == _lastTimestamp) {

            try {

                Thread.sleep(0, 1);

            } catch (InterruptedException ex) {

                // ignore.

            }

            timestamp = System.currentTimeMillis();

        }

        if (timestamp < _lastTimestamp) {

            /*
             * System.currentTimeMillis() is reporting a time that is less than
             * the last timestamp reported by this class. We switch over to an
             * auto-increment mode and recursively invoke ourselves to return a
             * one-up timestamp in order to keep the apparent time as reported
             * by this factory moving forward.
             */

            log
                    .warn("Entering auto-increment mode : milliseconds go backward: lastTimestamp="
                            + _lastTimestamp + ", millisTime=" + timestamp);

            _autoIncMode = true;

            return nextMillis();

        }

        // overflow and paranoia test.
        assertPositive(timestamp);

        // timestamp is Ok, so save it.
        _lastTimestamp = timestamp;

        // return the assigned timestamp value.
        return timestamp;

    }

}
