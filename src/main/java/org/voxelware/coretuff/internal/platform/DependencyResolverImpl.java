package org.voxelware.coretuff.internal.platform;

import org.voxelware.coretuff.api.addon.AddonManifest;
import org.voxelware.coretuff.api.platform.DependencyResolver;

import java.util.*;

public class DependencyResolverImpl implements DependencyResolver {

    @Override
    public ResolutionResult resolve(List<AddonManifest> manifests) {
        List<String> errors = new ArrayList<>();
        Map<String, AddonManifest> byId = new LinkedHashMap<>();
        for (AddonManifest m : manifests) {
            byId.put(m.id(), m);
        }

        List<AddonManifest> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> inProgress = new HashSet<>();

        for (AddonManifest m : manifests) {
            if (!visited.contains(m.id())) {
                try {
                    topoSort(m.id(), byId, visited, inProgress, sorted, errors);
                } catch (CycleDetectedException e) {
                    errors.add("Circular dependency detected: " + e.getMessage());
                }
            }
        }

        for (AddonManifest m : manifests) {
            for (String dep : m.dependencies()) {
                if (!byId.containsKey(dep)) {
                    errors.add("Missing dependency '" + dep + "' required by addon '" + m.id() + "'");
                }
            }
        }

        if (!errors.isEmpty()) {
            return ResolutionResult.failed(errors);
        }
        return ResolutionResult.ok(sorted);
    }

    private void topoSort(String id, Map<String, AddonManifest> byId,
                          Set<String> visited, Set<String> inProgress,
                          List<AddonManifest> sorted, List<String> errors) {
        if (inProgress.contains(id)) {
            throw new CycleDetectedException(id);
        }
        if (visited.contains(id)) return;

        AddonManifest manifest = byId.get(id);
        if (manifest == null) return;

        inProgress.add(id);
        for (String dep : manifest.dependencies()) {
            topoSort(dep, byId, visited, inProgress, sorted, errors);
        }
        inProgress.remove(id);
        visited.add(id);
        sorted.add(manifest);
    }

    private static class CycleDetectedException extends RuntimeException {
        CycleDetectedException(String id) {
            super(id);
        }
    }
}
