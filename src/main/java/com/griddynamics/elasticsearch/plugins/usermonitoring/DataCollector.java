package com.griddynamics.elasticsearch.plugins.usermonitoring;

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.index.shard.SearchOperationListener;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.xpack.core.security.user.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static com.griddynamics.elasticsearch.plugins.usermonitoring.CustomUsermonitoringPlugin.PLUGIN_SETTINGS_PREFIX;

public class DataCollector implements SearchOperationListener {

    private static final Setting<List<String>> SKIP_USERS = Setting.listSetting(PLUGIN_SETTINGS_PREFIX + "skip.users", Collections.emptyList(), Function.identity(), Setting.Property.NodeScope);

    public static List<Setting<?>> ALL_SETTINGS = Arrays.asList(
            SKIP_USERS
    );

    private final ThreadContext threadContext;
    private final Set<String> skipUsers;

    private final ConcurrentMap<String, UserData> userDataConcurrentMap = new ConcurrentHashMap<>();

    public DataCollector(ThreadContext threadContext, Settings settings) {
        this.threadContext = threadContext;
        this.skipUsers = getSkipUsers(settings);
    }

    private Set<String> getSkipUsers(Settings settings) {
        Set<String> users = new HashSet<>(SKIP_USERS.get(settings));
        return Collections.unmodifiableSet(users);
    }


    @Override
    public void onQueryPhase(SearchContext searchContext, long tookInNanos) {

        User user = Utils.extractUser(threadContext);
        String userName;
        if (user != null && (userName = user.principal()) != null && !skipUsers.contains(userName)) {
            final ShardId shardId = searchContext.indexShard().shardId();
            userDataConcurrentMap.compute(userName, (key, userData) -> {
                if (userData == null) {
                    return new UserData(shardId, tookInNanos);
                } else {
                    userData.add(shardId, tookInNanos);
                    return userData;
                }
            });


        }
    }

    public Map<String, UserData> getAndResetData() {
        synchronized (userDataConcurrentMap) { //By inner object
            Set<String> userNames = new HashSet<>(userDataConcurrentMap.keySet());
            Map<String, UserData> result = new HashMap<>(userNames.size());
            for (String username : userNames) {
                UserData userData = userDataConcurrentMap.remove(username);
                if (userData != null) {
                    result.put(username, userData);
                }
            }
            return result;
        }
    }

    /*public ConcurrentMap<String, UserData> getAndResetData() {
        Lock lock = readWriteLock.writeLock();
        boolean locked = false;
        try {
            if (locked = lock.tryLock(10, TimeUnit.SECONDS)) { //TODO
                ConcurrentMap<String, UserData> result = userDataConcurrentMap;
                userDataConcurrentMap = new ConcurrentHashMap<>(result.size());
                return result;
            }
            throw new DataCollectionException("Can not acquire lock");
        } catch (InterruptedException e) {
            //TODO process interrupted;
            throw new DataCollectionException("Thread interrupted", e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }*/


}
