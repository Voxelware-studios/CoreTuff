package org.voxelware.coretuff.internal.addon;

import org.voxelware.coretuff.api.addon.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ManifestParser {

    private ManifestParser() {}

    public static AddonManifest parse(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry("addon.yml");
            if (entry == null) {
                return null;
            }
            try (InputStream in = jar.getInputStream(entry)) {
                Map<String, Object> raw = loadFlat(in);
                return buildManifest(raw);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse addon.yml from " + jarPath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadFlat(InputStream in) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        try (Scanner scanner = new Scanner(in)) {
            String currentKey = null;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.isBlank() || line.trim().startsWith("#")) continue;
                if (!line.startsWith(" ") && !line.startsWith("\t")) {
                    int colon = line.indexOf(':');
                    if (colon != -1) {
                        currentKey = line.substring(0, colon).trim();
                        String value = line.substring(colon + 1).trim();
                        if (!value.isEmpty()) {
                            map.put(currentKey, parseValue(value));
                        } else {
                            map.put(currentKey, "");
                        }
                    }
                } else {
                    if (currentKey != null) {
                        int colon = line.indexOf(':');
                        if (colon != -1) {
                            String subKey = line.substring(0, colon).trim();
                            String value = line.substring(colon + 1).trim();
                            if (!value.isEmpty()) {
                                Object existing = map.get(currentKey);
                                if (existing instanceof Map) {
                                    ((Map<String, Object>) existing).put(subKey, parseValue(value));
                                } else {
                                    Map<String, Object> subMap = new LinkedHashMap<>();
                                    subMap.put(subKey, parseValue(value));
                                    map.put(currentKey, subMap);
                                }
                            }
                        }
                    }
                }
            }
        }
        return map;
    }

    private static Object parseValue(String value) {
        if (value.startsWith("[") && value.endsWith("]")) {
            String inner = value.substring(1, value.length() - 1).trim();
            if (inner.isEmpty()) return List.of();
            return Arrays.asList(inner.split(",\\s*"));
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static AddonManifest buildManifest(Map<String, Object> raw) {
        String name = (String) raw.get("name");
        String id = (String) raw.get("id");
        String version = (String) raw.get("version");
        String author = (String) raw.get("author");
        String description = (String) raw.get("description");
        String website = (String) raw.get("website");
        String coretuffVersion = (String) raw.get("coretuffVersion");

        List<String> deps = listOrEmpty(raw.get("dependencies"));
        List<String> optDeps = listOrEmpty(raw.get("optionalDependencies"));

        Set<Capability> capabilities = new HashSet<>();
        Object capsRaw = raw.get("requestedCapabilities");
        if (capsRaw instanceof List) {
            for (Object c : (List<Object>) capsRaw) {
                try {
                    capabilities.add(Capability.valueOf(c.toString().toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        SignatureInfo signature = null;
        Object sigRaw = raw.get("signature");
        if (sigRaw instanceof Map) {
            Map<String, Object> sigMap = (Map<String, Object>) sigRaw;
            String algo = (String) sigMap.get("algorithm");
            String val = (String) sigMap.get("value");
            if (algo != null && val != null) {
                signature = new SignatureInfo(algo, val);
            }
        }

        TrustLevel trustLevel = signature != null ? TrustLevel.TRUST_INTERNAL : TrustLevel.TRUST_PUBLIC;

        return new AddonManifest(name, id, version, author, description, website,
                coretuffVersion, deps, optDeps, capabilities, signature, trustLevel);
    }

    @SuppressWarnings("unchecked")
    private static List<String> listOrEmpty(Object obj) {
        if (obj instanceof List) return (List<String>) obj;
        if (obj instanceof String && !((String) obj).isBlank()) return List.of((String) obj);
        return List.of();
    }
}
