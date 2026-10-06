package com.mawlee.cointcore.invsee.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * Rebindable slot. Server reads/writes the live source; client only stores stacks from sync packets.
 */
public final class InvSeeBoundSlot extends Slot {
    private final boolean clientSide;
    private Source source = Source.EMPTY;
    private boolean readOnly = true;
    private ItemStack clientStack = ItemStack.EMPTY;

    public InvSeeBoundSlot(Container placeholder, int index, int x, int y, boolean clientSide) {
        super(placeholder, index, x, y);
        this.clientSide = clientSide;
    }

    public void bindEmpty() {
        source = Source.EMPTY;
        readOnly = true;
    }

    public void bindReadOnly(ItemStack stack) {
        source = Source.readOnly(stack);
        readOnly = true;
    }

    public void bindContainer(Container container, int slotIndex, boolean editable) {
        source = Source.container(container, slotIndex);
        readOnly = !editable;
    }

    public void bindHandler(IItemHandler handler, int slotIndex, boolean editable) {
        source = Source.handler(handler, slotIndex);
        readOnly = !editable;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public boolean hasItem() {
        return !getItem().isEmpty();
    }

    @Override
    public ItemStack getItem() {
        return clientSide ? clientStack : source.get();
    }

    @Override
    public void set(ItemStack stack) {
        ItemStack value = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (clientSide) {
            clientStack = value;
            return;
        }
        if (!readOnly) {
            source.set(value);
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        if (!clientSide) {
            source.setChanged();
        }
    }

    @Override
    public int getMaxStackSize() {
        return clientSide ? 64 : source.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(getMaxStackSize(), stack.getMaxStackSize());
    }

    @Override
    public ItemStack remove(int amount) {
        if (clientSide) {
            ItemStack current = clientStack;
            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack split = current.split(amount);
            if (current.isEmpty()) {
                clientStack = ItemStack.EMPTY;
            }
            return split;
        }
        if (readOnly) {
            return ItemStack.EMPTY;
        }
        return source.remove(amount);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (clientSide) {
            return false;
        }
        return !readOnly && source.mayPlace(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        if (clientSide) {
            return false;
        }
        return !readOnly && !getItem().isEmpty();
    }

    private interface Source {
        Source EMPTY = new Source() {
            @Override
            public ItemStack get() {
                return ItemStack.EMPTY;
            }

            @Override
            public void set(ItemStack stack) {
            }

            @Override
            public ItemStack remove(int amount) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public int getMaxStackSize() {
                return 64;
            }

            @Override
            public void setChanged() {
            }
        };

        static Source readOnly(ItemStack stack) {
            ItemStack held = stack.copy();
            return new Source() {
                @Override
                public ItemStack get() {
                    return held;
                }

                @Override
                public void set(ItemStack value) {
                }

                @Override
                public ItemStack remove(int amount) {
                    return ItemStack.EMPTY;
                }

                @Override
                public boolean mayPlace(ItemStack value) {
                    return false;
                }

                @Override
                public int getMaxStackSize() {
                    return Math.max(1, held.getMaxStackSize());
                }

                @Override
                public void setChanged() {
                }
            };
        }

        static Source container(Container container, int slot) {
            return new Source() {
                @Override
                public ItemStack get() {
                    return container.getItem(slot);
                }

                @Override
                public void set(ItemStack stack) {
                    container.setItem(slot, stack);
                }

                @Override
                public ItemStack remove(int amount) {
                    return container.removeItem(slot, amount);
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(slot, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return container.getMaxStackSize();
                }

                @Override
                public void setChanged() {
                    container.setChanged();
                }
            };
        }

        static Source handler(IItemHandler handler, int slot) {
            return new Source() {
                @Override
                public ItemStack get() {
                    return handler.getStackInSlot(slot);
                }

                @Override
                public void set(ItemStack stack) {
                    if (handler instanceof IItemHandlerModifiable modifiable) {
                        modifiable.setStackInSlot(slot, stack);
                        return;
                    }
                    handler.extractItem(slot, Integer.MAX_VALUE, false);
                    if (!stack.isEmpty()) {
                        handler.insertItem(slot, stack, false);
                    }
                }

                @Override
                public ItemStack remove(int amount) {
                    return handler.extractItem(slot, amount, false);
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return handler.isItemValid(slot, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return handler.getSlotLimit(slot);
                }

                @Override
                public void setChanged() {
                }
            };
        }

        ItemStack get();

        void set(ItemStack stack);

        ItemStack remove(int amount);

        boolean mayPlace(ItemStack stack);

        int getMaxStackSize();

        void setChanged();
    }
}
