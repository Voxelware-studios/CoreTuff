package org.voxelware.coretuff.api.platform;

import java.util.Objects;

public final class VersionRange {
    private final String raw;
    private final VersionBound lower;
    private final VersionBound upper;

    private VersionRange(String raw, VersionBound lower, VersionBound upper) {
        this.raw = raw;
        this.lower = lower;
        this.upper = upper;
    }

    public static VersionRange parse(String spec) {
        String trimmed = spec.trim();
        if (trimmed.startsWith("^")) {
            return parseCaret(trimmed.substring(1).trim());
        }
        if (trimmed.startsWith("~")) {
            return parseTilde(trimmed.substring(1).trim());
        }
        if (trimmed.startsWith(">=")) {
            int comma = trimmed.indexOf(',');
            if (comma != -1 && trimmed.contains("<")) {
                String lowerStr = trimmed.substring(2, comma).trim();
                String upperStr = trimmed.substring(comma + 1).trim();
                boolean upperInclusive = upperStr.startsWith("<=");
                if (upperInclusive) {
                    upperStr = upperStr.substring(2).trim();
                } else {
                    upperStr = upperStr.substring(1).trim();
                }
                return new VersionRange(trimmed,
                        new VersionBound(Version.parse(lowerStr), true),
                        new VersionBound(Version.parse(upperStr), upperInclusive));
            }
            return new VersionRange(trimmed,
                    new VersionBound(Version.parse(trimmed.substring(2).trim()), true),
                    null);
        }
        if (trimmed.startsWith(">")) {
            return new VersionRange(trimmed,
                    new VersionBound(Version.parse(trimmed.substring(1).trim()), false),
                    null);
        }
        if (trimmed.startsWith("<=")) {
            return new VersionRange(trimmed,
                    null,
                    new VersionBound(Version.parse(trimmed.substring(2).trim()), true));
        }
        if (trimmed.startsWith("<")) {
            return new VersionRange(trimmed,
                    null,
                    new VersionBound(Version.parse(trimmed.substring(1).trim()), false));
        }
        if (trimmed.startsWith("=")) {
            Version v = Version.parse(trimmed.substring(1).trim());
            return new VersionRange(trimmed,
                    new VersionBound(v, true),
                    new VersionBound(v, true));
        }

        Version v = Version.parse(trimmed);
        return new VersionRange(trimmed,
                new VersionBound(v, true),
                new VersionBound(v, true));
    }

    private static VersionRange parseCaret(String ver) {
        Version v = Version.parse(ver);
        Version lower = v;
        Version upper;
        if (v.major() != 0) {
            upper = new Version(v.major() + 1, 0, 0);
        } else if (v.minor() != 0) {
            upper = new Version(0, v.minor() + 1, 0);
        } else {
            upper = new Version(0, 0, v.patch() + 1);
        }
        return new VersionRange("^" + ver,
                new VersionBound(lower, true),
                new VersionBound(upper, false));
    }

    private static VersionRange parseTilde(String ver) {
        Version v = Version.parse(ver);
        return new VersionRange("~" + ver,
                new VersionBound(v, true),
                new VersionBound(new Version(v.major(), v.minor() + 1, 0), false));
    }

    public boolean contains(Version version) {
        if (lower != null) {
            int cmp = version.compareTo(lower.version());
            if (cmp < 0 || (cmp == 0 && !lower.inclusive())) return false;
        }
        if (upper != null) {
            int cmp = version.compareTo(upper.version());
            if (cmp > 0 || (cmp == 0 && !upper.inclusive())) return false;
        }
        return true;
    }

    public String raw() { return raw; }

    @Override
    public String toString() { return raw; }

    public record VersionBound(Version version, boolean inclusive) {}
    public record Version(int major, int minor, int patch) implements Comparable<Version> {
        public static Version parse(String s) {
            String[] parts = s.split("\\.");
            int maj = 0, min = 0, pat = 0;
            try {
                maj = parts.length > 0 ? Integer.parseInt(stripSuffix(parts[0])) : 0;
                min = parts.length > 1 ? Integer.parseInt(stripSuffix(parts[1])) : 0;
                pat = parts.length > 2 ? Integer.parseInt(stripSuffix(parts[2])) : 0;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid version string: " + s, e);
            }
            return new Version(maj, min, pat);
        }

        private static String stripSuffix(String part) {
            int hyphen = part.indexOf('-');
            int plus = part.indexOf('+');
            int cut = -1;
            if (hyphen >= 0 && plus >= 0) cut = Math.min(hyphen, plus);
            else if (hyphen >= 0) cut = hyphen;
            else if (plus >= 0) cut = plus;
            return cut >= 0 ? part.substring(0, cut) : part;
        }

        @Override
        public int compareTo(Version o) {
            int cmp = Integer.compare(this.major, o.major);
            if (cmp != 0) return cmp;
            cmp = Integer.compare(this.minor, o.minor);
            if (cmp != 0) return cmp;
            return Integer.compare(this.patch, o.patch);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionRange that)) return false;
        return raw.equals(that.raw);
    }

    @Override
    public int hashCode() { return raw.hashCode(); }
}
