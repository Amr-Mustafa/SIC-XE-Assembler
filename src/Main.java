import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Function;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class Main {

    private static File INFILE = null;
    private static FileWriter writer = null;
    private static Scanner parser = null;
    private static HashMap<String, Integer> SYMTAB;
    private static HashMap<String, Instruction> OPTAB;
    private static Integer LOCCTR = -1;

    private static Function<String,Integer> expressionParser = new Function<String,Integer>(){
        public Integer apply(String str)
        {
            ArrayDeque<Character> operations = new ArrayDeque<>(); // gets the operations in same sequence as general expression
            for(char c : str.toCharArray())
                if(c == '/' || c == '*' || c == '+' || c == '-')
                    operations.offer(c); // adds them in a FIFO Queue
            System.out.println("Existing operations: "+operations);
            String[] operands = str.split("[\\+\\-\\*/]"); //splits string with operands
            StringBuilder expression = new StringBuilder(); // a string builder for concatinating the expression with its actual values
            for(int i = 0 ; i < operands.length ; ++i)
            {
                // gets the values from symtable if label and if not puts the value directly
                expression.append(operands[i].matches("^.*[a-zA-Z].*$") ? SYMTAB.get(operands[i]).toString() : operands[i]);
                // gets the operation
                expression.append(operations.poll());
            }
            // converting the StringBuilder to String to substring later using length()
            String operation = expression.toString();
            // -4 as it adds null at end of String if concatinated normally or if StringBuilder converted to string
            try{ // because throws checked ScriptException
                return (Integer)(new ScriptEngineManager().getEngineByName("JavaScript").eval(operation.substring(0,operation.length()-4)));
            }catch(ScriptException ex)
            {
                ex.printStackTrace();
            }
            return -1;
        }
    };

    public static void main(String[] args) {

        /* Set up the required data structures. */
        SYMTAB = new HashMap<>();
        OPTAB  = new HashMap<>();

        /* Input the registers' addresses into the symbol table. */
        SYMTAB.put("A", 0);
        SYMTAB.put("X", 1);
        SYMTAB.put("L", 2);
        SYMTAB.put("B", 3);
        SYMTAB.put("S", 4);
        SYMTAB.put("T", 5);

        /* Fill the operation code table with each instruction and its numeric opcode. */
        OPTAB.put("RMO",   new Instruction(2, Integer.parseInt("AC",  16)));
        int t = OPTAB.get("RMO".strip()).getOpcode();
        OPTAB.put("ADD",   new Instruction(3, Integer.parseInt("18",  16)));
        OPTAB.put("ADDR",  new Instruction(2, Integer.parseInt("90",  16)));
        OPTAB.put("SUB",   new Instruction(3, Integer.parseInt("1C",  16)));
        OPTAB.put("SUBR",  new Instruction(2, Integer.parseInt("94",  16)));
        OPTAB.put("STA",   new Instruction(3, Integer.parseInt("0C",  16)));
        OPTAB.put("STB",   new Instruction(3, Integer.parseInt("78",  16)));
        OPTAB.put("STX",   new Instruction(3, Integer.parseInt("10",  16)));
        OPTAB.put("STS",   new Instruction(3, Integer.parseInt("7C",  16)));
        OPTAB.put("STCH",  new Instruction(3, Integer.parseInt("54",  16)));
        OPTAB.put("STL",   new Instruction(3, Integer.parseInt("14",  16)));
        OPTAB.put("LDA",   new Instruction(3, Integer.parseInt("00",  16)));
        OPTAB.put("LDB",   new Instruction(3, Integer.parseInt("68",  16)));
        OPTAB.put("LDL",   new Instruction(3, Integer.parseInt("08",  16)));
        OPTAB.put("LDS",   new Instruction(3, Integer.parseInt("6C",  16)));
        OPTAB.put("LDT",   new Instruction(3, Integer.parseInt("74",  16)));
        OPTAB.put("LDCH",  new Instruction(3, Integer.parseInt("50",  16)));
        OPTAB.put("LDX",   new Instruction(3, Integer.parseInt("04",  16)));
        OPTAB.put("COMP",  new Instruction(3, Integer.parseInt("28",  16)));
        OPTAB.put("COMPR", new Instruction(2, Integer.parseInt("88",  16)));
        OPTAB.put("J",     new Instruction(3, Integer.parseInt("3C",  16)));
        OPTAB.put("JEQ",   new Instruction(3, Integer.parseInt("30",  16)));
        OPTAB.put("JGT",   new Instruction(3, Integer.parseInt("34",  16)));
        OPTAB.put("JLT",   new Instruction(3, Integer.parseInt("38",  16)));
        OPTAB.put("TIX",   new Instruction(3, Integer.parseInt("2C",  16)));
        OPTAB.put("TIXR",  new Instruction(2, Integer.parseInt("B8",  16)));

        /* Select assembly source file. */
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showOpenDialog(null);

        /* If the user didn't choose any file, exit the program. */
        if(returnVal == JFileChooser.APPROVE_OPTION) INFILE = chooser.getSelectedFile(); else return ;

        /* We are going to parse the input file line by line. */
        try { parser = new Scanner(INFILE); }
        catch (Exception e) { System.out.println(e.toString()); }

        /* Set up the intermediate output file. */
        try {
            writer = new FileWriter("LSTFILE.txt");
            writer.write("LINE NO.   ADDRESS  LABEL     OPCODE             OPERANDS           COMMENTS\n");
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        int operationNumber = 0, lineNumber = 1;
        String line = "", label = "        ", opcode = "     ", operand = "                  ", comment = "",expressionEvaluation = "";

        /* Fixed-format error flags. */
        boolean misplacedLabel = false, misplacedOpcode = false, duplicateSymbol = false, missingOpcode = false,
                misplacedOperand = false, missingOperand = false, illegalFormat = false, unrecognizedOpcode = false,
                undefinedSymbol = false, incorrectHexString = false, illegalRegister = false, cantHaveLabel = false,
                wrongPrefix = false, immediate = false, incorrect = false,simpleExpression   = false, invalidExpression = false;

        /* Store undefined symbols. */
        ArrayList<String> undefinedSymbols = new ArrayList<>();






        //****************************************************************************\\
        //********************************* PASS ONE *********************************\\
        //****************************************************************************\\

        while (LOCCTR < 0) {

            /* Read the first line to set up the location counter. */
            if (parser.hasNext()) {

                // Read next line.
                line = parser.nextLine();

                // Skip comments.
                if (line.charAt(0) == '.') continue;

                // Extract the label.
                label = line.substring(0, 8);

                // Make sure the label is in its correct location.
                if (!Character.isAlphabetic(label.charAt(0)) && !label.trim().isBlank()) misplacedLabel = true;

                // Extract the opcode.
                opcode = line.substring(9, 14);

                // Make sure the opcode is in its correct location.
                if (!Character.isAlphabetic(opcode.charAt(0)) && !opcode.isBlank()) misplacedOpcode = true;

                // Extract the operand and comment if exists.
                if (line.length() > 35) {
                    operand = line.substring(17, 35).stripTrailing();
                    comment = line.substring(35);
                }
                else operand = line.substring(17).stripTrailing();

                if (opcode.stripTrailing().equals("START")) {

                    // Extract the initial value of the location counter.
                    LOCCTR = Integer.parseInt(operand, 16);

                    // Write to the intermediate file.
                    try {
                        String outputLine = String.format("%-10s %-8s %6s  %-18s %-15s %-31s\n", String.valueOf(lineNumber), Integer.toHexString(LOCCTR).toUpperCase(),
                                label, opcode, operand, comment);
                        writer.write(outputLine);
                    } catch (Exception e) {
                        System.out.println(e.toString());
                    }

                    // Prepare the next line.
                    lineNumber++;
                    if (parser.hasNext()) line = parser.nextLine();

                }

                else LOCCTR = 0;

            }
        }

        int startingAddress = LOCCTR;
        String programName = label;

        /* Parse the assembly file line by line. */
        while (!line.isEmpty() && !opcode.strip().equals("END")) {

            // Reset error flags.
            misplacedLabel     = false;
            misplacedOpcode    = false;
            duplicateSymbol    = false;
            missingOpcode      = false;
            misplacedOperand   = false;
            missingOperand     = false;
            illegalFormat      = false;
            unrecognizedOpcode = false;
            undefinedSymbol    = false;
            incorrectHexString = false;
            illegalRegister    = false;
            cantHaveLabel      = false;
            wrongPrefix        = false;
            incorrect          = false;
            immediate          = false;
            simpleExpression   = false;
            invalidExpression  = false;
            //reset expressionEvaluation value
            expressionEvaluation = "";

            comment = " ";
            operand = " ";

            /* Skip comments. */
            if (line.charAt(0) == '.') {

                // Write to the intermediate file.
                try {
                    String outputLine = String.format("%-10s %-8s %-31s\n", String.valueOf(lineNumber), Integer.toHexString(LOCCTR).toUpperCase(), line);
                    writer.write(outputLine);
                }
                catch (Exception e) { System.out.println(e.toString()); }

                lineNumber++;

                if (parser.hasNext()) line = parser.nextLine(); else line = "";

                // Go parse the next line if exists.
                continue;
            }

            /* Get and the validate the fields. */

            // Extract the label.
            label = line.substring(0, 8);
            // Make sure the label is in its correct location.
            if (!Character.isAlphabetic(label.charAt(0)) && !label.trim().isBlank()) misplacedLabel = true;

            // Extract the opcode.
            if (line.length() > 14) opcode = line.substring(9, 14);
            else opcode = line.substring(9);
            // Make sure the opcode is in its correct location.
            if (opcode.charAt(0) == ' ' && !opcode.isBlank()) misplacedOpcode = true;
            else if (opcode.strip().isBlank()) missingOpcode = true;

            if (misplacedOpcode) {
                // Write to the intermediate file.
                try {
                    String outputLine = String.format("%-10s %-8s %6s  %-18s %-15s %-31s\n", String.valueOf(lineNumber), Integer.toHexString(LOCCTR).toUpperCase(),
                            label, opcode, (immediate ? "#" + operand : operand), comment);
                    writer.write(outputLine);
                } catch (Exception e) {
                    System.out.println(e.toString());
                }
            }

            // Extract the operand and comment if exists.
            if (line.length() > 15) { // Operand exists.
                if (line.length() > 35) { // Comment exists.
                    operand = line.substring(17, 35);
                    comment = line.substring(35);
                } else { // Comment does not exist.
                    operand = line.substring(17);
                }
            } else operand = " ";

            // An instruction must have an operand.
            if (operand.strip().isBlank()) {
                missingOperand = true;
            }

            // Make sure the operand is in its correct location.
            if (!missingOperand && operand.charAt(0) == ' ' && !operand.isBlank()) misplacedOperand = true;

            if (!missingOperand && !misplacedOperand && operand.charAt(0) == '#') { immediate = true; operand = operand.substring(1); }

            // If a label exists, add it to symbol table.
            if (!label.trim().isBlank() && !misplacedLabel)
                if (!SYMTAB.containsKey(label.strip()))
                    SYMTAB.put(label.strip(), LOCCTR);
                else
                    duplicateSymbol = true;

            /* Process instructions. */
            if (!misplacedOperand && !missingOpcode && !misplacedOpcode &&!isDirective(opcode)) {

                // Make sure the opcode is recognized in the operation code table.
                if ((opcode.strip().charAt(0) == '+') && !OPTAB.containsKey(opcode.strip().substring(1))) unrecognizedOpcode = true;
                else if(!(opcode.strip().charAt(0) == '+') && !OPTAB.containsKey(opcode.strip())) unrecognizedOpcode = true;

                int instructionLength = 0;

                if (!Character.isAlphabetic(opcode.charAt(0)) && opcode.charAt(0) != '+') wrongPrefix = true;

                // Maybe format 4.
                if (!missingOperand && !unrecognizedOpcode && opcode.strip().charAt(0) == '+') {

                    if (OPTAB.get(opcode.strip().substring(1)).getFormat() == 2) illegalFormat = true;
                    else instructionLength = 4;

                    // Operand contains an undefined symbol ?
                    if (!illegalFormat && operand.matches(".*[a-zA-Z]+.*")) {
                        //check if is simple expression
                        simpleExpression = Main.tester(operand,str -> str.split("[\\+\\-\\*/]").length > 1);
                        if(simpleExpression) // if it is, check if it's valid >> no consecutive operation and no whitespaces
                        {
                            // check if starts with an operand or ends with an operand
                            if(operand.matches("^.*[\\+\\-\\*/]+$") || operand.matches("^[\\+\\-\\*/]+.*$"))
                                invalidExpression = true;
                            else // checks if it's surrounded by whitespace or consecutive operands
                                invalidExpression = Main.tester(operand,str -> {for(String e:str.split("[\\+\\-\\*/]"))
                                    if(e.trim().isEmpty() || e.matches("^.*[\\s]+.*$"))
                                        return true;
                                    return false;
                                });
                        }
                        if(simpleExpression && !invalidExpression) // checks if used symbols exists
                            undefinedSymbol = Main.tester((operand.charAt(0) == '#' || operand.charAt(0) == '@') ?
                                    operand.substring(1).strip() : operand.strip(), str -> {
                                for(String s : str.split("[\\+\\-\\*/]")) {
                                    System.out.println("Checking "+s);
                                    if (s.matches("^.*[a-zA-Z].*$") && !Main.SYMTAB.containsKey(s)) {
                                        System.out.println(s + " is invalid");
                                        return true;
                                    }
                                }

                                return false;
                            });
                        else if(simpleExpression && invalidExpression) // an expression and invalid, invalid expression error
                            undefinedSymbol = false;
                        else // not an expression
                            undefinedSymbol = Main.tester((operand.charAt(0) == '#' || operand.charAt(0) == '@') ?
                                    operand.substring(1).strip() : operand.strip(), str -> !Main.SYMTAB.containsKey(str));

                        if(simpleExpression && !invalidExpression && !undefinedSymbol) // evaluates expression
                        {
                            expressionEvaluation = Integer.toString(Main.computeGeneralExpression(operand,Main.expressionParser));
                            SYMTAB.put(operand,Integer.parseInt(expressionEvaluation));
                        }
                    }

                    // Maybe format 3 or 2.
                } else if (!missingOperand && !unrecognizedOpcode) {

                    // Format 2.
                    if (OPTAB.get(opcode.strip()).getFormat() == 2) {

                        // Validate register addresses.
                        String[] registers = operand.strip().split(",");
                        if (!SYMTAB.containsKey(registers[0].toUpperCase()) ||
                                !SYMTAB.containsKey(registers[1].toUpperCase())) {
                            illegalRegister = true;
                            instructionLength = 0;
                        } else instructionLength = 2;

                    // Format 3.
                    } else {

                        // Format 3 instructions take 3 bytes.
                        instructionLength = 3;

                        // Operand contains an undefined symbol.
                        if (!illegalFormat && operand.matches(".*[a-zA-Z]+.*")) {

                            // Check if operand is simple expression.
                            simpleExpression = Main.tester(operand,str -> str.split("[\\+\\-\\*/]").length > 1);

                            // If simple expression check if it's valid >> no consecutive operation and no whitespaces.
                            if(simpleExpression)  {

                                // check if starts with an operand or ends with an operand
                                if(operand.matches("^.*[\\+\\-\\*/]+$") || operand.matches("^[\\+\\-\\*/]+.*$"))
                                    invalidExpression = true;
                                else // checks if it's surrounded by whitespace or consecutive operands
                                    invalidExpression = Main.tester(operand,str -> {for(String e:str.split("[\\+\\-\\*/]"))
                                        if(e.trim().isEmpty() || e.matches("^.*[\\s]+.*$"))
                                            return true;
                                        return false;
                                    });
                            }
                            if(simpleExpression && !invalidExpression) // checks if used symbols exists
                                undefinedSymbol = Main.tester((operand.charAt(0) == '#' || operand.charAt(0) == '@') ?
                                        operand.substring(1).strip() : operand.strip(), str -> {
                                    for(String s : str.split("[\\+\\-\\*/]"))
                                    {
                                        System.out.println("Checking "+s);
                                        if(s.matches("^.*[a-zA-Z].*$") && !Main.SYMTAB.containsKey(s)) {
                                            System.out.println(SYMTAB);
                                            System.out.println(s+" is invalid");
                                            return true;
                                        }
                                    }
                                    return false;
                                });
                            else if(simpleExpression && invalidExpression) // an expression and invalid, invalid expression error
                                undefinedSymbol = false;

                            else // Not an expression.
                                undefinedSymbol = Main.tester((operand.charAt(0) == '#' || operand.charAt(0) == '@') ?
                                        operand.substring(1).strip() : operand.strip(), str -> !Main.SYMTAB.containsKey(str));

                            if(simpleExpression && !invalidExpression && !undefinedSymbol) // Evaluate expression.
                            {
                                expressionEvaluation = Integer.toString(Main.computeGeneralExpression(operand, Main.expressionParser));
                                System.out.println(expressionEvaluation);
                                SYMTAB.put(operand,Integer.parseInt(expressionEvaluation));
                            }
                        }
                    }

                }

                // Write to the intermediate file.
                try {
                    String outputLine = String.format("%-10s %-8s %6s  %-18s %-15s %-31s\n", String.valueOf(lineNumber), Integer.toHexString(LOCCTR).toUpperCase(),
                            label, opcode, (immediate ? "#" + operand : operand), comment);
                    writer.write(outputLine);
                } catch (Exception e) {
                    System.out.println(e.toString());
                }

                // Update the location counter.
                LOCCTR += instructionLength;

            } else if (!missingOpcode && !misplacedOpcode) {
                // Process directives.
                int dataLength = 0;

                if (!opcode.strip().equals("END") && operand.strip().isBlank()) missingOperand = true;

                if (!missingOperand && opcode.strip().equals("WORD")) {

                    if(operand.toLowerCase().charAt(0) == 'x' || operand.toLowerCase().charAt(0) == 'c')
                        incorrect = true;
                    else incorrectHexString=Main.incorrectHexaStringTest(operand,6);

                    dataLength = 3;


                } else if (!missingOperand && opcode.strip().equals("BYTE")) {

                    if(operand.trim().toLowerCase().charAt(0) == 'x') {
                        incorrectHexString = Main.incorrectHexaStringTest(operand, 2);
                        if (!incorrectHexString) dataLength = (operand.length() - 3) / 2; // Hexadecimal digit = nibble.
                    }
                    else dataLength = operand.length() - 3; // Character = byte.

                } else if (!missingOperand && opcode.strip().equals("RESW")) {

                    dataLength = 3 * Integer.parseInt(operand);

                } else if (!missingOperand && opcode.strip().equals("RESB")) {

                    dataLength = Integer.parseInt(operand);

                } else if (!missingOperand && opcode.strip().equals("ORG")) {

                    if(!label.trim().isEmpty()) cantHaveLabel = true;

                    if(operand.trim().matches("[0-9]+"))//if its all digit
                        LOCCTR = Integer.parseInt(operand);

                    else if(operand.substring(0,1).matches("[0-9]+"))//if the first char is digit and the rest is not
                        unrecognizedOpcode = true;

                    else if(!SYMTAB.containsKey(operand.trim()))//if the symbols are not defined
                        undefinedSymbol = true;

                    else {
                        LOCCTR = SYMTAB.get(operand.trim());
                    }

                } else if (!missingOperand && opcode.strip().equals("EQU")) {

                    if(operand.trim().matches("[0-9]+"))//if its all digit
                        SYMTAB.replace(label.trim(), Integer.parseInt(operand.trim()));

                    else if(operand.substring(0,1).matches("[0-9]+"))//if the first char is digit and the rest is not
                        unrecognizedOpcode = true;

                    else if(!SYMTAB.containsKey(operand.trim()))//if the symbols are not defined
                        undefinedSymbol = true;
                    else {
                        SYMTAB.replace(label.trim(), SYMTAB.get(operand.trim()));
                    }

                } else if (opcode.strip().equals("END")) {
                    if(!label.trim().isBlank()) cantHaveLabel = true;
                    missingOperand = false;

                }

                // Write to the intermediate file.
                try {
                    String outputLine = String.format("%-10s %-8s %6s  %-18s %-15s %-31s\n", String.valueOf(lineNumber), Integer.toHexString(LOCCTR).toUpperCase(),
                            label, opcode, (immediate ? "#" + operand : operand), comment);
                    writer.write(outputLine);
                } catch (Exception e) {
                    System.out.println(e.toString());
                }

                //if (undefinedSymbol) undefinedSymbols.add(operand.trim());

                LOCCTR += dataLength;
            }

            //for (String symbol : undefinedSymbols) undefinedSymbol = !SYMTAB.containsKey(symbol);

            try {
                if (misplacedLabel) writer.write("**** ERROR: MISPLACED LABEL ****" + "\n");
                if (misplacedOpcode) writer.write("**** ERROR: MISPLACED OPERATION CODE ****" + "\n");
                if (duplicateSymbol) writer.write("**** ERROR: DUPLICATE LABEL DEFINITION ****" + "\n");
                if (missingOpcode) writer.write("**** ERROR: MISSING OPERATION CODE ****" + "\n");
                if (illegalFormat) writer.write("**** ERROR: CAN'T BE FORMAT 4 INSTRUCTION ****" + "\n");
                if (unrecognizedOpcode) writer.write("**** ERROR: UNRECOGNIZED OPERATION CODE MNEMONIC ****" + "\n");
                if (undefinedSymbol) writer.write("**** ERROR: UNDEFINED SYMBOL ****" + "\n");
                if (illegalRegister) writer.write("**** ERROR: ILLEGAL ADDRESS FOR REGISTER ****" + "\n");
                if (incorrectHexString) writer.write("**** ERROR: NOT A HEXADECIMAL STRING ****" + "\n");
                if (missingOperand) writer.write("**** ERROR: MISSING OPERAND ****" + "\n");
                if (cantHaveLabel) writer.write("**** ERROR: STATEMENT CAN NOT HAVE A LABEL ****" + "\n");
                if (wrongPrefix) writer.write("**** ERROR: WRONG OPERATION PREFIX ****" + "\n");
                if (incorrect) writer.write("**** ERROR: EXTRA CHARACTER AT END OF STATEMENT ****" + "\n");
                if (invalidExpression) writer.write("**** ERROR: INVALID GENERAL EXPRESSION ****" + "\n");
            } catch (Exception e) { System.out.println(e.toString()); }

            if (parser.hasNext()) line = parser.nextLine(); else line = "";

            lineNumber++;

        }

        if (!opcode.equals("END")) {
            try { writer.write("**** ERROR: MISSING END STATEMENT ****"); }
            catch (Exception e) { System.out.println(e.toString()); }
        }

        try { writer.close(); } catch (Exception e) { System.out.println(e.toString()); }

        SYMTAB.forEach(Main::printSYMTAB);

        File file2 = new File("LSTFILE.txt");
        try { parser = new Scanner(file2); }
        catch (Exception e) { System.out.println(e.toString()); }

        Path path = Paths.get("LSTFILE.txt");
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            undefinedSymbol = true;

            for(int i = 0;parser.hasNextLine();i++) {
                line = parser.nextLine();

                /* Extract the opcode. */
                String[] fields = line.split("\\s+"); // Split the line based on spaces.
                if (OPTAB.containsKey(fields[2].charAt(0) == '+' ? fields[2].substring(1) : fields[2])
                        || isDirective(fields[2])) opcode = fields[2];
                else opcode = fields[3];

                if(opcode.trim().contains("ORG") || opcode.trim().equals("END")) continue;

                /* Extract the operand field. */
                if (line.charAt(0) != '.' && opcode.charAt(0) == '+' && opcode.equals(fields[2])) operand = fields[3];
                else if (opcode.equals(fields[2])) operand = fields[3];
                else operand = fields[4];

//                if (!operand.matches(".*[a-zA-Z]+.*")) {
//                    undefinedSymbol = false;
//                    continue;
//                }
                //System.out.println("OPERAND: " + operand);

                if(SYMTAB.containsKey(operand.trim())) undefinedSymbol = false;
                //System.out.println(undefinedSymbol);
                if(line.contains("UNDEFINED SYMBOL") && !undefinedSymbol) {
                    //System.out.println("line number -> "+i);
                    lines.remove(i);
                    i--;
                    Files.write(path, lines, StandardCharsets.UTF_8);
                    undefinedSymbol = true;
                }
            }

            parser = new Scanner(file2);
            for(int i = 0;parser.hasNextLine();i++) {
                line = parser.nextLine();
                if(line.contains("UNDEFINED SYMBOL"))
                    undefinedSymbol = true;
                else
                    undefinedSymbol = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //System.out.println(undefinedSymbol);

        /* Set up the intermediate assembler report file. */
        try {
            writer = new FileWriter("REPORT.txt");
            writer.write("LINE NO.   ADDRESS  LABEL     OPCODE             OPERANDS           COMMENTS\n");
        } catch (Exception e) { System.out.println(e.toString()); }

        try { parser = new Scanner(file2); }
        catch (Exception e) { System.out.println(e.toString()); }

        String line1 = new String("");
        String line2 = new String("");

        try {
            line1 = parser.nextLine();
            while(parser.hasNextLine())
            {
                line2 = parser.nextLine();

                if(line2.contains("ERROR") && ! line1.contains("ERROR")) {
                    writer.write(line1+"\n"+line2+"\n");
                }
                else if (line2.contains("ERROR") && line1.contains("ERROR")) {
                    writer.write(line2+"\n");
                }
                line1 = line2;
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        /* ******************************************************************************************************** */
        /*                                    Start of the second assembly pass.                                    */
        /* ******************************************************************************************************** */

        /* Any error in the previous pass propagates to the second pass and prevents assembly. */
        if (misplacedLabel || misplacedOpcode || duplicateSymbol || missingOpcode || illegalFormat ||
                unrecognizedOpcode || undefinedSymbol || illegalRegister || incorrectHexString || missingOperand ||
                cantHaveLabel || wrongPrefix || incorrect || invalidExpression) {
            System.out.println("ERROR: Can't perform second assembly pass.");
            return;
        }

        /* Parse the intermediate file line by line. */
        File file = new File("LSTFILE.txt");
        try { parser = new Scanner(file); }
        catch (Exception e) { System.out.println(e.toString()); }

        /* Prepare the object file. */
        try { writer = new FileWriter("OBJFILE.txt"); }
        catch (Exception e) { System.out.println(e.toString()); }

        /* Read first line. */
        if (parser.hasNext()) line = parser.nextLine(); // Skip header line.
        if (parser.hasNext()) line = parser.nextLine();

        /* Extract the opcode. */
        String[] fields = line.split("\\s+"); // Split the line based on spaces.
        if (OPTAB.containsKey(fields[2].charAt(0) == '+' ? fields[2].substring(1) : fields[2])
                || isDirective(fields[2])) opcode = fields[2];
        else opcode = fields[3];

        /* Book keeping data. */
        ArrayList<String> instructions = new ArrayList<>(); // Store the instructions for each text record.

        int instructionsLengthInBytes = 0, // We need to keep track of the length of instructions in each record.
            instructionLength = 0, // Current instruction length in bytes.
            addressOfCurrentInstruction = Integer.parseInt(fields[1], 16),
            addressOfFirstInstructionInRecord = 0,
            format = 0; // Format of the current instruction.

        boolean newTextRecord = false; // Determine whether to start a new text record or not.
        boolean written = false, lineIsAComment = false, firstInstructionInRecord = true;

        String value = "";

        int PC = startingAddress, BASE = 0;

        /* Iterate over each instruction/directive in the intermediate file. */
        while (!opcode.strip().equals("END")) {

            /* Skip comments. */
            if (fields[2].charAt(0) == '.') lineIsAComment = true;

            /* Process instructions only. */
            boolean isDirective = isDirective(opcode.strip());

            /* Extract the operand field. */
            if (!lineIsAComment && opcode.charAt(0) == '+' && opcode.equals(fields[2])) operand = fields[3];
            else if (opcode.equals(fields[2])) operand = fields[3];
            else operand = fields[4];

            if (lineIsAComment || isDirective) {

                /* Process directives. */
                if (isDirective && !opcode.equals("START")) {

                    value = ""; // Reset.

                    /* If the directive defines data, then we need to write it in the object program. */
                    if (opcode.equals("WORD") || opcode.equals("BYTE")) {

                        /* Generate the data value to be stored in the object program. */
                        /* If the operand is a symbol or an expression, get its value. */
                        if (operand.charAt(0) != 'X' && operand.charAt(0) != 'C' &&
                                operand.matches(".*[a-zA-Z]+.*")) value = Integer.toHexString(SYMTAB.get(operand));

                        /* If it is a hexadecimal string. */
                        else if (operand.charAt(0) == 'X') {
                            value = operand.substring(2, operand.length() - 1);
                            PC += value.length() / 2;
                        }

                        /* If it is a character string. */
                        else if (operand.charAt(0) == 'C') {

                            operand = operand.substring(2, operand.length() - 1);

                            PC += operand.length();

                            /* Convert each character to its hexadecimal representation. */
                            for (int i = 0, n = operand.length(); i < n; ++i) {
                                value += Integer.toHexString(operand.charAt(i));
                            }
                        }

                        /* If it is not a symbol, convert it to a proper value. */
                        else value = Integer.toHexString(Integer.parseInt(operand));

                        /* At this point we have a hexadecimal representation of the value to be stored.
                           We need to pad it as needed (WORD = 6 HEX digits, BYTE = 2 HEX digits). */
                        if (opcode.equals("WORD") && value.length() < 6) {
                            int amountOfPadding = 6 - value.length();
                            String padding = "";
                            for (int i = 0; i < amountOfPadding; ++i) padding += "0";
                            value = padding + value;
                            PC += 3;
                        } else if (opcode.equals("BYTE") && value.length() < 2) {
                            value = "0" + value;
                        }

                        addressOfCurrentInstruction = Integer.parseInt(fields[1], 16);
                        /* Now we have the proper representation needed to be written in the object file. */

                    /* RESB and RESW directives cause the current open text record to be written and closed. */
                    } else if (opcode.equals("RESB") || opcode.equals("RESW")) {

                        /* Reserve enough space. */
                        PC += Integer.parseInt(operand) * (opcode.equals("RESW") ? 3 : 1);

                        /* Both directives close the current open text record. */
                        try { writer.write(ObjectWriter.writeTextRecord(addressOfFirstInstructionInRecord,
                                addressOfCurrentInstruction, instructions)); }
                        catch (Exception e) { System.out.println(e.toString()); }

                        /* Reset state. */
                        instructions.clear();
                        firstInstructionInRecord = true;
                        newTextRecord = false;
                        written = true;

                        /* Get next line. */
                        if (parser.hasNext()) {
                            line = parser.nextLine();

                            /* Extract the opcode. */
                            fields = line.split("\\s+");
                            if (OPTAB.containsKey(fields[2].charAt(0) == '+' ? fields[2].substring(1) : fields[2]) ||
                                    isDirective(fields[2])) opcode = fields[2];
                            else opcode = fields[3];

                            addressOfCurrentInstruction = Integer.parseInt(fields[1], 16);

                        } else break;

                        continue;
                    }

                /* Skip comments. */
                } else {

                    /* Get next line. */
                    if (parser.hasNext()) {
                        line = parser.nextLine();

                        /* Extract the opcode. */
                        fields = line.split("\\s+");
                        if (OPTAB.containsKey(fields[2].charAt(0) == '+' ? fields[2].substring(1) : fields[2]) ||
                                isDirective(fields[2])) opcode = fields[2];
                        else opcode = fields[3];

                        addressOfCurrentInstruction = Integer.parseInt(fields[1], 16);

                    } else break;

                    lineIsAComment = false;
                    continue;
                }
            }

            if (firstInstructionInRecord) {
                addressOfFirstInstructionInRecord = addressOfCurrentInstruction;
                firstInstructionInRecord = false;
            }

            /* Assemble the instruction. */
            String instruction = "";
            if (!isDirective) {
                switch (OPTAB.get(opcode.charAt(0) == '+' ? opcode.substring(1) : opcode).getFormat()) { // Switch on format.

                    /* Format 2. op[0:7] r1[8:11] r2[12:15] */
                    case 2:
                        format = 2;

                        /* Update the PC to point to the next instruction. */
                        PC += 2;

                        /* Get the register operands. */
                        String[] registers = operand.split(",");

                        /* Assemble the instruction. */
                        instruction += Integer.toHexString(OPTAB.get(opcode.strip()).getOpcode());
                        instruction += Integer.toHexString(SYMTAB.get(registers[0]));
                        instruction += Integer.toHexString(SYMTAB.get(registers[1]));

                        /* Now that we have successfully assembled the instruction at hand, we need to see whether it
                               will fit into the current open text record or not. */
                        if (instructionsLengthInBytes + 2 <= 30) instructionsLengthInBytes += 2;
                        else {
                            newTextRecord = true;
                            instructionLength = 2;
                        }

                        break;

                    /* Format 3 or 4. */
                    case 3:

                        if (opcode.charAt(0) != '+') { // Format 3.
                            format = 3;

                            /* Update the PC to point to the next instruction. */
                            PC += 3;

                            /* We need to find out the values for the n,i,x,b,p,e flag bits and the disp field. */
                            int targetAddress, flags = 0, disp = 0;

                            if (operand.matches(".*[a-zA-Z]+.*")) { // The operand is a symbol.

                                /* Since the operand is a symbol and the instruction is of format 3,
                                   we must use relative addressing to locate the target address. */

                                if (operand.charAt(0) == '#' || operand.charAt(0) == '@')
                                    if (operand.contains(",X")) targetAddress = SYMTAB.get(operand.substring(1, operand.length() - 2));
                                    else targetAddress = SYMTAB.get(operand.substring(1)); // Substring to skip the '#' or '@'.
                                else if (operand.contains(",X"))
                                    targetAddress = SYMTAB.get(operand.substring(0, operand.length() - 2));
                                else
                                    targetAddress = SYMTAB.get(operand);

                                /* Now that we have the target address, we can compute the required displacement from PC.
                                   And, attempt either PC-relative or BASE-relative addressing modes. */

                                // Our assembler always attempts PC-relative addressing first by default.
                                if (targetAddress - PC <= 2047 && targetAddress - PC >= -2048) { // -2048 <= disp <= 2047.

                                    // PC-relative.
                                    flags = 2; // b=0,p=1,e=0.
                                    disp = targetAddress - PC;

                                    // PC-relative failed. Attempt base-relative addressing.
                                } else if (targetAddress - BASE <= 4095) { // 0 <= disp <= 4095.

                                    // Base-relative.
                                    flags = 4; // b=1,p=0,e=0.
                                    disp = targetAddress - BASE;

                                    // Neither PC-relative nor base-relative addressing modes are applicable.
                                } else {
                                    System.out.println("ERROR: DISPLACEMENT OUT OF RANGE.");
                                }

                                /* We calculated the target address, now we need to figure out how to use that address
                                   to actually get our operand from memory and set the appropriate flag bits. */

                                if (operand.charAt(0) == '#') { // Immediate.

                                    // n=0,i=1,x=0.
                                    if (flags == 2) flags = 18;
                                    else if (flags == 4) flags = 20;

                                    //disp = SYMTAB.get(operand.substring(1).strip());

                                } else if (operand.charAt(0) == '@') { // Indirect.

                                    // n=1,i=0,x=0.
                                    if (flags == 2) flags = 34;
                                    else if (flags == 4) flags = 36;

                                } else if (operand.contains(",X")) { // Direct, indexed.

                                    // n=1,i=1,x=1.
                                    if (flags == 2) flags = 58;
                                    else if (flags == 4) flags = 60;

                                } else { // Direct.

                                    // n=1,i=1,x=0.
                                    if (flags == 2) flags = 50;
                                    else if (flags == 4) flags = 52;

                                }

                            } else if (operand.equals("*")) { // E.g. J * instruction.

                                flags = 50; // n=i=1, b=0,p=1,e=0
                                disp = 0;

                            } else { // If the operand is not a symbol but a constant, then b=p=0.

                                flags = 16;
                                disp = Integer.parseInt(operand.substring(1));

                            }

                            /* At this point we have all the flag bits and the disp field ready, so we can finally
                                   assemble the instruction. */
                            instruction = Integer.toHexString((((OPTAB.get(opcode.strip()).getOpcode() >> 2) << 6) | 63) &
                                    (4032 | flags));
                            instruction += String.format("%03x", disp & 4095);
                            instruction = leftPad(instruction, 6, 3);

                            /* Now that we have successfully assembled the instruction at hand, we need to see whether it
                               will fit into the current open text record or not. */
                            if (instructionsLengthInBytes + 3 <= 30) instructionsLengthInBytes += 3;
                            else {
                                newTextRecord = true;
                                instructionLength = 3;
                            }

                        } else { // Format 4.
                            format = 4;

                            /* Update the PC to point to the next instruction. */
                            PC += 4;

                            /* We need to find out the values for the n,i,x,b,p,e flag bits and the address field. */
                            int targetAddress = 0, flags = 1; // b=p=0,e=1.

                            if (operand.matches(".*[a-zA-Z]+.*")) { // The operand is a symbol.

                                /* Since the operand is a symbol and this is a format 4 instruction then the target
                                   address is the operand itself (i.e. no relative addressing here). */
                                if (operand.charAt(0) == '#' || operand.charAt(0) == '@')
                                    targetAddress = SYMTAB.get(operand.substring(1)); // Substring to skip the '#' or '@'.
                                else if (operand.contains(",X")) targetAddress = SYMTAB.get(operand.substring(0, operand.length() - 2));
                                else targetAddress = SYMTAB.get(operand);

                                /* At this point we have the opcode and the address fields, we still need to identify the
                                   remaining three flags, namely the n,i, and x flags based on the operand. */
                                if (operand.charAt(0) == '#') { // Immediate.

                                    // n=0,i=1,x=0.
                                    flags = 17;

                                } else if (operand.charAt(0) == '@') { // Indirect.

                                    // n=1,i=0,x=0.
                                    flags = 33;

                                } else if (operand.contains(",x")) { // Direct, indexed.

                                    // n=0,i=0,x=1.
                                    flags = 57; // nixbpe: 111001

                                } else { // Direct.

                                    // n=1,i=1,x=0.
                                    flags = 49; // nixbpe: 110001

                                }

                            }

                            /* At this point we have all the flag bits and the disp field ready, so we can finally
                                   assemble the instruction. */
                            instruction = Integer.toHexString((((OPTAB.get(opcode.substring(1)).getOpcode() >> 2) << 6)
                                    | 63) & (4032 | flags));
                            instruction += String.format("%05x", targetAddress);
                            instruction = leftPad(instruction, 8, 4);

                            /* Now that we have successfully assembled the instruction at hand, we need to see whether it
                               will fit into the current open text record or not. */
                            if (instructionsLengthInBytes + 4 <= 30) instructionsLengthInBytes += 4;
                            else {
                                newTextRecord = true;
                                instructionLength = 4;
                            }

                        }

                        break;
                }
            } else instruction = value;
            System.out.println(instruction.toUpperCase());

            /* A text record can hold a maximum of 30 bytes worth of instructions. */
            if (newTextRecord) { // Write current record, generate a new record for this instruction, and reset state.

                // Write the current text record to the object file.
                try { writer.write(ObjectWriter.writeTextRecord(addressOfFirstInstructionInRecord,
                        addressOfCurrentInstruction, instructions)); }
                catch (Exception e) { System.out.println(e.toString()); }

                // Reset the instructions length counter.
                instructionsLengthInBytes = instructionLength;
                addressOfFirstInstructionInRecord = addressOfCurrentInstruction;
                instructions.clear();
                instructions.add(instruction);
                firstInstructionInRecord = true;
                newTextRecord = false;
                written = true;

            } else { // Add this instruction to the list of instructions associated with the current record.
                instructions.add(instruction);
                written = false;
            }

            /* Get next line. */
            if (parser.hasNext()) {

                line = parser.nextLine();

                /* Extract the opcode. */
                fields = line.split("\\s+");
                if (OPTAB.containsKey(fields[2].charAt(0) == '+' ? fields[2].substring(1) : fields[2]) ||
                        isDirective(fields[2])) opcode = fields[2];
                else opcode = fields[3];

                if (!opcode.equals("END"))
                    addressOfCurrentInstruction = Integer.parseInt(fields[1], 16);

            } else break;

        }

        /* If we reached the end of the file an haven't yet written the open text record, then write it. */
        if (!written) {
            // Write the current text record to the object file.
            try { writer.write(ObjectWriter.writeTextRecord(addressOfFirstInstructionInRecord,
                    addressOfCurrentInstruction, instructions)); }
            catch (Exception e) { System.out.println(e.toString()); }
        }

        /* Write the end record. */
        try { writer.write(ObjectWriter.writeEndRecord(startingAddress)); }
        catch (Exception e) { System.out.println(e.toString()); }

        /* Close the writer. */
        try { writer.close(); } catch (Exception e) { System.out.println(e.toString()); }

        /* The object field now contains all required text and end records, we need to append the header record. */

        File objFile = new File("OBJFILE.txt");
        try { parser = new Scanner(objFile); }
        catch (Exception e) { System.out.println(e.toString()); }

        /* Get the header record. */
        String objectCode = ObjectWriter.writeHeaderRecord(programName, startingAddress, addressOfCurrentInstruction);
        //System.out.println(startingAddress + " " + addressOfCurrentInstruction);
        /* Append it to the start of the object file. */
        while (parser.hasNext()) objectCode += parser.nextLine() + '\n';

        /* Now we need to overwrite the object file with the new one. */
        try { writer = new FileWriter("OBJFILE.txt"); }
        catch (Exception e) { System.out.println(e.toString()); }

        try { writer.write(objectCode); } catch (Exception e) { System.out.println(e.toString()); }

        try { writer.close(); } catch (Exception e) { System.out.println(e.toString()); }
        try { parser.close(); } catch (Exception e) { System.out.println(e.toString()); }
    }

    private static void printSYMTAB (String key, Integer value) {
        System.out.println(key + " -> " + Integer.toHexString(value).toUpperCase());
    }

    private static boolean isDirective (String opcode) {
        opcode = opcode.strip();
        return opcode.equals("BYTE") || opcode.equals("WORD") || opcode.equals("START")
                || opcode.equals("END") || opcode.equals("EQU") || opcode.equals("ORG")
                || opcode.equals("BASE") || opcode.equals("RESW") || opcode.equals("RESB");
    }

    private static boolean tester(String operand, Predicate<String> tester) {
        return tester.test(operand);
    }

    private static int computeGeneralExpression(String operand,Function<String,Integer> function) {
        return function.apply(operand);
    }

    private static boolean incorrectHexaStringTest(String operand,int maxLimit) {
        //if (operand.length() % 2 != 0) return false;
        if(maxLimit == 2) {
            if (operand.trim().matches("^[Xx]\'.{1," + maxLimit + "}\'$"))
                return Main.tester(operand.trim(),
                        str -> str.matches("^.*\'.*[^A-Fa-f\\d]+.*\'$"));
            else
                return true;
        }else if(maxLimit == 6)
        {
            if (operand.trim().matches("^.{1,}$"))
                return Main.tester(operand.trim(),
                        str -> str.matches("^.*[^A-Fa-f\\d]+.*$"));
            else
                return true;
        }
        return false;
    }

    private static String leftPad(String str, int str_length, int format){

        String PAD_STRING = format == 3 ? "000000" : "00000000";

        if(str.length() < str_length )
            return PAD_STRING.substring(str.length()) + str;
        else
            return str;
    }

}