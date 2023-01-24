package mythril.studio;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class EventListener implements Listener {

    @EventHandler
    public void onBarrelPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if(block.getType() != Material.BARREL) return;

        ItemStack item = event.getItemInHand();
        if(!item.hasItemMeta()) return;

        ItemMeta itemMeta = item.getItemMeta();
        if(!itemMeta.hasDisplayName()) return;

        TextComponent item_name = (TextComponent) itemMeta.displayName();
        if (item_name == null) return;

        if (item_name.children().size()>0) item_name = ((TextComponent) item_name.children().get(0));
        Player player = event.getPlayer();

        String barrel_name = item_name.content() + " §7(" + player.getName() + ")";
        if(!barrel_name.toLowerCase().contains("магазин")) return;

        String player_nickname = player.getName();
        ShopManager.createMerchant(block, player_nickname, barrel_name);
    }

    @EventHandler
    public void onBarrelBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if(block.getType() != Material.BARREL) return;

        Location barrelLoc = block.getLocation();
        String owner = ShopManager.getOwner(barrelLoc);
        if (owner == null) return;

        Player player = event.getPlayer();
        if(player.getName().equals(owner)) {
            if (ShopManager.isUsed(barrelLoc)) {
                event.setCancelled(true);
                return;
            }
            ShopManager.removeMerchant(block);
            return;
        }
        event.setCancelled(true);
        player.sendMessage("§4Только §6" + owner + " §4может ломать свой магазин!");
    }


    @EventHandler
    public void onOpenMerchant(PlayerInteractEvent event) {
        if (event.getAction().isLeftClick()) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BARREL) return;

        Player player = event.getPlayer();
        if (player.isSneaking()) return;

        Location barrel_loc = block.getLocation();
        Merchant merchant = ShopManager.getMerchant(barrel_loc);
        if (merchant == null) return;

        event.setCancelled(true);
        if (ShopManager.isUsed(barrel_loc)) {
            event.setCancelled(true);
            player.sendMessage("§4Этот магазин в данный момент используется кем-то другим!");
            return;
        }

        player.openMerchant(merchant, true);
        ShopManager.setUsed(barrel_loc, player.getName());
        ShopManager.backupBarrel(barrel_loc);
    }
    @EventHandler
    public void onCloseMerchant(InventoryCloseEvent event) {
        if (!event.getInventory().getType().equals(InventoryType.MERCHANT)) return;
        String playerName = event.getPlayer().getName();
        Location blockLoc = ShopManager.getLocation(playerName);
        if (blockLoc == null) return;

        ShopManager.setUsed(blockLoc, null);
    }

    @EventHandler
    public void onOpenEditor(PlayerInteractEvent event) {

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        Location barrelLoc = block.getLocation();
        if (!player.isSneaking()) return;

        String owner = ShopManager.getOwner(barrelLoc);
        if (owner == null) return;

        Inventory editorGUI = ShopManager.getEditor(barrelLoc);
        if (editorGUI == null) return;

        if (!owner.equals(player.getName())){
            event.setCancelled(true);
            player.sendMessage("§4Только §6" + owner + " §4может управлять данным магазином!");
            return;
        }
        if (ShopManager.isUsed(barrelLoc)) {
            event.setCancelled(true);
            player.sendMessage("§4Этот магазин в данный момент используется кем-то другим!");
            return;
        }
        if (!event.getAction().isLeftClick()) return;
        event.setCancelled(true);

        ShopManager.setUsed(barrelLoc, player.getName());
        player.openInventory(editorGUI);
    }

    @EventHandler
    public void onSaveEditor(InventoryCloseEvent event) {
        HumanEntity player = event.getPlayer();

        Location barrelLoc = ShopManager.getLocation(player.getName());
        if (barrelLoc == null) return;

        Inventory editorGUI = event.getInventory();
        if (!editorGUI.getType().equals(InventoryType.CHEST)) return;

        ShopManager.setEditor(barrelLoc, event.getInventory());
        ShopManager.setUsed(barrelLoc, null);
    }

    @EventHandler
    public void DropWhileBuy(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Location barrelLoc = ShopManager.getLocation(player.getName());
        if (barrelLoc == null) return;
        event.setCancelled(true);
    }
    @EventHandler
    public void PickupWhileBuy(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        Location barrel_loc = ShopManager.getLocation(player.getName());
        if (barrel_loc == null) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void checkGlassPaneItem(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        Location barrelLocation = ShopManager.getLocation(event.getWhoClicked().getName());
        if (barrelLocation == null) return;

        ItemMeta clickedItemMeta = clickedItem.getItemMeta();
        if (clickedItemMeta == null) return;

        if (!clickedItemMeta.hasCustomModelData()) return;

        int customModelData = clickedItemMeta.getCustomModelData();
        if (customModelData != 995) return;

        Inventory inventory = event.getInventory();
        clickedItem.setType(Material.AIR);
        event.setCancelled(true);
        inventory.setItem(event.getSlot(), clickedItem);
        event.getWhoClicked().sendMessage("§2Теперь поместите в этот слот необходимый предмет!");
    }


    @EventHandler
    public void buyResultEvent(InventoryClickEvent event) {
        Inventory merchantInv = event.getInventory();
        if (!merchantInv.getType().equals(InventoryType.MERCHANT)) return;
        if (event.getSlot() != 2) return;

        Player player = (Player) event.getWhoClicked();
        Location barrelLoc = ShopManager.getLocation(player.getName());
        if (barrelLoc == null) return;

        Inventory barrelInv = ((Barrel) barrelLoc.getBlock().getState()).getInventory();

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        MerchantRecipe merchantRecipe = ((MerchantInventory) merchantInv).getSelectedRecipe();
        if (merchantRecipe == null) return;

        if (event.isShiftClick()) {
            player.sendMessage("§4Чтобы совершить сделаку, перетащите предмет в свой инвентарь!");
            event.setCancelled(true);
            return;
        }


        if (barrelInv.isEmpty() || !barrelInv.containsAtLeast(clickedItem, clickedItem.getAmount())) {

            player.sendMessage("§4В магазине недостаточно ресурсов для обмена!");
            event.setCancelled(true);
            player.closeInventory();

            String ownerNickname = ShopManager.getOwner(barrelLoc);
            if (ownerNickname == null) return;
            Player owner = Bukkit.getPlayer(ownerNickname);
            if (owner == null) return;
            int X = (int) barrelLoc.getX();
            int Y = (int) barrelLoc.getY();
            int Z = (int) barrelLoc.getZ();
            String locString = X + ", " + Y + ", " + Z;
            owner.sendMessage("§6В вашем магазине §7("+ locString +") §6не хватает некоторых ресурсов для обмена!");
            return;
        }

        @NotNull List<ItemStack> recipeItems = merchantRecipe.getIngredients();
        for (ItemStack recipeItem : recipeItems) {
            @NotNull HashMap<Integer, ItemStack> tryToAdd = barrelInv.addItem(recipeItem);
            if (tryToAdd.isEmpty()) continue;
            event.setCancelled(true);
            player.sendMessage("§4В магазине недостаточно свободного места для хранения необходимых для обмена ресурсов!");
            player.closeInventory();
            String ownerNickname = ShopManager.getOwner(barrelLoc);
            if (ownerNickname == null) return;
            Player owner = Bukkit.getPlayer(ownerNickname);
            if (owner == null) return;
            int X = (int) barrelLoc.getX();
            int Y = (int) barrelLoc.getY();
            int Z = (int) barrelLoc.getZ();
            String locString = X + ", " + Y + ", " + Z;
            owner.sendMessage("§6В вашем ма §7("+ locString +") §6недостаточно свободного места для хранения необходимых для обмена ресурсов!");
            barrelInv.clear();
            Inventory restoredBarrel = ShopManager.rollbackBarrel(barrelLoc);
            barrelInv.setContents(restoredBarrel.getContents());
            return;
        }
        barrelInv.removeItemAnySlot(clickedItem);
        ShopManager.backupBarrel(barrelLoc);
    }

    @EventHandler
    public void barrelExplodeEvent(EntityExplodeEvent event) {
        @NotNull List<Block> explodeBlocks = event.blockList();
        for (Block block : explodeBlocks) {
            @NotNull Location barrelLoc = block.getLocation();
            if (ShopManager.getOwner(barrelLoc) == null) continue;
            event.blockList().remove(block);
        }
    }

    @EventHandler
    public void onItemMove(InventoryMoveItemEvent event) {
        Inventory barrelInv = event.getSource();
        Location barrelLoc = barrelInv.getLocation();
        String barrelOwner = ShopManager.getOwner(barrelLoc);
        if (barrelOwner != null) event.setCancelled(true);
    }
}
