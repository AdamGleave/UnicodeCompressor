package mphil.arg58.cl.cam.ac.uk;

import compression.tcs27.mlg.eng.cam.ac.uk.Codable;
import compression.tcs27.mlg.eng.cam.ac.uk.Decoder;
import compression.tcs27.mlg.eng.cam.ac.uk.Encoder;

import java.util.Collection;

/**
 * Created by adam on 04/03/16.
 */
public class Bernoulli<X> implements Codable<X> {
    private double bias;
    private X zero, one;

    private long mid;

    public Bernoulli(double bias, X zero, X one) {
        this.bias = bias;
        this.zero = zero;
        this.one = one;

        mid = (long)((double)(Long.MAX_VALUE) * bias);
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

    @Override
    public void encode(X x, Encoder ec) {
        if (x.equals(zero)) {
            ec.storeRegion(0, mid, Long.MAX_VALUE);
        } else if (x.equals(one)) {
            ec.storeRegion(mid, Long.MAX_VALUE, Long.MAX_VALUE);
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
        long target = dc.getTarget(Long.MAX_VALUE);
        if (target <= mid) {
            return zero;
        } else {
            return one;
        }
    }

    @Override
    public X decode(Collection<X> omit, Decoder dc) {
        // TODO
        return decode(dc);
    }
}
