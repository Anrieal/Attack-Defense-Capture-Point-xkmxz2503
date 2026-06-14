package com.xkmxz.attack_defense_capture_point_xkmxz.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xkmxz.attack_defense_capture_point_xkmxz.block.entity.CapturePointBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 据点方块的范围轮廓渲染器。
 * 当 showRange 为 true 时，在方块周围绘制一个圆形轮廓线。
 */
public class CapturePointBlockEntityRenderer implements BlockEntityRenderer<CapturePointBlockEntity> {

    private static final int SEGMENTS = 32; // 圆的分段数（越大越圆但性能越差）

    public CapturePointBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CapturePointBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        // 仅在启用范围显示时渲染
        if (!blockEntity.isShowRange()) return;

        double radius = blockEntity.getRadius();
        int color = blockEntity.getDisplayColor();
        Level level = blockEntity.getLevel();
        if (level == null) return;

        // 提取颜色分量
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        // 获取方块位置（世界坐标）
        double cx = blockEntity.getBlockPos().getX() + 0.5;
        double cy = blockEntity.getBlockPos().getY() + 0.5;
        double cz = blockEntity.getBlockPos().getZ() + 0.5;

        // 使用线条渲染
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);

        poseStack.pushPose();
        // 平移到方块中心
        poseStack.translate(0.5, 0.5, 0.5);

        Matrix4f matrix = poseStack.last().pose();

        // 绘制水平圆环（在 y=0 平面上）
        double angleStep = 2.0 * Math.PI / SEGMENTS;
        for (int i = 0; i < SEGMENTS; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;

            float x1 = (float) (radius * Math.cos(angle1));
            float z1 = (float) (radius * Math.sin(angle1));
            float x2 = (float) (radius * Math.cos(angle2));
            float z2 = (float) (radius * Math.sin(angle2));

            // 在 y=0 平面画圆
            consumer.addVertex(matrix, x1, 0, z1)
                    .setColor(r, g, b, a)
                    .setNormal(0, 1, 0);
            consumer.addVertex(matrix, x2, 0, z2)
                    .setColor(r, g, b, a)
                    .setNormal(0, 1, 0);
        }

        // 绘制垂直辅助标记（4条短竖线指示上下范围，让玩家感受立体空间）
        float vh = 0.5f; // 垂直标记的高度
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            float vx = (float) (radius * Math.cos(angle));
            float vz = (float) (radius * Math.sin(angle));

            // 从 -vh 到 vh 的竖线
            consumer.addVertex(matrix, vx, -vh, vz)
                    .setColor(r, g, b, a * 0.5f)
                    .setNormal(0, 1, 0);
            consumer.addVertex(matrix, vx, vh, vz)
                    .setColor(r, g, b, a * 0.5f)
                    .setNormal(0, 1, 0);
        }

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(CapturePointBlockEntity blockEntity) {
        return blockEntity.isShowRange();
    }

    @Override
    public boolean shouldRender(CapturePointBlockEntity blockEntity, Vec3 cameraPos) {
        // 始终检查渲染距离（默认 64 方块外不渲染）
        return BlockEntityRenderer.super.shouldRender(blockEntity, cameraPos);
    }

    @Override
    public int getViewDistance() {
        // 增加渲染距离，使大半径范围也能看到
        return 128;
    }
}
