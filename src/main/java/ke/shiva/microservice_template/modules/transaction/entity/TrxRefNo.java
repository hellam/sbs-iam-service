package ke.shiva.microservice_template.modules.transaction.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;

@Entity
@Table(name = "trx_ref_no")
public class TrxRefNo extends BaseEntity {

    @NotNull
    @Column(name = "trx_id")
    private Long trxId;

    @Size(max = 255)
    @NotNull
    @Column(name = "trx_ref_no", length = 255, unique = true)
    private String trxRefNo;

    @Size(max = 255)
    @NotNull
    @Column(name = "trx_type")
    private String trxType;

    // Getters and setters
    public Long getTrxId() {
        return trxId;
    }

    public void setTrxId(Long trxId) {
        this.trxId = trxId;
    }

    public String getTrxRefNo() {
        return trxRefNo;
    }

    public void setTrxRefNo(String trxRefNo) {
        this.trxRefNo = trxRefNo;
    }

    public String getTrxType() {
        return trxType;
    }

    public void setTrxType(String trxType) {
        this.trxType = trxType;
    }
}
