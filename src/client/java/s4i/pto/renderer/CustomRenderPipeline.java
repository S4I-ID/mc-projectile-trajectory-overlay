package s4i.pto.renderer;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
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

    private static Vector3f toVector3f(Vec3d vec3d) {
        return new Vector3f((float) vec3d.x, (float) vec3d.y, (float) vec3d.z);
    }

    public void renderLines(WorldRenderContext context, List<Line> lines, Vec3d offset, Vec3d cameraPos) {
        VertexConsumer vertexConsumer;
        try {
            vertexConsumer = context.consumers().getBuffer(RenderLayers.lines());
        } catch (Exception exception) {
            return;
        }

        MatrixStack matrices = context.matrices();
        matrices.push();
        MatrixStack.Entry lastMatrix = matrices.peek();
        for (Line line : lines) {
            List<LineSegment> vertices = line.getSegments();
            int i = 0;
            for (LineSegment segment : vertices) {
                Vec3d startDelta = offset.multiply(calculateOffsetFalloff(vertices.size(), i)).multiply(config.offsetMultiplier);
                Vec3d endDelta = offset.multiply(calculateOffsetFalloff(vertices.size(), i + 1)).multiply(config.offsetMultiplier);

                Vec3d dottedLineDelta = segment.end.subtract(segment.start).multiply(1f - config.dottedLineScale);
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
        matrices.pop();
    }

    private void addHighlightPointToVertexConsumer(VertexConsumer vertexConsumer, MatrixStack.Entry lastMatrix, Vec3d highlightPoint, Vec3d cameraPos,
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

    private void addLineToVertexConsumerNormalColorWidth(VertexConsumer vertexConsumer, MatrixStack.Entry lastMatrix, float lineWidth, Color4f color,
                                                         Vector3f start, Vector3f end) {
        vertexConsumer.vertex(lastMatrix, start).normal(lastMatrix, start)
                .color(color.r, color.g, color.b, color.a).lineWidth(lineWidth);
        vertexConsumer.vertex(lastMatrix,   end).normal(lastMatrix, end)
                .color(color.r, color.g, color.b, color.a).lineWidth(lineWidth);
    }

    private void addLineToVertexConsumerNormalColorWidth(VertexConsumer vertexConsumer, MatrixStack.Entry lastMatrix, float lineWidth, Color4f color,
                                                         float startX, float startY, float startZ, float endX, float endY, float endZ) {
        vertexConsumer.vertex(lastMatrix, startX, startY, startZ).normal(lastMatrix, startX, startY, startZ)
                .color(color.r, color.g, color.b, color.a).lineWidth(lineWidth);
        vertexConsumer.vertex(lastMatrix, endX, endY, endZ).normal(lastMatrix, endX, endY, endZ)
                .color(color.r, color.g, color.b, color.a).lineWidth(lineWidth);
    }

    public void renderBoxes(WorldRenderContext context, List<RenderBox> boxes, Vec3d cameraPos) {
        List<RenderBox> filledBoxes = Utils.filterList(boxes, RenderBox::hasFill);
        List<RenderBox> outlineBoxes = Utils.filterList(boxes, RenderBox::hasOutline);
        if (!filledBoxes.isEmpty()) {
            renderFilledBoxes(context, filledBoxes, cameraPos);
        }
        if (!outlineBoxes.isEmpty()) {
            renderOutlinedBoxes(context, outlineBoxes, cameraPos);
        }
    }

    private void renderFilledBoxes(WorldRenderContext context, List<RenderBox> boxes, Vec3d cameraPos) {
        VertexConsumer boxVertexConsumer;
        try {
            boxVertexConsumer = context.consumers().getBuffer(RenderLayers.debugFilledBox());
        } catch (Exception exception) {
            return;
        }
        MatrixStack matrices = context.matrices();
        matrices.push();
        MatrixStack.Entry lastMatrix = matrices.peek();

        for (RenderBox rb : boxes) {
            Color4f color = rb.getFillColor();
            FloatBox box = new FloatBox(rb.getBox(), cameraPos);
            // front
            boxVertexConsumer.vertex(lastMatrix, box.minX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.maxX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.maxX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.minX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a);

            // back
            boxVertexConsumer.vertex(lastMatrix, box.maxX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.minX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.minX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.maxX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a);

            // left
            boxVertexConsumer.vertex(lastMatrix, box.minX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.minX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.minX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.minX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a);

            // right
            boxVertexConsumer.vertex(lastMatrix, box.maxX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.maxX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.maxX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.maxX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a);

            // top
            boxVertexConsumer.vertex(lastMatrix, box.minX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.maxX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.maxX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.minX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a);

            // bottom
            boxVertexConsumer.vertex(lastMatrix, box.minX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.maxX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.maxX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a);
            boxVertexConsumer.vertex(lastMatrix, box.minX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a);
        }
        matrices.pop();
    }

    private void renderOutlinedBoxes(WorldRenderContext context, List<RenderBox> boxes, Vec3d cameraPos) {
        VertexConsumer lineConsumer;
        try {
            lineConsumer = context.consumers().getBuffer(RenderLayers.lines());
        } catch (Exception exception) {
            return;
        }
        MatrixStack matrices = context.matrices();
        matrices.push();
        MatrixStack.Entry lastMatrix = matrices.peek();

        for (RenderBox rb : boxes) {
            float lineWidth = 2.0f;
            Color4f color = rb.getOutlineColor();
            FloatBox box = new FloatBox(rb.getBox(), cameraPos);
            // bottom
            lineConsumer
                    .vertex(lastMatrix, box.maxX, box.minY, box.minZ).normal(lastMatrix, box.maxX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.maxX, box.minY, box.maxZ).normal(lastMatrix, box.maxX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.maxX, box.minY, box.maxZ).normal(lastMatrix, box.maxX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.minX, box.minY, box.maxZ).normal(lastMatrix, box.minX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.minX, box.minY, box.maxZ).normal(lastMatrix, box.minX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.minX, box.minY, box.minZ).normal(lastMatrix, box.minX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.minX, box.minY, box.minZ).normal(lastMatrix, box.minX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.maxX, box.minY, box.minZ).normal(lastMatrix, box.maxX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth);

            // top
            lineConsumer
                    .vertex(lastMatrix, box.maxX, box.maxY, box.minZ).normal(lastMatrix, box.maxX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.maxX, box.maxY, box.maxZ).normal(lastMatrix, box.maxX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.maxX, box.maxY, box.maxZ).normal(lastMatrix, box.maxX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.minX, box.maxY, box.maxZ).normal(lastMatrix, box.minX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.minX, box.maxY, box.maxZ).normal(lastMatrix, box.minX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.minX, box.maxY, box.minZ).normal(lastMatrix, box.minX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.minX, box.maxY, box.minZ).normal(lastMatrix, box.minX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.maxX, box.maxY, box.minZ).normal(lastMatrix, box.maxX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth);

            // sides
            lineConsumer
                    .vertex(lastMatrix, box.minX, box.minY, box.minZ).normal(lastMatrix, box.minX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.minX, box.maxY, box.minZ).normal(lastMatrix, box.minX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.maxX, box.minY, box.minZ).normal(lastMatrix, box.maxX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.maxX, box.maxY, box.minZ).normal(lastMatrix, box.maxX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.maxX, box.minY, box.maxZ).normal(lastMatrix, box.maxX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.maxX, box.maxY, box.maxZ).normal(lastMatrix, box.maxX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.minX, box.minY, box.maxZ).normal(lastMatrix, box.minX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth)
                    .vertex(lastMatrix, box.minX, box.maxY, box.maxZ).normal(lastMatrix, box.minX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).lineWidth(lineWidth);
        }
        matrices.pop();
    }

    private double calculateOffsetFalloff(int numberOfLines, int currentLine) {
        return Math.max(0, (numberOfLines - (currentLine + config.offsetFalloffStrength) * config.offsetSmoothnessMultiplier) / numberOfLines);
    }

    public void renderDeviationMarkers(WorldRenderContext context, List<DeviationBox> boxes, Vec3d cameraPos, boolean renderOtherSide) {
        VertexConsumer vertexConsumer;
        try {
            vertexConsumer = context.consumers().getBuffer(RenderLayers.lines());
        } catch (Exception exception) {
            return;
        }

        MatrixStack matrices = context.matrices();
        matrices.push();
        MatrixStack.Entry lastMatrix = matrices.peek();

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
        matrices.pop();
    }
}