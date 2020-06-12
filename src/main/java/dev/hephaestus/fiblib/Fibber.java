package dev.hephaestus.fiblib;

import net.minecraft.server.network.ServerPlayerEntity;

@Internal
public interface Fibber {
    static void fix(Object object, ServerPlayerEntity player) {
        Fibber fixed = ((Fibber) object);

        if (object != null)
            fixed.fix(player);

    }

    void fix(ServerPlayerEntity player);
}