package net.microfalx.talos.model;

import net.microfalx.lang.ClassUtils;
import net.microfalx.lang.NamedIdentityAware;
import net.microfalx.lang.StringUtils;
import net.microfalx.talos.core.MavenUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;

import java.time.ZonedDateTime;
import java.util.StringJoiner;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ExceptionUtils.*;

/**
 * Holds metrics about a failure.
 */
public class FailureMetrics extends NamedIdentityAware<String> {

    private String moduleId;
    private String mojoId;
    private String throwableClass;
    private String throwableStacktrace;
    private String rootCauseThrowableClass;
    private String rootCauseMessage;
    private final ZonedDateTime timestamp = ZonedDateTime.now();

    transient ProjectMetrics module;
    transient MojoMetrics mojo;

    protected FailureMetrics() {
    }

    public FailureMetrics(MavenProject project, Mojo mojo, String name, Throwable throwable) {
        requireNonNull(throwable);
        if (project != null) moduleId = project.getArtifactId();
        if (mojo != null) mojoId = MavenUtils.getId(mojo);
        if (name == null && project != null) name = project.getName();

        throwableClass = ClassUtils.getName(throwable);
        throwableStacktrace = getStackTrace(throwable);
        rootCauseThrowableClass = ClassUtils.getName(getRootCause(throwable));
        rootCauseMessage = getRootCauseDescription(throwable);

        setId(StringUtils.toIdentifier(moduleId, mojoId, throwableClass));
        setName(name);
    }

    public ProjectMetrics getModule() {
        return module;
    }

    public MojoMetrics getMojo() {
        return mojo;
    }

    protected String getModuleId() {
        return moduleId;
    }

    protected String getMojoId() {
        return mojoId;
    }

    public String getThrowableClass() {
        return throwableClass;
    }

    public String getThrowableStacktrace() {
        return throwableStacktrace;
    }

    public String getRootCauseThrowableClass() {
        return rootCauseThrowableClass;
    }

    public String getRootCauseMessage() {
        return rootCauseMessage;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FailureMetrics.class.getSimpleName() + "[", "]")
                .add("moduleId='" + moduleId + "'")
                .add("mojoId='" + mojoId + "'")
                .add("throwableClass='" + throwableClass + "'")
                .add("throwableStacktrace='" + throwableStacktrace + "'")
                .add("timestamp=" + timestamp)
                .toString();
    }
}
