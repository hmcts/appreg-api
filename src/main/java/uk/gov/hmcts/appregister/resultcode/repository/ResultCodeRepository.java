package uk.gov.hmcts.appregister.resultcode.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.resultcode.model.ResultCode;

public interface ResultCodeRepository extends JpaRepository<ResultCode, Long> {
    Optional<ResultCode> findByResultCode(String code);
}
