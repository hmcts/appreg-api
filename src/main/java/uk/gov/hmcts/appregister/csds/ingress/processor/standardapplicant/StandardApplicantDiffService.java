package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.database.StandardApplicantIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.exception.CsdsDuplicateRecordException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandardApplicantDiffService
        implements IngressDiffService<StandardApplicantDiffRequest, StandardApplicantDiffResult> {
    private final JdbcIngressTableReadService tableReadService;
    private final StandardApplicantIngressDatabaseRowMapper rowMapper;

    @Override
    public StandardApplicantDiffResult diff(StandardApplicantDiffRequest request) {
        val incomingByCode = new LinkedHashMap<String, StandardApplicantIngressRecord>();
        request.processedData().stream()
                .flatMap(page -> request.recordsExtractor().apply(page).stream())
                .map(request.recordMapper())
                .forEach(item -> addIncomingRecord(request.targetTable(), incomingByCode, item));

        val existingById =
                tableReadService.loadAll(request.targetTable(), rowMapper).stream()
                        .collect(
                                Collectors.toMap(
                                        StandardApplicantIngressRecord::id,
                                        item -> item,
                                        (first, second) -> second,
                                        LinkedHashMap::new));
        val diffRecords =
                new ArrayList<
                        IngressDiffRecord<
                                StandardApplicantIngressRecord,
                                StandardApplicantIngressRecord,
                                StandardApplicantIngressRecord>>();
        val existingByCode =
                existingById.values().stream()
                        .collect(
                                Collectors.toMap(
                                        StandardApplicantIngressRecord::code, item -> item));
        val incomingById = new LinkedHashMap<Long, StandardApplicantIngressRecord>();
        for (val incoming : incomingByCode.values()) {
            val existing = existingByCode.get(incoming.code());
            val intendedId =
                    existing == null
                            ? StandardApplicantIngressRecord.calculateId(
                                    incoming.pssId(), incoming.id())
                            : existing.id();
            if (intendedId == null) {
                throw new AppRegistryException(
                        CommonAppError.INTERNAL_SERVER_ERROR,
                        "Missing source ID for new standard applicant " + incoming.code());
            }
            val intended = incoming.withId(intendedId);
            val idOwner = existingById.get(intendedId);
            val incomingOwner = incomingById.putIfAbsent(intendedId, intended);
            if (existing == null
                    && incoming.pssId() == null
                    && ((idOwner != null && !idOwner.code().equals(incoming.code()))
                            || incomingOwner != null)) {
                log.error(
                        "Generated legacy SA_ID {} conflicts for incoming standard applicant [{}]",
                        intendedId,
                        describe(intended));
                throw new CsdsDuplicateRecordException(
                        "Generated legacy SA_ID "
                                + intendedId
                                + " conflicts for incoming standard applicant ["
                                + describe(intended)
                                + "]");
            }
            diffRecords.add(
                    new IngressDiffRecord<>(
                            existing == null ? IngressOperation.INSERT : IngressOperation.UPDATE,
                            intended,
                            existing,
                            intended,
                            existing == null
                                    ? "no existing standard_applicant_code match"
                                    : "existing standard_applicant_code match"));
        }
        return new StandardApplicantDiffResult(
                incomingById, existingById, List.copyOf(diffRecords));
    }

    private void addIncomingRecord(
            String targetTable,
            LinkedHashMap<String, StandardApplicantIngressRecord> incomingByCode,
            StandardApplicantIngressRecord item) {
        val existing = incomingByCode.putIfAbsent(item.code(), item);
        if (existing == null) {
            return;
        }
        log.error(
                "Duplicate incoming standard_applicant_code {} detected for {}. Existing record "
                        + "[{}], duplicate record [{}]",
                item.code(),
                targetTable,
                describe(existing),
                describe(item));
        throw new CsdsDuplicateRecordException(
                "Duplicate incoming standard_applicant_code "
                        + item.code()
                        + " detected for "
                        + targetTable);
    }

    private String describe(StandardApplicantIngressRecord item) {
        return "applicantId=%s, pssId=%s, code=%s, name=%s, startDate=%s, version=%s"
                .formatted(
                        item.id(),
                        item.pssId(),
                        item.code(),
                        item.name(),
                        item.startDate(),
                        item.version());
    }
}
