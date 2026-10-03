public final class Triangle {
    private static final double EPSILON = 1e-10;

    private final double a;
    private final double b;
    private final double c;

    public Triangle(double a, double b, double c) {
        validateSide(a);
        validateSide(b);
        validateSide(c);

        if (a + b <= c || a + c <= b || b + c <= a) {
            throw new IllegalArgumentException(
                    "The given sides do not form a valid triangle."
            );
        }

        this.a = a;
        this.b = b;
        this.c = c;
    }

    public double getA() {
        return a;
    }

    public double getB() {
        return b;
    }

    public double getC() {
        return c;
    }

    public double getPerimeter() {
        return a + b + c;
    }

    public double getArea() {
        double semiPerimeter = getPerimeter() / 2.0;

        return Math.sqrt(
                semiPerimeter
                        * (semiPerimeter - a)
                        * (semiPerimeter - b)
                        * (semiPerimeter - c)
        );
    }

    public boolean isEquilateral() {
        return areEqual(a, b) && areEqual(b, c);
    }

    private static boolean areEqual(double first, double second) {
        return Math.abs(first - second) < EPSILON;
    }

    private static void validateSide(double side) {
        if (!Double.isFinite(side) || side <= 0) {
            throw new IllegalArgumentException(
                    "Triangle side must be a finite positive number."
            );
        }
    }

    @Override
    public String toString() {
        return "Triangle{" +
                "a=" + a +
                ", b=" + b +
                ", c=" + c +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Triangle triangle)) {
            return false;
        }

        return Double.compare(a, triangle.a) == 0
                && Double.compare(b, triangle.b) == 0
                && Double.compare(c, triangle.c) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(a, b, c);
    }
}