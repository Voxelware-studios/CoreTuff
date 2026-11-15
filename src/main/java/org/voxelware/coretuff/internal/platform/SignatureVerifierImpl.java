package org.voxelware.coretuff.internal.platform;

import org.voxelware.coretuff.api.addon.AddonManifest;
import org.voxelware.coretuff.api.platform.SignatureVerifier;

import java.nio.file.Path;

public class SignatureVerifierImpl implements SignatureVerifier {

    private static final byte[] PUBLIC_KEY_BYTES = new byte[0];

    @Override
    public VerificationResult verify(AddonManifest manifest, Path jarPath) {
        if (!manifest.isSigned()) {
            return VerificationResult.unsigned();
        }

        String algorithm = manifest.signature().algorithm();
        if (!"RSA".equalsIgnoreCase(algorithm)) {
            return VerificationResult.failed("Unsupported signature algorithm: " + algorithm);
        }

        if (PUBLIC_KEY_BYTES.length == 0) {
            return VerificationResult.unsigned();
        }

        try {
            java.security.PublicKey publicKey = readPublicKey();
            java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);

            byte[] jarDigest = computeDigest(jarPath);
            sig.update(jarDigest);

            byte[] signatureValue = java.util.Base64.getDecoder().decode(manifest.signature().value());
            boolean valid = sig.verify(signatureValue);

            if (valid) {
                return VerificationResult.passed("RSA");
            } else {
                return VerificationResult.failed("Signature does not match jar contents");
            }
        } catch (Exception e) {
            return VerificationResult.failed("Verification error: " + e.getMessage());
        }
    }

    private java.security.PublicKey readPublicKey() throws Exception {
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(PUBLIC_KEY_BYTES));
    }

    private byte[] computeDigest(Path jarPath) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        try (java.io.InputStream is = java.nio.file.Files.newInputStream(jarPath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        return md.digest();
    }
}
