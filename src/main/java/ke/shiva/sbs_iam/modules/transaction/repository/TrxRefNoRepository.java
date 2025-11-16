package ke.shiva.sbs_iam.modules.transaction.repository;

import ke.shiva.sbs_iam.modules.transaction.entity.TrxRefNo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrxRefNoRepository extends JpaRepository<TrxRefNo, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TrxRefNo t WHERE t.trxType = :trxType")
    Optional<TrxRefNo> findByTrxTypeForUpdate(@Param("trxType") String trxType);
}
