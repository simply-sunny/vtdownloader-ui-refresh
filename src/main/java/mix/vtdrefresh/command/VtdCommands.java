package mix.vtdrefresh.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * {@code /vtd}, which does from chat what the screen does: make a zip, change one, turn one on or off.
 * Client side, so it works on any server. {@code --reload} applies the change straight away.
 */
public final class VtdCommands implements ClientModInitializer {
    private static final String RELOAD = "--reload";

    @FunctionalInterface
    private interface PacksAction {
        void run(CommandPacks.Target target, List<String> packs, boolean reload);
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("vtd")
                        .then(ClientCommands.literal("list")
                                .executes(ctx -> run(() -> CommandPacks.list(target(ctx, ""))))
                                .then(zip().executes(ctx -> run(() -> CommandPacks.show(target(ctx))))))
                        .then(toggle("load", true))
                        .then(toggle("unload", false))
                        .then(ClientCommands.literal("create")
                                .then(ClientCommands.argument("zip", StringArgumentType.string())
                                        .then(packs(ctx -> CommandPacks.allPackIds(), CommandPacks::create))))
                        .then(ClientCommands.literal("edit")
                                .then(zip()
                                        .then(ClientCommands.literal("add")
                                                .then(packs(ctx -> CommandPacks.allPackIds(), CommandPacks::add)))
                                        .then(ClientCommands.literal("remove")
                                                .then(packs(ctx -> CommandPacks.read(StringArgumentType.getString(ctx, "zip")),
                                                        CommandPacks::remove)))))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> toggle(String name, boolean on) {
        return ClientCommands.literal(name).then(zip()
                .executes(ctx -> run(() -> CommandPacks.setOn(target(ctx), on, false)))
                .then(ClientCommands.literal(RELOAD).executes(ctx -> run(() -> CommandPacks.setOn(target(ctx), on, true)))));
    }

    private static RequiredArgumentBuilder<FabricClientCommandSource, String> zip() {
        return ClientCommands.argument("zip", StringArgumentType.string()).suggests(ZIPS);
    }

    private static final SuggestionProvider<FabricClientCommandSource> ZIPS = (ctx, builder) -> {
        String typed = builder.getRemaining().replace("\"", "").toLowerCase(Locale.ROOT);
        for (String zip : CommandPacks.zips()) {
            if (zip.toLowerCase(Locale.ROOT).contains(typed)) builder.suggest(quote(zip));
        }
        return builder.buildFuture();
    };

    private static String quote(String zip) {
        return zip.chars().allMatch(c -> com.mojang.brigadier.StringReader.isAllowedInUnquotedString((char) c)) ? zip : '"' + zip.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static RequiredArgumentBuilder<FabricClientCommandSource, String> packs(
            Function<CommandContext<FabricClientCommandSource>, Collection<String>> options, PacksAction action) {
        return ClientCommands.argument("packs", StringArgumentType.greedyString())
                .suggests((ctx, builder) -> suggestPacks(builder, options.apply(ctx)))
                .executes(ctx -> {
                    List<String> words = Arrays.stream(StringArgumentType.getString(ctx, "packs").trim().split("\\s+"))
                            .filter(word -> !word.isEmpty()).toList();
                    boolean reload = words.contains(RELOAD);
                    return run(() -> action.run(target(ctx), words.stream().filter(word -> !word.equals(RELOAD)).toList(), reload));
                });
    }

    /** Suggests for the word being typed, not the whole list. */
    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestPacks(
            SuggestionsBuilder builder, Collection<String> options) {
        String remaining = builder.getRemaining();
        int start = remaining.lastIndexOf(' ') + 1;
        String word = remaining.substring(start).toLowerCase(Locale.ROOT);
        SuggestionsBuilder last = builder.createOffset(builder.getStart() + start);
        List<String> used = Arrays.asList(remaining.split(" "));
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).contains(word) && !used.contains(option)) last.suggest(option);
        }
        if (start > 0 && RELOAD.startsWith(word) && !used.contains(RELOAD)) last.suggest(RELOAD);
        return last.buildFuture();
    }

    private static CommandPacks.Target target(CommandContext<FabricClientCommandSource> ctx) {
        return target(ctx, StringArgumentType.getString(ctx, "zip"));
    }

    private static CommandPacks.Target target(CommandContext<FabricClientCommandSource> ctx, String zip) {
        FabricClientCommandSource source = ctx.getSource();
        return new CommandPacks.Target(zip, source::sendFeedback, source::sendError);
    }

    private static int run(Runnable command) {
        command.run();
        return 1;
    }
}
