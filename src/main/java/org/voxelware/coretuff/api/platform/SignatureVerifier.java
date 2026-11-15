package org.voxelware.coretuff.api.platform;

import org.voxelware.coretuff.api.addon.AddonManifest;

import java.nio.file.Path;

public interface SignatureVerifier {

    VerificationResult verify(AddonManifest manifest, Path jarPath);

    record VerificationResult(boolean valid, String algorithm, String detail) {
        public static VerificationResult passed(String algorithm) {
            return new VerificationResult(true, algorithm, "Signature verified");
        }
        public static VerificationResult failed(String detail) {
            return new VerificationResult(false, null, detail);
        }
        public static VerificationResult unsigned() {
            return new VerificationResult(true, null, "Unsigned addon — no verification performed");
        }
    }
}
