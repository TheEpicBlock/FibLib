package dev.hephaestus.fiblib.mixin.blocks.packets.chunkdata;

import dev.hephaestus.fiblib.FibLib;
import dev.hephaestus.fiblib.Fibber;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.IdList;
import net.minecraft.world.chunk.BiMapPalette;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BiMapPalette.class)
public class BiMapPaletteFibber<T> implements Fibber {
    ServerPlayerEntity player;
    @Shadow
    @Final
    private IdList<T> idList;

    @Redirect(method = "toPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/IdList;getId(Ljava/lang/Object;)I"))
    public int toPacketRedir(IdList<T> idList, T object) {
        return idList.getId((T) FibLib.Blocks.get((BlockState) object, this.player));
    }

    @Redirect(method = "getPacketSize", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/IdList;getId(Ljava/lang/Object;)I"))
    public int packetSizeRedir(IdList<T> idList, T object) {
        return idList.getId((T) FibLib.Blocks.get((BlockState) object, this.player));
    }


    @Override
    public void fix(ServerPlayerEntity player) {
        this.player = player;
    }
}
