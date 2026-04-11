package s4i.pto.model;

public class Color4f {
    public int r;
    public int g;
    public int b;
    public int a;

    public Color4f() {
        this.r = 255;
        this.g = 255;
        this.b = 255;
        this.a = 200;
    }

    public Color4f(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public static Color4f of(int r, int g, int b, int a) {
        return new Color4f(r, g, b, a);
    }

    public static Color4f of(int rgb, float a) {
        return new Color4f((
                rgb >> 16) & 255,
                (rgb >> 8) & 255,
                rgb & 255,
                (int) (a * 255));
    }
}
