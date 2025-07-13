package world.terax.pay.api;

import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class TitleAPI {
    public static void sendTitle(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        if (title != null) {
            IChatBaseComponent titleComponent = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + title + "\"}");
            PacketPlayOutTitle titlePacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, titleComponent);
            connection.sendPacket(titlePacket);
        }

        if (subtitle != null) {
            IChatBaseComponent subtitleComponent = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + subtitle + "\"}");
            PacketPlayOutTitle subtitlePacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, subtitleComponent);
            connection.sendPacket(subtitlePacket);
        }

        PacketPlayOutTitle timesPacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TIMES, null, fadeInTicks, stayTicks, fadeOutTicks);
        connection.sendPacket(timesPacket);
    }

    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 70, 20);
    }

    public static void broadcastTitle(String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendTitle(player, title, subtitle, fadeInTicks, stayTicks, fadeOutTicks);
        }
    }

    public static void broadcastTitle(String title, String subtitle) {
        broadcastTitle(title, subtitle, 10, 70, 20);
    }
}
