package world.terax.pay;

import org.bukkit.plugin.java.JavaPlugin;
import world.terax.pay.commands.PayCommand;
import world.terax.pay.listeners.WebhookListener;

public class TeraxPay extends JavaPlugin {

    private WebhookListener webhookListener;

    @Override
    public void onEnable() {
        this.getCommand("pay").setExecutor(new PayCommand());
        webhookListener = new WebhookListener();
    }

    @Override
    public void onDisable() {

    }
}
