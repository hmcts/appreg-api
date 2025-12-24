package uk.gov.hmcts.appregister.applicationcode.controller;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.applicationcode.api.ApplicationCodeSortFieldEnum;
import uk.gov.hmcts.appregister.applicationcode.service.ApplicationCodeService;
import uk.gov.hmcts.appregister.common.mapper.PageableMapper;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.api.ApplicationCodesApi;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodePage;

/**
 * REST controller for managing application codes.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ApplicationCodeController implements ApplicationCodesApi {
    private final ApplicationCodeService service;

    // Mapper converting OpenAPI paging params to Spring Data {@link Pageable}.
    private final PageableMapper pageableMapper;

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<ApplicationCodePage> getApplicationCodes(
            String code, String title, Integer page, Integer size, List<String> sort) {
        sort = sort == null || sort.isEmpty() ? List.of() : sort;

        // Map OpenAPI paging params into a Spring Pageable with default sort by name ascending
        PagingWrapper pageable =
                pageableMapper.from(
                        page,
                        size,
                        sort,
                        ApplicationCodeSortFieldEnum.CODE,
                        Sort.Direction.ASC,
                        ApplicationCodeSortFieldEnum::getEntityValue);

        log.info(
                "getApplicationCodes: code: {}, title: {}, page: {}, size: {}",
                code,
                title,
                page,
                size);
        return ResponseEntity.ok().body(service.findAll(code, title, pageable));
    }

    @Override
    @PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
    public ResponseEntity<ApplicationCodeGetDetailDto> getApplicationCodeByCodeAndDate(
            String code, LocalDate date) {
        ResponseEntity<ApplicationCodeGetDetailDto> response =
                ResponseEntity.ok(service.findByCode(code, date));
        log.info("getApplicationCodes: code: {}, date: {}", code, date);
        return response;
    }
}
