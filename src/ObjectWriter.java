import java.util.ArrayList;

public class ObjectWriter {

    public static String writeHeaderRecord(String programName, int startingAddress, int endingAddress) {
        return String.format("%c%-6s%06x%06x\n", 'H', programName.length() > 6 ? programName.substring(0, 6) : programName,
                startingAddress, endingAddress - startingAddress - 1).toUpperCase();
    }

    public static String writeEndRecord(int addressOfFirstExecutable) {
        return String.format("%c%06x\n", 'E', addressOfFirstExecutable).toUpperCase();
    }

    public static String writeTextRecord(int startingAddress, int endingAddress, ArrayList<String> instructions) {

        if (instructions.isEmpty()) return "";

        String record = "T" + String.format("%06x", startingAddress) + String.format("%02x", endingAddress -
                startingAddress);

        for (String instruction : instructions) {
            record += instruction;
        }

        return record.toUpperCase() + "\n";
    }

}
