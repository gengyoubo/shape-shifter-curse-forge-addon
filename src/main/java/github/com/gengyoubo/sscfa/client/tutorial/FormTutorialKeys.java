package github.com.gengyoubo.sscfa.client.tutorial;

import github.com.gengyoubo.sscfa.tutorial.FormTutorialCatalog;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.client.FormKeyMappings;

/** The native tutorial shows the same current key bindings as the rest of the mod. */
final class FormTutorialKeys {
    private FormTutorialKeys() {
    }

    static Component key(Minecraft client, ResourceLocation form, String token) {
        return switch (token) {
            case "primary" -> skillKey(form, true);
            case "secondary" -> skillKey(form, false);
            case "sneak" -> keyName(client.options.keyShift);
            case "attack" -> keyName(client.options.keyAttack);
            case "use" -> keyName(client.options.keyUse);
            case "jump" -> keyName(client.options.keyJump);
            case "sprint" -> keyName(client.options.keySprint);
            default -> Component.empty();
        };
    }

    private static Component skillKey(ResourceLocation form, boolean primary) {
        if (!primary && FormIdentifiers.UPGRADE_FAMILIAR_FOX.equals(form)) {
            return FormTutorialScreen.text("no_secondary");
        }
        KeyMapping direct = primary ? FormKeyMappings.ACTIVE_SKILL_1 : FormKeyMappings.ACTIVE_SKILL_2;
        if (direct != null && !direct.isUnbound()) return direct.getTranslatedKeyMessage();
        return FormTutorialScreen.text("unbound");
    }

    private static Component keyName(KeyMapping key) {
        return key == null || key.isUnbound() ? FormTutorialScreen.text("unbound") : key.getTranslatedKeyMessage();
    }
}
