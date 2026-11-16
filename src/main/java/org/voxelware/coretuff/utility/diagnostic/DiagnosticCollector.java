package org.voxelware.coretuff.utility.diagnostic;

import java.util.List;

public interface DiagnosticCollector {

    String sectionName();

    void collect(DiagnosticDumpService service, List<String> lines);
}
