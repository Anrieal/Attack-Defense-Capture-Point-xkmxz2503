package com.xkmxz.attack_defense_capture_point_xkmxz.nodegraph;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;

import java.util.List;

/**
 * 据点管理图 - 包含据点节点和区域节点，
 * 用于可视化和编辑攻防战中的占领点与区域关系。
 */
public class CapturePointGraph extends Graph {

    private static final List<Class<? extends Node>> SUPPORT_NODES = List.of(
            CapturePointNode.class,
            CaptureZoneNode.class
    );

    private static final List<TypeHandle> SUPPORT_TYPES = List.of(
            CapturePointTypes.POINT_SIGNAL,
            CapturePointTypes.ZONE_SIGNAL
    );

    @Override
    public List<Class<? extends Node>> getSupportNodes() {
        return SUPPORT_NODES;
    }

    @Override
    public List<TypeHandle> getSupportTypes() {
        return SUPPORT_TYPES;
    }
}
