package org.voxelware.coretuff.dashboard;

import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.dashboard.server.HttpServerManager;
import org.voxelware.coretuff.dashboard.auth.JwtProvider;
import org.voxelware.coretuff.dashboard.auth.PairingService;
import org.voxelware.coretuff.dashboard.auth.AuthMiddleware;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry;
import org.voxelware.coretuff.dashboard.settings.SettingsUtil;
import org.voxelware.coretuff.dashboard.endpoints.PairEndpoints;
import org.voxelware.coretuff.dashboard.endpoints.ConfigEndpoints;
import org.voxelware.coretuff.dashboard.endpoints.SystemEndpoints;
import org.voxelware.coretuff.dashboard.endpoints.ConsoleEndpoints;
import org.voxelware.coretuff.dashboard.endpoints.ModerationEndpoints;
import org.voxelware.coretuff.dashboard.endpoints.PlayerEndpoints;
import org.voxelware.coretuff.dashboard.players.PlayerDataService;

import org.voxelware.coretuff.dashboard.websocket.WebSocketManager;
import org.voxelware.coretuff.dashboard.audit.AuditService;
import org.voxelware.coretuff.dashboard.metrics.MetricsCollector;
import org.voxelware.coretuff.dashboard.console.ConsoleLogCollector;
import org.voxelware.coretuff.dashboard.health.HealthMonitor;

import java.util.logging.Level;

public final class DashboardModule {

    private static DashboardModule instance;

    private final CoreTuff plugin;
    private DashboardConfig config;
    private HttpServerManager httpServer;
    private JwtProvider jwtProvider;
    private PairingService pairingService;
    private AuthMiddleware authMiddleware;
    private SettingsRegistry settingsRegistry;
    private SettingsUtil settingsUtil;
    private AuditService auditService;
    private MetricsCollector metricsCollector;
    private ConsoleLogCollector consoleLogCollector;
    private WebSocketManager webSocketManager;
    private PlayerDataService playerDataService;

    private boolean running;
    private DashboardState state = DashboardState.STOPPED;

    public enum DashboardState {
        RUNNING,
        STOPPED,
        DISABLED,
        ERROR,
        STARTING
    }

    public DashboardModule(CoreTuff plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static DashboardModule getInstance() {
        return instance;
    }

    public synchronized void start() {
        if (running) {
            plugin.getLogger().warning("Dashboard module is already running.");
            return;
        }
        state = DashboardState.STARTING;
        try {
            this.config = new DashboardConfig();
            var cs = plugin.getConfigurationService();
            if (cs != null) config.setConfigurationService(cs);
            if (!config.isEnabled()) {
                plugin.getLogger().info("Dashboard module is disabled in config.");
                state = DashboardState.DISABLED;
                return;
            }

            this.auditService = new AuditService();
            this.settingsRegistry = new SettingsRegistry();
            this.settingsUtil = new SettingsUtil(plugin, plugin.getConfigurationService());
            this.jwtProvider = new JwtProvider(config.getJwtSecret());
            this.pairingService = new PairingService(jwtProvider, config);
            this.metricsCollector = new MetricsCollector(plugin);
            this.consoleLogCollector = new ConsoleLogCollector(plugin, 5000);
            this.authMiddleware = new AuthMiddleware(jwtProvider, config);
            this.webSocketManager = new WebSocketManager(jwtProvider, config, settingsRegistry);

            this.httpServer = new HttpServerManager(config, authMiddleware, webSocketManager);
            registerEndpoints();
            httpServer.start();

            metricsCollector.start();
            webSocketManager.startHeartbeat();

            HealthMonitor editorHealthMonitor = plugin.getEditorModule() != null
                    ? plugin.getEditorModule().getHealthMonitor() : null;
            if (editorHealthMonitor != null) {
                editorHealthMonitor.addListener(report -> webSocketManager.broadcastHealthUpdate(report));
            }

            running = true;
            state = DashboardState.RUNNING;
            plugin.getLogger().info("Dashboard module started on port " + config.getPort());
        } catch (Exception e) {
            state = DashboardState.ERROR;
            var im = getIncidentManager();
            if (im != null) {
                im.createIncident(
                    org.voxelware.coretuff.dashboard.health.IncidentType.DASHBOARD,
                    "Dashboard failed to start",
                    org.voxelware.coretuff.dashboard.health.IncidentSeverity.CRITICAL,
                    "Error: " + e.getMessage()
                );
            }
            plugin.getLogger().log(Level.SEVERE, "Failed to start dashboard module", e);
        }
    }

    public synchronized void stop() {
        if (!running) return;
        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
            if (webSocketManager != null) {
                webSocketManager.stop();
            }
            if (httpServer != null) {
                httpServer.stop();
            }
            if (pairingService != null) {
                pairingService.cleanup();
            }
            running = false;
            state = DashboardState.STOPPED;
            plugin.getLogger().info("Dashboard module stopped.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error stopping dashboard module", e);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public DashboardState getState() {
        return state;
    }

    public int getPort() {
        return config != null ? config.getPort() : -1;
    }

    public DashboardConfig getConfig() {
        return config;
    }

    public JwtProvider getJwtProvider() {
        return jwtProvider;
    }

    public PairingService getPairingService() {
        return pairingService;
    }

    public AuthMiddleware getAuthMiddleware() {
        return authMiddleware;
    }

    public SettingsRegistry getSettingsRegistry() {
        return settingsRegistry;
    }

    public SettingsUtil getSettingsUtil() {
        return settingsUtil;
    }

    public AuditService getAuditService() {
        return auditService;
    }

    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    public HealthMonitor getHealthMonitor() {
        var em = plugin.getEditorModule();
        return em != null ? em.getHealthMonitor() : null;
    }

    private org.voxelware.coretuff.dashboard.health.IncidentManager getIncidentManager() {
        var em = plugin.getEditorModule();
        return em != null && em.getHealthMonitor() != null ? em.getHealthMonitor().getIncidentManager() : null;
    }

    public ConsoleLogCollector getConsoleLogCollector() {
        return consoleLogCollector;
    }

    public WebSocketManager getWebSocketManager() {
        return webSocketManager;
    }

    public HttpServerManager getHttpServer() {
        return httpServer;
    }

    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }

    public void setPlayerDataService(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
    }

    public CoreTuff getPlugin() {
        return plugin;
    }

    private void registerEndpoints() {
        var pairEndpoints = new PairEndpoints(this);
        var configEndpoints = new ConfigEndpoints(this);
        var systemEndpoints = new SystemEndpoints(this);
        var consoleEndpoints = new ConsoleEndpoints(this);
        var moderationEndpoints = new ModerationEndpoints(this);
        httpServer.registerEndpoint("/api/v1/pair/code", pairEndpoints);
        httpServer.registerEndpoint("/api/v1/pair/exchange", pairEndpoints);
        httpServer.registerEndpoint("/api/v1/pair/status/", pairEndpoints);
        httpServer.registerEndpoint("/api/v1/schema", configEndpoints);
        httpServer.registerEndpoint("/api/v1/settings", configEndpoints);
        httpServer.registerEndpoint("/api/v1/reload", configEndpoints);
        httpServer.registerEndpoint("/api/v1/modules", systemEndpoints);
        httpServer.registerEndpoint("/api/v1/version", systemEndpoints);
        httpServer.registerEndpoint("/api/v1/metrics", systemEndpoints);
        httpServer.registerEndpoint("/api/v1/server", systemEndpoints);
        httpServer.registerEndpoint("/api/v1/health", systemEndpoints);
        httpServer.registerEndpoint("/api/v1/health/full", systemEndpoints);
        httpServer.registerEndpoint("/api/v1/health/plugins", systemEndpoints);
        httpServer.registerEndpoint("/api/v1/health/integrations", systemEndpoints);
        httpServer.registerEndpoint("/api/v1/console/logs", consoleEndpoints);
        httpServer.registerEndpoint("/api/v1/console/execute", consoleEndpoints);
        httpServer.registerEndpoint("/api/v1/moderation/summary", moderationEndpoints);
        httpServer.registerEndpoint("/api/v1/moderation/punishments", moderationEndpoints);
        httpServer.registerEndpoint("/api/v1/moderation/punishments/", moderationEndpoints);
        httpServer.registerEndpoint("/api/v1/moderation/players/", moderationEndpoints);
        httpServer.registerEndpoint("/api/v1/moderation/moderators", moderationEndpoints);
        var playerEndpoints = new PlayerEndpoints(this);
        httpServer.registerEndpoint("/api/v1/players", playerEndpoints);
    }
}
