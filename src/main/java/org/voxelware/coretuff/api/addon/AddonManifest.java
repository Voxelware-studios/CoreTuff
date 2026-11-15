package org.voxelware.coretuff.api.addon;

import java.util.*;

public final class AddonManifest {
    private final String name;
    private final String id;
    private final String version;
    private final String author;
    private final String description;
    private final String website;
    private final String coretuffVersion;
    private final List<String> dependencies;
    private final List<String> optionalDependencies;
    private final Set<Capability> requestedCapabilities;
    private final SignatureInfo signature;
    private final TrustLevel trustLevel;

    public AddonManifest(
            String name,
            String id,
            String version,
            String author,
            String description,
            String website,
            String coretuffVersion,
            List<String> dependencies,
            List<String> optionalDependencies,
            Set<Capability> requestedCapabilities,
            SignatureInfo signature,
            TrustLevel trustLevel) {
        this.name = Objects.requireNonNull(name, "name");
        this.id = Objects.requireNonNull(id, "id");
        this.version = Objects.requireNonNull(version, "version");
        this.author = Objects.requireNonNull(author, "author");
        this.description = description;
        this.website = website;
        this.coretuffVersion = Objects.requireNonNull(coretuffVersion, "coretuffVersion");
        this.dependencies = List.copyOf(dependencies == null ? List.of() : dependencies);
        this.optionalDependencies = List.copyOf(optionalDependencies == null ? List.of() : optionalDependencies);
        this.requestedCapabilities = Set.copyOf(requestedCapabilities == null ? Set.of() : requestedCapabilities);
        this.signature = signature;
        this.trustLevel = Objects.requireNonNull(trustLevel, "trustLevel");
    }

    public String name() { return name; }
    public String id() { return id; }
    public String version() { return version; }
    public String author() { return author; }
    public String description() { return description; }
    public String website() { return website; }
    public String coretuffVersion() { return coretuffVersion; }
    public List<String> dependencies() { return dependencies; }
    public List<String> optionalDependencies() { return optionalDependencies; }
    public Set<Capability> requestedCapabilities() { return requestedCapabilities; }
    public SignatureInfo signature() { return signature; }
    public TrustLevel trustLevel() { return trustLevel; }

    public boolean isSigned() { return signature != null; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AddonManifest that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}
