package com.heilous.land.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

@Slf4j
@Service
public class SlopeService {

    @Value("${slope.raster-path}")
    private String rasterPath;

    private static final int RASTER_WIDTH = 8013;
    private static final int RASTER_HEIGHT = 12720;

    private static final double ORIGIN_X = -64424.0;
    private static final double ORIGIN_Y = 1181753.0;
    private static final double PIXEL_SIZE = 90.0;

    private static final float NO_DATA = -9999.0f;

    private FileChannel channel;
    private CoordinateTransform coordinateTransform;

    @PostConstruct
    public void init() {
        try {
            Path path = Path.of(rasterPath);

            channel = FileChannel.open(
                    path,
                    StandardOpenOption.READ
            );

            long expectedSize =
                    (long) RASTER_WIDTH * RASTER_HEIGHT * Float.BYTES;

            if (channel.size() < expectedSize) {
                throw new IllegalStateException(
                        "경사도 파일 크기가 예상보다 작아. "
                                + "expected=" + expectedSize
                                + ", actual=" + channel.size()
                );
            }

            CRSFactory crsFactory = new CRSFactory();

            CoordinateReferenceSystem sourceCRS =
                    crsFactory.createFromName("EPSG:4326");

            CoordinateReferenceSystem targetCRS =
                    crsFactory.createFromName("EPSG:5186");

            if (sourceCRS == null || targetCRS == null) {
                throw new IllegalStateException(
                        "EPSG 좌표계를 찾을 수 없어."
                );
            }

            coordinateTransform =
                    new CoordinateTransformFactory()
                            .createTransform(sourceCRS, targetCRS);

            log.info("경사도 바이너리 초기화 완료, 파일={}, 크기={}", path.toAbsolutePath(), channel.size());

        } catch (IOException e) {
            throw new IllegalStateException(
                    "경사도 파일을 열 수 없어: " + rasterPath,
                    e
            );
        }
    }

    /**
     * 경도, 위도를 받아 해당 위치의 경사도를 반환해.
     * 조회할 수 없으면 NaN을 반환해.
     */
    public double getSlopeFromKoreaDem(
            double longitude,
            double latitude
    ) {
        if (!isAvailable()) {
            throw new IllegalStateException(
                    "경사도 서비스가 초기화되지 않았어."
            );
        }

        if (!isValidCoordinate(longitude, latitude)) {
            return Double.NaN;
        }

        ProjCoordinate target =
                transformToRaster(longitude, latitude);

        if (!Double.isFinite(target.x)
                || !Double.isFinite(target.y)) {
            return Double.NaN;
        }

        int col = (int) Math.floor(
                (target.x - ORIGIN_X) / PIXEL_SIZE
        );

        int row = (int) Math.floor(
                (ORIGIN_Y - target.y) / PIXEL_SIZE
        );

        if (col < 0 || col >= RASTER_WIDTH
                || row < 0 || row >= RASTER_HEIGHT) {
            return Double.NaN;
        }

        float value = readPixel(row, col);

        if (!isValidSlope(value)) {
            return Double.NaN;
        }

        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * BBOX 안의 유효한 경사도 픽셀을 double[]로 반환해.
     *
     * bbox 순서:
     * [minLon, minLat, maxLon, maxLat]
     *
     * 대표좌표가 BBOX 밖이면 BBOX 중심을 대표좌표로 이동해.
     * BBOX의 너비와 높이는 유지해.
     */
    public double[] getSlopeValuesFromBbox(
            double representativeLon,
            double representativeLat,
            double[] bbox
    ) {
        if (!isAvailable()) {
            throw new IllegalStateException(
                    "경사도 서비스가 초기화되지 않았어."
            );
        }

        validateBbox(representativeLon, representativeLat, bbox);

        // 대표좌표를 래스터 좌표계로 변환
        ProjCoordinate representative =
                transformToRaster(
                        representativeLon,
                        representativeLat
                );

        if (!Double.isFinite(representative.x)
                || !Double.isFinite(representative.y)) {
            throw new IllegalArgumentException(
                    "대표좌표 변환에 실패했어."
            );
        }

        /*
         * BBOX 네 모서리를 EPSG:5186으로 변환해.
         * 변환 후의 최소/최대 좌표로 래스터 검색 범위를 계산해.
         */
        double[][] corners = {
                {bbox[0], bbox[1]},
                {bbox[0], bbox[3]},
                {bbox[2], bbox[1]},
                {bbox[2], bbox[3]}
        };

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (double[] corner : corners) {
            ProjCoordinate point =
                    transformToRaster(corner[0], corner[1]);

            if (!Double.isFinite(point.x)
                    || !Double.isFinite(point.y)) {
                throw new IllegalArgumentException(
                        "BBOX 좌표 변환에 실패했어."
                );
            }

            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }

        // 대표좌표가 원본 지리 좌표 BBOX 안에 있는지 확인
        boolean containsRepresentative =
                representativeLon >= bbox[0]
                        && representativeLon <= bbox[2]
                        && representativeLat >= bbox[1]
                        && representativeLat <= bbox[3];

        boolean corrected = !containsRepresentative;

        if (corrected) {
            // 원래 BBOX의 폭과 높이를 유지하면서 중심을 이동해.
            double width = maxX - minX;
            double height = maxY - minY;

            minX = representative.x - width / 2.0;
            maxX = representative.x + width / 2.0;

            minY = representative.y - height / 2.0;
            maxY = representative.y + height / 2.0;

            log.debug("BBOX 보정: 대표좌표 중심으로 이동 lon={}, lat={}", representativeLon, representativeLat);
        }

        // 투영 좌표를 래스터 행/열 범위로 변환
        int startCol = clamp(
                (int) Math.floor(
                        (minX - ORIGIN_X) / PIXEL_SIZE
                ),
                0,
                RASTER_WIDTH
        );

        int endCol = clamp(
                (int) Math.ceil(
                        (maxX - ORIGIN_X) / PIXEL_SIZE
                ),
                0,
                RASTER_WIDTH
        );

        int startRow = clamp(
                (int) Math.floor(
                        (ORIGIN_Y - maxY) / PIXEL_SIZE
                ),
                0,
                RASTER_HEIGHT
        );

        int endRow = clamp(
                (int) Math.ceil(
                        (ORIGIN_Y - minY) / PIXEL_SIZE
                ),
                0,
                RASTER_HEIGHT
        );

        if (startCol >= endCol || startRow >= endRow) {
            return new double[0];
        }

        int cols = endCol - startCol;
        int rows = endRow - startRow;

        long maxPixels = (long) cols * rows;

        // 지나치게 큰 영역을 읽는 상황 방지
        if (maxPixels > 2_000_000L) {
            throw new IllegalArgumentException(
                    "조회 영역이 너무 커. BBOX 범위를 줄여 줘."
            );
        }

        double[] values = new double[(int) maxPixels];
        int count = 0;

        // 한 행씩 읽어서 메모리 사용량을 줄여.
        ByteBuffer buffer = ByteBuffer
                .allocate(cols * Float.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);

        try {
            for (int row = startRow; row < endRow; row++) {
                buffer.clear();

                long offset =
                        ((long) row * RASTER_WIDTH + startCol)
                                * Float.BYTES;

                while (buffer.hasRemaining()) {
                    int read = channel.read(
                            buffer,
                            offset + buffer.position()
                    );

                    if (read < 0) {
                        throw new IOException(
                                "래스터 행을 끝까지 읽지 못했어."
                        );
                    }

                    if (read == 0) {
                        Thread.yield();
                    }
                }

                buffer.flip();

                for (int col = startCol; col < endCol; col++) {
                    float value = buffer.getFloat();

                    if (!isValidSlope(value)) {
                        continue;
                    }

                    values[count++] = value;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "경사도 래스터 읽기 실패",
                    e
            );
        }

        double[] result = Arrays.copyOf(values, count);

        log.debug("경사도 조회 완료, 보정 여부={}, 조회 픽셀={}", corrected, result.length);

        return result;
    }

    /**
     * BBOX 내 유효한 픽셀의 단순 평균을 반환해.
     * 유효한 데이터가 없으면 NaN을 반환해.
     */
    public double getAverageSlopeFromBbox(
            double representativeLon,
            double representativeLat,
            double[] bbox
    ) {
        double[] values = getSlopeValuesFromBbox(
                representativeLon,
                representativeLat,
                bbox
        );

        return Arrays.stream(values)
                .average()
                .orElse(Double.NaN);
    }

    /**
     * 경도/위도를 EPSG:5186으로 변환해.
     */
    private ProjCoordinate transformToRaster(
            double longitude,
            double latitude
    ) {
        ProjCoordinate source =
                new ProjCoordinate(longitude, latitude);

        ProjCoordinate target = new ProjCoordinate();

        coordinateTransform.transform(source, target);

        return target;
    }

    /**
     * 래스터의 Float32 픽셀 하나를 읽어.
     */
    private float readPixel(int row, int col) {
        long offset =
                ((long) row * RASTER_WIDTH + col) * Float.BYTES;

        ByteBuffer buffer = ByteBuffer
                .allocate(Float.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);

        try {
            while (buffer.hasRemaining()) {
                int read = channel.read(
                        buffer,
                        offset + buffer.position()
                );

                if (read <= 0) {
                    return Float.NaN;
                }
            }

            buffer.flip();

            return buffer.getFloat();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "경사도 픽셀을 읽는 데 실패했어.",
                    e
            );
        }
    }

    /**
     * 유효한 경사도인지 확인해.
     */
    private boolean isValidSlope(float value) {
        return Float.isFinite(value)
                && value != NO_DATA
                && value >= 0
                && value <= 90;
    }

    private boolean isValidCoordinate(
            double longitude,
            double latitude
    ) {
        return Double.isFinite(longitude)
                && Double.isFinite(latitude)
                && longitude >= -180
                && longitude <= 180
                && latitude >= -90
                && latitude <= 90;
    }

    private void validateBbox(
            double representativeLon,
            double representativeLat,
            double[] bbox
    ) {
        if (!isValidCoordinate(
                representativeLon,
                representativeLat
        )) {
            throw new IllegalArgumentException(
                    "대표좌표가 올바르지 않아."
            );
        }

        if (bbox == null || bbox.length != 4) {
            throw new IllegalArgumentException(
                    "BBOX는 [minLon, minLat, maxLon, maxLat] 형식이어야 해."
            );
        }

        if (!Arrays.stream(bbox).allMatch(Double::isFinite)) {
            throw new IllegalArgumentException(
                    "BBOX에 유효하지 않은 좌표가 있어."
            );
        }

        if (bbox[0] < -180 || bbox[0] > 180
                || bbox[2] < -180 || bbox[2] > 180
                || bbox[1] < -90 || bbox[1] > 90
                || bbox[3] < -90 || bbox[3] > 90
                || bbox[0] >= bbox[2]
                || bbox[1] >= bbox[3]) {
            throw new IllegalArgumentException(
                    "BBOX 범위가 올바르지 않아."
            );
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public boolean isAvailable() {
        return channel != null
                && channel.isOpen()
                && coordinateTransform != null;
    }

    @PreDestroy
    public void destroy() {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                log.error("경사도 파일을 닫지 못했어: {}", e.getMessage());
            }
        }
    }
}
