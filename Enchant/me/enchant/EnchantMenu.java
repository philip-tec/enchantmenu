package me.enchant;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

public class EnchantMenu implements Listener, CommandExecutor {

    private final Map<UUID, List<EnchantOption>> selectedEnchants = new HashMap<>();
    private final Map<UUID, ItemStack> originalArmor = new HashMap<>();
    
    private final Map<UUID, ItemStack> existingHelmet = new HashMap<>();
    private final Map<UUID, ItemStack> existingChestplate = new HashMap<>();
    private final Map<UUID, ItemStack> existingLeggings = new HashMap<>();
    private final Map<UUID, ItemStack> existingBoots = new HashMap<>();
    private Economy econ;

    public EnchantMenu() {
        setupEconomy();
    }

    private FileConfiguration getConfig() {
        return EnchantPlugin.getInstance().getConfig();
    }

    private String getMenuTitle() {
        return ChatColor.translateAlternateColorCodes('&', 
            getConfig().getString("menu.title", "&5Enchant Menu"));
    }

    private void setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        econ = rsp.getProvider();
    }

    private enum EnchantOption {
        UNBREAKING_1("Unbreaking I", Enchantment.DURABILITY, 1, 300),
        UNBREAKING_2("Unbreaking II", Enchantment.DURABILITY, 2, 600),
        UNBREAKING_3("Unbreaking III", Enchantment.DURABILITY, 3, 1200),
        SHARPNESS_1("Sharpness I", Enchantment.DAMAGE_ALL, 1, 250),
        SHARPNESS_2("Sharpness II", Enchantment.DAMAGE_ALL, 2, 500),
        SHARPNESS_3("Sharpness III", Enchantment.DAMAGE_ALL, 3, 750),
        SHARPNESS_4("Sharpness IV", Enchantment.DAMAGE_ALL, 4, 1000),
        FIRE_ASPECT_1("Fire Aspect I", Enchantment.FIRE_ASPECT, 1, 2000),
        POWER_1("Power I", Enchantment.ARROW_DAMAGE, 1, 250),
        POWER_2("Power II", Enchantment.ARROW_DAMAGE, 2, 500),
        POWER_3("Power III", Enchantment.ARROW_DAMAGE, 3, 750),
        POWER_4("Power IV", Enchantment.ARROW_DAMAGE, 4, 1000),
        FLAME_1("Flame I", Enchantment.ARROW_FIRE, 1, 1800),
        PROTECTION_1("Protection I", Enchantment.PROTECTION_ENVIRONMENTAL, 1, 250),
        PROTECTION_2("Protection II", Enchantment.PROTECTION_ENVIRONMENTAL, 2, 500),
        PROTECTION_3("Protection III", Enchantment.PROTECTION_ENVIRONMENTAL, 3, 750),
        PROTECTION_4("Protection IV", Enchantment.PROTECTION_ENVIRONMENTAL, 4, 1200),
        FIRE_PROTECTION_1("Fire Protection I", Enchantment.PROTECTION_FIRE, 1, 750);

        private final String name;
        private final Enchantment enchantment;
        private final int level;
        private final int price;

        EnchantOption(String name, Enchantment enchantment, int level, int price) {
            this.name = name;
            this.enchantment = enchantment;
            this.level = level;
            this.price = price;
        }

        public String getName() { return name; }
        public Enchantment getEnchantment() { return enchantment; }
        public int getLevel() { return level; }
        public int getPrice() { return price; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("enchantreload")) {
            if (!sender.hasPermission("enchant.reload")) {
                sender.sendMessage(ChatColor.RED + "Du har ikke tilladelse til at bruge denne kommando.");
                return true;
            }
            
            EnchantPlugin.getInstance().reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "EnchantPlugin config reloaded!");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onRightClickNPC(PlayerInteractEntityEvent e) {
        Entity npc = e.getRightClicked();
        Player player = e.getPlayer();

        if (!npc.hasMetadata("NPC")) return;

        String rawName = npc.getCustomName() != null ? npc.getCustomName() : npc.getName();
        String stripped = ChatColor.stripColor(rawName);

        if (!stripped.equalsIgnoreCase("Enchant")) return;

        ItemStack held = player.getItemInHand();
        if (!isEnchantable(held)) {
            String msg = getConfig().getString("messages.no-item-held", "&cDu skal holde et item du vil enchante.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            return;
        }

        e.setCancelled(true);
        selectedEnchants.putIfAbsent(player.getUniqueId(), new ArrayList<>());
        
        String itemType = held.getType().name();
        if (itemType.contains("HELMET") || itemType.contains("CHESTPLATE") ||
            itemType.contains("LEGGINGS") || itemType.contains("BOOTS")) {
            
            originalArmor.put(player.getUniqueId(), held.clone());
            
            
            if (itemType.contains("HELMET") && player.getInventory().getHelmet() != null) {
                existingHelmet.put(player.getUniqueId(), player.getInventory().getHelmet().clone());
            }
            if (itemType.contains("CHESTPLATE") && player.getInventory().getChestplate() != null) {
                existingChestplate.put(player.getUniqueId(), player.getInventory().getChestplate().clone());
            }
            if (itemType.contains("LEGGINGS") && player.getInventory().getLeggings() != null) {
                existingLeggings.put(player.getUniqueId(), player.getInventory().getLeggings().clone());
            }
            if (itemType.contains("BOOTS") && player.getInventory().getBoots() != null) {
                existingBoots.put(player.getUniqueId(), player.getInventory().getBoots().clone());
            }
        }
        
        openMenu(player, held);
        
        if (itemType.contains("HELMET") || itemType.contains("CHESTPLATE") ||
            itemType.contains("LEGGINGS") || itemType.contains("BOOTS")) {
            
            Bukkit.getScheduler().runTaskLater(EnchantPlugin.getInstance(), () -> {
                player.setItemInHand(null);
            }, 1L);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        if (!e.getView().getTitle().equals(getMenuTitle())) return;
        
        Player player = (Player) e.getPlayer();
        UUID playerId = player.getUniqueId();
        
        Bukkit.getScheduler().runTaskLater(EnchantPlugin.getInstance(), () -> {
            if (player.getOpenInventory() == null || 
                !player.getOpenInventory().getTitle().equals(getMenuTitle())) {
                
                ItemStack original = originalArmor.get(playerId);
                if (original != null) {
                    String itemType = original.getType().name();
                    
                    
                    if (itemType.contains("HELMET")) {
                        player.getInventory().setHelmet(existingHelmet.get(playerId));
                    } else if (itemType.contains("CHESTPLATE")) {
                        player.getInventory().setChestplate(existingChestplate.get(playerId));
                    } else if (itemType.contains("LEGGINGS")) {
                        player.getInventory().setLeggings(existingLeggings.get(playerId));
                    } else if (itemType.contains("BOOTS")) {
                        player.getInventory().setBoots(existingBoots.get(playerId));
                    }
                    
                    
                    player.setItemInHand(original);
                    originalArmor.remove(playerId);
                }
                
                
                existingHelmet.remove(playerId);
                existingChestplate.remove(playerId);
                existingLeggings.remove(playerId);
                existingBoots.remove(playerId);
                selectedEnchants.remove(playerId);
            }
        }, 3L);
    }

    private boolean isEnchantable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String name = item.getType().name();
        return name.contains("SWORD") || name.contains("BOW") || name.contains("AXE") ||
               name.contains("HELMET") || name.contains("CHESTPLATE") ||
               name.contains("LEGGINGS") || name.contains("BOOTS");
    }

    private void openMenu(Player player, ItemStack item) {
        Inventory inv = Bukkit.createInventory(null, 54, getMenuTitle());
        String itemType = item.getType().name();

        for (int i = 0; i <= 8; i++) {
            if (i == 4) {
                String title = ChatColor.translateAlternateColorCodes('&', 
                    getConfig().getString("menu.info.title", "&5&lINFO"));
                List<String> lore = getConfig().getStringList("menu.info.lore");
                String[] loreArray = new String[lore.size()];
                for (int j = 0; j < lore.size(); j++) {
                    loreArray[j] = lore.get(j);
                }
                inv.setItem(i, createItem(Material.SIGN, title, loreArray));
            } else {
                inv.setItem(i, createColoredGlassPane((short) 2, " "));
            }
        }

        inv.setItem(21, createEnchantBook(EnchantOption.UNBREAKING_1, player));
        inv.setItem(22, createEnchantBook(EnchantOption.UNBREAKING_2, player));
        inv.setItem(23, createEnchantBook(EnchantOption.UNBREAKING_3, player));

        if (itemType.contains("SWORD") || itemType.contains("AXE")) {
            inv.setItem(29, createEnchantBook(EnchantOption.SHARPNESS_1, player));
            inv.setItem(30, createEnchantBook(EnchantOption.SHARPNESS_2, player));
            inv.setItem(31, createEnchantBook(EnchantOption.SHARPNESS_3, player));
            inv.setItem(32, createEnchantBook(EnchantOption.SHARPNESS_4, player));
            inv.setItem(33, createEnchantBook(EnchantOption.FIRE_ASPECT_1, player));
        } else if (itemType.contains("BOW")) {
            inv.setItem(29, createEnchantBook(EnchantOption.POWER_1, player));
            inv.setItem(30, createEnchantBook(EnchantOption.POWER_2, player));
            inv.setItem(31, createEnchantBook(EnchantOption.POWER_3, player));
            inv.setItem(32, createEnchantBook(EnchantOption.POWER_4, player));
            inv.setItem(33, createEnchantBook(EnchantOption.FLAME_1, player));
        } else if (itemType.contains("HELMET") || itemType.contains("CHESTPLATE") ||
                   itemType.contains("LEGGINGS") || itemType.contains("BOOTS")) {
            inv.setItem(29, createEnchantBook(EnchantOption.PROTECTION_1, player));
            inv.setItem(30, createEnchantBook(EnchantOption.PROTECTION_2, player));
            inv.setItem(31, createEnchantBook(EnchantOption.PROTECTION_3, player));
            inv.setItem(32, createEnchantBook(EnchantOption.PROTECTION_4, player));
            inv.setItem(33, createEnchantBook(EnchantOption.FIRE_PROTECTION_1, player));
        }

        for (int i = 45; i <= 53; i++) {
            if (i != 49) {
                inv.setItem(i, createColoredGlassPane((short) 0, " "));
            }
        }

        inv.setItem(49, createEnchantButton(player));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) e.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        if (!e.getView().getTitle().equals(getMenuTitle())) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        String display = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        for (EnchantOption option : EnchantOption.values()) {
            if (display.equalsIgnoreCase(option.getName())) {
                List<EnchantOption> selected = selectedEnchants.getOrDefault(playerId, new ArrayList<>());
                
                if (selected.contains(option)) {
                    selected.remove(option);
                } else {
                    EnchantOption conflictOption = null;
                    
                    for (EnchantOption existing : selected) {
                        if (existing.getEnchantment().equals(option.getEnchantment())) {
                            conflictOption = existing;
                            break;
                        }
                    }
                    
                    if (conflictOption != null) {
                        selected.remove(conflictOption);
                    }
                    selected.add(option);
                }
                
                selectedEnchants.put(playerId, selected);
                
                
                ItemStack heldItem = originalArmor.get(playerId);
                if (heldItem == null) {
                    heldItem = player.getItemInHand();
                }
                
                
                if (heldItem == null || heldItem.getType() == Material.AIR) {
                    if (player.getInventory().getHelmet() != null && isEnchantable(player.getInventory().getHelmet())) {
                        heldItem = player.getInventory().getHelmet();
                    } else if (player.getInventory().getChestplate() != null && isEnchantable(player.getInventory().getChestplate())) {
                        heldItem = player.getInventory().getChestplate();
                    } else if (player.getInventory().getLeggings() != null && isEnchantable(player.getInventory().getLeggings())) {
                        heldItem = player.getInventory().getLeggings();
                    } else if (player.getInventory().getBoots() != null && isEnchantable(player.getInventory().getBoots())) {
                        heldItem = player.getInventory().getBoots();
                    }
                }
                
                openMenu(player, heldItem);
                return;
            }
        }

        if (clicked.getType() == Material.ENCHANTMENT_TABLE) {
            List<EnchantOption> selected = selectedEnchants.get(playerId);
            
            if (selected == null || selected.isEmpty()) {
                player.closeInventory();
                return;
            }

            if (econ == null) {
                String msg = getConfig().getString("messages.economy-unavailable", "&cEconomy system ikke tilgængeligt!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                player.closeInventory();
                return;
            }

            int totalPrice = selected.stream().mapToInt(EnchantOption::getPrice).sum();
            
            if (econ.getBalance(player) < totalPrice) {
                String msg = getConfig().getString("messages.not-enough-money", "&cDu har ikke råd. Pris: $%price%");
                msg = msg.replace("%price%", String.valueOf(totalPrice));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                player.closeInventory();
                return;
            }

            ItemStack item = player.getItemInHand();
            
            ItemStack originalItem = originalArmor.get(playerId);
            if (originalItem != null) {
                item = originalItem;
            } else {
                if (item == null || item.getType() == Material.AIR || !isEnchantable(item)) {
                    ItemStack helmet = player.getInventory().getHelmet();
                    ItemStack chest = player.getInventory().getChestplate();
                    ItemStack legs = player.getInventory().getLeggings();
                    ItemStack boots = player.getInventory().getBoots();
                    
                    if (helmet != null && isEnchantable(helmet)) {
                        item = helmet;
                    } else if (chest != null && isEnchantable(chest)) {
                        item = chest;
                    } else if (legs != null && isEnchantable(legs)) {
                        item = legs;
                    } else if (boots != null && isEnchantable(boots)) {
                        item = boots;
                    }
                }
            }
            
            if (item == null || !isEnchantable(item)) {
                String msg = getConfig().getString("messages.no-item-held", "&cDu skal holde et item du vil enchante.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                player.closeInventory();
                return;
            }

            for (EnchantOption enchant : selected) {
                item.addUnsafeEnchantment(enchant.getEnchantment(), enchant.getLevel());
            }

            econ.withdrawPlayer(player, totalPrice);
            
            String itemType = item.getType().name();
            if (itemType.contains("HELMET")) {
                
                player.getInventory().setHelmet(existingHelmet.get(playerId));
                
                player.setItemInHand(item);
            } else if (itemType.contains("CHESTPLATE")) {
                player.getInventory().setChestplate(existingChestplate.get(playerId));
                player.setItemInHand(item);
            } else if (itemType.contains("LEGGINGS")) {
                player.getInventory().setLeggings(existingLeggings.get(playerId));
                player.setItemInHand(item);
            } else if (itemType.contains("BOOTS")) {
                player.getInventory().setBoots(existingBoots.get(playerId));
                player.setItemInHand(item);
            } else {
                
                player.setItemInHand(item);
            }

            String itemName = item.getType().name().toLowerCase().replace("_", " ");
            String msg = getConfig().getString("messages.enchant-success", "&8[&dEnchant&8] &7Du har enchanted &e%item%&7!");
            msg = msg.replace("%item%", itemName);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));

           
            selectedEnchants.remove(playerId);
            originalArmor.remove(playerId);
            existingHelmet.remove(playerId);
            existingChestplate.remove(playerId);
            existingLeggings.remove(playerId);
            existingBoots.remove(playerId);
            
            player.closeInventory();
        }
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createColoredGlassPane(short color, String name) {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, color);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        glass.setItemMeta(meta);
        return glass;
    }

    private ItemStack createEnchantBook(EnchantOption option, Player player) {
        List<EnchantOption> selected = selectedEnchants.getOrDefault(player.getUniqueId(), new ArrayList<>());
        Material mat = selected.contains(option) ? Material.ENCHANTED_BOOK : Material.BOOK;
        
        String titleTemplate = getConfig().getString("menu.enchant-book.title", "&5&l%enchantment%");
        String title = ChatColor.translateAlternateColorCodes('&', 
            titleTemplate.replace("%enchantment%", option.getName().toUpperCase()));
        
        String loreKey = selected.contains(option) ? "menu.enchant-book.lore-selected" : "menu.enchant-book.lore";
        List<String> loreConfig = getConfig().getStringList(loreKey);
        List<String> lore = new ArrayList<>();
        
        for (String line : loreConfig) {
            line = line.replace("%enchantment%", option.getName());
            line = line.replace("%price%", String.valueOf(option.getPrice()));
            lore.add(line);
        }
        
        return createItem(mat, title, lore.toArray(new String[0]));
    }

    private ItemStack createEnchantButton(Player player) {
        List<EnchantOption> selected = selectedEnchants.getOrDefault(player.getUniqueId(), new ArrayList<>());
        
        String title = ChatColor.translateAlternateColorCodes('&', 
            getConfig().getString("menu.enchant-button.title", "&5ENCHANT"));
        
        if (selected.isEmpty()) {
            List<String> loreConfig = getConfig().getStringList("menu.enchant-button.lore-empty");
            List<String> lore = new ArrayList<>();
            for (String line : loreConfig) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            return createItem(Material.ENCHANTMENT_TABLE, title, lore.toArray(new String[0]));
        }

        int total = selected.stream().mapToInt(EnchantOption::getPrice).sum();
        
        List<String> loreConfig = getConfig().getStringList("menu.enchant-button.lore");
        String enchantFormat = getConfig().getString("menu.enchant-button.enchant-format", "&e- %enchantment%");
        List<String> lore = new ArrayList<>();
        
        for (String line : loreConfig) {
            if (line.contains("%enchants%")) {
                for (EnchantOption opt : selected) {
                    String enchLine = enchantFormat.replace("%enchantment%", opt.getName());
                    lore.add(ChatColor.translateAlternateColorCodes('&', enchLine));
                }
            } else {
                line = line.replace("%price%", String.valueOf(total));
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
        }

        return createItem(Material.ENCHANTMENT_TABLE, title, lore.toArray(new String[0]));
    }
}