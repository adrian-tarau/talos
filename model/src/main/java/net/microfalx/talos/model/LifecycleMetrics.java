package net.microfalx.talos.model;

import net.microfalx.lang.StringUtils;

import static net.microfalx.lang.ArgumentUtils.requireNotEmpty;

/**
 * Holds metrics about lifecycle events.
 */
public class LifecycleMetrics extends AbstractTimeAwareMetrics<LifecycleMetrics> {

    protected LifecycleMetrics() {
    }

    public LifecycleMetrics(String name) {
        requireNotEmpty(name);
        setName(name);
        setId(StringUtils.toIdentifier(name));
    }
}
