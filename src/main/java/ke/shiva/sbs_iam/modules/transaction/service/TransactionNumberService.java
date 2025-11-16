package ke.shiva.sbs_iam.modules.transaction.service;
import ke.shiva.sbs_iam.modules.transaction.entity.TrxRefNo;
import ke.shiva.sbs_iam.modules.transaction.repository.TrxRefNoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class TransactionNumberService {

    @Autowired
    private TrxRefNoRepository trxRefNoRepository;

    private String generateRefNo(String trxType, String prefix, Optional<String> branchCode, long sequence) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String typeCode = trxType.length() >= 3 ? trxType.substring(0, 3).toUpperCase() : trxType.toUpperCase();
        String branch = branchCode.orElse("00");
        return String.format("%s%s%s%s%06d", prefix, date, branch, typeCode, sequence);
    }
    /**
     * Generate a unique transaction reference number.
     *
     * @param trxType    The type of transaction (e.g., "PAYMENT", "REFUND").
     * @param prefix     A prefix to be added to the reference number (e.g., "TXN").
     * @param branchCode Optional branch code to include in the reference number.
     * @return A unique transaction reference number.
     */

    @Transactional
    public String generateTrxNumber(String trxType, String prefix, String branchCode) {
        // lock and increment counter
        TrxRefNo counter = trxRefNoRepository.findByTrxTypeForUpdate(trxType)
                .orElseGet(() -> {
                    TrxRefNo c = new TrxRefNo();
                    c.setTrxType(trxType);
                    c.setTrxId(1000000000L);
                    return c;
                });

        counter.setTrxId(counter.getTrxId() + 1);
        trxRefNoRepository.save(counter);

        // build reference using updated sequence
        String refNo = generateRefNo(trxType, prefix, Optional.ofNullable(branchCode), counter.getTrxId());

        // store final reference for audit
        TrxRefNo record = new TrxRefNo();
        record.setTrxType(trxType);
        record.setTrxId(counter.getTrxId());
        record.setTrxRefNo(refNo);
        trxRefNoRepository.save(record);

        return refNo;
    }

    @Transactional
    public String generateTrxNumber(String trxType, String prefix) {
        return generateTrxNumber(trxType, prefix, null);
    }
}
