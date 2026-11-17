package com.exxxee.backpack.Inventory.menu;

import com.exxxee.backpack.Inventory.slot.ModuleSlot;
import com.exxxee.backpack.api.IBackpackHost;
import com.exxxee.backpack.container.ModuleContainer;
import com.exxxee.backpack.container.ShelfContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
public class ModuleMenu extends AbstractBackpackMenu {

    public int shelfSlot = -1;
    public boolean invoked;

    public ModuleMenu(int windowId, Player player, ModuleContainer container) {
        super(windowId, player);
        this.menuType = WindowMenuType.MODULE;
        container.setMenu(this);
        for (int i = 0; i < container.getContainerSize(); i++) {
            int col = i % 3;
            int row = i / 3;
            this.addSlot(new ModuleSlot(container, i, col * 18 + 6, row * 18 + 8));
        }
    }

    /**
     * 模块格子一变，先照父类把这份菜单的格子同步给客户端，
     * 再把模块内容写回身上那件背架的 SHELF_MODULES。
     * 写回只写数据，不发包；否则一次点击会在中途把状态推回客户端。
     */
    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        this.flushShelf();
    }

    private void flushShelf() {
        if (!(this.player instanceof IBackpackHost host)) return;
        for (AbstractBackpackMenu menu : host.getBackpackMenus()) {
            if (menu instanceof ShelfMenu shelfMenu
                    && !shelfMenu.slots.isEmpty()
                    && shelfMenu.slots.get(0).container instanceof ShelfContainer shelf) {
                shelf.flush();
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return this.quickMoveToHost(player, slotIndex);
    }

}
