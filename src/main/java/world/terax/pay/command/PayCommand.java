package world.terax.pay.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import com.google.gson.*;
import world.terax.pay.TeraxPay;
import world.terax.pay.util.ImageMapRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class PayCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player) || args.length != 2) {
            sender.sendMessage("§cUso: /pay <productSlug> <pix|link>");
            return true;
        }

        Player player = (Player) sender;
        String productSlug = args[0];
        String method = args[1].toLowerCase();

        Bukkit.getScheduler().runTaskAsynchronously(TeraxPay.getInstance(), () -> createInvoice(player, productSlug, method));
        return true;
    }

    private void createInvoice(Player player, String productSlug, String method) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("productSlug", productSlug);
            payload.addProperty("nick", player.getName());
            payload.addProperty("paymentMethod", method);

            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.terax.world/invoices").openConnection();
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

            if (code != 201) {
                player.sendMessage("§cErro ao criar pagamento (HTTP " + code + ")");
                return;
            }

            JsonObject checkout = new JsonParser().parse(responseBody).getAsJsonObject().getAsJsonObject("checkoutData");
            if ("pix".equals(method)) {
                String base64 = checkout.get("qrcode").getAsString();
                Bukkit.getScheduler().runTask(TeraxPay.getInstance(), () -> showQRCodeMap(player, base64));
            } else {
                player.sendMessage("§aSeu link de pagamento: §b" + checkout.get("link").getAsString());
            }
        } catch (Exception e) {
            player.sendMessage("§cErro ao processar pagamento.");
        }
    }

    private void showQRCodeMap(Player player, String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

            MapView map = Bukkit.createMap(player.getWorld());
            map.getRenderers().clear();
            map.addRenderer(new ImageMapRenderer(image));

            ItemStack mapItem = new ItemStack(Material.MAP, 1, map.getId());
            ItemMeta meta = mapItem.getItemMeta();
            meta.setDisplayName("§aQRCODE para pagamento!");
            mapItem.setItemMeta(meta);

            int hotbarSlot = 4;
            player.getInventory().setItem(hotbarSlot, mapItem);

            player.sendMessage("§aQRCODE emitido! caso queira cancelar, apenas clique com o botão direito do mouse.");
        } catch (Exception e) {
            player.sendMessage("§cErro ao carregar QR Code.");
        }
    }

}
