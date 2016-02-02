package cc.teil.sisyphus.mru.mobiletelemetry.Calibration;

import ZS.Solve.LMfunc;

/**
 * Created by mru on 11.01.16.
 */
public class LMPolynom implements LMfunc {
    @Override
    public double val(double[] x, double[] a) {
        return a[0] + a[1] * x[0] + a[2] * x[0] * x[0];
    }

    @Override
    public double grad(double[] x, double[] a, int ak) {
        if (ak == 0) {
            return 1;
        }
        if (ak == 1) {
            return x[0];
        }
        if (ak == 2) {
            return x[0] * x[0];
        }
        return 0;
    }

    @Override
    public double[] initial() {
        double[] a = new double[3];
        a[0] = 0.;
        a[1] = 1.;
        a[2] = 0.;
        return a;
    }

    @Override
    public Object[] testdata() {

        double[] a = new double[3];
        a[0] = 0;
        a[1] = 1;
        a[2] = 0;

        int npts = 10;
        double[][] x = new double[npts][1];
        double[] y = new double[npts];
        double[] s = new double[npts];

        for (int i = 0; i < npts; i++) {
            x[i][0] = (double) i / npts;
            y[i] = val(x[i], a);
            s[i] = 1.;
        }

        Object[] o = new Object[4];
        o[0] = x;
        o[1] = a;
        o[2] = y;
        o[3] = s;

        return o;
    }
}
