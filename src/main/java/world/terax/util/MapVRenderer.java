package world.terax.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class MapVRenderer extends MapRenderer {

    private boolean rendered = false;

    @Override
    public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
        if (rendered) return;

        // Limpa o mapa pintando tudo com branco (cor 0)
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                mapCanvas.setPixel(x, y, (byte) 0); // 0 = branco no mapa 1.8
            }
        }

        byte green = 34; // Cor verde no palette do mapa 1.8 (pode ajustar)

        // Desenha o V (ajuste a posição se quiser)
        mapCanvas.setPixel(50, 50, green);
        mapCanvas.setPixel(51, 51, green);
        mapCanvas.setPixel(52, 52, green);
        mapCanvas.setPixel(53, 51, green);
        mapCanvas.setPixel(54, 50, green);

        rendered = true;
    }

    public static void giveMapWithV(Player player) {
        MapView mapView = player.getServer().createMap(player.getWorld());
        mapView.getRenderers().clear();
        mapView.addRenderer(new MapVRenderer());

        ItemStack mapItem = new ItemStack(Material.MAP, 1);
        // Define a ID do mapa para a durabilidade do item no 1.8
        mapItem.setDurability(mapView.getId());

        player.getInventory().addItem(mapItem);
    }
}
