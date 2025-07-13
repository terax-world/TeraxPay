package world.terax.pay.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import world.terax.pay.TeraxPay;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class MapInteractListener implements Listener {

    private final TeraxPay plugin;

    public MapInteractListener() {
        this.plugin = TeraxPay.getInstance();
    }

//    @EventHandler
//    public void removerDepois(PlayerJoinEvent e){
//        Player player = e.getPlayer();
//        player.setFoodLevel(20);
//        player.setHealth(20);
//    }
//
//    @EventHandler
//    public void removerDepois2(FoodLevelChangeEvent e){
//        e.setCancelled(true);
//    }
//
//    @EventHandler
//    public void removerDepois3(EntityDamageEvent e){
//        e.setCancelled(true);
//    }

    @EventHandler
    public void onMapClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (item == null || item.getType() != Material.MAP) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String displayName = item.getItemMeta().getDisplayName();
        String expectedName = plugin.getConfig().getString("messages.qrcode-map-name");
        if (!displayName.equalsIgnoreCase(expectedName)) return;

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                String invoiceId = null;

                // 🕵️ Extrair invoiceId do lore
                ItemMeta meta = item.getItemMeta();
                if (meta.hasLore()) {
                    for (String line : meta.getLore()) {
                        String stripped = ChatColor.stripColor(line);
                        if (stripped.startsWith("#invoice:")) {
                            invoiceId = stripped.replace("#invoice:", "").trim();
                            break;
                        }
                    }
                }

                // Se achou, cancela a fatura
                if (invoiceId != null) {
                    cancelInvoice(invoiceId);
                    player.sendMessage(ChatColor.RED + plugin.getConfig().getString("messages.qrcode-cancel"));
                } else {
                    plugin.getLogger().warning("[INVOICE] Lore nao contem invoiceId.");
                }

                // Remover mapa e restaurar item anterior
                player.getInventory().remove(item);

                int hotbarSlot = plugin.getConfig().getInt("settings.map-slot", 4);
                ItemStack saved = plugin.getSavedMapItems().remove(player.getUniqueId());
                if (saved != null) {
                    player.getInventory().setItem(hotbarSlot, saved);
                }
                break;

            default:
                break;
        }
    }


    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String display = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        String blockedName = ChatColor.stripColor(plugin.getConfig().getString("messages.qrcode-map-name"));

        if (display.equalsIgnoreCase(blockedName)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String display = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        String blockedName = ChatColor.stripColor(plugin.getConfig().getString("messages.qrcode-map-name"));

        if (display.equalsIgnoreCase(blockedName)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack item = event.getOldCursor();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String display = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        String blockedName = ChatColor.stripColor(plugin.getConfig().getString("messages.qrcode-map-name"));

        if (display.equalsIgnoreCase(blockedName)) {
            event.setCancelled(true);
        }
    }

    private void cancelInvoice(String id) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String endpoint = plugin.getConfig().getString("settings.api-endpoint-invoices");
                HttpURLConnection con = (HttpURLConnection) new URL(endpoint + id).openConnection();
                con.setRequestMethod("DELETE");
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.getInputStream().close();

                plugin.getLogger().info("[INVOICE] Invoice cancelada com sucesso: " + id);
            } catch (Exception e) {
                plugin.getLogger().warning("[INVOICE] Falha ao cancelar invoice " + id + ": " + e.getMessage());
            }
        });
    }

}
