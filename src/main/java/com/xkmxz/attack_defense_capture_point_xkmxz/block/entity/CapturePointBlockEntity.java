package com.xkmxz.attack_defense_capture_point_xkmxz.block.entity;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.xkmxz.attack_defense_capture_point_xkmxz.manager.CaptureManager;
import com.xkmxz.attack_defense_capture_point_xkmxz.network.BlockEntityActionPayload;
import com.xkmxz.attack_defense_capture_point_xkmxz.nodegraph.CapturePointGraphScreen;
import com.xkmxz.attack_defense_capture_point_xkmxz.nodegraph.ToastNotification;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class CapturePointBlockEntity extends BlockEntity {

    public static BlockEntityType<CapturePointBlockEntity> TYPE;

    // ---- 持久化数据 ----
    // boundPointName 是方块与据点的绑定关系（唯一真正的本地数据）
    private String boundPointName = "";       // 绑定的据点名称（空=未绑定）

    // ---- 本地缓存（从 CaptureManager 同步，用于客户端渲染） ----
    private double radius = 5.0;              // 据点半径
    private int displayColor = 0xFFFF4444;    // 显示颜色（ARGB，默认红色）
    private boolean showRange = false;        // 是否显示范围轮廓

    // ---- NBT 键名 ----
    private static final String TAG_BOUND_POINT = "boundPointName";
    private static final String TAG_RADIUS = "radius";
    private static final String TAG_DISPLAY_COLOR = "displayColor";
    private static final String TAG_SHOW_RANGE = "showRange";

    public CapturePointBlockEntity(BlockPos pos, BlockState blockState) {
        super(TYPE, pos, blockState);
    }

    // ---- NBT 持久化 ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(TAG_BOUND_POINT, boundPointName);
        tag.putDouble(TAG_RADIUS, radius);
        tag.putInt(TAG_DISPLAY_COLOR, displayColor);
        tag.putBoolean(TAG_SHOW_RANGE, showRange);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        boundPointName = tag.contains(TAG_BOUND_POINT) ? tag.getString(TAG_BOUND_POINT) : "";
        radius = tag.contains(TAG_RADIUS) ? tag.getDouble(TAG_RADIUS) : 5.0;
        displayColor = tag.contains(TAG_DISPLAY_COLOR) ? tag.getInt(TAG_DISPLAY_COLOR) : 0xFFFF4444;
        showRange = tag.contains(TAG_SHOW_RANGE) && tag.getBoolean(TAG_SHOW_RANGE);
    }

    // ---- Getter（仅提供读取，修改通过网络包发送到服务端） ----

    public String getBoundPointName() { return boundPointName; }
    public double getRadius() { return radius; }
    public int getDisplayColor() { return displayColor; }
    public boolean isShowRange() { return showRange; }

    /** 同步数据到客户端（仅服务端调用） */
    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * 设置绑定名并同步到客户端（仅在服务端调用）。
     * 客户端UI请使用网络包通信。
     */
    public void setBoundPointNameFromServer(String name) {
        this.boundPointName = name;
        setChanged();
        syncToClient();
    }

    /**
     * 从服务端更新本地缓存数据（由 CaptureManager 变更后调用）。
     */
    public void updateCacheFromServer(double newRadius, int newColor, boolean newShowRange) {
        this.radius = newRadius;
        this.displayColor = newColor;
        this.showRange = newShowRange;
        setChanged();
        syncToClient();
    }

    // ================================================================
    //  网络包通信辅助方法
    // ================================================================

    /**
     * 通过网络包发送方块操作到服务端。
     * 客户端→服务端严格分离，不直接访问 CaptureManager。
     *
     * @param action 操作类型
     * @param data   操作数据（逗号分隔）
     */
    private void sendAction(String action, String data) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new BlockEntityActionPayload(worldPosition, action, data));
    }

    /**
     * 获取服务端 CaptureManager（仅限读取/单机回退）。
     * 警告：此方法仅在单机/内嵌服务端模式下可用，
     * 专用服务器模式下返回 null。
     * 所有写入操作必须通过 sendAction() 网络包实现。
     */
    private CaptureManager getServerCaptureManager() {
        if (level instanceof ServerLevel sl) {
            return CaptureManager.get(sl);
        }
        var mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            return CaptureManager.get(mc.getSingleplayerServer().getLevel(level.dimension()));
        }
        return null;
    }

    // ================================================================
    //  打开方块控制菜单（客户端调用）
    // ================================================================

    public void openUI(Player player) {
        if (level == null || level.isClientSide) {
            openMenuScreen();
        }
    }

    /**
     * 创建并显示方块功能菜单屏幕。
     * 自动缩放：宽度取屏幕 40%（最大 260px），高度按内容紧凑计算，居中显示。
     */
    private void openMenuScreen() {
        var mc = Minecraft.getInstance();
        var win = mc.getWindow();
        int scw = win.getGuiScaledWidth();

        // 自适应宽度：屏幕 40% 但不超过 260px
        int panelW = Math.min(scw * 40 / 100, 260);
        // 紧凑尺寸
        int btnH = 22;
        int gap = 3;
        int titleH = 14;  // 标题行高
        int statusH = 12; // 绑定状态行高
        int bottomH = 20;
        int pad = 6;
        int panelH = pad + titleH + statusH + gap + 6 * (btnH + gap) + bottomH + pad;

        int bg = 0xFF1A1A2E;
        int btnBg = 0xFF16213E;

        var root = new UIElement()
                .layout(l -> l.width(panelW).height(panelH).paddingAll(pad).gapAll(gap)
                        .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN))
                .style(s -> s.background(Sprites.BORDER)
                        .backgroundTexture(new ColorRectTexture(bg)));

        // ---- 标题区域：标题在上，绑定状态在下 ----
        var titleLabel = new Label().setText(Component.translatable("gui.capture_point_block.menu.title"));
        titleLabel.layout(l -> l.widthPercent(100).height(titleH));
        titleLabel.textStyle(s -> s.fontSize(10.0f).textColor(0xFFAAAAAA));
        root.addChildren(titleLabel);

        var boundLabel = new Label();
        updateBoundLabel(boundLabel);
        boundLabel.layout(l -> l.widthPercent(100).height(statusH));
        boundLabel.textStyle(s -> s.fontSize(9.0f).textColor(0xFF888888));
        root.addChildren(boundLabel);

        // ---- 功能按钮（紧凑） ----
        root.addChildren(createFuncButton(btnBg, btnH,
                "gui.capture_point_block.func1", this::funcCreatePoint));
        root.addChildren(createFuncButton(btnBg, btnH,
                "gui.capture_point_block.func2", this::funcBindZone));
        root.addChildren(createFuncButton(btnBg, btnH,
                "gui.capture_point_block.func3", this::funcViewStatus));
        root.addChildren(createFuncButton(btnBg, btnH,
                "gui.capture_point_block.func4", () -> funcSetRadius(mc)));
        root.addChildren(createFuncButton(btnBg, btnH,
                "gui.capture_point_block.func5", () -> funcToggleShowRange(mc)));
        root.addChildren(createFuncButton(btnBg, btnH,
                "gui.capture_point_block.func6", () -> funcRemoveBinding(mc)));

        // ---- 底部按钮行（紧凑） ----
        var bottomRow = new UIElement()
                .layout(l -> l.widthPercent(100).height(bottomH)
                        .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW).gapAll(3));
        var openGraphBtn = new Button().setText(Component.translatable("gui.capture_point_block.open_graph"));
        openGraphBtn.layout(l -> l.flex(1).heightPercent(100));
        openGraphBtn.setOnClick(e -> {
            mc.setScreen(null);
            new CapturePointGraphScreen(level).open();
        });
        var closeBtn = new Button().setText(Component.translatable("gui.capture_point_block.close"));
        closeBtn.layout(l -> l.width(44).heightPercent(100));
        closeBtn.setOnClick(e -> mc.setScreen(null));
        bottomRow.addChildren(openGraphBtn, closeBtn);
        root.addChildren(bottomRow);

        // 居中容器
        var wrap = new UIElement()
                .layout(l -> l.widthPercent(100).heightPercent(100).paddingAll(0).gapAll(0)
                        .justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER)
                        .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        wrap.addChildren(root);

        var ui = ModularUI.of(UI.of(wrap));
        mc.setScreen(new ModularUIScreen(ui,
                Component.translatable("gui.capture_point_block.menu.title")));
    }

    private UIElement createFuncButton(int bg, int h, String langKey, Runnable onClick) {
        var btn = new Button()
                .setText(Component.translatable(langKey));
        btn.layout(l -> l.widthPercent(100).height(h));
        btn.style(s -> s.background(Sprites.BORDER)
                .backgroundTexture(new ColorRectTexture(bg)));
        btn.setOnClick(e -> onClick.run());
        return btn;
    }

    private void updateBoundLabel(Label label) {
        if (boundPointName.isEmpty()) {
            label.setText(Component.translatable("gui.capture_point_block.unbound"));
        } else {
            label.setText(Component.translatable("gui.capture_point_block.bound", boundPointName));
        }
    }

    // ================================================================
    //  6 个功能实现 — 全部通过网络包通信，禁止直接访问服务端 CaptureManager
    // ================================================================

    /**
     * 功能1: 以据点方块坐标为中心创建并绑定据点。
     * 通过网络包发送到服务端处理。
     */
    private void funcCreatePoint() {
        if (!boundPointName.isEmpty()) {
            ToastNotification.push(ToastNotification.Type.INFO,
                    Component.translatable("toast.capture_point_block.already_bound", boundPointName));
            reopenMenu();
            return;
        }

        var pos = worldPosition;
        // 生成默认名称：capture_<x>_<y>_<z>
        String name = "capture_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();

        // 乐观更新：先设置本地绑定名
        boundPointName = name;
        setChanged();

        // 发送网络包到服务端创建据点
        sendAction("create_point_at", name + "," + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + radius);

        ToastNotification.push(ToastNotification.Type.SUCCESS,
                Component.translatable("toast.capture_point_block.created", name));
        reopenMenu();
    }

    /**
     * 功能2: 将已绑定的据点绑定到区域。
     * 弹出区域选择对话框。
     * 注意：区域列表需要服务端数据，专用服务器模式下可能不可用。
     */
    private void funcBindZone() {
        if (boundPointName.isEmpty()) {
            ToastNotification.push(ToastNotification.Type.ERROR,
                    Component.translatable("toast.capture_point_block.not_bound"));
            reopenMenu();
            return;
        }

        openZoneSelectDialog();
    }

    /**
     * 功能3: 查看据点状态。
     * 使用本地缓存数据展示；完整状态信息（所属、区域）需要服务端数据。
     */
    private void funcViewStatus() {
        if (boundPointName.isEmpty()) {
            ToastNotification.push(ToastNotification.Type.INFO,
                    Component.translatable("toast.capture_point_block.not_bound"));
            reopenMenu();
            return;
        }

        // 优先显示本地缓存数据（始终可用）
        ToastNotification.push(ToastNotification.Type.INFO,
                Component.translatable("toast.capture_point_block.status_name", boundPointName));
        ToastNotification.push(ToastNotification.Type.INFO,
                Component.translatable("toast.capture_point_block.status_pos",
                        worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()));
        ToastNotification.push(ToastNotification.Type.INFO,
                Component.translatable("toast.capture_point_block.status_radius", (int) radius));
        ToastNotification.push(ToastNotification.Type.INFO,
                Component.translatable("toast.capture_point_block.status_range", showRange ? "ON" : "OFF"));

        // 尝试获取服务端补充数据（仅单机可用）
        var mgr = getServerCaptureManager();
        if (mgr != null) {
            var entry = mgr.getPoints().get(boundPointName);
            if (entry != null) {
                String owner = entry.owner() != null ? entry.owner() : "none";
                ToastNotification.push(ToastNotification.Type.INFO,
                        Component.translatable("toast.capture_point_block.status_owner", owner));
            }
        }

        reopenMenu();
    }

    /**
     * 功能4: 设定据点大小（半径）。
     * 通过网络包发送到服务端处理。
     */
    private void funcSetRadius(Minecraft mc) {
        openInputDialog(
                Component.translatable("gui.capture_point_block.dialog.radius.title"),
                Component.translatable("gui.capture_point_block.dialog.radius.label"),
                String.valueOf((int) radius),
                (input) -> {
                    try {
                        double newRadius = Double.parseDouble(input);
                        if (newRadius < 1 || newRadius > 100) {
                            ToastNotification.push(ToastNotification.Type.ERROR,
                                    Component.translatable("toast.capture_point_block.radius_invalid"));
                            reopenMenu();
                            return;
                        }

                        // 更新本地缓存
                        radius = newRadius;
                        setChanged();

                        // 如果已绑定据点，发送网络包到服务端
                        if (!boundPointName.isEmpty()) {
                            sendAction("set_radius", boundPointName + "," + newRadius);
                        }

                        ToastNotification.push(ToastNotification.Type.SUCCESS,
                                Component.translatable("toast.capture_point_block.radius_set", (int) newRadius));
                    } catch (NumberFormatException e) {
                        ToastNotification.push(ToastNotification.Type.ERROR,
                                Component.translatable("toast.capture_point_block.radius_invalid"));
                    }
                    reopenMenu();
                }
        );
    }

    /**
     * 功能5: 切换显示据点范围并允许自定义颜色。
     * 通过网络包发送到服务端处理。
     */
    private void funcToggleShowRange(Minecraft mc) {
        // 切换显示状态
        showRange = !showRange;
        setChanged();

        // 通过网络包同步到服务端
        if (!boundPointName.isEmpty()) {
            sendAction("toggle_range", boundPointName + "," + showRange);
        }

        if (showRange) {
            openColorPickerDialog();
        } else {
            ToastNotification.push(ToastNotification.Type.SUCCESS,
                    Component.translatable("toast.capture_point_block.range_off"));
            reopenMenu();
        }
    }

    /**
     * 功能6: 移除绑定 — 打开子菜单选择操作。
     * 通过网络包发送到服务端处理。
     */
    private void funcRemoveBinding(Minecraft mc) {
        if (boundPointName.isEmpty()) {
            ToastNotification.push(ToastNotification.Type.ERROR,
                    Component.translatable("toast.capture_point_block.not_bound"));
            reopenMenu();
            return;
        }

        int dw = 260, dh = 110;
        var root = new UIElement()
                .layout(l -> l.width(dw).height(dh).paddingAll(10).gapAll(6)
                        .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN))
                .style(s -> s.background(Sprites.BORDER)
                        .backgroundTexture(new ColorRectTexture(0xFF1A1A2E)));

        var title = new Label().setText(Component.translatable("gui.capture_point_block.dialog.unbind.title"));
        title.layout(l -> l.widthPercent(100).heightAuto());
        root.addChildren(title);

        var unbindBlockBtn = new Button().setText(Component.translatable("gui.capture_point_block.dialog.unbind.block"));
        unbindBlockBtn.layout(l -> l.widthPercent(100).height(24));
        unbindBlockBtn.setOnClick(e -> {
            // 仅清除方块实体的绑定，不删除服务端据点
            boundPointName = "";
            setChanged();
            ToastNotification.push(ToastNotification.Type.SUCCESS,
                    Component.translatable("toast.capture_point_block.unbind_block"));
            mc.setScreen(null);
            reopenMenu();
        });
        root.addChildren(unbindBlockBtn);

        var unbindZoneBtn = new Button().setText(Component.translatable("gui.capture_point_block.dialog.unbind.zone"));
        unbindZoneBtn.layout(l -> l.widthPercent(100).height(24));
        unbindZoneBtn.setOnClick(e -> {
            String currentName = boundPointName;
            // 发送网络包到服务端处理
            sendAction("remove_from_zone", currentName);
            ToastNotification.push(ToastNotification.Type.SUCCESS,
                    Component.translatable("toast.capture_point_block.unbind_zone", currentName));
            mc.setScreen(null);
            reopenMenu();
        });
        root.addChildren(unbindZoneBtn);

        var cancelBtn = new Button().setText(Component.translatable("gui.capture_point_graph.dialog.cancel"));
        cancelBtn.layout(l -> l.widthPercent(100).height(20));
        cancelBtn.setOnClick(e -> {
            mc.setScreen(null);
            reopenMenu();
        });
        root.addChildren(cancelBtn);

        var ui = ModularUI.of(UI.of(root));
        mc.setScreen(new ModularUIScreen(ui,
                Component.translatable("gui.capture_point_block.dialog.unbind.title")));
    }

    // ================================================================
    //  区域选择对话框
    //  注意：区域列表需要服务端数据。
    //  单机模式下通过 getServerCaptureManager() 读取，
    //  专用服务器模式下不可用，请使用 /capturepoint 命令。
    // ================================================================

    private void openZoneSelectDialog() {
        var mc = Minecraft.getInstance();

        // 尝试获取区域列表（仅单机可用）
        var mgr = getServerCaptureManager();
        if (mgr == null) {
            ToastNotification.push(ToastNotification.Type.ERROR,
                    Component.translatable("toast.capture_point_block.server_only"));
            reopenMenu();
            return;
        }

        var zones = mgr.getZones();
        if (zones.isEmpty()) {
            ToastNotification.push(ToastNotification.Type.ERROR,
                    Component.translatable("toast.capture_point_block.no_zones"));
            reopenMenu();
            return;
        }

        int dw = 300, dh = 40 + zones.size() * 30 + 40;
        var root = new UIElement()
                .layout(l -> l.width(dw).height(dh).paddingAll(12).gapAll(6)
                        .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN))
                .style(s -> s.background(Sprites.BORDER)
                        .backgroundTexture(new ColorRectTexture(0xFF1A1A2E)));

        var title = new Label().setText(Component.translatable("gui.capture_point_block.dialog.select_zone"));
        title.layout(l -> l.widthPercent(100).heightAuto());
        root.addChildren(title);

        for (var entry : zones.values()) {
            var btn = new Button().setText(Component.literal(entry.name()));
            btn.layout(l -> l.widthPercent(100).height(28));
            String zoneName = entry.name();
            btn.setOnClick(e -> {
                // 发送网络包到服务端
                sendAction("add_to_zone", zoneName + "," + boundPointName);
                ToastNotification.push(ToastNotification.Type.SUCCESS,
                        Component.translatable("toast.capture_point_block.bound_to_zone", boundPointName, zoneName));
                mc.setScreen(null);
                reopenMenu();
            });
            root.addChildren(btn);
        }

        var cancelBtn = new Button().setText(Component.translatable("gui.capture_point_graph.dialog.cancel"));
        cancelBtn.layout(l -> l.widthPercent(100).height(28));
        cancelBtn.setOnClick(e -> {
            mc.setScreen(null);
            reopenMenu();
        });
        root.addChildren(cancelBtn);

        var ui = ModularUI.of(UI.of(root));
        mc.setScreen(new ModularUIScreen(ui,
                Component.translatable("gui.capture_point_block.dialog.select_zone")));
    }

    // ================================================================
    //  颜色选择对话框
    // ================================================================

    /** 预设颜色列表 */
    private static final int[] PRESET_COLORS = {
            0xFFFF4444, // 红
            0xFFFF9800, // 橙
            0xFFFFEB3B, // 黄
            0xFF4CAF50, // 绿
            0xFF2196F3, // 蓝
            0xFF9C27B0, // 紫
            0xFFFFFFFF, // 白
            0xFF000000  // 黑
    };

    private void openColorPickerDialog() {
        var mc = Minecraft.getInstance();
        int cols = 4;
        int rows = (int) Math.ceil((double) PRESET_COLORS.length / cols);
        int cw = 50, ch = 36, cgap = 6;
        int dw = cols * (cw + cgap) + 24 + cgap;
        int dh = rows * (ch + cgap) + 80;

        var root = new UIElement()
                .layout(l -> l.width(dw).height(dh).paddingAll(12).gapAll(8)
                        .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN))
                .style(s -> s.background(Sprites.BORDER)
                        .backgroundTexture(new ColorRectTexture(0xFF1A1A2E)));

        var title = new Label().setText(Component.translatable("gui.capture_point_block.dialog.color_picker"));
        title.layout(l -> l.widthPercent(100).heightAuto());
        root.addChildren(title);

        // 颜色网格
        var grid = new UIElement()
                .layout(l -> l.widthPercent(100).heightAuto()
                        .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW)
                        .flexWrap(dev.vfyjxf.taffy.style.FlexWrap.WRAP).gapAll(cgap));
        grid.style(s -> s.backgroundTexture(new ColorRectTexture(0x00000000)));

        for (int color : PRESET_COLORS) {
            var colorBtn = new UIElement()
                    .layout(l -> l.width(cw).height(ch))
                    .style(s -> s.background(Sprites.BORDER)
                            .backgroundTexture(new ColorRectTexture(color)));
            int selectedColor = color;
            colorBtn.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_DOWN, ev -> {
                // 更新本地缓存
                displayColor = selectedColor;
                setChanged();

                // 通过网络包同步到服务端
                if (!boundPointName.isEmpty()) {
                    sendAction("set_color", boundPointName + "," + selectedColor);
                }

                ToastNotification.push(ToastNotification.Type.SUCCESS,
                        Component.translatable("toast.capture_point_block.color_set"));
                mc.setScreen(null);
                reopenMenu();
            });
            grid.addChildren(colorBtn);
        }
        root.addChildren(grid);

        // 底部提示
        var hint = new Label().setText(Component.translatable("gui.capture_point_block.dialog.color_picker.hint"));
        hint.layout(l -> l.widthPercent(100).heightAuto());
        root.addChildren(hint);

        // 取消 & 关闭范围显示
        var btnRow = new UIElement()
                .layout(l -> l.widthPercent(100).height(28)
                        .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW).gapAll(6));
        var cancelBtn = new Button().setText(Component.translatable("gui.capture_point_graph.dialog.cancel"));
        cancelBtn.layout(l -> l.width(80).heightPercent(100));
        cancelBtn.setOnClick(e -> {
            mc.setScreen(null);
            reopenMenu();
        });
        var disableBtn = new Button().setText(Component.translatable("gui.capture_point_block.dialog.disable_range"));
        disableBtn.layout(l -> l.flex(1).heightPercent(100));
        disableBtn.setOnClick(e -> {
            showRange = false;
            setChanged();
            if (!boundPointName.isEmpty()) {
                sendAction("toggle_range", boundPointName + ",false");
            }
            ToastNotification.push(ToastNotification.Type.INFO,
                    Component.translatable("toast.capture_point_block.range_off"));
            mc.setScreen(null);
            reopenMenu();
        });
        btnRow.addChildren(disableBtn, cancelBtn);
        root.addChildren(btnRow);

        var ui = ModularUI.of(UI.of(root));
        mc.setScreen(new ModularUIScreen(ui,
                Component.translatable("gui.capture_point_block.dialog.color_picker")));
    }

    // ================================================================
    //  输入对话框（通用）
    // ================================================================

    private void openInputDialog(Component titleText, Component labelText,
                                  String defaultValue, Consumer<String> onConfirm) {
        var mc = Minecraft.getInstance();
        int dw = 320, dh = 160;

        var root = new UIElement()
                .layout(l -> l.width(dw).height(dh).paddingAll(12).gapAll(8)
                        .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN))
                .style(s -> s.background(Sprites.BORDER)
                        .backgroundTexture(new ColorRectTexture(0xFF1A1A2E)));

        var title = new Label().setText(titleText);
        title.layout(l -> l.widthPercent(100).heightAuto());
        root.addChildren(title);

        var label = new Label().setText(labelText);
        label.layout(l -> l.widthPercent(100).heightAuto());
        root.addChildren(label);

        var textField = new TextField();
        textField.layout(l -> l.widthPercent(100).height(28));
        textField.textFieldStyle(s -> {
            s.textColor(0xFFFFFFFF);
            s.fontSize(14.0f);
        });
        textField.setValue(defaultValue, false);
        root.addChildren(textField);

        root.addChildren(new UIElement().layout(l -> l.flex(1)));

        var btnRow = new UIElement()
                .layout(l -> l.widthPercent(100).height(30)
                        .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW).gapAll(8));
        var confirmBtn = new Button().setText(Component.translatable("gui.capture_point_graph.dialog.confirm"));
        confirmBtn.layout(l -> l.flex(1).heightPercent(100));
        confirmBtn.setOnClick(e -> {
            var input = textField.getText().trim();
            onConfirm.accept(input);
        });
        var cancelBtn = new Button().setText(Component.translatable("gui.capture_point_graph.dialog.cancel"));
        cancelBtn.layout(l -> l.flex(1).heightPercent(100));
        cancelBtn.setOnClick(e -> {
            mc.setScreen(null);
            reopenMenu();
        });
        btnRow.addChildren(confirmBtn, cancelBtn);
        root.addChildren(btnRow);

        var ui = ModularUI.of(UI.of(root));
        mc.setScreen(new ModularUIScreen(ui, titleText));
    }

    // ================================================================
    //  工具方法
    // ================================================================

    /** 重新打开菜单 */
    private void reopenMenu() {
        Minecraft.getInstance().setScreen(null);
        openMenuScreen();
    }
}
