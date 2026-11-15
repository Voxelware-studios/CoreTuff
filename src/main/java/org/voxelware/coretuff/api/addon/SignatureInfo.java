package org.voxelware.coretuff.api.addon;

import java.util.Objects;

public final class SignatureInfo {
    private final String algorithm;
    private final String value;

    public SignatureInfo(String algorithm, String value) {
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        this.value = Objects.requireNonNull(value, "value");
    }

    public String algorithm() { return algorithm; }
    public String value() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SignatureInfo that)) return false;
        return algorithm.equals(that.algorithm) && value.equals(that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(algorithm, value); }
}
