package data.raytrace;

public enum SpatialUnit {
    um(-6), mm(-3), cm(-2), dm(-1), m(0), km(3);

    public final int magnitude;

    SpatialUnit(int magnitude) {
        this.magnitude = magnitude;
    }
}
