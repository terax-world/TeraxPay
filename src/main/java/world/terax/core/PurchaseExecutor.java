package world.terax.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import world.terax.model.PurchasePayload;

public class PurchaseExecutor {
    public void execute(PurchasePayload payload) {
        Player player = Bukkit.getPlayer(payload.playerNick);
        if (player == null) return;

        for (String cmd : payload.commands) {
            String parsed = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }

        for (String perm : payload.permissions) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "lp user " + player.getName() + " permission settemp " + perm + " true " + payload.durationDays + "d");
        }

        player.sendMessage("§aPagamento aprovado! Produto §e" + payload.productName + " §aadicionado.");
    }
}
