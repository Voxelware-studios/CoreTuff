package org.voxelware.coretuff.internal.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class AddonLoggerFactory {

    private static final Map<String, Logger> loggers = new ConcurrentHashMap<>();

    private AddonLoggerFactory() {}

    public static Logger create(String addonId, String addonName) {
        return loggers.computeIfAbsent(addonId, id -> {
            Logger parent = Logger.getLogger("CoreTuff.Addons");
            Logger logger = new AddonLogger(addonId, addonName, parent.getParent() != null ? parent.getParent() : parent);
            logger.setLevel(parent.getLevel());
            logger.setUseParentHandlers(true);
            return logger;
        });
    }

    public static Logger get(String addonId) {
        return loggers.get(addonId);
    }

    private static class AddonLogger extends Logger {
        private final String addonId;
        private final String addonName;

        AddonLogger(String addonId, String addonName, Logger parent) {
            super("CoreTuff.Addons." + addonId, null);
            this.addonId = addonId;
            this.addonName = addonName;
            setParent(parent);
        }

        @Override
        public void log(LogRecord record) {
            record.setMessage("[" + addonName + "] " + record.getMessage());
            super.log(record);
        }
    }
}
