package com.blackrikka.ironworks.storage.query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the DB query text into a filter over {@link QueryRow}. Pure logic, no Minecraft classes.
 * <p>
 * The text is split on whitespace; all tokens must match (AND). Token forms:
 * <ul>
 *   <li>{@code iron} : partial match on path only, case ignored, {@code _} and space are the same. Use
 *       {@code @modid} to filter by namespace</li>
 *   <li>{@code @minecraft} : namespace equals</li>
 *   <li>{@code #minecraft:logs} : tag; without a namespace, {@code minecraft:} and {@code c:} are both tried</li>
 *   <li>{@code >100} {@code >=100} {@code <5} {@code <=5} {@code =64} : count comparison, {@code k} = 1000 and
 *       {@code m} = 1000000 suffixes are allowed ({@code 1k}, {@code 2.5m})</li>
 *   <li>{@code -...} : negation of any of the above</li>
 * </ul>
 * Invalid tokens are not ignored: they are reported in {@link ParseResult#errors()}, and the valid tokens still
 * form the filter. An empty query matches every row.
 */
public final class DbQuery {
    private static final Pattern AMOUNT = Pattern.compile("(\\d+(?:\\.\\d+)?)([km]?)", Pattern.CASE_INSENSITIVE);

    private DbQuery() {
    }

    /**
     * @param filter the filter built from the valid tokens
     * @param errors one entry per invalid token, in the form {@code "<token>: <reason>"}
     */
    public record ParseResult(Predicate<QueryRow> filter, List<String> errors) {
        public ParseResult {
            errors = List.copyOf(errors);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    /** Parses {@code query}. {@code null} and blank text give a filter that matches every row. */
    public static ParseResult parse(String query) {
        List<String> errors = new ArrayList<>();
        List<Predicate<QueryRow>> conditions = new ArrayList<>();
        if (query != null) {
            for (String token : query.trim().split("\\s+")) {
                if (token.isEmpty()) {
                    continue;
                }
                boolean negate = token.startsWith("-");
                String body = negate ? token.substring(1) : token;
                if (body.isEmpty()) {
                    errors.add(token + ": empty negation");
                    continue;
                }
                String problem = null;
                Predicate<QueryRow> condition = null;
                try {
                    condition = compile(body);
                } catch (IllegalArgumentException e) {
                    problem = e.getMessage();
                }
                if (condition == null) {
                    errors.add(token + ": " + (problem == null ? "invalid token" : problem));
                    continue;
                }
                conditions.add(negate ? condition.negate() : condition);
            }
        }
        Predicate<QueryRow> filter = row -> {
            for (Predicate<QueryRow> condition : conditions) {
                if (!condition.test(row)) {
                    return false;
                }
            }
            return true;
        };
        return new ParseResult(filter, errors);
    }

    /** Builds the predicate for one token without its leading {@code -}. Throws IllegalArgumentException if invalid. */
    private static Predicate<QueryRow> compile(String body) {
        char first = body.charAt(0);
        switch (first) {
            case '@' -> {
                String namespace = body.substring(1).toLowerCase(Locale.ROOT);
                if (namespace.isEmpty()) {
                    throw new IllegalArgumentException("missing mod id");
                }
                return row -> row.namespace().equals(namespace);
            }
            case '#' -> {
                String tag = body.substring(1).toLowerCase(Locale.ROOT);
                if (tag.isEmpty()) {
                    throw new IllegalArgumentException("missing tag");
                }
                if (tag.contains(":")) {
                    return row -> row.tags().contains(tag);
                }
                String minecraft = "minecraft:" + tag;
                String common = "c:" + tag;
                return row -> row.tags().contains(minecraft) || row.tags().contains(common);
            }
            case '>', '<', '=' -> {
                return compileCount(body);
            }
            default -> {
                String term = normalize(body);
                if (term.isBlank()) {
                    throw new IllegalArgumentException("empty search word");
                }
                return row -> normalize(row.path()).contains(term);
            }
        }
    }

    private static Predicate<QueryRow> compileCount(String body) {
        String operator;
        if (body.startsWith(">=") || body.startsWith("<=")) {
            operator = body.substring(0, 2);
        } else {
            operator = body.substring(0, 1);
        }
        Long amount = parseAmount(body.substring(operator.length()));
        if (amount == null) {
            throw new IllegalArgumentException("invalid count");
        }
        long value = amount;
        return switch (operator) {
            case ">" -> row -> row.count() > value;
            case ">=" -> row -> row.count() >= value;
            case "<" -> row -> row.count() < value;
            case "<=" -> row -> row.count() <= value;
            default -> row -> row.count() == value;
        };
    }

    /** A whole, non-negative number with an optional {@code k} or {@code m} suffix; null if invalid. */
    static Long parseAmount(String text) {
        Matcher matcher = AMOUNT.matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        String suffix = matcher.group(2).toLowerCase(Locale.ROOT);
        long multiplier = switch (suffix) {
            case "k" -> 1_000L;
            case "m" -> 1_000_000L;
            default -> 1L;
        };
        try {
            return new BigDecimal(matcher.group(1)).multiply(BigDecimal.valueOf(multiplier)).longValueExact();
        } catch (ArithmeticException e) {
            return null;
        }
    }

    /** Lowercase, with {@code _} read as a space, so {@code iron_ore} and {@code iron ore} are the same. */
    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
