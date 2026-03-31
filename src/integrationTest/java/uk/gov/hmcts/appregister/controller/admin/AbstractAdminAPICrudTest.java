package uk.gov.hmcts.appregister.controller.admin;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class AbstractAdminAPICrudTest extends BaseIntegration {
    protected static final String WEB_CONTEXT = "admin";
    protected static final String DATABASE_JOBS_PATH = "/database/jobs/";

    @Autowired protected DatabaseJobRepository databaseJobRepository;

    protected TokenGenerator createAdminToken() {
        return getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();
    }

    protected TokenGenerator createUserToken() {
        return getATokenWithValidCredentials().roles(List.of(RoleEnum.USER)).build();
    }
}
