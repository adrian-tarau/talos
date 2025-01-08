package net.microfalx.maven.plugin;

import org.apache.maven.execution.MavenSession;

/**
 * Resolves various Maven related configuration.
 */
public class MavenConfiguration extends net.microfalx.maven.core.MavenConfiguration {

    public MavenConfiguration(MavenSession session) {
        super(session);
    }
}
