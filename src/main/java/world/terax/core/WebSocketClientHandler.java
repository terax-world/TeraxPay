package world.terax.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import world.terax.TeraxPay;
import world.terax.model.PluginMessage;
import world.terax.util.MapQRCodeUtil;
import world.terax.util.MapVRenderer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class WebSocketClientHandler {

    private final String url;
    public WebSocketClient client;

    public WebSocketClientHandler(String url) {
        this.url = url;
    }

    public void connect() {
        try {
            client = new WebSocketClient(new URI(url)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    Bukkit.getLogger().info("[Terax] Conectado ao WebSocket com sucesso.");
                }

                @Override
                public void onMessage(String message) {
                    Bukkit.getScheduler().runTask(TeraxPay.getInstance(), () -> {
                        handlePluginMessage(message);
                        try {
                            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                            String type = json.get("type").getAsString();

                            if (type.equals("purchase_approved")) {
                                String playerIdStr = json.get("playerId").getAsString();
                                UUID playerId = UUID.fromString(playerIdStr);

                                for (Player online : Bukkit.getOnlinePlayers()) {
                                    if (online.getUniqueId().equals(playerId)) {
                                        online.sendMessage("§aPagamento aprovado com sucesso!");
                                        MapVRenderer.giveMapWithV(online);
                                        break;
                                    }
                                }
                            }

                        } catch (Exception e) {
                            Bukkit.getLogger().severe("[Terax] Erro ao processar mensagem WebSocket: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Bukkit.getLogger().warning("[Terax] WebSocket desconectado: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    Bukkit.getLogger().severe("[Terax] Erro no WebSocket: " + ex.getMessage());
                }
            };

            client.connect();
        } catch (URISyntaxException e) {
            Bukkit.getLogger().severe("[Terax] URL WebSocket inválida: " + e.getMessage());
        }
    }

    public void close() {
        if (client != null && client.isOpen()) {
            client.close();
        }
    }

    private void handlePluginMessage(String json) {
        try {
            Gson gson = new Gson();
            PluginMessage message = gson.fromJson(json, PluginMessage.class);

            Player player = Bukkit.getPlayerExact(message.getPlayerNick());
            if (player == null) return;

            // Remover QR Code e dar o mapa com o V
            MapQRCodeUtil.removeQRCodeMapFromInventory(player);
            MapVRenderer.giveMapWithV(player);

            player.sendMessage("§aPagamento aprovado! Obrigado por comprar " + message.getProductName());

        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§c[Terax] Erro ao processar mensagem do plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
