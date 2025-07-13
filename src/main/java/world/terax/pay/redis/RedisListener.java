package world.terax.pay.redis;

import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import world.terax.pay.TeraxPay;
import world.terax.pay.util.ImageMapRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

@SuppressWarnings({"deprecation", "CallToPrintStackTrace"})
public class RedisListener extends JedisPubSub implements Runnable {

    private final TeraxPay plugin = TeraxPay.getInstance();

    private volatile boolean running = true;
    private Jedis jedis;

    @Override
    public void onMessage(String channel, String message) {
        if (!running || !TeraxPay.getInstance().isEnabled()) return;

        plugin.getLogger().info("[REDIS] Mensagem recebida no canal: " + channel);

        if (!channel.equals(plugin.getConfig().getString("settings.redis-channel", "invoice:update"))) {
            plugin.getLogger().info("[REDIS] Canal ignorado: " + channel);
            return;
        }

        plugin.getLogger().info("[REDIS] Conteúdo da mensagem: " + message);

        try {
            JsonObject data = new JsonParser().parse(message).getAsJsonObject();

            String status = data.get("status").getAsString();
            String nick = data.get("nick").getAsString();
            String expiration = data.has("expiration") ? data.get("expiration").getAsString() : "";

            plugin.getLogger().info("[REDIS] status=" + status + ", nick=" + nick + ", expiration=" + expiration);

            if (!"approved".equalsIgnoreCase(status)) {
                plugin.getLogger().info("[REDIS] Status diferente de 'approved', ignorando.");
                return;
            }

            // Adicionando log aqui antes de processar o comando
            plugin.getLogger().info("[REDIS] Processando comandos para o jogador: " + nick);

            JsonArray commands = data.has("commands") ? data.getAsJsonArray("commands") : new JsonArray();
            JsonArray commandsRemove = data.has("commandsRemove") ? data.getAsJsonArray("commandsRemove") : new JsonArray();

            plugin.getLogger().info("[REDIS] Comandos para executar: " + commands);
            plugin.getLogger().info("[REDIS] Comandos para remover: " + commandsRemove);

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayerExact(nick);
                if (player == null) {
                    plugin.getLogger().warning("[REDIS] Jogador não encontrado: " + nick);
                    return;
                }

                // Comando para o jogador
                for (JsonElement cmd : commands) {
                    String command = cmd.getAsString().replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    plugin.getLogger().info("[REDIS] Comando executado: " + command);
                }

                long delayTicks = parseExpirationToTicks(expiration);
                plugin.getLogger().info("[REDIS] Delay para remover comandos em ticks: " + delayTicks);

                // Se houver comandos para remover após um delay
                if (delayTicks > 0 && commandsRemove.size() > 0) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Player p = Bukkit.getPlayerExact(nick);
                        if (p == null) {
                            plugin.getLogger().warning("[REDIS] Jogador não encontrado para remoção: " + nick);
                            return;
                        }

                        for (JsonElement cmd : commandsRemove) {
                            String commandRemove = cmd.getAsString().replace("%player%", p.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandRemove);
                            plugin.getLogger().info("[REDIS] Comando de remoção executado: " + commandRemove);
                        }
                    }, delayTicks);
                }

                // Log de imagem (imagem aprovada)
                try {
                    File file = new File(plugin.getDataFolder(), "approved.png");
                    if (!file.exists()) {
                        plugin.getLogger().warning("[REDIS] Arquivo 'approved.png' não encontrado.");
                        return;
                    }

                    BufferedImage img = ImageIO.read(file);
                    MapView view = Bukkit.createMap(player.getWorld());
                    view.getRenderers().clear();
                    view.addRenderer(new ImageMapRenderer(img));

                    int hotbarSlot = plugin.getConfig().getInt("settings.map-slot", 4);

                    ItemStack mapItem = new ItemStack(Material.MAP, 1, view.getId());
                    ItemMeta meta = mapItem.getItemMeta();
                    meta.setDisplayName(plugin.getConfig().getString("messages.payment-confirmed"));
                    mapItem.setItemMeta(meta);

                    player.getInventory().setItem(hotbarSlot, mapItem);
                    player.sendMessage(plugin.getConfig().getString("messages.payment-confirmed"));
                    plugin.getLogger().info("[REDIS] Mapa com imagem 'approved.png' enviado para: " + nick);
                } catch (Exception e) {
                    plugin.getLogger().severe("[REDIS] Erro ao aplicar imagem 'approved.png': " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("[REDIS] Erro ao processar a mensagem: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        String host = plugin.getConfig().getString("settings.redis.host");
        int port = plugin.getConfig().getInt("settings.redis.port");
        String password = plugin.getConfig().getString("settings.redis.password");
        String channel = plugin.getConfig().getString("settings.redis-channel");

        plugin.getLogger().info("[REDIS] Conectando ao Redis: host=" + host + ", port=" + port + ", channel=" + channel);

        try (Jedis jedis = new Jedis(host, port)) {
            jedis.auth(password);
            plugin.getLogger().info("[REDIS] Autenticado com sucesso no Redis.");
            jedis.subscribe(this, channel);
            plugin.getLogger().info(plugin.getConfig().getString("messages.redis-connected"));
        } catch (Exception e) {
            plugin.getLogger().severe("[REDIS] Erro ao conectar ao Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void stop(){
        running = false;
        try {
            if (jedis != null && jedis.isConnected()) {
                jedis.close();
            }

            this.unsubscribe();
        } catch (Exception ignored){

        }
    }

    private long parseExpirationToTicks(String expiration) {
        try {
            if (expiration == null || expiration.isEmpty()) return 0;

            long multiplier;
            char unit = Character.toUpperCase(expiration.charAt(expiration.length() - 1));
            int amount = Integer.parseInt(expiration.substring(0, expiration.length() - 1));

            plugin.getLogger().info("[REDIS] Convertendo expiração para ticks: " + expiration + " -> " + amount + " " + unit);

            switch (unit) {
                case 'M': // minutos
                    multiplier = 60L * 20L;
                    break;
                case 'H': // horas
                    multiplier = 60L * 60L * 20L;
                    break;
                case 'D': // dias
                    multiplier = 24L * 60L * 60L * 20L;
                    break;
                default:
                    return 0;
            }

            return amount * multiplier;
        } catch (Exception e) {
            plugin.getLogger().severe("[REDIS] Erro ao calcular expiração: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }


}
