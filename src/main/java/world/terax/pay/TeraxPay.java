package world.terax.pay;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import world.terax.pay.command.PayCommand;
import world.terax.pay.redis.RedisListener;
import world.terax.pay.util.MapInteractListener;

public class TeraxPay extends JavaPlugin {
    @Getter
    private static TeraxPay instance;

    @Override
    public void onEnable() {
        instance = this;

        getCommand("pay").setExecutor(new PayCommand());
        saveResource("approved.png", false);

        getServer().getPluginManager().registerEvents(new MapInteractListener(), this);
        Bukkit.getScheduler().runTaskAsynchronously(this, new RedisListener());
    }

    @Override
    public void onDisable() {
    }

}
