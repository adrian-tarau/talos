package net.microfalx.maven.model;

import net.microfalx.lang.NamedIdentityAware;
import net.microfalx.lang.StringUtils;

import java.time.Duration;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ArgumentUtils.requireNotEmpty;

/**
 * Holds metrics about lifecycle events.
 */
public class LifecycleMetrics extends NamedIdentityAware<String> {

    private Duration duration = Duration.ZERO;

    protected LifecycleMetrics() {
    }

    public LifecycleMetrics(String name) {
        requireNotEmpty(name);
        setName(name);
        setId(StringUtils.toIdentifier(name));
    }

    public Duration getDuration() {
        return duration;
    }

    public LifecycleMetrics addDuration(Duration duration) {
        requireNonNull(duration);
        this.duration = duration.plus(duration);
        return this;
    }
}
