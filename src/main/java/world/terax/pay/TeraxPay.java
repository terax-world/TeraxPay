package world.terax.pay;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import world.terax.pay.command.PayCommand;
import world.terax.pay.redis.RedisListener;
import world.terax.pay.util.MapInteractListener;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeraxPay extends JavaPlugin {

    @Getter
    private static TeraxPay instance;

    private RedisListener redisListener;

    private final Map<UUID, ItemStack> savedMapItems = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        getCommand("pay").setExecutor(new PayCommand());

        getServer().getPluginManager().registerEvents(new MapInteractListener(), this);

        redisListener = new RedisListener();
        redisListener.start();
    }

    @Override
    public void onDisable() {
        if (redisListener != null) {
            redisListener.stop();
        }
    }

    public Map<UUID, ItemStack> getSavedMapItems(){
        return savedMapItems;
    }

}
