package net.afterday.compas.util;

/* JADX INFO: loaded from: classes.dex */
public class Convert {
    public static float RGB_GREY = -1.0f;
    public static float RGB_BLUE = -2.0f;

    public static double rad(double signal) {
        if (signal > -40.0d) {
            return map(signal, -40.0d, 0.0d, 100.0d, 100000.0d);
        }
        if (signal > -50.0d) {
            return map(signal, -50.0d, -40.0d, 15.0d, 100.0d);
        }
        if (signal > -60.0d) {
            return map(signal, -60.0d, -50.0d, 7.0d, 15.0d);
        }
        if (signal > -80.0d) {
            return map(signal, -80.0d, -60.0d, 1.0d, 7.0d);
        }
        if (signal > -100.0d) {
            return map(signal, -100.0d, -80.0d, 0.0d, 1.0d);
        }
        return 0.0d;
    }

    public static double ano(double signal) {
        if (signal > -60.0d) {
            return 100.0d;
        }
        if (signal > -70.0d) {
            return map(signal, -70.0d, -60.0d, 7.0d, 15.0d);
        }
        if (signal > -80.0d) {
            return map(signal, -80.0d, -70.0d, 1.0d, 7.0d);
        }
        if (signal > -90.0d) {
            return map(signal, -90.0d, -80.0d, 0.0d, 1.0d);
        }
        return 0.0d;
    }

    public static double men(double signal) {
        if (signal > -60.0d) {
            return 100.0d;
        }
        if (signal > -80.0d) {
            return map(signal, -80.0d, -60.0d, 7.0d, 15.0d);
        }
        if (signal > -90.0d) {
            return map(signal, -90.0d, -80.0d, 1.0d, 7.0d);
        }
        if (signal > -100.0d) {
            return map(signal, -100.0d, -80.0d, 0.0d, 1.0d);
        }
        return 0.0d;
    }

    public static double bur(double signal) {
        return ano(signal);
    }

    public static double con(double signal) {
        return men(signal);
    }

    public static double healing(float signal, float rate) {
        if (java.lang.Math.abs(signal) <= rate) {
            return 100.0d;
        }
        return 0.0d;
    }

    public static double map(double value, double sFrom, double sTo, double dFrom, double dTo) {
        return (((value - sFrom) / (sTo - sFrom)) * (dTo - dFrom)) + dFrom;
    }

    public static float map(float value, float sFrom, float sTo, float dFrom, float dTo) {
        return (((value - sFrom) / (sTo - sFrom)) * (dTo - dFrom)) + dFrom;
    }

    public static int[] numberToRGB(double number) {
        int[] rgb = {255, 0, 0, 0};
        if (number == -1.0d) {
            rgb[1] = 65;
            rgb[2] = 65;
            rgb[3] = 65;
        } else if (number == -2.0d) {
            rgb[1] = 0;
            rgb[2] = 255;
            rgb[3] = 255;
        } else if (number >= 0.0d && number < 50.0d) {
            rgb[1] = 255;
            rgb[2] = (int) map(number, 0.0d, 50.0d, 0.0d, 255.0d);
            rgb[3] = 0;
        } else if (number >= 50.0d && number < 100.0d) {
            rgb[1] = (int) map(number, 50.0d, 100.0d, 255.0d, 0.0d);
            rgb[2] = 255;
            rgb[3] = 0;
        } else if (number >= 100.0d) {
            rgb[1] = 0;
            rgb[2] = 255;
            rgb[3] = 0;
        }
        return rgb;
    }
}
