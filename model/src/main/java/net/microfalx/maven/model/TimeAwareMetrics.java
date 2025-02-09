package net.microfalx.maven.model;

import net.microfalx.lang.Identifiable;
import net.microfalx.lang.Nameable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;

/**
 * An interface for metrics which are time-aware.
 *
 * @param <T> the self-type
 */
public interface TimeAwareMetrics<T extends TimeAwareMetrics<T>> extends Identifiable<String>, Nameable {

    /**
     * Returns the timeline of the object.
     *
     * @return a non-null instance
     */
    Collection<ActiveInterval> getTimeline();

    /**
     * Returns the time when the metrics started to be collected.
     *
     * @return a non-null instance
     */
    ZonedDateTime getStartTime();

    /**
     * Changes the time when the metrics started to be collected.
     *
     * @param startTime the start time.
     * @return self
     */
    T setStartTime(ZonedDateTime startTime);

    /**
     * Returns the time when the metrics have been completed.
     *
     * @return a non-null instance
     */
    ZonedDateTime getEndTime();

    /**
     * Changes the time when the collection has stopped.
     *
     * @param endTime the end time
     * @return self
     */
    T setEndTime(ZonedDateTime endTime);

    /**
     * Returns the number of times the object was activated.
     *
     * @return a positive integer.
     */
    int getExecutionCount();

    /**
     * Returns the total duration (between start and end).
     *
     * @return a non-null instance
     */
    Duration getDuration();

    /**
     * Returns the duration when the object was active.
     * <p>
     * The active time is the time between calls to {@link #setStartTime(ZonedDateTime)} and {@link #setEndTime(ZonedDateTime)}.
     *
     * @return a non-null instance
     */
    Duration getActiveDuration();

    /**
     * Holds an active interval on the timeline.
     */
    interface ActiveInterval {

        /**
         * Returns the time when the object has become active.
         *
         * @return a non-null instance
         */
        ZonedDateTime getStartTime();

        /**
         * Returns the time when the object has become inactive.
         *
         * @return a non-null instance
         */
        ZonedDateTime getEndTime();

    }
}
