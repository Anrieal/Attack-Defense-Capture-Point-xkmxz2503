package com.xkmxz.attack_defense_capture_point_xkmxz.gui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * Client-side control panel for managing capture points and zones.
 * Uses chat commands to interact with the server-side CaptureManager.
 */
public class ControlPanelUI {

    private final Level level;

    public ControlPanelUI(Level level) {
        this.level = level;
    }

    public void open() {
        var root = new UIElement()
                .layout(layout -> layout.paddingAll(7).gapAll(5).width(400).height(280))
                .style(style -> style.background(Sprites.BORDER));

        // Title
        var title = new Label()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.title"));
        root.addChildren(title);

        // ---- Capture Point Operations ----
        var pointsLabel = new Label()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.points_section"));
        root.addChildren(pointsLabel);

        var createAtPosBtn = new Button()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.create_at_pos"))
                .setOnClick(event -> {
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        player.connection.sendCommand("capturepoint create " + generatePointName());
                        player.sendSystemMessage(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.created_hint"));
                    }
                });
        root.addChildren(createAtPosBtn);

        var listBtn = new Button()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.list_points"))
                .setOnClick(event -> {
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        player.connection.sendCommand("capturepoint list");
                    }
                });
        root.addChildren(listBtn);

        // ---- Zone Operations ----
        var zonesLabel = new Label()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.zones_section"));
        root.addChildren(zonesLabel);

        var zoneStatusBtn = new Button()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.zone_status"))
                .setOnClick(event -> {
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        player.connection.sendCommand("capturepoint zone status");
                    }
                });
        root.addChildren(zoneStatusBtn);

        // ---- Quick Actions ----
        var actionsLabel = new Label()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.quick_actions"));
        root.addChildren(actionsLabel);

        // Quick create zone A with point
        var quickZoneABtn = new Button()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.create_zone_a"))
                .setOnClick(event -> {
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        player.connection.sendCommand("capturepoint zone create ZoneA");
                        player.connection.sendCommand("capturepoint create A1");
                        player.connection.sendCommand("capturepoint zone addpoint ZoneA A1");
                        player.connection.sendCommand("capturepoint create A2");
                        player.connection.sendCommand("capturepoint zone addpoint ZoneA A2");
                        player.sendSystemMessage(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.zone_created_hint"));
                    }
                });
        root.addChildren(quickZoneABtn);

        var quickZoneBBtn = new Button()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.create_zone_b"))
                .setOnClick(event -> {
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        player.connection.sendCommand("capturepoint zone create ZoneB ZoneA");
                        player.connection.sendCommand("capturepoint create B1");
                        player.connection.sendCommand("capturepoint zone addpoint ZoneB B1");
                        player.connection.sendCommand("capturepoint create B2");
                        player.connection.sendCommand("capturepoint zone addpoint ZoneB B2");
                        player.sendSystemMessage(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.zone_created_hint"));
                    }
                });
        root.addChildren(quickZoneBBtn);

        // Refresh info message
        var infoLabel = new Label()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.info_hint"));
        root.addChildren(infoLabel);

        // Close button
        var closeBtn = new Button()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.close"))
                .setOnClick(event -> {
                    Minecraft.getInstance().setScreen(null);
                });
        root.addChildren(closeBtn);

        var ui = ModularUI.of(UI.of(root));
        Minecraft.getInstance().setScreen(
                new ModularUIScreen(ui, Component.translatable("gui.attack_defense_capture_point_xkmxz.control_panel.title"))
        );
    }

    private static int pointCounter = 0;

    private static String generatePointName() {
        pointCounter++;
        return "P" + pointCounter;
    }
}
