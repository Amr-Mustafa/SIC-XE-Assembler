import java.util.HashMap;

public class Assembler {

    private HashMap<String, Integer> SYMTAB;
    private HashMap<String, Integer> OPTAB;
    private Integer LOCCTR;

    public void Assembler() {

        this.SYMTAB = new HashMap<>();
        this.OPTAB  = new HashMap<>();
        this.LOCCTR = 0;


    }

}
