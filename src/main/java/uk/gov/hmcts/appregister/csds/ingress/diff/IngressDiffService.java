package uk.gov.hmcts.appregister.csds.ingress.diff;

/**
 * Calculates ingress actions from processed source data.
 */
public interface IngressDiffService<ProcessedT, DiffT> {
    DiffT diff(ProcessedT processedData);
}
