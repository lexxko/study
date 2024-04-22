package ru.itfb;

import exception.CriticalException;
import exception.NonCriticalException;
import exception.ShapeApplicationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ShapeCalc {
    private static final BigDecimal PI = BigDecimal.valueOf(3.14159);
    private static final boolean STRICT_VALIDATION = false;
    private static final Long CACHE_DURATION_IN_SEC = 1L;

    final static Map<Class<? extends IShape>, Function<List<Long>, List<? extends IShape>>> FETCH_MAP;
    final static Function<List<Long>, List<? extends IShape>> fetchFlatShapes = ShapeCalc::fetchFlatShapes;
    final static Function<List<Long>, List<? extends IShape>> fetchVolumetricShapes = ShapeCalc::fetchVolumetricShapes;

    static {
        FETCH_MAP = Map.of(
                Circle.class, fetchFlatShapes,
                Square.class, fetchFlatShapes,
                Sphere.class, fetchVolumetricShapes,
                Parallelogram.class, fetchFlatShapes,
                Cube.class, fetchVolumetricShapes);
    }

    @SneakyThrows
    public static void main(String[] args) {
        final ShapeFactory shapeFactory = new ShapeFactory(STRICT_VALIDATION);

        final Circle circle = Circle.create(1L, BigDecimal.TEN.negate(), 1, STRICT_VALIDATION);
        final FlatShape square = Square.create(2L, BigDecimal.TEN, -2, STRICT_VALIDATION);
        final Sphere sphere = Sphere.create(3L, BigDecimal.TWO, 10, STRICT_VALIDATION);
        // Есть взаимная зависимость между высотой и боковой стороной (высота <= боковой стороны),
        // поэтому вместо задания трёх длин лучше использовать длины двух сторон и угол,
        // здесь сделано для простоты
        final FlatShape parallelogram = Parallelogram.create(4L, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TWO, 3, STRICT_VALIDATION);
        final VolumetricShape cube = Cube.create(5L, BigDecimal.TEN, 15, STRICT_VALIDATION);


        Thread.sleep(2000);


        final Circle circle2 = shapeFactory.createShape(Circle.class, List.of(6L, BigDecimal.TWO.negate(), 2));
        final FlatShape square2 = shapeFactory.createShape(Square.class, List.of(7L, BigDecimal.ONE, 3));
        final Sphere sphere2 = shapeFactory.createShape(Sphere.class, List.of(8L, BigDecimal.TEN, -10));


        System.out.println("Summary area of all shapes: " +
                sumArea(circle, square, sphere, parallelogram, cube, circle2, square2, sphere2));
        System.out.println("Summary perimeter of flat shapes: " +
                sumPerimeter(circle, square, parallelogram, circle2, square2));
        System.out.println("Summary volume of volumetric shapes: " +
                sumVolume(sphere, cube, sphere2));
        System.out.println("Average radius of round shapes: " +
                calculateAverageRadius(circle, sphere, circle2, sphere2));
        System.out.println("Average scale of shapes: " +
                calculateAverageScale(circle, square, sphere, parallelogram, cube, circle2, square2, sphere2));


        System.out.println("Summary area of refreshed and unified shapes: " +
                refreshUnifyScaleAndCalcArea(
                        FETCH_MAP,
                        2,
                        List.of(circle, square, sphere, parallelogram, cube, circle2, square2, sphere2),
                        false));
    }

    @NotNull
    private static BigDecimal sumArea(@NotNull Shape... shapes) {
        return Arrays.stream(shapes)
                .map(Shape::getArea)
                .reduce(BigDecimal::add)
                .orElseThrow();
    }

    @NotNull
    private static BigDecimal sumPerimeter(@NotNull FlatShape... shapes) {
        return Arrays.stream(shapes)
                .map(FlatShape::getPerimeter)
                .reduce(BigDecimal::add)
                .orElseThrow();
    }

    @NotNull
    private static BigDecimal sumVolume(@NotNull VolumetricShape... shapes) {
        return Arrays.stream(shapes)
                .map(VolumetricShape::getVolume)
                .reduce(BigDecimal::add)
                .orElseThrow();
    }

    @NotNull
    private static BigDecimal calculateAverageRadius(@NotNull IRound... shapes) {
        return Arrays.stream(shapes)
                .map(IRound::getRadius)
                .reduce(BigDecimal::add)
                .map(sum -> sum.divide(BigDecimal.valueOf(shapes.length), RoundingMode.HALF_UP))
                .orElseThrow();
    }

    @NotNull
    private static Integer calculateAverageScale(@NotNull Shape... shapes) {
        return Arrays.stream(shapes)
                .map(Shape::getScale)
                .reduce(Integer::sum)
                .map(sum -> sum / shapes.length)
                .orElseThrow();
    }

    @Data
    @AllArgsConstructor
    static abstract class Shape implements IShape {
        protected final Long id;
        protected BigDecimal area;
        protected Integer scale;
        protected final transient LocalDateTime cachingTime = LocalDateTime.now();

        @NotNull
        protected static BigDecimal rescaleValue(@NotNull BigDecimal currentValue,
                                                 @NotNull Integer currentScale,
                                                 @NotNull Integer newScale) {
            return currentValue
                    .multiply(BigDecimal.valueOf(newScale))
                    .divide(BigDecimal.valueOf(currentScale), RoundingMode.HALF_UP);
        }
    }

    @Getter
    static abstract class FlatShape extends Shape {
        protected BigDecimal perimeter;

        public FlatShape(@NotNull Long id, @NotNull BigDecimal area, @NotNull BigDecimal perimeter, @NotNull Integer scale) {
            super(id, area, scale);
            this.perimeter = perimeter;
        }
    }

    @Getter
    static abstract class VolumetricShape extends Shape {
        protected BigDecimal volume;

        public VolumetricShape(@NotNull Long id,
                               @NotNull BigDecimal area,
                               @NotNull BigDecimal volume,
                               @NotNull Integer scale) {
            super(id, area, scale);
            this.volume = volume;
        }
    }

    @Getter
    static class Circle extends FlatShape implements IRound {
        private BigDecimal radius;

        Circle(@NotNull Long id, @NotNull BigDecimal radius, @NotNull Integer scale) {
            super(id, calcArea(radius), calcPerimeter(radius), scale);
            this.radius = radius;
        }

        @Override
        public void rescale(@NotNull Integer newScale, boolean strict) {
            final Integer verifiedNewScale = checkAndAbs(newScale, Math::abs, strict);
            this.radius = rescaleValue(radius, scale, newScale);
            this.area = calcArea(radius);
            this.perimeter = calcPerimeter(radius);
            this.scale = verifiedNewScale;
        }

        @Override
        public @NotNull Class<? extends Shape> getType() {
            return Circle.class;
        }

        @NotNull
        private static BigDecimal calcPerimeter(@NotNull BigDecimal radius) {
            return radius
                    .multiply(PI)
                    .multiply(BigDecimal.TWO);
        }

        @NotNull
        private static BigDecimal calcArea(@NotNull BigDecimal radius) {
            return radius
                    .pow(2)
                    .multiply(PI);
        }

        @NotNull
        public static Circle create(@NotNull Long id, @NotNull BigDecimal radius, @NotNull Integer scale, boolean strict) {
            return new Circle(id, checkAndAbs(radius, BigDecimal::abs, strict), checkAndAbs(scale, Math::abs, strict));
        }
    }

    @Getter
    static class Sphere extends VolumetricShape implements IRound {
        // Прим.: У сферы, как и у круга, есть радиус, но нет периметра
        // Поэтому не вполне корректно делать и наследование сферы от круга, и делать круг как сферу с нулевым объёмом
        private BigDecimal radius;

        public Sphere(@NotNull Long id, @NotNull BigDecimal radius, @NotNull Integer scale) {
            super(id, calcArea(radius), calcVolume(radius), scale);
            this.radius = radius;
        }

        @Override
        public void rescale(@NotNull Integer newScale, boolean strict) {
            final Integer verifiedNewScale = checkAndAbs(newScale, Math::abs, strict);
            this.radius = rescaleValue(radius, scale, newScale);
            this.area = calcArea(radius);
            this.volume = calcVolume(radius);
            this.scale = verifiedNewScale;
        }

        @Override
        public @NotNull Class<? extends Shape> getType() {
            return Sphere.class;
        }

        @NotNull
        private static BigDecimal calcVolume(@NotNull BigDecimal radius) {
            return radius
                    .pow(3)
                    .multiply(PI)
                    .multiply(BigDecimal.valueOf(4))
                    .divide(BigDecimal.valueOf(3), RoundingMode.HALF_UP);
        }

        @NotNull
        private static BigDecimal calcArea(@NotNull BigDecimal radius) {
            return radius
                    .pow(3)
                    .multiply(PI);
        }

        @NotNull
        public static Sphere create(@NotNull Long id, @NotNull BigDecimal radius, @NotNull Integer scale, boolean strict) {
            return new Sphere(id, checkAndAbs(radius, BigDecimal::abs, strict), checkAndAbs(scale, Math::abs, strict));
        }
    }

    @Getter
    static class Parallelogram extends FlatShape {
        protected BigDecimal base; // Основание: __
        protected BigDecimal height; // Высота: |
        protected BigDecimal side; // Боковая грань: /

        public Parallelogram(@NotNull Long id,
                             @NotNull BigDecimal base,
                             @NotNull BigDecimal height,
                             @NotNull BigDecimal side,
                             @NotNull Integer scale) {
            super(id, calcArea(base, height), calcPerimeter(base, side), scale);
            this.base = base;
            this.height = height;
            this.side = side;
        }

        @Override
        public void rescale(@NotNull Integer newScale, boolean strict) {
            final Integer verifiedNewScale = checkAndAbs(newScale, Math::abs, strict);
            this.base = rescaleValue(base, scale, newScale);
            this.height = rescaleValue(height, scale, newScale);
            this.side = rescaleValue(side, scale, newScale);
            this.area = calcArea(base, height);
            this.perimeter = calcPerimeter(base, side);
            this.scale = verifiedNewScale;
        }

        @Override
        public @NotNull Class<? extends Shape> getType() {
            return Parallelogram.class;
        }

        @NotNull
        protected static BigDecimal calcArea(@NotNull BigDecimal base, @NotNull BigDecimal height) {
            return base.multiply(height);
        }

        @NotNull
        protected static BigDecimal calcPerimeter(@NotNull BigDecimal base, @NotNull BigDecimal side) {
            return base
                    .add(side)
                    .multiply(BigDecimal.TWO);
        }

        @NotNull
        public static Parallelogram create(@NotNull Long id,
                                           @NotNull BigDecimal base,
                                           @NotNull BigDecimal height,
                                           @NotNull BigDecimal side,
                                           @NotNull Integer scale,
                                           boolean strict) {
            return new Parallelogram(
                    id,
                    checkAndAbs(base, BigDecimal::abs, strict),
                    checkAndAbs(height, BigDecimal::abs, strict),
                    checkAndAbs(side, BigDecimal::abs, strict),
                    checkAndAbs(scale, Math::abs, strict));
        }
    }

    @Getter
    static class Square extends Parallelogram {
        public Square(@NotNull Long id, @NotNull BigDecimal side, @NotNull Integer scale) {
            super(id, side, side, side, scale);
        }

        @Override
        public void rescale(@NotNull Integer newScale, boolean strict) {
            final Integer verifiedNewScale = checkAndAbs(newScale, Math::abs, strict);
            final BigDecimal newSide = rescaleValue(side, scale, newScale);
            this.base = newSide;
            this.height = newSide;
            this.side = newSide;
            this.area = calcArea(newSide, newSide);
            this.perimeter = calcPerimeter(newSide, newSide);
            this.scale = verifiedNewScale;
        }

        @Override
        public @NotNull Class<? extends Shape> getType() {
            return Square.class;
        }

        @NotNull
        public static Square create(@NotNull Long id, @NotNull BigDecimal side, @NotNull Integer scale, boolean strict) {
            return new Square(id, checkAndAbs(side, BigDecimal::abs, strict), checkAndAbs(scale, Math::abs, strict));
        }
    }

    @Getter
    static class Cube extends VolumetricShape {
        private BigDecimal side;

        public Cube(@NotNull Long id, @NotNull BigDecimal side, @NotNull Integer scale) {
            super(id, calcArea(side), calcVolume(side), scale);
            this.side = side;
        }

        @Override
        public void rescale(@NotNull Integer newScale, boolean strict) {
            final Integer verifiedNewScale = checkAndAbs(newScale, Math::abs, strict);
            final BigDecimal newSide = rescaleValue(side, scale, newScale);
            this.side = newSide;
            this.area = calcArea(newSide);
            this.volume = calcVolume(newSide);
            this.scale = verifiedNewScale;
        }

        @Override
        public @NotNull Class<? extends Shape> getType() {
            return Cube.class;
        }

        @NotNull
        private static BigDecimal calcVolume(@NotNull BigDecimal side) {
            return side.pow(3);
        }

        @NotNull
        private static BigDecimal calcArea(@NotNull BigDecimal side) {
            return side
                    .pow(2)
                    .multiply(BigDecimal.valueOf(6));
        }

        @NotNull
        public static Cube create(@NotNull Long id, @NotNull BigDecimal side, @NotNull Integer scale, boolean strict) {
            return new Cube(id, checkAndAbs(side, BigDecimal::abs, strict), checkAndAbs(scale, Math::abs, strict));
        }
    }

    interface IRound {
        @NotNull
        BigDecimal getRadius();
    }

    interface ISurface {
        @NotNull
        BigDecimal getArea();
    }

    interface IIdentifyable {
        @NotNull
        Long getId();
    }

    interface ICacheable {
        @NotNull
        LocalDateTime getCachingTime();
    }

    interface IScalable {
        @NotNull
        Integer getScale();

        void rescale(@NotNull Integer newScale, boolean strict);
    }

    interface IShape extends IIdentifyable, ICacheable, IScalable, ISurface {
        @NotNull
        Class<? extends Shape> getType();
    }

    @NotNull
    private static <T extends Number> T checkAndAbs(@NotNull T t, @NotNull UnaryOperator<@NotNull T> absFunc, boolean strict) {
        if (strict && t.doubleValue() <= 0) {
            throw new RuntimeException("Negative values are not allowed: " + t);
        }
        return absFunc.apply(t);
    }


    private static BigDecimal refreshUnifyScaleAndCalcArea(
            @NotNull Map<Class<? extends IShape>, Function<List<Long>, List<? extends IShape>>> fetchFuncs,
            @NotNull Integer scale,
            @NotNull List<? extends IShape> shapes,
            boolean strict) {

        final LocalDateTime cacheStallTime = LocalDateTime.now().minusSeconds(CACHE_DURATION_IN_SEC);
        final Map<Boolean, List<IShape>> split = shapes.stream()
                .collect(Collectors.groupingBy(s -> cacheStallTime.isAfter(s.getCachingTime())));

        final List<IShape> outdatedShapes = Optional.ofNullable(split.get(Boolean.TRUE))
                .orElse(List.of());

        final Stream<? extends IShape> refreshedShapes = outdatedShapes.stream()
                .collect(Collectors.groupingBy(s -> fetchFuncs.get(s.getType())))
                .entrySet().stream()
                .map(entry -> entry.getKey()
                        .apply(entry.getValue().stream()
                                .map(IShape::getId).toList()))
                .flatMap(List::stream);

        final List<IShape> validShapes = Optional.ofNullable(split.get(Boolean.FALSE))
                .orElse(List.of());

        return Stream.concat(validShapes.stream(), refreshedShapes)
                .filter(Objects::nonNull)
                .peek(s -> s.rescale(scale, strict))
                .map(IShape::getArea)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }

    // Так лучше не делать без острой необходимости
    static class ShapeFactory {
        private final boolean strict;

        ShapeFactory(boolean strict) {
            this.strict = strict;
        }

        @NotNull
        @SneakyThrows({
                NoSuchMethodException.class,
                InstantiationException.class,
                IllegalAccessException.class,
                InvocationTargetException.class})
        public <T extends Shape> T createShape(@NotNull Class<T> clazz, @NotNull List<? extends Number> params) {
            final Class<?>[] paramTypes = params.stream()
                    .map(Object::getClass)
                    .toArray(Class<?>[]::new);
            final var validParams = params.stream()
                    .map(this::checkAndAbs)
                    .toArray(Number[]::new);
            return clazz.getDeclaredConstructor(paramTypes)
                    .newInstance((Object[]) validParams);
        }

        @NotNull
        @SuppressWarnings("unchecked")
        private <T extends Number> T checkAndAbs(@NotNull T t) {
            if (t instanceof BigDecimal b) {
                return (T) ShapeCalc.checkAndAbs(b, BigDecimal::abs, strict);
            } else if (t instanceof Integer i) {
                return (T) ShapeCalc.checkAndAbs(i, Math::abs, strict);
            } else if (t instanceof Long i) {
                return (T) ShapeCalc.checkAndAbs(i, Math::abs, strict);
            } else {
                throw new RuntimeException("Type is not supported: " + t.getClass().getName());
            }
        }
    }

    private static List<FlatShape> fetchFlatShapes(@NotNull List<Long> ids) {
        return List.of(
                Circle.create(1L, BigDecimal.TEN.negate(), 2, STRICT_VALIDATION),
                Square.create(2L, BigDecimal.TEN, -4, STRICT_VALIDATION),
                Parallelogram.create(4L, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TWO, 6, STRICT_VALIDATION));
    }

    private static List<VolumetricShape> fetchVolumetricShapes(@NotNull List<Long> ids) {
        return List.of(
                Sphere.create(3L, BigDecimal.TWO, 20, STRICT_VALIDATION),
                Cube.create(5L, BigDecimal.TEN, 30, STRICT_VALIDATION));
    }

    @FunctionalInterface
    public interface ThrowingFun<T> {
        Optional<T> get(T t) throws CriticalException, NonCriticalException;
    }

    public static <T> Optional<T> wrap(ThrowingFun<T> fun, T t) {
        try {
            return fun.get(t);
        } catch (CriticalException ex) {
            throw new ShapeApplicationException("Alarm!");
        } catch (NonCriticalException ex) {
            return Optional.empty();
        }
    }
}