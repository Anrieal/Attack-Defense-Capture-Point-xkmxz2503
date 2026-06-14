package com.xkmxz.attack_defense_capture_point_xkmxz.manager;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class CaptureManager extends SavedData {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_NAME = "capture_points_data";

    private final Map<String, CapturePointEntry> points = new LinkedHashMap<>();
    private final Map<String, ZoneEntry> zones = new LinkedHashMap<>();

    // ---- Data Records ----

    public record CapturePointEntry(String name, BlockPos pos, @Nullable String owner) {
        public CompoundTag toNbt() {
            var tag = new CompoundTag();
            tag.putString("name", name);
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            if (owner != null) tag.putString("owner", owner);
            return tag;
        }

        public static CapturePointEntry fromNbt(CompoundTag tag) {
            var name = tag.getString("name");
            var pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            var owner = tag.contains("owner") ? tag.getString("owner") : null;
            return new CapturePointEntry(name, pos, owner);
        }

        public CapturePointEntry withOwner(@Nullable String newOwner) {
            return new CapturePointEntry(name, pos, newOwner);
        }
    }

    public record ZoneEntry(String name, List<String> capturePoints, @Nullable String requiredZone) {

        public CompoundTag toNbt() {
            var tag = new CompoundTag();
            tag.putString("name", name);
            var list = new ListTag();
            for (var cp : capturePoints) {
                list.add(StringTag.valueOf(cp));
            }
            tag.put("capturePoints", list);
            if (requiredZone != null) tag.putString("requiredZone", requiredZone);
            return tag;
        }

        public static ZoneEntry fromNbt(CompoundTag tag) {
            var name = tag.getString("name");
            var list = tag.getList("capturePoints", Tag.TAG_STRING);
            var points = new ArrayList<String>();
            for (int i = 0; i < list.size(); i++) {
                points.add(list.getString(i));
            }
            var requiredZone = tag.contains("requiredZone") ? tag.getString("requiredZone") : null;
            return new ZoneEntry(name, points, requiredZone);
        }

        public boolean isCaptured(Map<String, CapturePointEntry> pointsMap) {
            for (var cpName : capturePoints) {
                var cp = pointsMap.get(cpName);
                if (cp == null || cp.owner() == null) return false;
            }
            return true;
        }
    }

    // ---- Singleton Access ----

    public static CaptureManager get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("CaptureManager can only be accessed from server side");
        }
        DimensionDataStorage storage = serverLevel.getDataStorage();
        return storage.computeIfAbsent(new SavedData.Factory<>(CaptureManager::new, CaptureManager::new), DATA_NAME);
    }

    public CaptureManager() {
    }

    public CaptureManager(CompoundTag tag, HolderLookup.Provider registries) {
        load(tag, registries);
    }

    // ---- Data Access ----

    public Map<String, CapturePointEntry> getPoints() {
        return Collections.unmodifiableMap(points);
    }

    public Map<String, ZoneEntry> getZones() {
        return Collections.unmodifiableMap(zones);
    }

    public void addOrUpdatePoint(String name, BlockPos pos) {
        points.put(name, new CapturePointEntry(name, pos, null));
        setDirty();
    }

    public void removePoint(String name) {
        points.remove(name);
        setDirty();
    }

    public void setPointOwner(String name, @Nullable String owner) {
        var existing = points.get(name);
        if (existing != null) {
            points.put(name, existing.withOwner(owner));
            setDirty();
        }
    }

    public void createZone(String name, @Nullable String requiredZone) {
        zones.put(name, new ZoneEntry(name, new ArrayList<>(), requiredZone));
        setDirty();
    }

    public void removeZone(String name) {
        zones.remove(name);
        setDirty();
    }

    public void addPointToZone(String zoneName, String pointName) {
        var zone = zones.get(zoneName);
        if (zone != null) {
            var newList = new ArrayList<>(zone.capturePoints());
            if (!newList.contains(pointName)) {
                newList.add(pointName);
                zones.put(zoneName, new ZoneEntry(zone.name(), newList, zone.requiredZone()));
                setDirty();
            }
        }
    }

    public void removePointFromZone(String zoneName, String pointName) {
        var zone = zones.get(zoneName);
        if (zone != null) {
            var newList = new ArrayList<>(zone.capturePoints());
            newList.remove(pointName);
            zones.put(zoneName, new ZoneEntry(zone.name(), newList, zone.requiredZone()));
            setDirty();
        }
    }

    public boolean isZoneCaptured(String zoneName) {
        var zone = zones.get(zoneName);
        if (zone == null) return false;
        for (var cpName : zone.capturePoints()) {
            var cp = points.get(cpName);
            if (cp == null || cp.owner() == null) return false;
        }
        return true;
    }

    public boolean canAccessZone(String zoneName) {
        var zone = zones.get(zoneName);
        if (zone == null) return false;
        if (zone.requiredZone() == null || zone.requiredZone().isEmpty()) return true;
        return isZoneCaptured(zone.requiredZone());
    }

    // ---- Persistence ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var pointsList = new ListTag();
        for (var entry : points.values()) {
            pointsList.add(entry.toNbt());
        }
        tag.put("points", pointsList);

        var zonesList = new ListTag();
        for (var entry : zones.values()) {
            zonesList.add(entry.toNbt());
        }
        tag.put("zones", zonesList);

        return tag;
    }

    private void load(CompoundTag tag, HolderLookup.Provider registries) {
        points.clear();
        zones.clear();

        var pointsList = tag.getList("points", Tag.TAG_COMPOUND);
        for (int i = 0; i < pointsList.size(); i++) {
            var entry = CapturePointEntry.fromNbt(pointsList.getCompound(i));
            points.put(entry.name(), entry);
        }

        var zonesList = tag.getList("zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < zonesList.size(); i++) {
            var entry = ZoneEntry.fromNbt(zonesList.getCompound(i));
            zones.put(entry.name(), entry);
        }

        LOGGER.info("Loaded {} capture points and {} zones", points.size(), zones.size());
    }
}
