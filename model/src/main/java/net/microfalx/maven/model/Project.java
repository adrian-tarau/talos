package net.microfalx.maven.model;

import net.microfalx.lang.ObjectUtils;
import net.microfalx.lang.UriUtils;
import net.microfalx.maven.core.MavenUtils;
import org.apache.maven.project.MavenProject;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static net.microfalx.lang.StringUtils.isNotEmpty;

/**
 * A Maven project.
 */
public class Project extends Dependency {

    private URI uri;
    private Map<String, String> properties;

    public Project() {
    }

    public Project(String groupId, String artifactId, String version) {
        super(groupId, artifactId, version);
    }

    public Project(MavenProject project, boolean verbose) {
        this(project.getGroupId(), project.getArtifactId(), project.getVersion());
        if (verbose) init(project);
    }

    public URI getUri() {
        return uri;
    }

    public Map<String, String> getProperties() {
        return properties != null ? unmodifiableMap(properties) : emptyMap();
    }

    private void init(MavenProject project) {
        setName(project.getName());
        setDescription(project.getDescription());
        try {
            uri = isNotEmpty(project.getUrl()) ? UriUtils.parseUri(project.getUrl()) : null;
        } catch (Exception e) {
            // ideally, the project home page should be valid, but if not, ignore any failure
        }
        properties = new HashMap<>();
        project.getProperties().forEach((k, v) -> {
            String name = ObjectUtils.toString(k);
            String value = ObjectUtils.toString(v);
            properties.put(name, MavenUtils.maskSecret(name, value));
        });
    }
}
