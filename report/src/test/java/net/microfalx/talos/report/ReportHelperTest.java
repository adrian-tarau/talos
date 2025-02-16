package net.microfalx.talos.report;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportHelperTest extends AbstractFragmentBuilder {

    @Test
    void getTestDurationDistributionSingle() throws IOException {
        ReportHelper helper = new ReportHelper(createSingleModuleProject());
        assertEquals(15, helper.getTestDurationDistribution().size());
    }

    @Test
    void getTestDurationDistributionMulti() throws IOException {
        ReportHelper helper = new ReportHelper(createMultiModuleProject());
        assertEquals(15, helper.getTestDurationDistribution().size());
    }

}