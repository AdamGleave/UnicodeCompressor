package uk.ac.cam.cl.arg58.mphil;

import uk.ac.cam.eng.ml.tcs27.compression.tcs27.Codable;
import uk.ac.cam.eng.ml.tcs27.compression.tcs27.Decoder;
import uk.ac.cam.eng.ml.tcs27.compression.tcs27.Encoder;

import java.util.Collection;

/**
 * Created by adam on 04/03/16.
 */
public class Bernoulli<X> implements Codable<X> {
    private double bias;
    private X zero, one;

    public Bernoulli(double bias, X zero, X one) {
        this.bias = bias;
        this.zero = zero;
        this.one = one;
    }

    public double getBias() {
        return bias;
    }

    public X getZero() {
        return zero;
    }

    public X getOne() {
        return one;
    }

    private long computeMid(long range) {
        long mid = (long)((double)range * bias);
        if (mid == 0) {
            mid = 1;
        } else if (mid == range) {
            mid = range - 1;
        }

        return mid;
    }

    @Override
    public void encode(X x, Encoder ec) {
        long range = ec.getRange();
        long mid = computeMid(range);

        if (x.equals(zero)) {
            ec.storeRegion(0, mid);
        } else if (x.equals(one)) {
            ec.storeRegion(mid, range);
        } else {
            throw new IllegalArgumentException("x is neither zero or one.");
        }
    }

    @Override
    public void encode(X x, Collection<X> omit, Encoder ec) {
        // TODO
        encode(x, ec);
    }

    @Override
    public X decode(Decoder dc) {
        long target = dc.getTarget();
        long range = dc.getRange();
        long mid = computeMid(range);

        if (target < mid) {
            dc.loadRegion(0, mid);
            return zero;
        } else {
            dc.loadRegion(mid, range);
            return one;
        }
    }

    @Override
    public X decode(Collection<X> omit, Decoder dc) {
        // TODO
        return decode(dc);
    }
}
