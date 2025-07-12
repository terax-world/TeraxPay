package world.terax.pay.redis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

    @Override
    public void onMessage(String channel, String message) {
        if (!"invoice:update".equals(channel)) return;

        JsonObject data = new JsonParser().parse(message).getAsJsonObject();

        String status = data.get("status").getAsString();
        String nick = data.get("nick").getAsString();
        JsonArray commands = data.has("commands") ? data.getAsJsonArray("commands") : new JsonArray();
        JsonArray permissions = data.has("permissions") ? data.getAsJsonArray("permissions") : new JsonArray();


        if (!"approved".equalsIgnoreCase(status)) {
            return;
        } else {
            Bukkit.getScheduler().runTask(TeraxPay.getInstance(), () -> {

                Player player = Bukkit.getPlayerExact(nick);
                if (player == null) return;

                for (JsonElement cmd : commands){
                    String command = cmd.getAsString().replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }

                for (JsonElement perm : permissions){
                    String permission = perm.getAsString();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "lp user " + player.getName() + " permission set" + permission);

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "pex user " + player.getName() + " add " + permission);
                }


                try {
                    File file = new File(TeraxPay.getInstance().getDataFolder(), "approved.png");
                    if (!file.exists()) return;

                    BufferedImage img = ImageIO.read(file);
                    MapView view = Bukkit.createMap(player.getWorld());
                    view.getRenderers().clear();
                    view.addRenderer(new ImageMapRenderer(img));

                    ItemStack mapItem = new ItemStack(Material.MAP, 1, view.getId());
                    ItemMeta meta = mapItem.getItemMeta();
                    meta.setDisplayName("§aPagamento confirmado!");
                    mapItem.setItemMeta(meta);

                    int hotbarSlot = 4;
                    player.getInventory().setItem(hotbarSlot, mapItem);

                    player.sendMessage("§aPagamento confirmado!");
                } catch (Exception ignored) {
                }
            });
        }
    }

    @Override
    public void run() {
        try (Jedis jedis = new Jedis("redis-19647.c308.sa-east-1-1.ec2.redns.redis-cloud.com", 19647)) {
            jedis.auth("odOmwuzBID24UTE6rb9ABCLvoFKtbnzD");
            jedis.subscribe(this, "invoice:update");
            TeraxPay.getInstance().getLogger().info("Conectado ao Redis com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            TeraxPay.getInstance().getLogger().severe("Erro ao conectar Redis: " + e);
        }
    }
}
