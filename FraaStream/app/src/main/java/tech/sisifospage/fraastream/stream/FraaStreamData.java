package tech.sisifospage.fraastream.stream;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Created by lorenzorubio on 19/6/16.
 */
public class FraaStreamData {

    private BigInteger headerId;
    private FraaStreamDataUnit[] dataUnits;

    public FraaStreamData() {
        super();
    }

    public BigInteger getHeaderId() {
        return headerId;
    }

    public void setHeaderId(final BigInteger headerId) {
        this.headerId = headerId;
    }

    public FraaStreamDataUnit[] getDataUnits() {
        return dataUnits;
    }

    public void setDataUnits(final FraaStreamDataUnit[] dataUnits) {
        this.dataUnits = dataUnits;
    }

    public void addDataUnit(final FraaStreamDataUnit dataUnit) {
        if (this.dataUnits == null) {
            this.dataUnits = new FraaStreamDataUnit[1];
        } else {
            int newLength = this.dataUnits.length + 1;
            this.dataUnits = Arrays.copyOf(this.dataUnits, newLength);
        }
        this.dataUnits[this.dataUnits.length - 1] = dataUnit;
    }
}
