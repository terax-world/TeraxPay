package world.terax.pay.redis;

import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import world.terax.pay.TeraxPay;
import world.terax.pay.api.TitleAPI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"CallToPrintStackTrace"})
public class RedisListener extends JedisPubSub implements Runnable {

    private final TeraxPay plugin = TeraxPay.getInstance();

    private Thread listenerThread;

    private volatile boolean running = true;
    private Jedis jedis;
    private final Set<String> processedInvoices = ConcurrentHashMap.newKeySet();


    @Override
    public void onMessage(String channel, String message) {
        if (!running || !TeraxPay.getInstance().isEnabled()) return;

        plugin.getLogger().info("[REDIS] Mensagem recebida no canal: " + channel);

        if (!channel.equals(plugin.getConfig().getString("settings.redis-channel", "invoice:update"))) {
            plugin.getLogger().info("[REDIS] Canal ignorado: " + channel);
            return;
        }

        plugin.getLogger().info("[REDIS] Conteudo da mensagem: " + message);

        try {
            JsonParser parser = new JsonParser();
            JsonElement parsed = parser.parse(message);

            if (parsed.isJsonPrimitive() && parsed.getAsJsonPrimitive().isString()) {
                parsed = parser.parse(parsed.getAsString());
            }

            JsonObject data = parsed.getAsJsonObject();

            String id = data.get("id").getAsString();
            if (processedInvoices.contains(id)) {
                plugin.getLogger().info("[REDIS] Invoice ja processada: " + id);
                return;
            }
            processedInvoices.add(id);

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
                    plugin.getLogger().warning("[REDIS] Jogador nao encontrado: " + nick);
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
                            plugin.getLogger().warning("[REDIS] Jogador nao encontrado para remocao: " + nick);
                            return;
                        }

                        for (JsonElement cmd : commandsRemove) {
                            String commandRemove = cmd.getAsString().replace("%player%", p.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandRemove);
                            plugin.getLogger().info("[REDIS] Comando de remocao executado: " + commandRemove);
                        }

                        markInvoiceAsFinished(id);
                    }, delayTicks);
                }

                try {
                    TitleAPI.sendTitle(
                            player,
                            plugin.getConfig().getString("messages.message-agradecimento.title"),
                            plugin.getConfig().getString("messages.message-agradecimento.subtitle")
                    );

                    ItemStack item = player.getItemInHand();

                    if (item == null || item.getType() != Material.MAP) return;
                    if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

                    String displayName = item.getItemMeta().getDisplayName();
                    if (!displayName.equalsIgnoreCase(plugin.getConfig().getString("messages.qrcode-map-name"))) return;

                    // Remover o mapa
                    player.getInventory().removeItem(item);

                    // Restaurar o item anterior se existir
                    ItemStack backup = plugin.getSavedMapItems().remove(player.getUniqueId());
                    int hotbarSlot = plugin.getConfig().getInt("settings.map-slot", 4);

                    if (backup != null) {
                        player.getInventory().setItem(hotbarSlot, backup);
                    }

                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
        } catch (Exception e) {
            plugin.getLogger().severe("[REDIS] Erro ao processar a mensagem: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void start() {
        running = true;
        listenerThread = new Thread(this, "Redis-Listener-Thread");
        listenerThread.start();
    }


    @Override
    public void run() {
        String host = plugin.getConfig().getString("settings.redis.host");
        int port = plugin.getConfig().getInt("settings.redis.port");
        String password = plugin.getConfig().getString("settings.redis.password");
        String channel = plugin.getConfig().getString("settings.redis-channel");

        plugin.getLogger().info("[REDIS] Conectando ao Redis: host=" + host + ", port=" + port + ", channel=" + channel);

        try {
            jedis = new Jedis(host, port);
            jedis.auth(password);
            plugin.getLogger().info("[REDIS] Autenticado com sucesso no Redis.");
            jedis.subscribe(this, channel); // BLOQUEIA AQUI
            plugin.getLogger().info(plugin.getConfig().getString("messages.redis-connected"));
        } catch (Exception e) {
        } finally {
            if (jedis != null) {
                try {
                    jedis.close();
                } catch (Exception ignored) {

                }
            }
        }
    }



    public void stop() {
        running = false;
        try {
            this.unsubscribe();
            if (jedis != null && jedis.isConnected()) {
                jedis.close();
            }
            plugin.getLogger().info("[REDIS] RedisListener parado com sucesso.");
        } catch (Exception e) {
            plugin.getLogger().warning("[REDIS] Erro ao parar o RedisListener: " + e.getMessage());
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

    private void markInvoiceAsFinished(String id) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String apiUrl = plugin.getConfig().getString("settings.api-endpoint-invoices") + id + "/finish";
                HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
                con.setRequestMethod("POST");
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.getInputStream().close();

                plugin.getLogger().info("[REDIS] Invoice marcada como finalizada na API: " + id);
            } catch (Exception e) {
                plugin.getLogger().warning("[REDIS] Erro ao marcar invoice como finalizada: " + e.getMessage());
            }
        });
    }



}
