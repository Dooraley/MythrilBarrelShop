package mythril.studio;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.logging.Level;

import static mythril.studio.Main.plugin;
import static mythril.studio.ShopManager.*;

public class Utils {

    public static HashMap<String, String> barrels_data = new HashMap<>();
    public static HashMap<String, String> editors_data = new HashMap<>();

    public static void saveData() {
        try {
            File editorGuiFile = new File(plugin.getDataFolder(), "editor_guis.dat");
            File barrelsListFile = new File(plugin.getDataFolder(), "barrels_list.dat");
            if (!editorGuiFile.exists()) {
                boolean result = editorGuiFile.createNewFile();
                if (!result) {
                    plugin.getLogger().log(Level.SEVERE, "Ошибка создания файла данных editor_guis.dat");
                }
            }
            if (!barrelsListFile.exists()) {
                boolean result = barrelsListFile.createNewFile();
                if (!result) {
                    plugin.getLogger().log(Level.SEVERE, "Ошибка создания файла данных editor_guis.dat");
                }
            }
            ObjectOutputStream editorGuiOOS = new ObjectOutputStream(Files.newOutputStream(editorGuiFile.toPath()));
            ObjectOutputStream barrelsListOOS = new ObjectOutputStream(Files.newOutputStream(barrelsListFile.toPath()));
            for (Location loc : editor_guis.keySet()) {
                String world = loc.getWorld().getName();
                double x = loc.getX();
                double y = loc.getY();
                double z = loc.getZ();
                String locationString = world + "," + x + "," + y + "," + z;
                String invString = InventoryToString(editor_guis.get(loc));
                editors_data.put(locationString, invString);
            }
            for (Location loc : barrels_list.keySet()) {
                String world = loc.getWorld().getName();
                double x = loc.getX();
                double y = loc.getY();
                double z = loc.getZ();
                String locationString = world + "," + x + "," + y + "," + z;
                String name = barrels_list.get(loc);
                barrels_data.put(locationString, name);
            }
            editorGuiOOS.writeObject(editors_data);
            barrelsListOOS.writeObject(barrels_data);
            editorGuiOOS.close();
            barrelsListOOS.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadData() {
        try {
            File editorGuiFile = new File(plugin.getDataFolder(), "editor_guis.dat");
            File barrelsListFile = new File(plugin.getDataFolder(), "barrels_list.dat");
            if (!editorGuiFile.exists()) {
                boolean result = editorGuiFile.createNewFile();
                if (!result) {
                    plugin.getLogger().log(Level.SEVERE, "Ошибка создания файла данных editor_guis.dat");
                }
            }
            if (!barrelsListFile.exists()) {
                boolean result = barrelsListFile.createNewFile();
                if (!result) {
                    plugin.getLogger().log(Level.SEVERE, "Ошибка создания файла данных editor_guis.dat");
                }
            }

            if (editorGuiFile.length() == 0 || barrelsListFile.length() == 0) return;
            ObjectInputStream editorGuiOIS = new ObjectInputStream(Files.newInputStream(editorGuiFile.toPath()));
            editors_data = (HashMap<String, String>) editorGuiOIS.readObject();
            editorGuiOIS.close();
            for (String locString : editors_data.keySet()) {
                String[] locArray = locString.split(",");
                Location loc = new Location(Bukkit.getWorld(locArray[0]), Double.parseDouble(locArray[1]), Double.parseDouble(locArray[2]), Double.parseDouble(locArray[3]));
                Inventory inv = StringToInventory(editors_data.get(locString));
                editor_guis.put(loc, inv);
            }
            ObjectInputStream barrelsListOIS = new ObjectInputStream(Files.newInputStream(barrelsListFile.toPath()));
            barrels_data = (HashMap<String, String>) barrelsListOIS.readObject();
            barrelsListOIS.close();
            for (String locString : barrels_data.keySet()) {
                String[] locArray = locString.split(",");
                Location loc = new Location(Bukkit.getWorld(locArray[0]), Double.parseDouble(locArray[1]), Double.parseDouble(locArray[2]), Double.parseDouble(locArray[3]));
                String name = barrels_data.get(locString);
                barrels_list.put(loc, name);
                @NotNull Merchant merchant = Bukkit.createMerchant(Component.text(getTitle(loc)));
                merchants_guis.put(loc, merchant);
                setMerchant(loc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String InventoryToString(Inventory inventory) {
        int size = inventory.getSize();
        StringBuilder inv_string = new StringBuilder(size + ";");
        for (ItemStack item : inventory) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
                dataOutput.writeObject(item);
                dataOutput.close();
                inv_string.append(Base64Coder.encodeLines(outputStream.toByteArray())).append(";");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Невозможно выполнить десериализацию предметов. Если Вы не знаете причину возникновения ошибки, обратитесь к автору плагина.");
            }
        }
        return inv_string.toString();
    }

    public static Inventory StringToInventory(String invString) {
        String[] serializedBlocks = invString.split(";");
        int size = Integer.parseInt(serializedBlocks[0]);
        Inventory inventory = Bukkit.createInventory(null, size, Component.text("§8Настройка товара"));
        for (int i = 1; i < serializedBlocks.length; i++) {
            ItemStack item = null;
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(serializedBlocks[i]));
                BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
                item = (ItemStack) dataInput.readObject();
                dataInput.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            inventory.setItem(i - 1, item == null ? new ItemStack(Material.AIR) : item);
        }
        return inventory;
    }
}