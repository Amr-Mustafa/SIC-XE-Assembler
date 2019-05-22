# SIC-XE-Assembler
A simple fixed-format assembler for the SIC/XE architecture described in Leland L. Beck's "Systems Programming: An Introduction to Systems Programming".

## Project Description
The assembler is capable of handling source lines that are instructions, storage declaration, comments, and assembler directives.
It handles format 2, 3, and 4 instructions and generates the object code for each instruction and data definition directive.

The input to the assembler is the source file written in the proper format described below. The output is the list file, symbol table, object file, and error report.

### Source File Format
Col 1-8:   Label
Col 9:     Blank
Col 10-15: Mnemonic Opcode
Col 16-17: Blank
Col 18-35: Operand
Col 36-66: Comment
