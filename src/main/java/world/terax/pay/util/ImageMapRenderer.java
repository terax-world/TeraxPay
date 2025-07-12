package world.terax.pay.util;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.*;
import java.awt.image.BufferedImage;

@SuppressWarnings("deprecation")
public class ImageMapRenderer extends MapRenderer {
    private final BufferedImage image;
    private boolean rendered = false;

    public ImageMapRenderer(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        if (rendered) return;

        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                int rgb = image.getRGB(x * image.getWidth() / 128, y * image.getHeight() / 128);
                canvas.setPixel(x, y, MapPalette.matchColor(new Color(rgb, true)));
            }
        }
        rendered = true;
    }
}
