package com.extendedclip.deluxemenus.hooks;

import com.extendedclip.deluxemenus.cache.SimpleCache;
import com.valtheris.valtherisitems.api.ValtherisItemsAPI;
import com.valtheris.valtherisitems.item.CustomItemDefinition;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ValtherisItemsHook implements ItemHook, SimpleCache {

    private static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("valtherisitems", "item_id");

    private final ValtherisItemsAPI api;
    private final Map<String, ItemStack> cache = new ConcurrentHashMap<>();

    public ValtherisItemsHook(@NotNull ValtherisItemsAPI api) {
        this.api = api;
    }

    @Override
    public ItemStack getItem(@NotNull String... arguments) {
        if (arguments.length == 0) {
            return new ItemStack(Material.STONE);
        }

        final ItemStack item = cache.computeIfAbsent(arguments[0], (id) -> {
            final Optional<ItemStack> result = api.createItem(id, null, 1);
            return result.map(ItemStack::clone).orElse(null);
        });

        return (item == null) ? new ItemStack(Material.STONE) : item.clone();
    }

    @Override
    public boolean itemMatchesIdentifiers(@NotNull ItemStack item, @NotNull String... arguments) {
        if (arguments.length == 0) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String itemId = pdc.get(ITEM_ID_KEY, PersistentDataType.STRING);
        return arguments[0].equalsIgnoreCase(itemId);
    }

    @Override
    public String getPrefix() {
        return "valtherisitems-";
    }

    @Override
    public void clearCache() {
        this.cache.clear();
    }
}
