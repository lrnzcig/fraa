package xre;

import java.math.BigInteger;
import java.util.Date;

public class FraaStreamHeader {
	
	private BigInteger id;
	private Date createdAt;
	private String macAddress;
	private String label;

	public FraaStreamHeader() {
		super();
	}

	public BigInteger getId() {
		return id;
	}

	public void setId(final BigInteger id) {
		this.id = id;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(final Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(final String macAddress) {
		this.macAddress = macAddress;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	

}
