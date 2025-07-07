package uk.gov.hmcts.appregister.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.appregister.model.ResultCode;

import java.util.Optional;

public interface ResultCodeRepository extends JpaRepository<ResultCode, Long> {
    Optional<ResultCode> findByResultCode(String code);
}
