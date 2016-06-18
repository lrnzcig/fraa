package xre;

import java.math.BigInteger;

public class FraaStreamDataUnit {
	
	private BigInteger headerId;
	private BigInteger index;
	private Float x;
	private Float y;
	private Float z;

	public FraaStreamDataUnit() {
		super();
	}

	public BigInteger getHeaderId() {
		return headerId;
	}

	public void setHeaderId(final BigInteger headerId) {
		this.headerId = headerId;
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
