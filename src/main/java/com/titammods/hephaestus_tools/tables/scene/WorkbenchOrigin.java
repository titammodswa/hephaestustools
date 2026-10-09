package com.titammods.hephaestus_tools.tables.scene;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public record WorkbenchOrigin(double x, double y, double z, float eyeHeight, float yaw, float pitch) {

    public static final int BYTES = 3 * Double.BYTES + 3 * Float.BYTES;

    public static WorkbenchOrigin of(Player player) {
        return new WorkbenchOrigin(player.getX(), player.getY(), player.getZ(),
                player.getEyeHeight(), player.getYRot(), player.getXRot());
    }

    public Vec3 feet() {
        return new Vec3(x, y, z);
    }

    public Vec3 eye() {
        return new Vec3(x, y + eyeHeight, z);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(eyeHeight);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
    }

    public static WorkbenchOrigin read(FriendlyByteBuf buf) {
        return new WorkbenchOrigin(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readFloat());
    }
}
