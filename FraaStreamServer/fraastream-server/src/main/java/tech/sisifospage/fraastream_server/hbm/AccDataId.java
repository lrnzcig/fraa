package tech.sisifospage.fraastream_server.hbm;

import java.io.Serializable;
import java.math.BigInteger;

import javax.persistence.Column;

public class AccDataId implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1471667019951860073L;

	@Column(name="header_id")
	private BigInteger headerId;
	@Column(name="id")
	private BigInteger id;

	public AccDataId() {
		super();
	}

	public AccDataId(final BigInteger headerId, final BigInteger id) {
		super();
		this.headerId = headerId;
		this.id = id;
	}

	public BigInteger getHeaderId() {
		return headerId;
	}

	public void setHeaderId(final BigInteger headerId) {
		this.headerId = headerId;
	}

	public BigInteger getId() {
		return id;
	}

	public void setId(final BigInteger id) {
		this.id = id;
	}
	
	
}
