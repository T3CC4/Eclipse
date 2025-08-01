package de.tecca.eclipse.api.database;

import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.List;
import java.util.Map;

public interface QueryBuilder {

    QueryBuilder select(String... columns);
    QueryBuilder where(String column, Object value);
    QueryBuilder where(String column, String operator, Object value);
    QueryBuilder whereIn(String column, Object... values);
    QueryBuilder whereNotNull(String column);
    QueryBuilder orderBy(String column);
    QueryBuilder orderBy(String column, String direction);
    QueryBuilder limit(int limit);
    QueryBuilder offset(int offset);

    CompletableFuture<List<Map<String, Object>>> get();
    CompletableFuture<Optional<Map<String, Object>>> first();
    CompletableFuture<Long> count();

    QueryBuilder insert();
    QueryBuilder insertOrUpdate();
    QueryBuilder value(String column, Object value);
    QueryBuilder values(Map<String, Object> values);

    QueryBuilder update();
    QueryBuilder set(String column, Object value);
    QueryBuilder increment(String column, Number amount);
    QueryBuilder decrement(String column, Number amount);

    QueryBuilder delete();

    CompletableFuture<Integer> execute();
    CompletableFuture<Void> executeVoid();

    QueryBuilder batch();
    QueryBuilder addBatch();
    CompletableFuture<int[]> executeBatch();

    List<Map<String, Object>> sync();
    Optional<Map<String, Object>> syncFirst();
    int syncExecute();
}