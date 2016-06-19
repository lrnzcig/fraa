package tech.sisifospage.fraastream;

import java.math.BigInteger;

/**
 * Created by lorenzorubio on 29/5/16.
 */
public class FraaStreamDataUnit {

    private BigInteger index;
    private Float x;
    private Float y;
    private Float z;

    public FraaStreamDataUnit() {
        super();
    }

    public BigInteger getIndex() {
        return index;
    }

    public void setIndex(final BigInteger index) {
        this.index = index;
    }

    public Float getX() {
        return x;
    }

    public void setX(final Float x) {
        this.x = x;
    }

    public Float getY() {
        return y;
    }

    public void setY(final Float y) {
        this.y = y;
    }

    public Float getZ() {
        return z;
    }

    public void setZ(final Float z) {
        this.z = z;
    }

}
