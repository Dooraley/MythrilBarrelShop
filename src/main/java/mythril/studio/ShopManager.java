package mythril.studio;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mythril.studio.Main.config;


public class ShopManager {

    public static HashMap<Location, String> barrels_list = new HashMap<>();
    public static HashMap<Location, String> use_list = new HashMap<>();
    public static HashMap<Location, Merchant> merchants_guis = new HashMap<>();
    public static HashMap<Location, Inventory> editor_guis = new HashMap<>();
    public static HashMap<Location, String> barrels_backups = new HashMap<>();


    public static void createMerchant(Block barrel_block, String owner, String title) {
        Merchant merchant = Bukkit.createMerchant(Component.text(title));
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("Настройка товара"));

        Material greenMaterial = Material.getMaterial(config.getString("editor_gui.first_item.material", "LIME_STAINED_GLASS_PANE"));
        Material green2Material = Material.getMaterial(config.getString("editor_gui.second_item.material", "LIME_STAINED_GLASS_PANE"));
        Material redMaterial = Material.getMaterial(config.getString("editor_gui.result_item.material", "RED_STAINED_GLASS_PANE"));

        if(greenMaterial == null || green2Material == null){
            greenMaterial = Material.LIME_STAINED_GLASS_PANE;
        }
        if(green2Material == null){
            green2Material = Material.LIME_STAINED_GLASS_PANE;
        }
        if(redMaterial == null){
            redMaterial = Material.RED_STAINED_GLASS_PANE;
        }

        ItemStack green_glass = new ItemStack(greenMaterial);
        ItemStack green2_glass = new ItemStack(green2Material);
        ItemStack red_glass = new ItemStack(redMaterial);

        ItemMeta greenGlassItemMeta = green_glass.getItemMeta();
        greenGlassItemMeta.setCustomModelData(995);
        greenGlassItemMeta.displayName(Component.text(config.getString("editor_gui.first_item.title", "Ошибка названия в конфиг-файле!")));
        List<Component> green_lore = new ArrayList<>();
        for (String lore: config.getStringList("editor_gui.first_item.lore")) {
            green_lore.add(Component.text(lore));
        }
        greenGlassItemMeta.lore(green_lore);
        green_glass.setItemMeta(greenGlassItemMeta);

        ItemMeta green2GlassItemMeta = green2_glass.getItemMeta();
        green2GlassItemMeta.setCustomModelData(995);
        green2GlassItemMeta.displayName(Component.text(config.getString("editor_gui.second_item.title", "Ошибка названия в конфиг-файле!")));
        List<Component> green2_lore = new ArrayList<>();
        for (String lore: config.getStringList("editor_gui.second_item.lore")) {
            green2_lore.add(Component.text(lore));
        }

        green2GlassItemMeta.lore(green2_lore);
        green2_glass.setItemMeta(green2GlassItemMeta);

        green2GlassItemMeta.lore(green2_lore);ItemMeta redGlassItemMeta = red_glass.getItemMeta();
        redGlassItemMeta.setCustomModelData(995);
        redGlassItemMeta.displayName(Component.text(config.getString("editor_gui.result_item.title", "Ошибка названия в конфиг-файле!")));
        List<Component> red_lore = new ArrayList<>();
        for (String lore: config.getStringList("editor_gui.result_item.lore")) {
            red_lore.add(Component.text(lore));
        }
        redGlassItemMeta.lore(red_lore);
        red_glass.setItemMeta(redGlassItemMeta);

        for (int i = 0; i < 9; i++) {
                inventory.setItem(i, green_glass);
                inventory.setItem(i+9, green2_glass);
                inventory.setItem(i+18, red_glass);
        }
        Location barrel_loc = barrel_block.getLocation();
        merchants_guis.put(barrel_loc, merchant);
        editor_guis.put(barrel_loc, inventory);
        barrels_list.put(barrel_loc, owner+"|"+title);
    }
    public static void removeMerchant(Block barrel_block) {
        Location barrel_loc = barrel_block.getLocation();
        if (!barrels_list.containsKey(barrel_loc)) {
            return;
        }
        Inventory editor_gui = editor_guis.get(barrel_loc);
        if (editor_gui == null) {
            return;
        }
        for (ItemStack editor_item : editor_gui.getContents()) {
            if (editor_item == null) {
                continue;
            }
            ItemMeta item_meta = editor_item.getItemMeta();
            if (item_meta.hasCustomModelData() && item_meta.getCustomModelData() == 995) {
                continue;
            }
            barrel_loc.getWorld().dropItemNaturally(barrel_loc, editor_item);
        }
        barrels_list.remove(barrel_loc);
        use_list.remove(barrel_loc);
        merchants_guis.remove(barrel_loc);
        editor_guis.remove(barrel_loc);
    }

    public static @Nullable String  getOwner (Location barrelLoc) {
        if (barrels_list.get(barrelLoc) == null) return null;
        return barrels_list.get(barrelLoc).split("\\|")[0];
    }

    public static String getTitle (Location barrelLoc) {
        return barrels_list.get(barrelLoc).split("\\|")[1];
    }

    public static Merchant getMerchant (Location barrelLoc) {
        return merchants_guis.get(barrelLoc);
    }

    public static void setMerchant(Location barrelLoc) {
        Inventory editorGui = getEditor(barrelLoc);
        Merchant merchantGui = getMerchant(barrelLoc);
        if (editorGui == null || merchantGui == null) {
            return;
        }

        List<MerchantRecipe> merchantRecipes = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
                ItemStack firstItem = editorGui.getItem(i);
                ItemStack secondItem = editorGui.getItem(i + 9);
                ItemStack resultItem = editorGui.getItem(i + 18);
                if (resultItem == null) {
                    continue;
                }
                MerchantRecipe recipe = new MerchantRecipe(resultItem, 1000000);
                recipe.setExperienceReward(false);
                addIngredientIfValid(firstItem, recipe);
                addIngredientIfValid(secondItem, recipe);
                if (resultItem.getItemMeta().hasCustomModelData() && resultItem.getItemMeta().getCustomModelData() == 995) {
                    continue;
                }
                if (!recipe.getIngredients().isEmpty()) {
                    merchantRecipes.add(recipe);
                }
        }

        merchantGui.setRecipes(merchantRecipes);
        merchants_guis.put(barrelLoc, merchantGui);
    }

    public static Inventory getEditor (Location barrel_loc) {
        return editor_guis.get(barrel_loc);
    }
    public static void setEditor (Location barrelLoc, Inventory editor_gui) {
        editor_guis.put(barrelLoc, editor_gui);
        ShopManager.setMerchant(barrelLoc);
    }
    public static void setUsed (Location barrel_loc, @Nullable String player_name) {
        use_list.put(barrel_loc, player_name);
    }
    public static boolean isUsed (Location barrel_loc) {
        return use_list.get(barrel_loc) != null;
    }

    public static void backupBarrel (Location barrelLoc) {
        Inventory barrelInv = ((Barrel) barrelLoc.getBlock().getState()).getInventory();
        String backup = Utils.InventoryToString(barrelInv);
        barrels_backups.put(barrelLoc, backup);
    }
    public static Inventory rollbackBarrel (Location barrelLoc) {
        Inventory restore = Utils.StringToInventory(barrels_backups.get(barrelLoc));
        barrels_backups.remove(barrelLoc);
        return restore;
    }

    public static Location getLocation (String player_name) {
        return use_list.entrySet().stream()
                .filter(e -> player_name.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .findAny()
                .orElse(null);
    }

    private static void addIngredientIfValid(ItemStack item, MerchantRecipe recipe) {
        if (item == null) {
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (!itemMeta.hasCustomModelData()) {
            recipe.addIngredient(item);
            return;
        }
        if (itemMeta.getCustomModelData() != 995) {
            recipe.addIngredient(item);
        }
    }
}
