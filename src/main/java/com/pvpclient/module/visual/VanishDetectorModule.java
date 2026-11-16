package com.pvpclient.module.visual;

import com.pvpclient.module.ClientModule;
import com.pvpclient.module.Category;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import java.util.HashSet;
import java.util.Set;

public class VanishDetectorModule extends ClientModule {

    public static final Set<String> vanishedStaff = new HashSet<>();
    public static int staffCount = 0;

    public VanishDetectorModule() {
        super("VanishDetector", "Показывает количество модераторов в Ванише", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (!this.isEnabled()) {
            vanishedStaff.clear();
            staffCount = 0;
            return;
        }
        staffCount = vanishedStaff.size();
    }

    public static void onReceivePacket(Object packet) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        if (packet instanceof PlayerListS2CPacket listPacket) {
            // ГЛАВНЫЙ ОБХОД: Вместо проверки конкретного Action (REMOVE/UPDATE),
            // мы просто проверяем, пришел ли не пустой пакет обновления таба.
            // Если он содержит записи, мы легитно сканируем ники из него.
            if (listPacket.getEntries() != null && !listPacket.getEntries().isEmpty()) {
                for (PlayerListS2CPacket.Entry entry : listPacket.getEntries()) {
                    if (entry.profile() != null) {
                        String profileName = entry.profile().getName();
                        if (isStaffNickname(profileName)) {
                            // Проверяем: если игрока с таким ником больше нет в обычном сетевом списке (TAB)
                            // значит, этот пакет убрал его, и админ ушел в Ваниш!
                            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(entry.profile().getId()) == null) {
                                vanishedStaff.add(profileName);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isStaffNickname(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("mod") || lower.contains("admin") || lower.contains("helper") 
            || lower.contains("staff") || lower.contains("rw_") || lower.contains("ft_");
    }
}
