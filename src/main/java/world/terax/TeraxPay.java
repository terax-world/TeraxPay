package world.terax;

import org.bukkit.plugin.java.JavaPlugin;
import world.terax.command.PayCommand;
import world.terax.core.PluginSocketHandler;
import world.terax.core.WebSocketClientHandler;
import world.terax.service.PurchaseService;

import java.net.URISyntaxException;

public final class TeraxPay extends JavaPlugin {

    private static TeraxPay instance;
    private WebSocketClientHandler socketClient;
    private PluginSocketHandler socketHandler;
    private PurchaseService purchaseService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        purchaseService = new PurchaseService();

        String socketUrl = getConfig().getString("socket-url");
        if (socketUrl == null || socketUrl.isEmpty()) {
            getLogger().severe("[Terax] socket-url está ausente no config.yml");
            return;
        }

        try {
            socketHandler = new PluginSocketHandler(socketUrl, purchaseService);
            socketHandler.connect();
        } catch (URISyntaxException e) {
            getLogger().severe("[Terax] URL do WebSocket inválida: " + e.getMessage());
        }

        socketClient = new WebSocketClientHandler(socketUrl);
        socketClient.connect();

        this.getCommand("pay").setExecutor(new PayCommand());

        getLogger().info("[TeraxPay] iniciado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (socketClient != null && socketClient.client.isOpen()) {
            socketClient.close();
        }
    }

    public static TeraxPay getInstance() {
        return instance;
    }

    public PurchaseService getPurchaseService() {
        return purchaseService;
    }
}
