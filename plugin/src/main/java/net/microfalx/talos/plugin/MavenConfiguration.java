package net.microfalx.talos.plugin;

import org.apache.maven.execution.MavenSession;

/**
 * Resolves various Maven related configuration.
 */
public class MavenConfiguration extends net.microfalx.talos.core.MavenConfiguration {

    public MavenConfiguration(MavenSession session) {
        super(session);
    }
}
