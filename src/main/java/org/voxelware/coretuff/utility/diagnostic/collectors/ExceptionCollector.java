package org.voxelware.coretuff.utility.diagnostic.collectors;

import org.voxelware.coretuff.utility.diagnostic.DiagnosticCollector;
import org.voxelware.coretuff.utility.diagnostic.DiagnosticDumpService;
import org.voxelware.coretuff.utility.diagnostic.ExceptionRingBuffer;

import java.util.List;

public class ExceptionCollector implements DiagnosticCollector {

    private final ExceptionRingBuffer buffer;

    public ExceptionCollector(ExceptionRingBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public String sectionName() {
        return "Recent Exceptions";
    }

    @Override
    public void collect(DiagnosticDumpService service, List<String> lines) {
        List<ExceptionRingBuffer.Record> records = buffer.snapshot();
        if (records.isEmpty()) {
            lines.add("  No recent CoreTuff exceptions.");
            return;
        }

        lines.add("  Timestamp            | Type                         | Source                      | Message");
        lines.add("  " + "-".repeat(110));
        for (ExceptionRingBuffer.Record record : records) {
            lines.add("  " + record.formatted());
        }
    }
}
