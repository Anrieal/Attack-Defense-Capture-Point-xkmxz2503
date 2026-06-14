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
    private static final String TAG_VERSION = "version";

    private final Map<String, CapturePointEntry> points = new LinkedHashMap<>();
    private final Map<String, ZoneEntry> zones = new LinkedHashMap<>();
    private long version = 0; // 数据版本号，每次写入递增，用于 GUI 检测外部修改

    // ---- Data Records ----

    public record CapturePointEntry(String name, BlockPos pos, @Nullable String owner,
                                    double radius, int displayColor, boolean showRange) {
        public static final double DEFAULT_RADIUS = 5.0;
        public static final int DEFAULT_COLOR = 0xFFFF4444;

        public CompoundTag toNbt() {
            var tag = new CompoundTag();
            tag.putString("name", name);
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            if (owner != null) tag.putString("owner", owner);
            tag.putDouble("radius", radius);
            tag.putInt("displayColor", displayColor);
            tag.putBoolean("showRange", showRange);
            return tag;
        }

        public static CapturePointEntry fromNbt(CompoundTag tag) {
            var name = tag.getString("name");
            var pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            var owner = tag.contains("owner") ? tag.getString("owner") : null;
            var radius = tag.contains("radius") ? tag.getDouble("radius") : DEFAULT_RADIUS;
            var displayColor = tag.contains("displayColor") ? tag.getInt("displayColor") : DEFAULT_COLOR;
            var showRange = tag.contains("showRange") && tag.getBoolean("showRange");
            return new CapturePointEntry(name, pos, owner, radius, displayColor, showRange);
        }

        public CapturePointEntry withOwner(@Nullable String newOwner) {
            return new CapturePointEntry(name, pos, newOwner, radius, displayColor, showRange);
        }

        public CapturePointEntry withRadius(double newRadius) {
            return new CapturePointEntry(name, pos, owner, newRadius, displayColor, showRange);
        }

        public CapturePointEntry withDisplayColor(int newColor) {
            return new CapturePointEntry(name, pos, owner, radius, newColor, showRange);
        }

        public CapturePointEntry withShowRange(boolean newShowRange) {
            return new CapturePointEntry(name, pos, owner, radius, displayColor, newShowRange);
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

    // ---- Version & Batch Write ----

    /** 获取当前数据版本号，每次写入递增 */
    public long getVersion() {
        return version;
    }

    /** 递增版本号并标记脏数据 */
    private void bumpVersion() {
        version++;
        setDirty();
    }

    /**
     * 批量应用 GUI 编辑器的完整数据快照（原子操作，版本号只递增一次）。
     * 此方法是 GUI 编辑模式保存时的唯一写入入口，
     * 命令和方块的其他单次写入仍走各自的 setXxx 方法。
     */
    public void applyGraphSnapshot(Map<String, CapturePointEntry> newPoints,
                                    Map<String, ZoneEntry> newZones) {
        points.clear();
        zones.clear();
        points.putAll(newPoints);
        zones.putAll(newZones);
        bumpVersion();
        LOGGER.info("Applied graph snapshot: {} points, {} zones (version {})",
                points.size(), zones.size(), version);
    }

    // ---- Data Access ----

    public Map<String, CapturePointEntry> getPoints() {
        return Collections.unmodifiableMap(points);
    }

    public Map<String, ZoneEntry> getZones() {
        return Collections.unmodifiableMap(zones);
    }

    public void addOrUpdatePoint(String name, BlockPos pos) {
        points.put(name, new CapturePointEntry(name, pos, null,
                CapturePointEntry.DEFAULT_RADIUS, CapturePointEntry.DEFAULT_COLOR, false));
        bumpVersion();
    }

    public void addOrUpdatePointWithRadius(String name, BlockPos pos, double radius) {
        points.put(name, new CapturePointEntry(name, pos, null,
                radius, CapturePointEntry.DEFAULT_COLOR, false));
        bumpVersion();
    }

    public void removePoint(String name) {
        points.remove(name);
        bumpVersion();
    }

    public void setPointOwner(String name, @Nullable String owner) {
        var existing = points.get(name);
        if (existing != null) {
            points.put(name, existing.withOwner(owner));
            bumpVersion();
        }
    }

    public void setPointRadius(String name, double radius) {
        var existing = points.get(name);
        if (existing != null) {
            points.put(name, existing.withRadius(radius));
            bumpVersion();
        }
    }

    public void setPointDisplayColor(String name, int color) {
        var existing = points.get(name);
        if (existing != null) {
            points.put(name, existing.withDisplayColor(color));
            bumpVersion();
        }
    }

    public void setPointShowRange(String name, boolean showRange) {
        var existing = points.get(name);
        if (existing != null) {
            points.put(name, existing.withShowRange(showRange));
            bumpVersion();
        }
    }

    public void createZone(String name, @Nullable String requiredZone) {
        zones.put(name, new ZoneEntry(name, new ArrayList<>(), requiredZone));
        bumpVersion();
    }

    public void removeZone(String name) {
        zones.remove(name);
        bumpVersion();
    }

    public void addPointToZone(String zoneName, String pointName) {
        var zone = zones.get(zoneName);
        if (zone != null) {
            var newList = new ArrayList<>(zone.capturePoints());
            if (!newList.contains(pointName)) {
                newList.add(pointName);
                zones.put(zoneName, new ZoneEntry(zone.name(), newList, zone.requiredZone()));
                bumpVersion();
            }
        }
    }

    public void removePointFromZone(String zoneName, String pointName) {
        var zone = zones.get(zoneName);
        if (zone != null) {
            var newList = new ArrayList<>(zone.capturePoints());
            newList.remove(pointName);
            zones.put(zoneName, new ZoneEntry(zone.name(), newList, zone.requiredZone()));
            bumpVersion();
        }
    }

    /**
     * 设置区域的依赖区域（requiredZone），即攻防模式下前置区域。
     * 此方法直接操作内部 zones 列表，不同于 getZones() 返回的不可修改视图。
     * @param zoneName 要修改的区域名称
     * @param requiredZone 依赖的前置区域名称（null 表示清除依赖）
     */
    public void setZoneRequiredZone(String zoneName, @Nullable String requiredZone) {
        var zone = zones.get(zoneName);
        if (zone != null) {
            // 保留原有据点列表，仅修改区域依赖
            zones.put(zoneName, new ZoneEntry(zone.name(), new ArrayList<>(zone.capturePoints()), requiredZone));
            bumpVersion();
        }
    }

    /**
     * 查找包含指定据点的区域名称。
     * @return 区域名称，如果据点不属于任何区域则返回 null
     */
    @Nullable
    public String findZoneForPoint(String pointName) {
        for (var entry : zones.entrySet()) {
            if (entry.getValue().capturePoints().contains(pointName)) {
                return entry.getKey();
            }
        }
        return null;
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

        tag.putLong(TAG_VERSION, version);

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

        version = tag.contains(TAG_VERSION, Tag.TAG_LONG) ? tag.getLong(TAG_VERSION) : 0;

        LOGGER.info("Loaded {} capture points, {} zones (version {})", points.size(), zones.size(), version);
    }
}
