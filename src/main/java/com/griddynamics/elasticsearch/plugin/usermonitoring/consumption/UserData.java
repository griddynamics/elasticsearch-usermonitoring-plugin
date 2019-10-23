package com.griddynamics.elasticsearch.plugin.usermonitoring.consumption;

import org.elasticsearch.index.shard.ShardId;

import java.util.HashMap;
import java.util.Map;

public class UserData {
    private final Map<ShardId, Long> indexShardTime = new HashMap<>(); //TODO precalculate size

    public UserData() {
    }

    public UserData(ShardId shardId, long tookInNanos) {
        indexShardTime.put(shardId, tookInNanos);
    }

    public void add(ShardId shardId, long tookInNanos) {
        this.indexShardTime.merge(shardId, tookInNanos, Long::sum);
    }

    public Map<ShardId, Long> getIndexShardTime() {
        return indexShardTime;
    }
}
