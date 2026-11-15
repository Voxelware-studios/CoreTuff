package org.voxelware.coretuff.api.platform;

import org.voxelware.coretuff.api.addon.AddonManifest;

import java.util.List;

public interface DependencyResolver {

    ResolutionResult resolve(List<AddonManifest> manifests);

    record ResolutionResult(boolean success, List<AddonManifest> ordered, List<String> errors) {
        public static ResolutionResult ok(List<AddonManifest> ordered) {
            return new ResolutionResult(true, ordered, List.of());
        }
        public static ResolutionResult failed(List<String> errors) {
            return new ResolutionResult(false, List.of(), errors);
        }
    }
}
