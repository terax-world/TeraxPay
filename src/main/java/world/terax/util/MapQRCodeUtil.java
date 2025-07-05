package world.terax.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

@SuppressWarnings("deprecation")
public class MapQRCodeUtil {

    // Dá o mapa com QR code para o jogador e retorna o mapId gerado
    public static short giveMapWithQRCode(Player player, String base64Image) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

            MapView map = Bukkit.createMap(player.getWorld());
            map.getRenderers().clear();
            map.addRenderer(new QRMapRenderer(image));

            short mapId = map.getId();

            ItemStack mapItem = new ItemStack(Material.MAP, 1, mapId);
            MapMeta meta = (MapMeta) mapItem.getItemMeta();

            if (meta != null) {
                meta.setDisplayName("§aQR Code Pix");
                mapItem.setItemMeta(meta);
            }

            player.getInventory().addItem(mapItem);

            return mapId;
        } catch (Exception e) {
            player.sendMessage("§cErro ao gerar o QR Code em mapa: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    // Atualiza o mapa existente para mostrar o "V" verde de pagamento aprovado
    public static void updateMapToV(Player player, short mapId) {
        MapView mapView = Bukkit.getMap(mapId);
        if (mapView == null) {
            player.sendMessage("§cMapa não encontrado para atualizar!");
            return;
        }

        mapView.getRenderers().clear();
        mapView.addRenderer(new MapVRenderer());

        // Envia o mapa atualizado para o jogador
        player.sendMap(mapView);
    }

    // Renderer do QR code
    private static class QRMapRenderer extends MapRenderer {
        private final BufferedImage image;
        private boolean rendered = false;

        public QRMapRenderer(BufferedImage image) {
            super(false);
            this.image = resizeImage(image, 128, 128);
        }

        @Override
        public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
            if (rendered) return;
            mapCanvas.drawImage(0, 0, image);
            rendered = true;
        }

        private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
            Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
            BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = outputImage.createGraphics();
            g2d.drawImage(resultingImage, 0, 0, null);
            g2d.dispose();
            return outputImage;
        }
    }

    // Renderer do "V" verde para pagamento aprovado
    private static class MapVRenderer extends MapRenderer {
        private boolean rendered = false;

        @Override
        public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
            if (rendered) return;

            // Limpa o mapa pintando tudo branco (cor 0)
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    mapCanvas.setPixel(x, y, (byte) 0); // Branco no mapa 1.8
                }
            }

            byte green = 34; // Cor verde no palette do mapa 1.8

            // Desenha o "V" (ajuste posições se quiser)
            mapCanvas.setPixel(50, 50, green);
            mapCanvas.setPixel(51, 51, green);
            mapCanvas.setPixel(52, 52, green);
            mapCanvas.setPixel(53, 51, green);
            mapCanvas.setPixel(54, 50, green);

            rendered = true;
        }
    }

    public static void removeQRCodeMapFromInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.MAP) {
                MapMeta meta = (MapMeta) item.getItemMeta();
                if (meta != null && meta.getDisplayName() != null && meta.getDisplayName().contains("QR Code Pix")) {
                    player.getInventory().remove(item);
                    break;
                }
            }
        }
    }

}
