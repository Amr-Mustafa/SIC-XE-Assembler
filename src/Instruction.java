public class Instruction {

    private int format;
    private int opcode;

    public Instruction(int format, int opcode) {
        this.format = format;
        this.opcode = opcode;
    }

    public int getFormat() {
        return format;
    }

    public int getOpcode() {
        return opcode;
    }
}
