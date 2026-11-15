package org.voxelware.coretuff.dashboard.console;

import org.voxelware.coretuff.CoreTuff;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class ConsoleLogCollector {

    private final LogRecord[] buffer;
    private final int capacity;
    private int head;
    private int count;
    private final Object lock = new Object();
    private final Handler handler;

    public ConsoleLogCollector(CoreTuff plugin, int capacity) {
        this.capacity = capacity;
        this.buffer = new LogRecord[capacity];
        this.head = 0;
        this.count = 0;
        this.handler = createHandler();
        Logger root = Logger.getLogger("");
        root.addHandler(handler);
    }

    public void uninstall() {
        Logger root = Logger.getLogger("");
        root.removeHandler(handler);
    }

    private Handler createHandler() {
        return new Handler() {
            @Override
            public void publish(LogRecord record) {
                add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        };
    }

    private void add(LogRecord record) {
        synchronized (lock) {
            buffer[head] = record;
            head = (head + 1) % capacity;
            if (count < capacity) {
                count++;
            }
        }
    }

    public List<String> getRecent(int n) {
        synchronized (lock) {
            int size = Math.min(n, count);
            List<String> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int idx = getIndex(i);
                LogRecord record = buffer[idx];
                if (record != null) {
                    result.add(formatRecord(record));
                }
            }
            return result;
        }
    }

    public List<String> getAll() {
        return getRecent(capacity);
    }

    public List<String> search(String query) {
        if (query == null || query.isBlank()) return getAll();
        String lower = query.toLowerCase();
        synchronized (lock) {
            List<String> result = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int idx = getIndex(i);
                LogRecord record = buffer[idx];
                if (record != null && record.getMessage() != null
                        && record.getMessage().toLowerCase().contains(lower)) {
                    result.add(formatRecord(record));
                }
            }
            return result;
        }
    }

    public void clear() {
        synchronized (lock) {
            for (int i = 0; i < capacity; i++) {
                buffer[i] = null;
            }
            head = 0;
            count = 0;
        }
    }

    public int size() {
        return count;
    }

    private int getIndex(int offset) {
        if (count < capacity) return offset;
        return (head + offset) % capacity;
    }

    private String formatRecord(LogRecord record) {
        String level = record.getLevel().getName();
        String message = record.getMessage();
        if (message == null) message = "";
        return "[%s] [%s] %s".formatted(
                new SimpleDateFormat("HH:mm:ss").format(new Date(record.getMillis())),
                level,
                message
        );
    }
}
