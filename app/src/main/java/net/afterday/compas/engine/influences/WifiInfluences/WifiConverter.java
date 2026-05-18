package net.afterday.compas.engine.influences.WifiInfluences;

/* JADX INFO: loaded from: classes.dex */
public class WifiConverter {
    public static double convert(int type, int signal) {
        switch (type) {
            case 0:
                return rad(signal);
            case 1:
                return ano(signal);
            case 2:
                return men(signal);
            case 3:
                return bur(signal);
            case 4:
                return con(signal);
            case 5:
                return rad(signal);
            case 6:
                return art(signal);
            case 7:
                return mon(signal);
            default:
                return 0.0d;
        }
    }

    public static double rad(double signal) {
        if (signal > -45.0d) {
            return map(signal, -45.0d, 0.0d, 100.0d, 1000.0d);
        }
        if (signal > -50.0d) {
            return map(signal, -50.0d, -45.0d, 16.0d, 50.0d);
        }
        if (signal > -55.0d) {
            return map(signal, -55.0d, -50.0d, 15.0d, 16.0d);
        }
        if (signal > -60.0d) {
            return map(signal, -60.0d, -50.0d, 7.0d, 15.0d);
        }
        if (signal > -70.0d) {
            return map(signal, -70.0d, -60.0d, 1.0d, 7.0d);
        }
        if (signal > -100.0d) {
            return map(signal, -100.0d, -70.0d, 0.0d, 1.0d);
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

    public static double art(double signal) {
        if (signal > -40.0d) {
            return map(signal, -40.0d, 0.0d, 100.0d, 1000.0d);
        }
        if (signal > -50.0d) {
            return map(signal, -50.0d, -40.0d, 50.0d, 100.0d);
        }
        if (signal > -60.0d) {
            return map(signal, -60.0d, -50.0d, 15.0d, 50.0d);
        }
        if (signal > -80.0d) {
            return map(signal, -80.0d, -60.0d, 7.0d, 15.0d);
        }
        if (signal > -100.0d) {
            return map(signal, -100.0d, -80.0d, 0.0d, 7.0d);
        }
        return 0.0d;
    }

    public static double mon(double signal) {
        if (signal > -100.0d) {
            return map(signal, -100.0d, 0.0d, 100.0d, 1000.0d);
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
        if (Math.abs(signal) <= rate) {
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
}
