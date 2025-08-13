// by R. Malope, T. Coutts, K. Lesese, H. Chagaluka, 2025

// This is a program that develops a parser for Modula-2 declarations
// Modification of an original program written by P.D. Terry, Rhodes University, Modified by KL Bradshaw 2022

import java.util.*;
import library.*;

class Mod2decl4 {

    // +++++++++++++++++++++++++ File Handling and Error handlers
    // ++++++++++++++++++++

    static InFile input;
    static OutFile output;

    static String newFileName(String oldFileName, String ext) {
        // Creates new file name by changing extension of oldFileName to ext
        int i = oldFileName.lastIndexOf('.');
        if (i < 0)
            return oldFileName + ext;
        else
            return oldFileName.substring(0, i) + ext;
    }

    static void reportError(String errorMessage) {
        // Displays errorMessage on standard output and on reflected output
        System.out.println(errorMessage);
        output.writeLine(errorMessage);
    }

    static void abort(String errorMessage) {
        // Abandons parsing after issuing error message
        reportError(errorMessage);
        output.close();
        System.exit(1);
    }

    // +++++++++++++++++++++++ token kinds enumeration +++++++++++++++++++++++++

    static final int noSym = 0,
            EOFSym = 1,
            identifierSym = 2,
            numberSym = 3,
            typeSym = 4,
            varSym = 5,
            arraySym = 6,
            recordSym = 7,
            endSym = 8,
            setSym = 9,
            ofSym = 10,
            pointerSym = 11,
            toSym = 12,
            equalsSym = 13,
            commaSym = 14,
            semicolonSym = 15,
            colonSym = 16,
            dotSym = 17,
            leftBracketSym = 18,
            rightBracketSym = 19,
            leftParenSym = 20,
            rightParenSym = 21,
            starSym = 22;

    static final int simpleTypeFirst = identifierSym;

    // +++++++++++++++++++++++++++++ Character Handler ++++++++++++++++++++++++++

    static final char EOF = '\0';
    static boolean atEndOfFile = false;

    // Declaring ch as a global variable is done for expediency - global variables
    // are not always a good thing

    static char ch; // look ahead character for scanner

    static void getChar() {
        // obtains next character ch from input, or CHR(0) if EOF reached
        // Reflect ch to output
        if (atEndOfFile)
            ch = EOF;
        else {
            ch = input.readChar();
            atEndOfFile = ch == EOF;
            if (!atEndOfFile)
                output.write(ch);
        }
    } // getChar

    // +++++++++++++++++++++++++++++++ Scanner ++++++++++++++++++++++++++++++++++

    // Declaring sym as a global variable is done for expediency - global variables
    // are not always a good thing

    static Token sym;

    static void getSym() {
        // scans for next sym from input
        while (ch > EOF && ch <= ' ')
            getChar();
        StringBuilder symLex = new StringBuilder();
        int symKind = noSym;

        if (ch == EOF) {
            symKind = EOFSym;
        }
        // handling identifiers and keywords
        else if (Character.isLetter(ch)) {
            while (Character.isLetterOrDigit(ch)) {
                symLex.append(ch);
                getChar();
            }
            String ident = symLex.toString();
            // checking for keywords
            switch (ident) {
                case "TYPE":
                    symKind = typeSym;
                    break;
                case "VAR":
                    symKind = varSym;
                    break;
                case "ARRAY":
                    symKind = arraySym;
                    break;
                case "RECORD":
                    symKind = recordSym;
                    break;
                case "END":
                    symKind = endSym;
                    break;
                case "SET":
                    symKind = setSym;
                    break;
                case "OF":
                    symKind = ofSym;
                    break;
                case "POINTER":
                    symKind = pointerSym;
                    break;
                case "TO":
                    symKind = toSym;
                    break;
                default:
                    symKind = identifierSym;
                    break;
            }
        }
        // handling numbers
        else if (Character.isDigit(ch)) {
            while (Character.isDigit(ch)) {
                symLex.append(ch);
                getChar();
            }
            symKind = numberSym;
        }

        // handling comments
        else if (ch == '(') {
            getChar();
            if (ch == '*') {
                // It's a comment, skip until *)
                getChar();
                while (true) {
                    if (ch == '*') {
                        getChar();
                        if (ch == ')') {
                            getChar();
                            break;
                        }
                    } else if (ch == EOF) {
                        abort("Unterminated comment");
                    } else {
                        getChar();
                    }
                }
                // recursively calling getSym to get next token after comment
                getSym();
                return;
            } else {
                // It's just a '(' symbol
                symLex.append('(');
                symKind = leftParenSym;
                getChar();
            }
        }
        // handling other symbols
        else {
            switch (ch) {
                case '=':
                    symKind = equalsSym;
                    symLex.append(ch);
                    getChar();
                    break;
                case ',':
                    symKind = commaSym;
                    symLex.append(ch);
                    getChar();
                    break;
                case ';':
                    symKind = semicolonSym;
                    symLex.append(ch);
                    getChar();
                    break;
                case ':':
                    symKind = colonSym;
                    symLex.append(ch);
                    getChar();
                    break;
                case '.':
                    symKind = dotSym;
                    symLex.append(ch);
                    getChar();
                    break;
                case '[':
                    symKind = leftBracketSym;
                    symLex.append(ch);
                    getChar();
                    break;
                case ']':
                    symKind = rightBracketSym;
                    symLex.append(ch);
                    getChar();
                    break;
                case '(':
                    symKind = leftParenSym;
                    symLex.append(ch);
                    getChar();
                    break;
                case ')':
                    symKind = rightParenSym;
                    symLex.append(ch);
                    getChar();
                    break;
                case '*':
                    symKind = starSym;
                    symLex.append(ch);
                    getChar();
                    break;
                default:
                    abort("Invalid character '" + ch + "'");
            }
        }

        sym = new Token(symKind, symLex.toString());
    } // getSym

    // +++++++++++++++++++++++++++++++ Parser +++++++++++++++++++++++++++++++++++

    // Utility methods for error handling
    static void accept(int wantedSym, String errorMessage) {
        if (sym.kind == wantedSym)
            getSym();
        else
            abort(errorMessage);
    }

    static void accept(IntSet allowedSet, String errorMessage) {
        if (allowedSet.contains(sym.kind))
            getSym();
        else
            abort(errorMessage);
    }

    // Parser methods for each non-terminal
    static void Mod2Decl() {
        // Mod2Decl = { Declaration } .
        while (sym.kind == typeSym || sym.kind == varSym) {
            Declaration();
        }
        accept(EOFSym, "EOF expected");
    }

    static void Declaration() {
        // Declaration = "TYPE" { TypeDecl SYNC ";" } | "VAR" { VarDecl SYNC ";" } .
        if (sym.kind == typeSym) {
            getSym();
            while (sym.kind == identifierSym) {
                TypeDecl();
                accept(semicolonSym, "; expected");
            }
        } else if (sym.kind == varSym) {
            getSym();
            while (sym.kind == identifierSym) {
                VarDecl();
                accept(semicolonSym, "; expected");
            }
        } else {
            abort("TYPE or VAR expected");
        }
    }

    static void TypeDecl() {
        // TypeDecl = identifier "=" Type .
        accept(identifierSym, "identifier expected");
        accept(equalsSym, "= expected");
        Type();
    }

    static void VarDecl() {
        // VarDecl = IdentList ":" Type .
        IdentList();
        accept(colonSym, ": expected");
        Type();
    }

    static void Type() {
        // Type = SimpleType | ArrayType | RecordType | SetType | PointerType .
        IntSet typeStarts = new IntSet(simpleTypeFirst, arraySym, recordSym, setSym, pointerSym);
        accept(typeStarts, "type expected");

        switch (sym.kind) {
            case identifierSym:
            case leftParenSym:
            case leftBracketSym:
                SimpleType();
                break;
            case arraySym:
                ArrayType();
                break;
            case recordSym:
                RecordType();
                break;
            case setSym:
                SetType();
                break;
            case pointerSym:
                PointerType();
                break;
            default:
                abort("invalid type");
        }
    }

    static void SimpleType() {
        // SimpleType = QualIdent [ Subrange ] | Enumeration | Subrange .
        if (sym.kind == leftParenSym) {
            Enumeration();
        } else if (sym.kind == leftBracketSym) {
            Subrange();
        } else {
            QualIdent();
            if (sym.kind == leftBracketSym) {
                Subrange();
            }
        }
    }

    static void QualIdent() {
        // QualIdent = identifier { "." identifier } .
        accept(identifierSym, "identifier expected");
        while (sym.kind == dotSym) {
            getSym();
            accept(identifierSym, "identifier expected");
        }
    }

    static void Subrange() {
        // Subrange = "[" Constant ".." Constant "]" .
        accept(leftBracketSym, "[ expected");
        Constant();
        accept(equalsSym, ".. expected"); // Note: Using equalsSym for ".." in this grammar
        Constant();
        accept(rightBracketSym, "] expected");
    }

    static void Constant() {
        // Constant = number | identifier .
        if (sym.kind == numberSym) {
            getSym();
        } else if (sym.kind == identifierSym) {
            getSym();
        } else {
            abort("number or identifier expected");
        }
    }

    static void Enumeration() {
        // Enumeration = "(" IdentList ")" .
        accept(leftParenSym, "( expected");
        IdentList();
        accept(rightParenSym, ") expected");
    }

    static void IdentList() {
        // IdentList = identifier { "," identifier } .
        accept(identifierSym, "identifier expected");
        while (sym.kind == commaSym) {
            getSym();
            accept(identifierSym, "identifier expected");
        }
    }

    static void ArrayType() {
        // ArrayType = "ARRAY" SimpleType { "," SimpleType } "OF" Type.
        accept(arraySym, "ARRAY expected");
        SimpleType();
        while (sym.kind == commaSym) {
            getSym();
            SimpleType();
        }
        accept(ofSym, "OF expected");
        Type();
    }

    static void RecordType() {
        // RecordType = "RECORD" FieldLists "END" .
        accept(recordSym, "RECORD expected");
        FieldLists();
        accept(endSym, "END expected");
    }

    static void FieldLists() {
        // FieldLists = FieldList { ";" FieldList } .
        FieldList();
        while (sym.kind == semicolonSym) {
            getSym();
            FieldList();
        }
    }

    static void FieldList() {
        // FieldList = [ IdentList ":" Type ] .
        if (sym.kind == identifierSym) {
            IdentList();
            accept(colonSym, ": expected");
            Type();
        }
    }

    static void SetType() {
        // SetType = "SET" "OF" SimpleType .
        accept(setSym, "SET expected");
        accept(ofSym, "OF expected");
        SimpleType();
    }

    static void PointerType() {
        // PointerType = "POINTER" "TO" Type .
        accept(pointerSym, "POINTER expected");
        accept(toSym, "TO expected");
        Type();
    }

    // +++++++++++++++++++++ Main driver function +++++++++++++++++++++++++++++++

    public static void main(String[] args) {
        // Open input and output files from command line arguments
        if (args.length == 0) {
            System.out.println("Usage: MOD2 FileName");
            System.exit(1);
        }
        input = new InFile(args[0]);
        output = new OutFile(newFileName(args[0], ".out"));

        getChar(); // Lookahead character

        // To test the scanner we can use a loop like the following:

        // do {
        // getSym(); // Lookahead symbol
        // OutFile.StdOut.write(sym.kind, 3);
        // OutFile.StdOut.writeLine(" " + sym.val);
        // } while (sym.kind != EOFSym);

        getSym(); // Lookahead symbol
        Mod2Decl(); // Start to parse from the goal symbol
        // if we get back here everything must have been satisfactory
        System.out.println("Parsed correctly");

        /*
         * After the scanner is debugged, comment out lines 127 to 131 and uncomment
         * lines 135 to 138.
         * In other words, replace the code immediately above with this code:
         * 
         * 
         */
        output.close();
    } // main

} // Mod2decl4

class Token {
    public int kind;
    public String val;

    public Token(int kind, String val) {
        this.kind = kind;
        this.val = val;
    }

} // Token