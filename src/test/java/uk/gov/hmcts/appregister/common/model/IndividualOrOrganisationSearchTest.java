package uk.gov.hmcts.appregister.common.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IndividualOrOrganisationSearchTest {
    @Test
    void testSurnameOnly() {
        IndividualOrOrganisationSearch search = IndividualOrOrganisationSearch
                .of(IndividualOrOrganisationSearch.CHARACTER_TO_SPLIT_SURNAME  + "Smith");

        Assertions.assertNull( search.getName());
        Assertions.assertEquals("Smith", search.getSurname());
    }

    @Test
    void testForeNameAndSurName() {
        IndividualOrOrganisationSearch search = IndividualOrOrganisationSearch
                .of("Mike" + IndividualOrOrganisationSearch.CHARACTER_TO_SPLIT_SURNAME  + "Smith");

        Assertions.assertEquals("Mike", search.getName());
        Assertions.assertEquals("Smith", search.getSurname());
    }

    @Test
    void testForeName() {
        IndividualOrOrganisationSearch search = IndividualOrOrganisationSearch
                .of("Mike");

        Assertions.assertEquals("Mike", search.getName());
        Assertions.assertNull( search.getSurname());
    }
}
