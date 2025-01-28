package net.microfalx.maven.model;

import net.microfalx.lang.Hashing;
import net.microfalx.lang.NamedIdentityAware;

import java.util.StringJoiner;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

public class TestMetrics extends NamedIdentityAware<String> {

    private String className;

    private float time;

    private String failureMessage;

    private String failureType;

    private String failureErrorLine;

    private String failureDetail;

    private String module;

    private boolean failure;

    private boolean error;

    private boolean skipped;

    protected TestMetrics() {
    }

    public TestMetrics(String module, String className, String name) {
        requireNonNull(module);
        requireNonNull(className);
        requireNonNull(name);
        this.module = module;
        this.className = className;
        setName(name);
        Hashing hashing = Hashing.create();
        hashing.update(module);
        hashing.update(className);
        hashing.update(name);
        setId(hashing.asString());
    }

    public String getClassName() {
        return className;
    }

    public TestMetrics setClassName(String className) {
        this.className = className;
        return this;
    }

    public float getTime() {
        return time;
    }

    public TestMetrics setTime(float time) {
        this.time = time;
        return this;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public TestMetrics setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    public String getFailureType() {
        return failureType;
    }

    public TestMetrics setFailureType(String failureType) {
        this.failureType = failureType;
        return this;
    }

    public String getFailureErrorLine() {
        return failureErrorLine;
    }

    public TestMetrics setFailureErrorLine(String failureErrorLine) {
        this.failureErrorLine = failureErrorLine;
        return this;
    }

    public String getFailureDetail() {
        return failureDetail;
    }

    public TestMetrics setFailureDetail(String failureDetail) {
        this.failureDetail = failureDetail;
        return this;
    }

    public String getModule() {
        return module;
    }

    public boolean isFailureOrError() {
        return isError() || isFailure();
    }

    public boolean isFailure() {
        return failure;
    }

    public TestMetrics setFailure(boolean failure) {
        this.failure = failure;
        return this;
    }

    public boolean isError() {
        return error;
    }

    public TestMetrics setError(boolean error) {
        this.error = error;
        return this;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public TestMetrics setSkipped(boolean skipped) {
        this.skipped = skipped;
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TestMetrics.class.getSimpleName() + "[", "]")
                .add("className='" + className + "'")
                .add("name='" + getName() + "'")
                .add("time=" + time)
                .add("failureMessage='" + failureMessage + "'")
                .add("failureType='" + failureType + "'")
                .add("failureErrorLine='" + failureErrorLine + "'")
                .add("failureDetail='" + failureDetail + "'")
                .add("module='" + module + "'")
                .add("failure=" + failure)
                .add("error=" + error)
                .add("skipped=" + skipped)
                .toString();
    }
}
