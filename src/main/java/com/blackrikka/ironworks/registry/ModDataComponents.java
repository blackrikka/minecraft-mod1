package com.blackrikka.ironworks.registry;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.storage.DriveData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Ironworks.MOD_ID);

    // M3a: contents of a storage drive. Persistent (saved with the item) and synced to clients for tooltips.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DriveData>> DRIVE_DATA =
            DATA_COMPONENTS.registerComponentType("drive_data",
                    builder -> builder.persistent(DriveData.CODEC).networkSynchronized(DriveData.STREAM_CODEC));
}
