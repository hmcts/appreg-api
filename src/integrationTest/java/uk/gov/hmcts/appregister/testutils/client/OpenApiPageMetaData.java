package uk.gov.hmcts.appregister.testutils.client;

/**
 * This class represents the specific paging meta data related to the open api specifications.
 */
public class OpenApiPageMetaData implements PageMetaData {
    @Override
    public String getPageNumberQueryName() {
        return "pageNumber";
    }

    @Override
    public String getPageSizeQueryName() {
        return "pageSize";
    }

    @Override
    public String getSortName() {
        return "sort";
    }
}
