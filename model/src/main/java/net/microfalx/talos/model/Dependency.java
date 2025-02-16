package net.microfalx.talos.model;

import net.microfalx.lang.NamedIdentityAware;

import java.util.Objects;
import java.util.StringJoiner;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.StringUtils.defaultIfEmpty;
import static net.microfalx.lang.StringUtils.toIdentifier;

/**
 * Base class for all dependencies.
 */
public class Dependency extends NamedIdentityAware<String> {

    private String groupId;
    private String artifactId;
    private String version;
    private String type = "jar";
    private String scope = "compile";
    private boolean optional;

    private boolean transitive;

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

    public boolean isTransitive() {
        return transitive;
    }

    public Dependency setTransitive(boolean transitive) {
        this.transitive = transitive;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public Dependency setScope(String scope) {
        this.scope = defaultIfEmpty(scope, "compile");
        return this;
    }

    public String getType() {
        return type;
    }

    public Dependency setType(String type) {
        this.type = defaultIfEmpty(type, "jar");
        return this;
    }

    public boolean isOptional() {
        return optional;
    }

    public Dependency setOptional(boolean optional) {
        this.optional = optional;
        return this;
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
