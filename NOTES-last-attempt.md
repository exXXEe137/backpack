# 19:00 最后修补：转录原文（未回放）

## row 202  d:\workspace\projects\backpack\src\main\java\com\exxxee\backpack\container\ShelfContainer.java

OLD:
`java
        List<ItemStack> snapshot = new ArrayList<>(this.items.size());
        for (int i = 0; i < this.items.size(); i++) {
            snapshot.add(this.items.get(i).copy());
        }
        ItemStack chest = this.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(BackpackItems.BACKPACK_SHELF)) {
            chest.set(BackpackDataComponents.SHELF_MODULES, snapshot);
        }
        if (this.shelf.is(BackpackItems.BACKPACK_SHELF) && this.shelf != chest) {
            this.shelf.set(BackpackDataComponents.SHELF_MODULES, new ArrayList<>(snapshot));
        }
    }
`

NEW:
`java
        List<ItemStack> snapshot = this.snapshot();
        ItemStack chest = this.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(BackpackItems.BACKPACK_SHELF)) {
            chest.set(BackpackDataComponents.SHELF_MODULES, snapshot);
        }
        if (this.shelf.is(BackpackItems.BACKPACK_SHELF) && this.shelf != chest) {
            this.shelf.set(BackpackDataComponents.SHELF_MODULES, this.snapshot());
        }
    }

    private List<ItemStack> snapshot() {
        List<ItemStack> snapshot = new ArrayList<>(this.items.size());
        for (int i = 0; i < this.items.size(); i++) {
            snapshot.add(this.items.get(i).copy());
        }
        return snapshot;
    }
`

## row 203  d:\workspace\projects\backpack\src\main\java\com\exxxee\backpack\container\ShelfContainer.java

OLD:
`java
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
`

NEW:
`java
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
`
