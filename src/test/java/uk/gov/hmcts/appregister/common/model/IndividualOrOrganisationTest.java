package uk.gov.hmcts.appregister.common.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IndividualOrOrganisationTest {
    @Test
    void testSurnameOnly() {
        IndividualOrOrganisation search =
                IndividualOrOrganisation.of(
                        IndividualOrOrganisation.CHARACTER_TO_SPLIT_SURNAME + "Smith");

        Assertions.assertNull(search.getName());
        Assertions.assertEquals("Smith", search.getSurname());
    }

    @Test
    void testForeNameAndSurName() {
        IndividualOrOrganisation search =
                IndividualOrOrganisation.of(
                        "Mike" + IndividualOrOrganisation.CHARACTER_TO_SPLIT_SURNAME + "Smith");

        Assertions.assertEquals("Mike", search.getName());
        Assertions.assertEquals("Smith", search.getSurname());
    }

    @Test
    void testForeName() {
        IndividualOrOrganisation search = IndividualOrOrganisation.of("Mike");

        Assertions.assertEquals("Mike", search.getName());
        Assertions.assertNull(search.getSurname());
    }

    @Test
    void testGetIndividualNameString() {

        Assertions.assertEquals(
                "Mike Smith", IndividualOrOrganisation.getIndividualNameString("Mike", " Smith"));
    }

    @Test
    void testGetIndividualNameStringNoForename() {
        Assertions.assertEquals(
                IndividualOrOrganisation.DEFAULT_NAME + " Smith",
                IndividualOrOrganisation.getIndividualNameString(null, " Smith"));
    }

    @Test
    void testGetIndividualNameStringNoSurname() {
        Assertions.assertEquals(
                "Mike " + IndividualOrOrganisation.DEFAULT_NAME,
                IndividualOrOrganisation.getIndividualNameString("Mike", null));
    }

    @Test
    void testGetIndividualNameStringNoNames() {
        Assertions.assertEquals(
                IndividualOrOrganisation.DEFAULT_NAME + " " + IndividualOrOrganisation.DEFAULT_NAME,
                IndividualOrOrganisation.getIndividualNameString(null, null));
    }
}
