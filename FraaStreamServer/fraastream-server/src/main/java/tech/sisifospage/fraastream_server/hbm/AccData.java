package tech.sisifospage.fraastream_server.hbm;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="acc_data")
public class AccData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7145634661055552596L;
	
	@Id
	private AccDataId id;
	private float x;
	private float y;
	private float z;

	public AccData() {
		super();
	}

	public AccData(final AccDataId id, final float x, final float y, final float z) {
		super();
		this.id = id;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public AccDataId getId() {
		return id;
	}

	public void setId(final AccDataId id) {
		this.id = id;
	}

	public float getX() {
		return x;
	}

	public void setX(final float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(final float y) {
		this.y = y;
	}

	public float getZ() {
		return z;
	}

	public void setZ(final float z) {
		this.z = z;
	}
	
	
	
}
