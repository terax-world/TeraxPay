package world.terax.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import world.terax.service.PurchaseService;

public class PayCommand implements CommandExecutor {

    private final PurchaseService purchaseService = new PurchaseService();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Comando apenas para jogadores.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 2) {
            player.sendMessage("§cUso correto: /pay <produto> <pix|credit>");
            return true;
        }

        String productSlug = args[0];
        String method = args[1].toLowerCase();

        if (!method.equals("pix") && !method.equals("credit")) {
            player.sendMessage("§cMétodo inválido. Use pix ou credit.");
            return true;
        }

        player.sendMessage("§7Criando sua compra...");
        purchaseService.createPurchase(player, productSlug, method);
        return true;
    }

}
