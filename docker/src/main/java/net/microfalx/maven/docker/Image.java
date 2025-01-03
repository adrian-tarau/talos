package net.microfalx.maven.docker;

import net.microfalx.lang.Hashing;
import net.microfalx.lang.IdentityAware;
import net.microfalx.lang.NamedAndTaggedIdentifyAware;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.microfalx.lang.ArgumentUtils.requireNotEmpty;

/**
 * Represents a container image.
 */
public class Image extends NamedAndTaggedIdentifyAware<String> {

    private long size;
    private Long virtualSize;
    private String digest;
    private String author;
    private String os;
    private String architecture;
    private LocalDateTime createdAt;
    private Map<String, String> labels;

    /**
     * Returns the digest of the image.
     *
     * @return a non-null instance
     */
    public String getDigest() {
        return digest;
    }

    /**
     * Returns the size of the image.
     *
     * @return a positive integer
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the virtual size of the image.
     *
     * @return a positive integer
     */
    public Long getVirtualSize() {
        return virtualSize;
    }

    /**
     * Returns the time when the container was created.
     *
     * @return a non-null instance
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the author.
     *
     * @return a non-null instance
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Returns the architecture.
     *
     * @return a non-null instance
     */
    public String getArchitecture() {
        return architecture;
    }

    /**
     * Returns the OS.
     *
     * @return a non-null instance
     */
    public String getOs() {
        return os;
    }

    /**
     * Returns the labels associated with the image.
     *
     * @return a non-null instance
     */
    public Map<String, String> getLabels() {
        return Collections.unmodifiableMap(labels);
    }

    /**
     * Changes the image name.
     *
     * @param name the new name
     * @return a new instance, with a different name
     */
    public Image withName(String name) {
        Image copy = (Image) copy();
        copy.setName(name);
        return copy;
    }

    public static class Builder extends NamedAndTaggedIdentifyAware.Builder<String> {

        private long size;
        private Long virtualSize;
        private String digest = Hashing.EMPTY;
        private String author;
        private String os;
        private String architecture;
        private LocalDateTime createdAt;
        private Map<String, String> labels = new HashMap<>();

        public Builder(String id) {
            super(id);
        }

        public Builder digest(String digest) {
            requireNotEmpty(digest);
            this.digest = digest;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder virtualSize(Long virtualSize) {
            this.virtualSize = virtualSize;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder os(String os) {
            this.os = os;
            return this;
        }

        public Builder architecture(String architecture) {
            this.architecture = architecture;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder label(String name, String value) {
            requireNotEmpty(name);
            this.labels.put(name, value);
            return this;
        }

        @Override
        protected IdentityAware<String> create() {
            return new Image();
        }

        @Override
        public Image build() {
            Image image = (Image) super.build();
            image.digest = digest;
            image.size = size;
            image.virtualSize = virtualSize;
            image.author = author;
            image.os = os;
            image.architecture = architecture;
            image.createdAt = createdAt;
            image.labels = labels;
            return image;
        }
    }


}
