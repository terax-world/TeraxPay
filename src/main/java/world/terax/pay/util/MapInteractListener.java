package world.terax.pay.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import world.terax.pay.TeraxPay;

public class MapInteractListener implements Listener {

    private final TeraxPay plugin;

    public MapInteractListener() {
        this.plugin = TeraxPay.getInstance();
    }

    @EventHandler
    public void onMapClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (item == null || item.getType() != Material.MAP) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String displayName = item.getItemMeta().getDisplayName();
        if (!displayName.equalsIgnoreCase(plugin.getConfig().getString("messages.payment-confirmed")) && !displayName.equalsIgnoreCase(plugin.getConfig().getString("messages.qrcode-map-name"))) return;

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                player.getInventory().remove(item);
                break;
            default:
                break;
        }
    }


}
