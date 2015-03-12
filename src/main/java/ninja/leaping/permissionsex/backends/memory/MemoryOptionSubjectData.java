/**
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.leaping.permissionsex.backends.memory;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import org.spongepowered.api.service.permission.context.Context;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MemoryOptionSubjectData implements ImmutableOptionSubjectData {
    protected static final ObjectMapper<DataEntry> MAPPER;
    static {
        try {
            MAPPER = ObjectMapper.forClass(DataEntry.class);
        } catch (ObjectMappingException e) {
            throw new ExceptionInInitializerError(e); // This error indicates a programming issue
        }
    }
    @ConfigSerializable
    protected static class DataEntry {


        @Setting private Map<String, Integer> permissions;
        @Setting private Map<String, String> options;
        @Setting private List<String> parents;
        @Setting("permissions-default") private int defaultValue;

        private DataEntry(Map<String, Integer> permissions, Map<String, String> options, List<String> parents, int defaultValue) {
            this.permissions = permissions;
            this.options = options;
            this.parents = parents;
            this.defaultValue = defaultValue;
        }

        private DataEntry() { // Objectmapper constructor
        }

        public DataEntry withOption(String key, String value) {
            return new DataEntry(permissions, ImmutableMap.<String, String>builder().putAll(options).put(key, value).build(), parents, defaultValue);
        }

        public DataEntry withoutOption(String key) {
            if (!options.containsKey(key)) {
                return this;
            }

            Map<String, String> newOptions = new HashMap<>(options);
            newOptions.remove(key);
            return new DataEntry(permissions, newOptions, parents, defaultValue);

        }

        public DataEntry withoutOptions() {
            return new DataEntry(permissions, null, parents, defaultValue);
        }

        public DataEntry withPermission(String permission, int value) {
            return new DataEntry(ImmutableMap.<String, Integer>builder().putAll(permissions).put(permission, value).build(), options, parents, defaultValue);

        }

        public DataEntry withoutPermission(String permission) {
            if (!permissions.containsKey(permission)) {
                return this;
            }

            Map<String, Integer> newPermissions = new HashMap<>(permissions);
            newPermissions.remove(permission);
            return new DataEntry(newPermissions, options, parents, defaultValue);
        }

        public DataEntry withoutPermissions() {
            return new DataEntry(null, options, parents, defaultValue);
        }

        public DataEntry withDefaultValue(int defaultValue) {
            return new DataEntry(permissions, options, parents, defaultValue);
        }

        public DataEntry withAddedParent(String parent) {
                return new DataEntry(permissions, options, ImmutableList.<String>builder().add(parent).addAll(parents).build(), defaultValue);
        }

        public DataEntry withRemovedParent(String parent) {
            final List<String> newParents = new ArrayList<>(parents);
            newParents.remove(parent);
            return new DataEntry(permissions, options, newParents, defaultValue);
        }

        public DataEntry withoutParents() {
            return new DataEntry(permissions, options, null, defaultValue);
        }

        @Override
        public String toString() {
            return "DataEntry{" +
                    "permissions=" + permissions +
                    ", options=" + options +
                    ", parents=" + parents +
                    ", defaultValue=" + defaultValue +
                    '}';
        }
    }

    protected static DataEntry newEntry() {
        return new DataEntry();
    }

    protected MemoryOptionSubjectData newData(Map<Set<Context>, DataEntry> contexts) {
        return new MemoryOptionSubjectData(contexts);
    }

    protected final Map<Set<Context>, DataEntry> contexts;

    MemoryOptionSubjectData() {
        this.contexts = ImmutableMap.of();
    }

    protected MemoryOptionSubjectData(Map<Set<Context>, DataEntry> contexts) {
        this.contexts = contexts;
    }

    private DataEntry getDataEntryOrNew(Set<Context> contexts) {
        DataEntry res = this.contexts.get(contexts);
        if (res == null) {
            res = new DataEntry();
        }
        return res;
    }

    private <E> ImmutableSet<E> immutSet(Set<E> set) {
        return ImmutableSet.copyOf(set);
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        return Maps.transformValues(contexts, new Function<DataEntry, Map<String, String>>() {
            @Nullable
            @Override
            public Map<String, String> apply(@Nullable DataEntry dataEntry) {
                return dataEntry.options;
            }
        });
    }

    @Override
    public Map<String, String> getOptions(Set<Context> contexts) {
        final DataEntry entry = this.contexts.get(contexts);
        return entry == null || entry.options == null ? Collections.<String, String>emptyMap() : entry.options;
    }

    @Override
    public ImmutableOptionSubjectData setOption(Set<Context> contexts, String key, String value) {
        if (value == null) {
            return newData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(immutSet(contexts), getDataEntryOrNew(contexts).withoutOption(key)).build());
        } else {
            return newData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(immutSet(contexts), getDataEntryOrNew(contexts).withOption(key, value)).build());
        }
    }

    @Override
    public ImmutableOptionSubjectData clearOptions(Set<Context> contexts) {
        if (!this.contexts.containsKey(contexts)) {
            return this;
        }
        return newData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(immutSet(contexts), getDataEntryOrNew(contexts).withoutOptions()).build());
    }

    @Override
    public ImmutableOptionSubjectData clearOptions() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Context>, DataEntry> newValue = Maps.transformValues(this.contexts, new Function<DataEntry, DataEntry>() {
            @Nullable
            @Override
            public DataEntry apply(@Nullable DataEntry dataEntry) {
                return dataEntry.withoutOptions();
            }
        });
        return newData(newValue);
    }

    @Override
    public Map<Set<Context>, Map<String, Integer>> getAllPermissions() {
        return Maps.filterValues(Maps.transformValues(contexts, new Function<DataEntry, Map<String, Integer>>() {
            @Nullable
            @Override
            public Map<String, Integer> apply(@Nullable DataEntry dataEntry) {
                return dataEntry.permissions;
            }
        }), Predicates.notNull());
    }

    @Override
    public Map<String, Integer> getPermissions(Set<Context> set) {
        final DataEntry entry = this.contexts.get(set);
        return entry == null || entry.permissions == null ? Collections.<String, Integer>emptyMap() : entry.permissions;
    }

    @Override
    public ImmutableOptionSubjectData setPermission(Set<Context> contexts, String permission, int value) {
        if (value == 0) {
            return newData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(immutSet(contexts), getDataEntryOrNew(contexts).withoutPermission(permission)).build());
        } else {
            return newData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(immutSet(contexts), getDataEntryOrNew(contexts).withPermission(permission, value)).build());
        }
    }

    @Override
    public ImmutableOptionSubjectData clearPermissions() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Context>, DataEntry> newValue = Maps.transformValues(this.contexts, new Function<DataEntry, DataEntry>() {
            @Nullable
            @Override
            public DataEntry apply(@Nullable DataEntry dataEntry) {
                return dataEntry.withoutPermissions();
            }
        });
        return newData(newValue);
    }

    @Override
    public ImmutableOptionSubjectData clearPermissions(Set<Context> contexts) {
        if (!this.contexts.containsKey(contexts)) {
            return this;
        }
        return newData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(immutSet(contexts), getDataEntryOrNew(contexts).withoutPermissions()).build());

    }

    private static final Function<String, Map.Entry<String, String>> PARENT_TRANSFORM_FUNC = new Function<String, Map.Entry<String, String>>() {
                    @Nullable
                    @Override
                    public Map.Entry<String, String> apply(String input) {
                        String[] split = input.split(":", 2);
                        return Maps.immutableEntry(split.length > 1 ? split[0] : "group", split.length > 1 ? split[1]: split[0]);
                    }
                };

    @Override
    public Map<Set<Context>, List<Map.Entry<String, String>>> getAllParents() {
        return Maps.filterValues(Maps.transformValues(contexts, new Function<DataEntry, List<Map.Entry<String, String>>>() {
            @Nullable
            @Override
            public List<Map.Entry<String, String>> apply(@Nullable DataEntry dataEntry) {
                return dataEntry.parents == null ? null : Lists.transform(dataEntry.parents, PARENT_TRANSFORM_FUNC);
            }
        }), Predicates.notNull());
    }

    @Override
    public List<Map.Entry<String, String>> getParents(Set<Context> contexts) {
        DataEntry ent = this.contexts.get(contexts);
        return ent == null || ent.parents == null ? Collections.<Map.Entry<String, String>>emptyList() : Lists.transform(ent.parents, PARENT_TRANSFORM_FUNC);
    }

    @Override
    public ImmutableOptionSubjectData addParent(Set<Context> contexts, String type, String ident) {
        DataEntry entry = getDataEntryOrNew(contexts);
        return newData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(immutSet(contexts), entry.withAddedParent(type + ":" + ident)).build());
    }

    @Override
    public ImmutableOptionSubjectData removeParent(Set<Context> contexts, String type, String identifier) {
        DataEntry ent = this.contexts.get(contexts);
        if (ent == null) {
            return this;
        }

        final String combined = type + ":" + identifier;
        if (!ent.parents.contains(combined)) {
            return this;
        }
        return newData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(immutSet(contexts), ent.withRemovedParent(combined)).build());
    }

    @Override
    public ImmutableOptionSubjectData clearParents() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Context>, DataEntry> newValue = Maps.transformValues(this.contexts, new Function<DataEntry, DataEntry>() {
            @Nullable
            @Override
            public DataEntry apply(@Nullable DataEntry dataEntry) {
                return dataEntry.withoutParents();
            }
        });
        return newData(newValue);
    }

    @Override
    public ImmutableOptionSubjectData clearParents(Set<Context> contexts) {
        if (!this.contexts.containsKey(contexts)) {
            return this;
        }
        return newData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(immutSet(contexts), getDataEntryOrNew(contexts).withoutParents()).build());
    }

    public int getDefaultValue(Set<Context> contexts) {
        DataEntry ent = this.contexts.get(contexts);
        return ent == null ? 0 : ent.defaultValue;
    }

    public ImmutableOptionSubjectData setDefaultValue(Set<Context> contexts, int defaultValue) {
        return newData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(immutSet(contexts), getDataEntryOrNew(contexts).withDefaultValue(defaultValue)).build());
    }

    @Override
    public Iterable<Set<Context>> getActiveContexts() {
        return contexts.keySet();
    }

    @Override
    public String toString() {
        return "MemoryOptionSubjectData{" +
                "contexts=" + contexts +
                '}';
    }
}