package world.terax.pay.service;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import world.terax.pay.utils.QRCodeUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

@SuppressWarnings({"CallToPrintStackTrace", "deprecation"})
public class MapService {

    @SuppressWarnings("CallToPrintStackTrace")
    public void createQRCodeMap(Player player, String qrCodeData) {
        try {
            byte[] qrCodeImage = QRCodeUtil.generateQRCode(qrCodeData);

            // Cria novo MapView
            MapView mapView = player.getServer().createMap(player.getWorld());
            mapView.getRenderers().clear();
            mapView.addRenderer(new MapRenderer() {
                @Override
                public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
                    renderQRCode(mapCanvas, qrCodeImage);
                }
            });

            // Cria item de mapa com o ID correspondente ao mapView
            ItemStack mapItem = new ItemStack(Material.MAP, 1, mapView.getId());

            // Entrega ao jogador
            player.getInventory().addItem(mapItem);
            player.sendMessage("Aqui está o QR Code para o pagamento! Use o mapa para escanear.");
        } catch (Exception e) {
            player.sendMessage("Erro ao gerar o QR Code.");
            e.printStackTrace();
        }
    }


    private void renderQRCode(MapCanvas mapCanvas, byte[] qrCodeImage) {
        try {
            BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(qrCodeImage));
            for (int y = 0; y < qrImage.getHeight(); y++) {
                for (int x = 0; x < qrImage.getWidth(); x++) {
                    int rgb = qrImage.getRGB(x, y);
                    Color color = new Color(rgb);
                    if (color.getRed() == 0) {
                        mapCanvas.setPixel(x, y, (byte) 0);
                    } else {
                        mapCanvas.setPixel(x, y, (byte) 15);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    public void updateMapForPaymentApproval(Player player) {
        ItemStack mapItem = new ItemStack(Material.MAP);
        MapView mapView = player.getServer().getMap((byte) -1);

        mapView.getRenderers().clear();
        mapView.addRenderer(new MapRenderer() {
            @Override
            public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
                BufferedImage approvalImage;
                try {
                    approvalImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/approved.png")));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                for (int y = 0; y < approvalImage.getHeight(); y++) {
                    for (int x = 0; x < approvalImage.getWidth(); x++) {
                        int rgb = approvalImage.getRGB(x, y);
                        Color color = new Color(rgb);
                        if (color.getRed() == 0) {
                            mapCanvas.setPixel(x, y, (byte) 0);
                        } else {
                            mapCanvas.setPixel(x, y, (byte) 15);
                        }
                    }
                }
            }
        });

        player.getInventory().addItem(mapItem);
        player.sendMessage("Pagamento aprovado! O mapa foi atualizado.");
    }
}
