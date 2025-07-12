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
        if (!channel.equals(plugin.getConfig().getString("settings.redis-channel", "invoice:update"))) return;

        JsonObject data = new JsonParser().parse(message).getAsJsonObject();

        String status = data.get("status").getAsString();
        String nick = data.get("nick").getAsString();
        JsonArray commands = data.has("commands") ? data.getAsJsonArray("commands") : new JsonArray();
        JsonArray permissions = data.has("permissions") ? data.getAsJsonArray("permissions") : new JsonArray();

        if (!"approved".equalsIgnoreCase(status)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayerExact(nick);
            if (player == null) return;

            for (JsonElement cmd : commands) {
                String command = cmd.getAsString().replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }

            for (JsonElement perm : permissions) {
                String permission = perm.getAsString();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "lp user " + player.getName() + " permission set" + permission);

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "pex user " + player.getName() + " add " + permission);
            }

            try {
                File file = new File(plugin.getDataFolder(), "approved.png");
                if (!file.exists()) return;

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
            } catch (Exception ignored) {}
        });
    }

    @Override
    public void run() {
        String host = plugin.getConfig().getString("settings.redis.host");
        int port = plugin.getConfig().getInt("settings.redis.port");
        String password = plugin.getConfig().getString("settings.redis.password");
        String channel = plugin.getConfig().getString("settings.redis-channel");

        try (Jedis jedis = new Jedis(host, port)) {
            jedis.auth(password);
            jedis.subscribe(this, channel);
            plugin.getLogger().info(plugin.getConfig().getString("messages.redis-connected"));
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe(plugin.getConfig().getString("messages.redis-error") + ": " + e.getMessage());
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
}
