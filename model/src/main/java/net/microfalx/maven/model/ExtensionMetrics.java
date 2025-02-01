package net.microfalx.maven.model;

import org.apache.maven.model.Extension;

/**
 * Holds metrics about a project extension.
 */
public class ExtensionMetrics extends Dependency {

    protected ExtensionMetrics() {
    }

    public ExtensionMetrics(Extension extension) {
        super(extension.getGroupId(), extension.getArtifactId(), extension.getVersion());
    }
}
