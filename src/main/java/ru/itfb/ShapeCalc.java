package ru.itfb;

import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

class ShapeCalc {
    private static final BigDecimal PI = BigDecimal.valueOf(3.14159);
    private static final boolean STRICT_VALIDATION = false;

    public static void main(String[] args) {
        final ShapeFactory shapeFactory = new ShapeFactory(STRICT_VALIDATION);

        final Circle circle = Circle.create(BigDecimal.TEN.negate(), 1, STRICT_VALIDATION);
        final FlatShape square = Square.create(BigDecimal.TEN, -2, STRICT_VALIDATION);
        final Sphere sphere = Sphere.create(BigDecimal.TWO, 10, STRICT_VALIDATION);
        // Есть взаимная зависимость между высотой и боковой стороной (высота <= боковой стороны),
        // поэтому вместо задания трёх длин лучше использовать длины двух сторон и угол,
        // здесь сделано для простоты
        final FlatShape parallelogram = Parallelogram.create(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TWO, 3, STRICT_VALIDATION);
        final VolumetricShape cube = Cube.create(BigDecimal.TEN, 15, STRICT_VALIDATION);


        final Circle circle2 = shapeFactory.createShape(Circle.class, List.of(BigDecimal.TWO.negate(), 2));
        final FlatShape square2 = shapeFactory.createShape(Square.class, List.of(BigDecimal.ONE, 3));
        final Sphere sphere2 = shapeFactory.createShape(Sphere.class, List.of(BigDecimal.TEN, -10));


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
    static abstract class Shape {
        private final BigDecimal area;
        private final Integer scale;
    }

    @Getter
    static abstract class FlatShape extends Shape {
        private final BigDecimal perimeter;

        public FlatShape(@NotNull BigDecimal area, @NotNull BigDecimal perimeter, @NotNull Integer scale) {
            super(area, scale);
            this.perimeter = perimeter;
        }
    }

    @Getter
    static abstract class VolumetricShape extends Shape {
        private final BigDecimal volume;

        public VolumetricShape(@NotNull BigDecimal area, @NotNull BigDecimal volume, @NotNull Integer scale) {
            super(area, scale);
            this.volume = volume;
        }
    }

    @Getter
    static class Circle extends FlatShape implements IRound {
        private final BigDecimal radius;

        Circle(@NotNull BigDecimal radius, @NotNull Integer scale) {
            super(calcArea(radius), calcPerimeter(radius), scale);
            this.radius = radius;
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
        public static Circle create(@NotNull BigDecimal radius, @NotNull Integer scale, boolean strict) {
            return new Circle(checkAndAbs(radius, BigDecimal::abs, strict), scale);
        }
    }

    @Getter
    static class Sphere extends VolumetricShape implements IRound {
        // Прим.: У сферы, как и у круга, есть радиус, но нет периметра
        // Поэтому не вполне корректно делать и наследование сферы от круга, и делать круг как сферу с нулевым объёмом
        private final BigDecimal radius;

        public Sphere(@NotNull BigDecimal radius, @NotNull Integer scale) {
            super(calcArea(radius), calcVolume(radius), scale);
            this.radius = radius;
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
        public static Sphere create(@NotNull BigDecimal radius, @NotNull Integer scale, boolean strict) {
            return new Sphere(checkAndAbs(radius, BigDecimal::abs, strict), checkAndAbs(scale, Math::abs, strict));
        }
    }

    @Getter
    static class Parallelogram extends FlatShape {
        private final BigDecimal base; // Основание: __
        private final BigDecimal height; // Высота: |
        private final BigDecimal side; // Боковая грань: /

        public Parallelogram(@NotNull BigDecimal base, @NotNull BigDecimal height, @NotNull BigDecimal side, @NotNull Integer scale) {
            super(calcArea(base, height), calcPerimeter(base, side), scale);
            this.base = base;
            this.height = height;
            this.side = side;
        }

        @NotNull
        private static BigDecimal calcArea(@NotNull BigDecimal base, @NotNull BigDecimal height) {
            return base.multiply(height);
        }

        @NotNull
        private static BigDecimal calcPerimeter(@NotNull BigDecimal base, @NotNull BigDecimal side) {
            return base
                    .add(side)
                    .multiply(BigDecimal.TWO);
        }

        @NotNull
        public static Parallelogram create(@NotNull BigDecimal base, @NotNull BigDecimal height, @NotNull BigDecimal side,
                                           @NotNull Integer scale, boolean strict) {
            return new Parallelogram(
                    checkAndAbs(base, BigDecimal::abs, strict),
                    checkAndAbs(height, BigDecimal::abs, strict),
                    checkAndAbs(side, BigDecimal::abs, strict),
                    checkAndAbs(scale, Math::abs, strict));
        }
    }

    @Getter
    static class Square extends Parallelogram {
        public Square(@NotNull BigDecimal side, @NotNull Integer scale) {
            super(side, side, side, scale);
        }

        @NotNull
        public static Square create(@NotNull BigDecimal side, @NotNull Integer scale, boolean strict) {
            return new Square(checkAndAbs(side, BigDecimal::abs, strict), checkAndAbs(scale, Math::abs, strict));
        }
    }

    @Getter
    static class Cube extends VolumetricShape {
        private final BigDecimal side;

        public Cube(@NotNull BigDecimal side, @NotNull Integer scale) {
            super(calcArea(side), calcVolume(side), scale);
            this.side = side;
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
        public static Cube create(@NotNull BigDecimal side, @NotNull Integer scale, boolean strict) {
            return new Cube(checkAndAbs(side, BigDecimal::abs, strict), checkAndAbs(scale, Math::abs, strict));
        }
    }

    interface IRound {
        @NotNull
        BigDecimal getRadius();
    }

    @NotNull
    private static <T extends Number> T checkAndAbs(@NotNull T t, @NotNull UnaryOperator<@NotNull T> absFunc, boolean strict) {
        if (strict && t.doubleValue() <= 0) {
            throw new RuntimeException("Negative values are not allowed: " + t);
        }
        return absFunc.apply(t);
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
            } else {
                throw new RuntimeException("Type is not supported: " + t.getClass().getName());
            }
        }
    }
}