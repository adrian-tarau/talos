package net.microfalx.maven.model;

import net.microfalx.lang.NamedIdentityAware;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;

import static java.time.Duration.ofNanos;
import static java.util.Collections.unmodifiableCollection;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Base class for {@link TimeAwareMetrics} and {@link net.microfalx.lang.Identifiable} and {@link net.microfalx.lang.Nameable}
 *
 * @param <T> the self-type
 */
public abstract class AbstractTimeAwareMetrics<T extends AbstractTimeAwareMetrics<T>> extends NamedIdentityAware<String>
        implements TimeAwareMetrics<T> {

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    private final Collection<ActiveInterval> timeline = new ArrayList<>();

    private int executionCount;
    private long durationNanos;

    private static final ThreadLocal<Long> START_TIME = ThreadLocal.withInitial(System::nanoTime);
    private static final ThreadLocal<ActiveIntervalImpl> INTERVAL = new ThreadLocal<>();

    @Override
    public synchronized Collection<ActiveInterval> getTimeline() {
        return unmodifiableCollection(timeline);
    }

    public synchronized final ZonedDateTime getStartTime() {
        if (startTime == null) startTime = ZonedDateTime.now();
        return startTime;
    }

    public synchronized final T setStartTime(ZonedDateTime startTime) {
        requireNonNull(startTime);
        INTERVAL.set(new ActiveIntervalImpl(startTime));
        START_TIME.set(System.nanoTime());
        if (this.startTime == null) {
            this.startTime = startTime;
        } else {
            setEndTime(endTime);
        }
        return self();
    }

    public synchronized final ZonedDateTime getEndTime() {
        if (endTime == null) endTime = ZonedDateTime.now();
        return endTime;
    }

    public synchronized final T setEndTime(ZonedDateTime endTime) {
        requireNonNull(endTime);
        this.endTime = endTime;
        ActiveIntervalImpl activeInterval = INTERVAL.get();
        if (activeInterval != null) {
            activeInterval.endTime = endTime;
            timeline.add(activeInterval);
            INTERVAL.remove();
        }
        this.durationNanos += System.nanoTime() - START_TIME.get();
        this.executionCount++;
        return self();
    }

    @Override
    public synchronized final int getExecutionCount() {
        return executionCount;
    }

    public synchronized final Duration getDuration() {
        return Duration.between(getStartTime(), getEndTime());
    }

    public synchronized final T addActiveDuration(Duration duration) {
        this.durationNanos += duration.toNanos();
        return self();
    }

    @Override
    public synchronized final Duration getActiveDuration() {
        return ofNanos(durationNanos);
    }

    public synchronized final Duration getAverageActiveDuration() {
        return getExecutionCount() > 0 ? getDuration().dividedBy(getExecutionCount()) : Duration.ZERO;
    }

    @SuppressWarnings("unchecked")
    protected final T self() {
        return (T) this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("id=" + getId())
                .add("name=" + getName())
                .add("startTime=" + startTime)
                .add("endTime=" + endTime)
                .add("executionCount=" + executionCount)
                .add("durationNanos=" + durationNanos)
                .toString();
    }

    protected static final class ActiveIntervalImpl implements ActiveInterval {

        private ZonedDateTime startTime;
        private ZonedDateTime endTime;

        protected ActiveIntervalImpl() {
        }

        private ActiveIntervalImpl(ZonedDateTime startTime) {
            this.startTime = startTime;
        }

        @Override
        public ZonedDateTime getStartTime() {
            return startTime;
        }

        @Override
        public ZonedDateTime getEndTime() {
            return endTime;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", ActiveIntervalImpl.class.getSimpleName() + "[", "]")
                    .add("startTime=" + startTime)
                    .add("endTime=" + endTime)
                    .toString();
        }
    }
}
