package com.blackrikka.ironworks.storage.query;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbQueryTest {
    private static final QueryRow IRON_ORE = row("minecraft", "iron_ore", 100, "minecraft:iron_ores", "c:ores");
    private static final QueryRow OAK_LOG = row("minecraft", "oak_log", 5, "minecraft:logs", "minecraft:logs_that_burn");
    private static final QueryRow BIRCH_LOG = row("minecraft", "birch_log", 2000, "minecraft:logs");
    private static final QueryRow RUBBER_LOG = row("ironworks", "rubber_log", 64, "c:logs");
    private static final QueryRow GOLD = row("minecraft", "gold_ingot", 1_000, "c:ingots/gold");
    private static final QueryRow RAW_SPACE = row("minecraft", "iron ore", 7);

    private static QueryRow row(String namespace, String path, long count, String... tags) {
        return new QueryRow(namespace, path, Set.of(tags), count);
    }

    private static boolean matches(String query, QueryRow row) {
        return DbQuery.parse(query).filter().test(row);
    }

    private static List<String> errorsOf(String query) {
        return DbQuery.parse(query).errors();
    }

    @Test
    void partialMatchFindsSubstringOfPath() {
        assertTrue(matches("iron", IRON_ORE));
        assertFalse(matches("iron", GOLD));
    }

    @Test
    void partialMatchIgnoresCase() {
        assertTrue(matches("IRON", IRON_ORE));
        assertTrue(matches("Gold", GOLD));
    }

    @Test
    void underscoreAndSpaceAreTheSame() {
        assertTrue(matches("iron_ore", RAW_SPACE));
        assertTrue(matches("iron_ore", IRON_ORE));
        assertTrue(matches("ron_o", IRON_ORE));
    }

    @Test
    void partialMatchAlsoReadsNamespace() {
        assertTrue(matches("minecr", IRON_ORE));
        assertFalse(matches("minecr", RUBBER_LOG));
    }

    @Test
    void modIdTokenMatchesNamespaceExactly() {
        assertTrue(matches("@minecraft", IRON_ORE));
        assertFalse(matches("@minecraft", RUBBER_LOG));
        assertTrue(matches("@IRONWORKS", RUBBER_LOG));
        assertFalse(matches("@mine", IRON_ORE));
    }

    @Test
    void tagWithNamespaceMatchesExactTag() {
        assertTrue(matches("#minecraft:logs", OAK_LOG));
        assertFalse(matches("#minecraft:logs", RUBBER_LOG));
        assertFalse(matches("#minecraft:logs_that_burn", BIRCH_LOG));
    }

    @Test
    void tagWithoutNamespaceTriesMinecraftAndCommonNamespaces() {
        assertTrue(matches("#logs", BIRCH_LOG));
        assertTrue(matches("#logs", RUBBER_LOG));
        assertTrue(matches("#ores", IRON_ORE));
        assertFalse(matches("#logs", GOLD));
    }

    @Test
    void countComparisons() {
        assertTrue(matches(">100", BIRCH_LOG));
        assertFalse(matches(">100", IRON_ORE));
        assertTrue(matches(">=100", IRON_ORE));
        assertTrue(matches("<=5", OAK_LOG));
        assertFalse(matches("<5", OAK_LOG));
        assertTrue(matches("=64", RUBBER_LOG));
        assertFalse(matches("=65", RUBBER_LOG));
    }

    @Test
    void countSuffixesKAndM() {
        assertTrue(matches(">1k", new QueryRow("minecraft", "x", Set.of(), 1500)));
        assertFalse(matches(">1k", new QueryRow("minecraft", "x", Set.of(), 1000)));
        assertTrue(matches("=1k", new QueryRow("minecraft", "x", Set.of(), 1000)));
        assertTrue(matches(">=2.5m", new QueryRow("minecraft", "x", Set.of(), 2_500_000)));
        assertFalse(matches(">=2.5m", new QueryRow("minecraft", "x", Set.of(), 2_499_999)));
        assertTrue(matches("<2.5K", OAK_LOG));
    }

    @Test
    void invalidCountIsReported() {
        assertEquals(1, errorsOf(">abc").size());
        assertTrue(errorsOf(">abc").get(0).startsWith(">abc"));
        assertEquals(1, errorsOf(">-5").size());
        assertEquals(1, errorsOf(">1.5").size());
        assertEquals(1, errorsOf("=").size());
    }

    @Test
    void negationExcludesMatches() {
        assertFalse(matches("-@minecraft", IRON_ORE));
        assertTrue(matches("-@minecraft", RUBBER_LOG));
        assertFalse(matches("-iron", IRON_ORE));
        assertTrue(matches("-iron", OAK_LOG));
        assertFalse(matches("-#logs", OAK_LOG));
        assertTrue(matches("-=64", OAK_LOG));
        assertFalse(matches("-=64", RUBBER_LOG));
    }

    @Test
    void negationAloneIsAnError() {
        assertEquals(1, errorsOf("-").size());
    }

    @Test
    void tokensAreCombinedWithAnd() {
        assertTrue(matches("iron @minecraft >10", IRON_ORE));
        assertFalse(matches("iron @ironworks", IRON_ORE));
        assertFalse(matches("iron >1000", IRON_ORE));
        assertTrue(matches("log @minecraft #logs", BIRCH_LOG));
        assertFalse(matches("log @minecraft #logs", RUBBER_LOG));
    }

    @Test
    void invalidTokenIsReportedAndTheOthersStillFilter() {
        DbQuery.ParseResult result = DbQuery.parse("iron >abc");
        assertEquals(1, result.errors().size());
        assertTrue(result.hasErrors());
        assertTrue(result.filter().test(IRON_ORE));
        assertFalse(result.filter().test(GOLD));
    }

    @Test
    void emptyTokensWithoutNameAreReported() {
        assertEquals(1, errorsOf("@").size());
        assertEquals(1, errorsOf("#").size());
        assertEquals(1, errorsOf("_").size());
    }

    @Test
    void emptyQueryMatchesEverything() {
        assertTrue(matches("", IRON_ORE));
        assertTrue(matches("   ", RUBBER_LOG));
        assertTrue(matches(null, GOLD));
        assertTrue(errorsOf("").isEmpty());
        assertFalse(DbQuery.parse(null).hasErrors());
    }

    @Test
    void validQueryHasNoErrors() {
        assertTrue(DbQuery.parse("iron @minecraft #c:ores >=1k -foo").errors().isEmpty());
    }

    @Test
    void queryRowLowercasesIdentityFields() {
        QueryRow mixed = new QueryRow("MineCraft", "Iron_Ore", Set.of("Minecraft:Logs"), 3);
        assertEquals("minecraft", mixed.namespace());
        assertEquals("iron_ore", mixed.path());
        assertEquals("minecraft:iron_ore", mixed.id());
        assertTrue(matches("#logs", mixed));
    }
}
