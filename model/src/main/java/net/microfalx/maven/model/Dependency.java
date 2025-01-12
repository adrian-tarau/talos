package net.microfalx.maven.model;

import net.microfalx.lang.NamedIdentityAware;

import java.util.Objects;
import java.util.StringJoiner;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.StringUtils.toIdentifier;

/**
 * Base class for all dependencies.
 */
public class Dependency extends NamedIdentityAware<String> {

    private String groupId;
    private String artifactId;
    private String version;

    protected Dependency() {
    }

    public Dependency(String groupId, String artifactId, String version) {
        requireNonNull(groupId);
        requireNonNull(artifactId);
        requireNonNull(version);
        setId(toIdentifier(groupId + ":" + artifactId));
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public final String getGroupId() {
        return groupId;
    }

    public final String getArtifactId() {
        return artifactId;
    }

    public final String getVersion() {
        return version;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dependency)) return false;
        if (!super.equals(o)) return false;
        Dependency that = (Dependency) o;
        return Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(super.hashCode(), groupId, artifactId);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("id='" + getId() + "'")
                .add("name='" + getName() + "'")
                .add("groupId='" + groupId + "'")
                .add("artifactId='" + artifactId + "'")
                .add("version='" + version + "'")
                .add("description='" + getDescription() + "'")
                .toString();
    }

    @Override
    protected String dynamicName() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
