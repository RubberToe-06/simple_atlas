package rubbertoe.simple_atlas.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public class AtlasCartographyActionTrigger extends SimpleCriterionTrigger<AtlasCartographyActionTrigger.TriggerInstance> {
    public static final AtlasCartographyActionTrigger INSTANCE = new AtlasCartographyActionTrigger();

    @Override
    public @NonNull Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(@NonNull ServerPlayer player, @NonNull Action action) {
        this.trigger(player, trigger -> trigger.matches(action));
    }

    public enum Action {
        DUPLICATE,
        MERGE;

        public static final Codec<Action> CODEC = Codec.STRING.comapFlatMap(
                name -> Arrays.stream(values())
                        .filter(action -> action.serializedName().equals(name))
                        .findFirst()
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Unknown atlas cartography action: " + name)),
                Action::serializedName
        );

        public String serializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Action> action) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Action.CODEC.optionalFieldOf("action").forGetter(TriggerInstance::action)
        ).apply(instance, TriggerInstance::new));

        public boolean matches(Action action) {
            return this.action.map(expected -> expected == action).orElse(true);
        }

        public static Criterion<TriggerInstance> duplicatedAtlas() {
            return INSTANCE.createCriterion(new TriggerInstance(Optional.empty(), Optional.of(Action.DUPLICATE)));
        }

        public static Criterion<TriggerInstance> mergedAtlases() {
            return INSTANCE.createCriterion(new TriggerInstance(Optional.empty(), Optional.of(Action.MERGE)));
        }
    }
}

