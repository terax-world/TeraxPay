package world.terax.core;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import world.terax.service.PurchaseService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class PluginSocketHandler extends WebSocketClient {

    private final PurchaseService purchaseService;
    private final Gson gson = new Gson();

    public PluginSocketHandler(String uri, PurchaseService purchaseService) throws URISyntaxException {
        super(new URI(uri));
        this.purchaseService = purchaseService;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Bukkit.getLogger().info("[TeraxPlugin] WebSocket conectado com a API");
    }

    @Override
    public void onMessage(String message) {
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("TeraxPlugin"), () -> {
            // Exemplo do JSON que você deve receber:
            // { "type": "purchase_approved", "playerId": "uuid-do-jogador" }

            try {
                PluginMessage msg = gson.fromJson(message, PluginMessage.class);

                if ("purchase_approved".equalsIgnoreCase(msg.getType())) {
                    UUID playerUUID = UUID.fromString(msg.getPlayerId());
                    Player player = Bukkit.getPlayer(playerUUID);

                    if (player != null && player.isOnline()) {
                        purchaseService.markPaymentApproved(player);
                    } else {
                        Bukkit.getLogger().warning("[TeraxPlugin] Jogador não encontrado ou offline: " + playerUUID);
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[TeraxPlugin] Erro ao processar mensagem do WebSocket: " + e.getMessage());
            }
        });
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Bukkit.getLogger().info("[TeraxPlugin] WebSocket desconectado: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        Bukkit.getLogger().severe("[TeraxPlugin] Erro no WebSocket: " + ex.getMessage());
    }

    // Classe interna para deserializar mensagem JSON
    private static class PluginMessage {
        private String type;
        private String playerId;

        public String getType() {
            return type;
        }

        public String getPlayerId() {
            return playerId;
        }
    }
}
