package pl.autowystaw;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;

public class AutoWystawClient implements ClientModInitializer {

    private static final int DELAY_TICKS = 30; // 20 ticków = 1 s
    private static final int WEAKNESS_DURATION = 4800; // 4:00 = 4800 ticków
    private static final Path CONFIG =
            FabricLoader.getInstance().getConfigDir().resolve("autowystaw.txt");

    private static boolean enabled = false;
    private static String command = "wystaw 1 160"; // bez ukośnika
    private static int ticks = 0;

    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        loadConfig();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autowystaw.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.autowystaw"
        ));

        // Komendy: /autowystaw start|stop|cmd <komenda>|status
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("autowystaw")
                .then(ClientCommandManager.literal("start").executes(ctx -> {
                    setEnabled(true); return 1;
                }))
                .then(ClientCommandManager.literal("stop").executes(ctx -> {
                    setEnabled(false); return 1;
                }))
                .then(ClientCommandManager.literal("status").executes(ctx -> {
                    msg("Stan: " + (enabled ? "WŁĄCZONY" : "wyłączony") + " | komenda: /" + command);
                    return 1;
                }))
                .then(ClientCommandManager.literal("cmd")
                    .then(ClientCommandManager.argument("komenda", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String c = StringArgumentType.getString(ctx, "komenda").trim();
                            if (c.startsWith("/")) c = c.substring(1);
                            command = c;
                            saveConfig();
                            msg("Ustawiono komendę: /" + command);
                            return 1;
                        })))
                .executes(ctx -> {
                    setEnabled(!enabled); return 1;
                })
            ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            setEnabled(!enabled);
        }

        ClientPlayerEntity player = client.player;
        if (!enabled || player == null || client.interactionManager == null) return;
        if (client.currentScreen != null) return; // nie rób nic przy otwartym GUI

        if (++ticks < DELAY_TICKS) return;
        ticks = 0;

        PlayerInventory inv = player.getInventory();

        // 1. Jeśli w ręce jest właściwa mikstura – wystaw
        if (isTargetPotion(inv.getMainHandStack())) {
            player.networkHandler.sendChatCommand(command);
            return;
        }

        // 2. Znajdź miksturę w ekwipunku i przenieś ją do aktywnego slotu
        int hotbarIndex = inv.selectedSlot;
        for (int i = 0; i < 36; i++) {
            if (isTargetPotion(inv.getStack(i))) {
                if (i < 9) {
                    inv.selectedSlot = i; // jest na pasku – po prostu ją wybierz
                } else {
                    client.interactionManager.clickSlot(
                            player.playerScreenHandler.syncId,
                            i,
                            hotbarIndex,
                            SlotActionType.SWAP,
                            player
                    );
                }
                return; // wysyłanie komendy w następnym cyklu
            }
        }

        msg("Brak Splash Potion of Weakness (04:00) w ekwipunku. Wyłączam.");
        enabled = false;
    }

    /** Splash Potion of Weakness I, 4:00. */
    private static boolean isTargetPotion(ItemStack s) {
        if (s.isEmpty() || !s.isOf(Items.SPLASH_POTION)) return false;

        PotionContentsComponent contents = s.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;

        for (StatusEffectInstance e : contents.getEffects()) {
            if (e.getEffectType().matches(StatusEffects.WEAKNESS)
                    && e.getAmplifier() == 0
                    && e.getDuration() == WEAKNESS_DURATION) {
                return true;
            }
        }
        return false;
    }

    private static void setEnabled(boolean value) {
        enabled = value;
        ticks = DELAY_TICKS; // pierwsza akcja od razu
        msg(value ? "WŁĄCZONY (/" + command + ")" : "wyłączony");
    }

    private static void msg(String s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[AutoWystaw] " + s), false);
        }
    }

    private static void loadConfig() {
        try {
            if (Files.exists(CONFIG)) {
                String s = Files.readString(CONFIG).trim();
                if (!s.isEmpty()) command = s;
            }
        } catch (Exception ignored) {}
    }

    private static void saveConfig() {
        try {
            Files.writeString(CONFIG, command);
        } catch (Exception ignored) {}
    }
}
