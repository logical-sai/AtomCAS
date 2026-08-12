package com.AtomCAS;

import com.AtomCAS.utilities.LargeInteger;

import java.io.*;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

public class Time {
    private static final Path SAVE_FILE = Path.of("atomcas-time.dat");

    LargeInteger year;
    int month;
    int day;
    int hour;
    int minute;
    int second;

    public Time() {
        year = LargeInteger.valueOf(2026);
        month = 8;
        day = 1;
        hour = 22;
        minute = 26;
        second = 30;
    }
    boolean isLeapYear() {
        if (year.mod(4) != 0)
            return false;
        if (year.mod(100) == 0)
            return year.mod(400) == 0;
        return true;
    }
    int daysInMonth() {
        return switch (month) {
            case 2 -> isLeapYear() ? 29 : 28;
            case 4, 6, 9, 11 -> 30;
            default -> 31;
        };
    }
    void update() {addSeconds(1);}
    void addSeconds(long amount) {
        if (amount < 0)
            throw new IllegalArgumentException("Cannot move time backwards");
        long totalSeconds = second + amount;

        second = (int) (totalSeconds % 60);
        long totalMinutes = minute + totalSeconds / 60;

        minute = (int) (totalMinutes % 60);
        long totalHours = hour + totalMinutes / 60;

        hour = (int) (totalHours % 24);
        long daysToAdd = totalHours / 24;

        while (daysToAdd > 0) {
            int remainingInMonth = daysInMonth() - day;

            if (daysToAdd <= remainingInMonth) {
                day += (int) daysToAdd;
                return;
            }

            daysToAdd -= remainingInMonth + 1;
            day = 1;
            month++;

            if (month == 13) {
                month = 1;
                year = year.add(1);
            }
        }
    }

    void save() throws IOException {
        Path temporary = SAVE_FILE.resolveSibling(SAVE_FILE.getFileName() + ".tmp");

        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
            output.writeUTF(year.toString());
            output.writeInt(month);
            output.writeInt(day);
            output.writeInt(hour);
            output.writeInt(minute);
            output.writeInt(second);
            // Real-world reference point for restoring elapsed time.
            output.writeLong(Instant.now().getEpochSecond());
        }
        try {Files.move(temporary, SAVE_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);}
        catch (AtomicMoveNotSupportedException exception) {Files.move(temporary, SAVE_FILE, StandardCopyOption.REPLACE_EXISTING);}
    }

    static Time load() throws IOException {
        if (!Files.exists(SAVE_FILE))
            return new Time();
        Time result = new Time();
        long savedAt;

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(SAVE_FILE)))) {
            result.year = new LargeInteger(input.readUTF());
            result.month = input.readInt();
            result.day = input.readInt();
            result.hour = input.readInt();
            result.minute = input.readInt();
            result.second = input.readInt();
            savedAt = input.readLong();
        }
        long now = Instant.now().getEpochSecond();
        long elapsed = Math.max(0, now - savedAt);

        result.addSeconds(elapsed);
        return result;
    }

    @Override
    public String toString() {return day + "/" + month + "/" + year + " (%02d:%02d:%02d)".formatted(hour, minute, second);}
    static void main() throws Exception {
        Time time = Time.load();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {try {time.save();} catch (IOException exception) {exception.printStackTrace(System.out);}}));
        long previous = System.nanoTime();
        long accumulated = 0;

        while (true) {
            Thread.sleep(100);

            long current = System.nanoTime();
            accumulated += current - previous;
            previous = current;

            while (accumulated >= 1_000_000_000L) {
                accumulated -= 1_000_000_000L;
                time.update();
                if (time.second == 0) {
                    System.out.println(time);
                    time.save();
                }
            }
        }
    }
}