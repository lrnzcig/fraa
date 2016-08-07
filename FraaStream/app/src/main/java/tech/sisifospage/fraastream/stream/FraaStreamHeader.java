package tech.sisifospage.fraastream.stream;

import java.util.Date;

/**
 * Created by lorenzorubio on 31/7/16.
 */
public class FraaStreamHeader {

    private Integer id;
    private Date createdAt;
    private String macAddress;
    private String label;

    public FraaStreamHeader() {
        super();
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
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
