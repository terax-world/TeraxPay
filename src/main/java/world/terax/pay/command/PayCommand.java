package world.terax.pay.command;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;
import com.google.gson.*;

import world.terax.pay.TeraxPay;
import world.terax.pay.util.ImageMapRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class PayCommand implements CommandExecutor {

    private final TeraxPay plugin;

    public PayCommand() {
        this.plugin = TeraxPay.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player) || args.length != 2) {
            sender.sendMessage(plugin.getConfig().getString("messages.usage"));
            return true;
        }

        Player player = (Player) sender;
        String productSlug = args[0];
        String method = args[1].toLowerCase();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> createInvoice(player, productSlug, method));
        return true;
    }

    private void createInvoice(Player player, String productSlug, String method) {
        try {
            String apiUrl = plugin.getConfig().getString("settings.api-endpoint-invoices");

            JsonObject payload = new JsonObject();
            payload.addProperty("productSlug", productSlug);
            payload.addProperty("nick", player.getName());
            payload.addProperty("paymentMethod", method);

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream is = (code < 400) ? conn.getInputStream() : conn.getErrorStream();
            String responseBody = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));

            if (code < 200 || code >= 300) {
                String msg = plugin.getConfig().getString("messages.error-http").replace("%code%", String.valueOf(code));
                player.sendMessage(msg);

                plugin.getLogger().warning("[HTTP] Erro ao criar pagamento: codigo " + code);
                plugin.getLogger().warning("[HTTP] Resposta da API: " + responseBody);
                plugin.getLogger().warning("[HTTP] Payload enviado: " + payload.toString());

                return;
            }


            JsonObject checkout = new JsonParser().parse(responseBody).getAsJsonObject().getAsJsonObject("checkoutData");

            String invoiceId = checkout.get("invoiceId").getAsString();

            if ("pix".equals(method)) {
                String base64 = checkout.get("qrcode").getAsString();
                Bukkit.getScheduler().runTask(plugin, () -> showQRCodeMap(player, base64, invoiceId));
            }
            else {
                String link = checkout.get("link").getAsString();

                FileConfiguration config = plugin.getConfig();
                String text = config.getString("messages.payment-link.text");
                String clickable = config.getString("messages.payment-link.clickable");
                String hover = config.getString("messages.payment-link.hover");

                TextComponent component = new TextComponent(text.replace("%clickable%", clickable));
                component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
                component.setHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(hover).create()
                ));

                player.spigot().sendMessage(component);
            }


        } catch (Exception e) {
            player.sendMessage(plugin.getConfig().getString("messages.error-generic"));
        }
    }

    private void showQRCodeMap(Player player, String base64, String invoiceId) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

            MapView map = Bukkit.createMap(player.getWorld());
            map.getRenderers().clear();
            map.addRenderer(new ImageMapRenderer(image));

            int hotbarSlot = plugin.getConfig().getInt("settings.map-slot", 4);

            // Salvar o item atual do slot, se existir
            ItemStack previous = player.getInventory().getItem(hotbarSlot);
            if (previous != null && previous.getType() != Material.AIR) {
                plugin.getSavedMapItems().put(player.getUniqueId(), previous.clone());
            }

            ItemStack mapItem = new ItemStack(Material.MAP, 1, map.getId());
            ItemMeta meta = mapItem.getItemMeta();
            meta.setDisplayName(plugin.getConfig().getString("messages.qrcode-map-name"));

            // Lore com o invoiceId oculto
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "#invoice:" + invoiceId);
            meta.setLore(lore);

            mapItem.setItemMeta(meta);
            player.getInventory().setItem(hotbarSlot, mapItem);

            player.sendMessage(plugin.getConfig().getString("messages.qrcode-ready"));
            player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 1.0f, 1.0f);

        } catch (Exception e) {
            player.sendMessage(plugin.getConfig().getString("messages.qrcode-error"));
            plugin.getLogger().severe("[MAP] Erro ao mostrar mapa: " + e.getMessage());
            e.printStackTrace();
        }
    }


}