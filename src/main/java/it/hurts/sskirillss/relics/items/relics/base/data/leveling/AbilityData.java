package it.hurts.sskirillss.relics.items.relics.base.data.leveling;

import com.mojang.datafixers.util.Function3;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import lombok.Builder;
import lombok.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class AbilityData {
    private final String id;

    public static AbilityDataBuilder builder(String id) {
        AbilityDataBuilder builder = new AbilityDataBuilder();

        builder.id(id);

        return builder;
    }

    @Builder.Default
    private Function3<Player, ItemStack, String, String> icon = (player, stack, ability) -> ability;

    @Builder.Default
    private Map<String, StatData> stats;

    @Builder.Default
    private int maxLevel = 10;

    @Builder.Default
    private int requiredLevel = 0;

    @Builder.Default
    private int requiredPoints = 1;

    @Builder.Default
    private CastData castData;

    public static class AbilityDataBuilder {
        private Map<String, StatData> stats = new HashMap<>();
        private CastData castData = CastData.builder().build();

        private AbilityDataBuilder castData(CastData data) {
            return this;
        }

        public AbilityDataBuilder active(CastData data) {
            this.castData = data;

            return this;
        }

        public AbilityDataBuilder stat(StatData stat) {
            stats.put(stat.getId(), stat);

            return this;
        }

        private AbilityDataBuilder id(String id) {
            this.id = id;

            return this;
        }
    }
}