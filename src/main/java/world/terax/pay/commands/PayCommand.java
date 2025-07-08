package world.terax.pay.commands;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import world.terax.pay.listeners.WebhookListener;
import world.terax.pay.model.Payment;
import world.terax.pay.service.PaymentService;

public class PayCommand implements CommandExecutor {

    private PaymentService paymentService;
    private WebhookListener webhookListener;

    public PayCommand() {
        this.paymentService = new PaymentService();
        this.webhookListener = new WebhookListener();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Uso: /pay <slug-do-produto> <qrcode/link> [nick]");
            return false;
        }

        String slug = args[0];
        String type = args[1];
        Player player = (args.length == 3) ? sender.getServer().getPlayer(args[2]) : (Player) sender;

        if (player == null) {
            sender.sendMessage("Jogador não encontrado!");
            return false;
        }

        if ("link".equalsIgnoreCase(type)) {
            Payment payment = paymentService.generateLink(slug);
            webhookListener.trackPayment(payment.getQrCode(), player);
            sendClickableLink(player, payment.getQrCode());

        } else if ("pix".equalsIgnoreCase(type)) {
            Payment payment = paymentService.generatePix(slug);
            webhookListener.trackPayment(payment.getQrCode(), player);
            paymentService.sendQRCodeToPlayer(player, payment.getQrCode());

        } else {
            sender.sendMessage("Tipo inválido. Use 'link' ou 'pix'.");
            return false;
        }

        return true;
    }

    private void sendClickableLink(Player player, String link) {
        TextComponent message = new TextComponent("§aClique §a§lAQUI §apara acessar o link no seu navegador.");
        message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));

        player.sendMessage(" ");
        player.spigot().sendMessage(message);
        player.sendMessage(" ");
    }
}
