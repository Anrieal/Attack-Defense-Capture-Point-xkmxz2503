package com.xkmxz.attack_defense_capture_point_xkmxz.nodegraph;

/**
 * 判断器条件类型枚举 — 只能选择，不能手动输入。<br>
 * <br>
 * <b>适用信号类型：</b>
 * <ul>
 *   <li>POINT_SIGNAL + ZONE_SIGNAL：captured, not_captured, owner_team</li>
 *   <li>仅 POINT_SIGNAL：capturing, not_capturing</li>
 * </ul>
 */
public enum ConditionMode {
    CAPTURED("captured"),
    NOT_CAPTURED("not_captured"),
    OWNER_TEAM("owner_team"),
    CAPTURING("capturing"),
    NOT_CAPTURING("not_capturing");

    private final String id;

    ConditionMode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /** 通过 ID 获取枚举，不存在返回 CAPTURED */
    public static ConditionMode fromId(String id) {
        if (id == null || id.isEmpty()) return CAPTURED;
        for (var mode : values()) {
            if (mode.id.equals(id)) return mode;
        }
        return CAPTURED;
    }
}
