package tech.sisifospage.fraastream_server.hbm;

import java.math.BigInteger;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="header")
public class Header {

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	private BigInteger id;
	private Date createdAt;
	private String macAddress;
	private String label;
	
	public Header() {
		super();
	}
	
	public Header(final BigInteger id, final Date createdAt, final String macAddress, final String label) {
		super();
		this.id = id;
		this.createdAt = createdAt;
		this.macAddress = macAddress;
		this.label = label;
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
