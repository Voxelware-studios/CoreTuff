package org.voxelware.coretuff.dashboard.health;

import org.voxelware.coretuff.CoreTuff;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.logging.Logger;

public final class PerformanceMonitor {

    private static final Logger LOG = Logger.getLogger(PerformanceMonitor.class.getName());

    private static final double DEFAULT_TPS_WARNING = 19.0;
    private static final double DEFAULT_TPS_CRITICAL = 18.0;
    private static final double DEFAULT_MSPT_WARNING = 40.0;
    private static final double DEFAULT_MSPT_CRITICAL = 80.0;
    private static final double DEFAULT_MEMORY_WARNING = 80.0;
    private static final double DEFAULT_MEMORY_CRITICAL = 90.0;
    private static final double DEFAULT_CPU_WARNING = 80.0;
    private static final double DEFAULT_CPU_CRITICAL = 95.0;
    private static final long DEFAULT_WARNING_DURATION_MS = 30_000;
    private static final long DEFAULT_CRITICAL_DURATION_MS = 15_000;

    private final CoreTuff plugin;
    private final IncidentManager incidentManager;

    private double tpsWarning = DEFAULT_TPS_WARNING;
    private double tpsCritical = DEFAULT_TPS_CRITICAL;
    private double msptWarning = DEFAULT_MSPT_WARNING;
    private double msptCritical = DEFAULT_MSPT_CRITICAL;
    private double memoryWarning = DEFAULT_MEMORY_WARNING;
    private double memoryCritical = DEFAULT_MEMORY_CRITICAL;
    private double cpuWarning = DEFAULT_CPU_WARNING;
    private double cpuCritical = DEFAULT_CPU_CRITICAL;
    private long warningDurationMs = DEFAULT_WARNING_DURATION_MS;
    private long criticalDurationMs = DEFAULT_CRITICAL_DURATION_MS;

    private long tpsWarningStart = -1;
    private long tpsCriticalStart = -1;
    private long msptWarningStart = -1;
    private long msptCriticalStart = -1;
    private long memoryWarningStart = -1;
    private long memoryCriticalStart = -1;
    private long cpuWarningStart = -1;
    private long cpuCriticalStart = -1;

    private Incident activePerformanceIncident;

    public PerformanceMonitor(CoreTuff plugin, IncidentManager incidentManager) {
        this.plugin = plugin;
        this.incidentManager = incidentManager;
    }

    public void evaluate() {
        double[] tps = getTps();
        double mspt = getMspt();
        double memoryPercent = getMemoryPercent();
        double cpuPercent = getCpuPercent();

        long now = System.currentTimeMillis();

        tpsWarningStart = trackCondition(tpsWarningStart, tps[0] < tpsWarning, now);
        tpsCriticalStart = trackCondition(tpsCriticalStart, tps[0] < tpsCritical, now);
        msptWarningStart = trackCondition(msptWarningStart, mspt > msptWarning, now);
        msptCriticalStart = trackCondition(msptCriticalStart, mspt > msptCritical, now);
        memoryWarningStart = trackCondition(memoryWarningStart, memoryPercent > memoryWarning, now);
        memoryCriticalStart = trackCondition(memoryCriticalStart, memoryPercent > memoryCritical, now);
        cpuWarningStart = trackCondition(cpuWarningStart, cpuPercent >= 0 && cpuPercent > cpuWarning, now);
        cpuCriticalStart = trackCondition(cpuCriticalStart, cpuPercent >= 0 && cpuPercent > cpuCritical, now);

        IncidentSeverity worstSeverity = evaluateSeverity(now);

        if (worstSeverity != null) {
            if (activePerformanceIncident == null || activePerformanceIncident.isResolved()) {
                String title = "Performance degradation";
                String desc = buildDescription(tps[0], mspt, memoryPercent, cpuPercent, worstSeverity);
                activePerformanceIncident = incidentManager.createIncident(
                        IncidentType.PERFORMANCE, title, worstSeverity, desc);
                addPerformanceSymptoms(activePerformanceIncident, tps[0], mspt, memoryPercent, cpuPercent);
            } else if (worstSeverity.isWorseThan(activePerformanceIncident.getSeverity())) {
                incidentManager.escalateIncident(activePerformanceIncident, worstSeverity,
                        "Escalated to " + worstSeverity.name());
                addPerformanceSymptoms(activePerformanceIncident, tps[0], mspt, memoryPercent, cpuPercent);
            } else {
                addPerformanceSymptoms(activePerformanceIncident, tps[0], mspt, memoryPercent, cpuPercent);
            }
        } else if (activePerformanceIncident != null && !activePerformanceIncident.isResolved()) {
            incidentManager.resolveIncident(activePerformanceIncident, "Performance returned to normal");
            activePerformanceIncident = null;
        }
    }

    private long trackCondition(long start, boolean conditionNow, long now) {
        if (conditionNow) {
            return start == -1 ? now : start;
        }
        return -1;
    }

    private IncidentSeverity evaluateSeverity(long now) {
        boolean anyWarning = false;
        boolean anyCritical = false;

        if (tpsCriticalStart != -1 && (now - tpsCriticalStart) >= criticalDurationMs) anyCritical = true;
        else if (tpsWarningStart != -1 && (now - tpsWarningStart) >= warningDurationMs) anyWarning = true;

        if (msptCriticalStart != -1 && (now - msptCriticalStart) >= criticalDurationMs) anyCritical = true;
        else if (msptWarningStart != -1 && (now - msptWarningStart) >= warningDurationMs) anyWarning = true;

        if (memoryCriticalStart != -1 && (now - memoryCriticalStart) >= criticalDurationMs) anyCritical = true;
        else if (memoryWarningStart != -1 && (now - memoryWarningStart) >= warningDurationMs) anyWarning = true;

        if (cpuCriticalStart != -1 && (now - cpuCriticalStart) >= criticalDurationMs) anyCritical = true;
        else if (cpuWarningStart != -1 && (now - cpuWarningStart) >= warningDurationMs) anyWarning = true;

        if (anyCritical) return IncidentSeverity.CRITICAL;
        if (anyWarning) return IncidentSeverity.WARNING;
        return null;
    }

    private String buildDescription(double tps, double mspt, double mem, double cpu, IncidentSeverity sev) {
        StringBuilder sb = new StringBuilder();
        sb.append(sev.name()).append(" performance issue detected.");
        if (tps < tpsCritical) sb.append(" TPS: ").append(String.format("%.1f", tps)).append(".");
        if (mspt > msptCritical) sb.append(" MSPT: ").append(String.format("%.0f", mspt)).append("ms.");
        if (mem > memoryCritical) sb.append(" Memory: ").append(String.format("%.0f", mem)).append("%.");
        if (cpu >= 0 && cpu > cpuCritical) sb.append(" CPU: ").append(String.format("%.0f", cpu)).append("%.");
        return sb.toString();
    }

    private void addPerformanceSymptoms(Incident incident, double tps, double mspt, double mem, double cpu) {
        incident.addSymptom("tps", String.format("%.1f", tps));
        incident.addSymptom("mspt", String.format("%.0f", mspt) + "ms");
        incident.addSymptom("memory", String.format("%.0f", mem) + "%");
        if (cpu >= 0) {
            incident.addSymptom("cpu", String.format("%.0f", cpu) + "%");
        }
        incident.updateValue("tps", tps);
        incident.updateValue("mspt", mspt);
        incident.updateValue("memory_percent", mem);
        if (cpu >= 0) incident.updateValue("cpu_percent", cpu);
    }

    private double[] getTps() {
        try {
            return plugin.isFolia() ? new double[]{20.0, 20.0, 20.0} : plugin.getServer().getTPS();
        } catch (Exception e) {
            return new double[]{20.0, 20.0, 20.0};
        }
    }

    private double getMspt() {
        try {
            var method = plugin.getServer().getClass().getMethod("getAverageTickTime");
            Object result = method.invoke(plugin.getServer());
            return result instanceof Double d ? d : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double getMemoryPercent() {
        try {
            MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heap = memBean.getHeapMemoryUsage();
            long max = heap.getMax();
            if (max <= 0) return 0;
            return (heap.getUsed() * 100.0) / max;
        } catch (Exception e) {
            return 0;
        }
    }

    private double getCpuPercent() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
                return sunBean.getProcessCpuLoad() * 100;
            }
        } catch (Exception ignored) {}
        return -1;
    }

    public Incident getActivePerformanceIncident() {
        return activePerformanceIncident;
    }
}
