package s4i.pto.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import s4i.pto.config.ModConfig;
import s4i.pto.model.Color4f;
import s4i.pto.model.FloatBox;
import s4i.pto.model.LineSource;
import s4i.pto.model.simulation.DeviationBox;
import s4i.pto.model.simulation.Line;
import s4i.pto.model.simulation.LineSegment;
import s4i.pto.model.simulation.RenderBox;
import s4i.pto.utils.ColorUtils;
import s4i.pto.utils.Utils;

import java.util.List;

public class CustomRenderPipeline {
    private static CustomRenderPipeline instance;
    private ModConfig config;

    private CustomRenderPipeline() {}

    public static CustomRenderPipeline getInstance() {
        if (instance == null) {
            instance = new CustomRenderPipeline();
            instance.init();
        }
        return instance;
    }

    private void init() {
        this.config = ModConfig.getInstance();
    }

    private static Vector3f toVector3f(Vec3 vec3d) {
        return new Vector3f((float) vec3d.x, (float) vec3d.y, (float) vec3d.z);
    }

    public void renderLines(LevelRenderContext context, List<Line> lines, Vec3 offset, Vec3 cameraPos) {
        VertexConsumer vertexConsumer;
        try {
            vertexConsumer = context.bufferSource().getBuffer(RenderTypes.lines());
        } catch (Exception exception) {
            return;
        }

        PoseStack matrices = context.poseStack();
        matrices.pushPose();
        PoseStack.Pose lastMatrix = matrices.last();
        for (Line line : lines) {
            List<LineSegment> vertices = line.getSegments();
            int i = 0;
            for (LineSegment segment : vertices) {
                Vec3 startDelta = offset.scale(calculateOffsetFalloff(vertices.size(), i)).scale(config.offsetMultiplier);
                Vec3 endDelta = offset.scale(calculateOffsetFalloff(vertices.size(), i + 1)).scale(config.offsetMultiplier);

                Vec3 dottedLineDelta = segment.end.subtract(segment.start).scale(1f - config.dottedLineScale);
                Vector3f start = toVector3f(segment.start.add(startDelta).subtract(cameraPos).add(dottedLineDelta));
                Vector3f end = toVector3f(segment.end.add(endDelta).subtract(cameraPos));

                addLineToVertexConsumerNormalColorWidth(vertexConsumer, lastMatrix,
                        Utils.mixFloats(config.lineWidth, config.lineWidth * config.lineWidthScaling, (float) i / vertices.size()),
                        segment.color, start, end);
                i += 1;
            }

            if (config.highlightLandingPoint && i > 0) {
                addHighlightPointToVertexConsumer(vertexConsumer, lastMatrix, vertices.getLast().end, cameraPos, line.getLineSource());
            }
        }
        matrices.popPose();
    }

    private void addHighlightPointToVertexConsumer(VertexConsumer vertexConsumer, PoseStack.Pose lastMatrix, Vec3 highlightPoint, Vec3 cameraPos,
                                                   LineSource lineSource) {
        Vector3f pos = toVector3f(highlightPoint.subtract(cameraPos));
        Color4f highlightColor = ColorUtils.getHighlightPointColor(config, lineSource);
        float highlightLength = 0.15f;
        float highlightWidth = 4.0f;
        addLineToVertexConsumerNormalColorWidth(vertexConsumer, lastMatrix, highlightWidth, highlightColor,
                pos.x + highlightLength, pos.y, pos.z,
                pos.x - highlightLength, pos.y, pos.z);
        addLineToVertexConsumerNormalColorWidth(vertexConsumer, lastMatrix, highlightWidth, highlightColor,
                pos.x, pos.y + highlightLength, pos.z,
                pos.x, pos.y - highlightLength, pos.z);
        addLineToVertexConsumerNormalColorWidth(vertexConsumer, lastMatrix, highlightWidth, highlightColor,
                pos.x, pos.y, pos.z + highlightLength,
                pos.x, pos.y, pos.z - highlightLength);
    }

    private void addLineToVertexConsumerNormalColorWidth(VertexConsumer vertexConsumer, PoseStack.Pose lastMatrix, float lineWidth, Color4f color,
                                                         Vector3f start, Vector3f end) {
        vertexConsumer.addVertex(lastMatrix, start).setNormal(lastMatrix, start)
                .setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        vertexConsumer.addVertex(lastMatrix,   end).setNormal(lastMatrix, end)
                .setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
    }

    private void addLineToVertexConsumerNormalColorWidth(VertexConsumer vertexConsumer, PoseStack.Pose lastMatrix, float lineWidth, Color4f color,
                                                         float startX, float startY, float startZ, float endX, float endY, float endZ) {
        vertexConsumer.addVertex(lastMatrix, startX, startY, startZ).setNormal(lastMatrix, startX, startY, startZ)
                .setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        vertexConsumer.addVertex(lastMatrix, endX, endY, endZ).setNormal(lastMatrix, endX, endY, endZ)
                .setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
    }

    public void renderBoxes(LevelRenderContext context, List<RenderBox> boxes, Vec3 cameraPos) {
        List<RenderBox> filledBoxes = Utils.filterList(boxes, RenderBox::hasFill);
        List<RenderBox> outlineBoxes = Utils.filterList(boxes, RenderBox::hasOutline);
        if (!filledBoxes.isEmpty()) {
            renderFilledBoxes(context, filledBoxes, cameraPos);
        }
        if (!outlineBoxes.isEmpty()) {
            renderOutlinedBoxes(context, outlineBoxes, cameraPos);
        }
    }

    private void renderFilledBoxes(LevelRenderContext context, List<RenderBox> boxes, Vec3 cameraPos) {
        VertexConsumer boxVertexConsumer;
        try {
            boxVertexConsumer = context.bufferSource().getBuffer(RenderTypes.debugFilledBox());
        } catch (Exception exception) {
            return;
        }
        PoseStack matrices = context.poseStack();
        matrices.pushPose();
        PoseStack.Pose lastMatrix = matrices.last();

        for (RenderBox rb : boxes) {
            Color4f color = rb.getFillColor();
            FloatBox box = new FloatBox(rb.getBox(), cameraPos);
            // front
            boxVertexConsumer.addVertex(lastMatrix, box.minX, box.minY, box.maxZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.maxX, box.minY, box.maxZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.maxX, box.maxY, box.maxZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.minX, box.maxY, box.maxZ).setColor(color.r, color.g, color.b, color.a);

            // back
            boxVertexConsumer.addVertex(lastMatrix, box.maxX, box.minY, box.minZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.minX, box.minY, box.minZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.minX, box.maxY, box.minZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.maxX, box.maxY, box.minZ).setColor(color.r, color.g, color.b, color.a);

            // left
            boxVertexConsumer.addVertex(lastMatrix, box.minX, box.minY, box.minZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.minX, box.minY, box.maxZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.minX, box.maxY, box.maxZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.minX, box.maxY, box.minZ).setColor(color.r, color.g, color.b, color.a);

            // right
            boxVertexConsumer.addVertex(lastMatrix, box.maxX, box.minY, box.maxZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.maxX, box.minY, box.minZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.maxX, box.maxY, box.minZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.maxX, box.maxY, box.maxZ).setColor(color.r, color.g, color.b, color.a);

            // top
            boxVertexConsumer.addVertex(lastMatrix, box.minX, box.maxY, box.maxZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.maxX, box.maxY, box.maxZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.maxX, box.maxY, box.minZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.minX, box.maxY, box.minZ).setColor(color.r, color.g, color.b, color.a);

            // bottom
            boxVertexConsumer.addVertex(lastMatrix, box.minX, box.minY, box.minZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.maxX, box.minY, box.minZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.maxX, box.minY, box.maxZ).setColor(color.r, color.g, color.b, color.a);
            boxVertexConsumer.addVertex(lastMatrix, box.minX, box.minY, box.maxZ).setColor(color.r, color.g, color.b, color.a);
        }
        matrices.popPose();
    }

    private void renderOutlinedBoxes(LevelRenderContext context, List<RenderBox> boxes, Vec3 cameraPos) {
        VertexConsumer lineConsumer;
        try {
            lineConsumer = context.bufferSource().getBuffer(RenderTypes.lines());
        } catch (Exception exception) {
            return;
        }
        PoseStack matrices = context.poseStack();
        matrices.pushPose();
        PoseStack.Pose lastMatrix = matrices.last();

        for (RenderBox rb : boxes) {
            float lineWidth = 2.0f;
            Color4f color = rb.getOutlineColor();
            FloatBox box = new FloatBox(rb.getBox(), cameraPos);
            // bottom
            lineConsumer
                    .addVertex(lastMatrix, box.maxX, box.minY, box.minZ).setNormal(lastMatrix, box.maxX, box.minY, box.minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.maxX, box.minY, box.maxZ).setNormal(lastMatrix, box.maxX, box.minY, box.maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.maxX, box.minY, box.maxZ).setNormal(lastMatrix, box.maxX, box.minY, box.maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.minX, box.minY, box.maxZ).setNormal(lastMatrix, box.minX, box.minY, box.maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.minX, box.minY, box.maxZ).setNormal(lastMatrix, box.minX, box.minY, box.maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.minX, box.minY, box.minZ).setNormal(lastMatrix, box.minX, box.minY, box.minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.minX, box.minY, box.minZ).setNormal(lastMatrix, box.minX, box.minY, box.minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.maxX, box.minY, box.minZ).setNormal(lastMatrix, box.maxX, box.minY, box.minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

            // top
            lineConsumer
                    .addVertex(lastMatrix, box.maxX, box.maxY, box.minZ).setNormal(lastMatrix, box.maxX, box.maxY, box.minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.maxX, box.maxY, box.maxZ).setNormal(lastMatrix, box.maxX, box.maxY, box.maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.maxX, box.maxY, box.maxZ).setNormal(lastMatrix, box.maxX, box.maxY, box.maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.minX, box.maxY, box.maxZ).setNormal(lastMatrix, box.minX, box.maxY, box.maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.minX, box.maxY, box.maxZ).setNormal(lastMatrix, box.minX, box.maxY, box.maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.minX, box.maxY, box.minZ).setNormal(lastMatrix, box.minX, box.maxY, box.minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.minX, box.maxY, box.minZ).setNormal(lastMatrix, box.minX, box.maxY, box.minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.maxX, box.maxY, box.minZ).setNormal(lastMatrix, box.maxX, box.maxY, box.minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

            // sides
            lineConsumer
                    .addVertex(lastMatrix, box.minX, box.minY, box.minZ).setNormal(lastMatrix, box.minX, box.minY, box.minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.minX, box.maxY, box.minZ).setNormal(lastMatrix, box.minX, box.maxY, box.minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.maxX, box.minY, box.minZ).setNormal(lastMatrix, box.maxX, box.minY, box.minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.maxX, box.maxY, box.minZ).setNormal(lastMatrix, box.maxX, box.maxY, box.minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.maxX, box.minY, box.maxZ).setNormal(lastMatrix, box.maxX, box.minY, box.maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.maxX, box.maxY, box.maxZ).setNormal(lastMatrix, box.maxX, box.maxY, box.maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.minX, box.minY, box.maxZ).setNormal(lastMatrix, box.minX, box.minY, box.maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth)
                    .addVertex(lastMatrix, box.minX, box.maxY, box.maxZ).setNormal(lastMatrix, box.minX, box.maxY, box.maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }
        matrices.popPose();
    }

    private double calculateOffsetFalloff(int numberOfLines, int currentLine) {
        return Math.max(0, (numberOfLines - (currentLine + config.offsetFalloffStrength) * config.offsetSmoothnessMultiplier) / numberOfLines);
    }

    public void renderDeviationMarkers(LevelRenderContext context, List<DeviationBox> boxes, Vec3 cameraPos, boolean renderOtherSide) {
        VertexConsumer vertexConsumer;
        try {
            vertexConsumer = context.bufferSource().getBuffer(RenderTypes.lines());
        } catch (Exception exception) {
            return;
        }

        PoseStack matrices = context.poseStack();
        matrices.pushPose();
        PoseStack.Pose lastMatrix = matrices.last();

        float markerWidth = 7.0f;
        for (DeviationBox deviationBox : boxes) {
            FloatBox box = new FloatBox(deviationBox, cameraPos);
            if (renderOtherSide) {
                addLineToVertexConsumerNormalColorWidth(vertexConsumer, lastMatrix, markerWidth, deviationBox.color,
                        box.maxX, box.maxY, box.minZ,
                        box.maxX, box.minY, box.minZ);
                addLineToVertexConsumerNormalColorWidth(vertexConsumer, lastMatrix, markerWidth, deviationBox.color,
                        box.maxX, box.minY, box.minZ,
                        box.minX, box.minY, box.maxZ);

                addLineToVertexConsumerNormalColorWidth(vertexConsumer, lastMatrix, markerWidth, deviationBox.color,
                        box.maxX, box.maxY, box.minZ,
                        box.minX, box.maxY, box.maxZ);
                addLineToVertexConsumerNormalColorWidth(vertexConsumer, lastMatrix, markerWidth, deviationBox.color,
                        box.minX, box.maxY, box.maxZ,
                        box.minX, box.minY, box.maxZ);
            } else {
                addLineToVertexConsumerNormalColorWidth(vertexConsumer, lastMatrix, markerWidth, deviationBox.color,
                        box.minX, box.maxY, box.minZ,
                        box.minX, box.minY, box.minZ);
                addLineToVertexConsumerNormalColorWidth(vertexConsumer, lastMatrix, markerWidth, deviationBox.color,
                        box.minX, box.minY, box.minZ,
                        box.maxX, box.minY, box.maxZ);

                addLineToVertexConsumerNormalColorWidth(vertexConsumer, lastMatrix, markerWidth, deviationBox.color,
                        box.minX, box.maxY, box.minZ,
                        box.maxX, box.maxY, box.maxZ);
                addLineToVertexConsumerNormalColorWidth(vertexConsumer, lastMatrix, markerWidth, deviationBox.color,
                        box.maxX, box.maxY, box.maxZ,
                        box.maxX, box.minY, box.maxZ);
            }
        }
        matrices.popPose();
    }
}