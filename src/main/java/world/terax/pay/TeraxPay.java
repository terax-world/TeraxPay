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

        saveDefaultConfig();

        getCommand("pay").setExecutor(new PayCommand());
        saveResource("approved.png", false);

        getServer().getPluginManager().registerEvents(new MapInteractListener(), this);

        new Thread(() -> {
            try {
                RedisListener listener = new RedisListener();
                listener.run();
            } catch (Exception e) {
                getLogger().severe("Erro ao iniciar o RedisListener: " + e.getMessage());
            }
        }, "Redis-Listener-Thread").start();
    }

    @Override
    public void onDisable() {
        RedisListener redisListener = new RedisListener();
        if (redisListener != null) {
            redisListener.stop();
        }
    }

}
