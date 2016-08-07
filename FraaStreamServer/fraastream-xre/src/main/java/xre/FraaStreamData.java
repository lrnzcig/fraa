package xre;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;

public class FraaStreamData {

	private BigInteger headerId;
	private FraaStreamDataUnit[] dataUnits;
	private UUID requestId;

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

	public UUID getRequestId() {
		return requestId;
	}

	public void setRequestId(final UUID requestId) {
		this.requestId = requestId;
	}
	
}
