package mythril.studio;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;


public final class Main extends JavaPlugin implements Listener {
    public static Main plugin;
    public static FileConfiguration config;


    @Override
    public void onEnable() {
        plugin = this;
        plugin.saveDefaultConfig();
        config = this.getConfig();

        Utils.loadData();
        Bukkit.getServer().getPluginManager().registerEvents(new EventListener(), this);
        getLogger().info("\n================================================\n             MythrilBarrelShop включён!\n  Авторы плагина: https://vk.com/mythrilstudio\n================================================");

    }

    @Override
    public void onDisable() {
        Utils.saveData();
        getLogger().info("\n================================================\n             MythrilBarrelShop выключен!\n  Авторы плагина: https://vk.com/mythrilstudio\n================================================");

    }
}
