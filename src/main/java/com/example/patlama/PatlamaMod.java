package com.example.patlama;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class PatlamaMod implements ModInitializer {

    // ---- AYARLAR ----
    private static final String PATLAYAN_OYUNCU = "tarmut";
    private static final float GUC = 2.0f;              // TNT = 4.0, Creeper = 3.0
    private static final boolean KENDINE_HASAR = false; // Patlayan kendi patlamasindan hasar alsin mi
    private static final int ACLIK_EKSILT = 1;          // 1 = yarim but, 2 = 1 but
    private static final long BEKLEME_MS = 1000;        // Iki patlama arasi (ms)
    // -----------------

    private static boolean patliyor = false;
    private static long sonPatlama = 0;

    private static boolean patlayanMi(Object entity) {
        return entity instanceof ServerPlayer p
                && p.getName().getString().equalsIgnoreCase(PATLAYAN_OYUNCU);
    }

    @Override
    public void onInitialize() {
        // Kendi patlamasinin hasarini engelle
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (patliyor && !KENDINE_HASAR && patlayanMi(entity)) {
                return false;
            }
            return true;
        });

        // Hasar alinca patla
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (patliyor) return;
            if (!patlayanMi(entity)) return;

            ServerPlayer player = (ServerPlayer) entity;
            if (!player.isAlive()) return;

            long simdi = System.currentTimeMillis();
            if (simdi - sonPatlama < BEKLEME_MS) return;
            sonPatlama = simdi;

            ServerLevel level = (ServerLevel) player.level();

            patliyor = true;
            try {
                // NONE: bloklari kirmaz, sadece canli varliklara hasar verir
                level.explode(player, player.getX(), player.getY(), player.getZ(),
                        GUC, Level.ExplosionInteraction.NONE);
            } finally {
                patliyor = false;
            }

            // Aclik barini azalt
            player.getFoodData().setFoodLevel(
                    Math.max(0, player.getFoodData().getFoodLevel() - ACLIK_EKSILT));
        });
    }
}
