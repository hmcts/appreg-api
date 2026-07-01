package uk.gov.hmcts.appregister.csds.ingress.diff;

public interface IngressDiffService<ProcessedT, DiffT> {
    DiffT diff(ProcessedT processedData);
}
