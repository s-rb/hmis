/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.pharmacy;

import com.divudi.entity.Category;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 *
 * @author buddhika
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class MeasurementUnit extends Category implements Serializable {

    private static final long serialVersionUID = 1L;
    private boolean strengthUnit;
    private boolean packUnit;
    private boolean issueUnit;
    

    public boolean isStrengthUnit() {
        return strengthUnit;
    }

    public void setStrengthUnit(boolean strengthUnit) {
        this.strengthUnit = strengthUnit;
    }

    public boolean isPackUnit() {
        return packUnit;
    }

    public void setPackUnit(boolean packUnit) {
        this.packUnit = packUnit;
    }

    public boolean isIssueUnit() {
        return issueUnit;
    }

    public void setIssueUnit(boolean issueUnit) {
        this.issueUnit = issueUnit;
    }

}
