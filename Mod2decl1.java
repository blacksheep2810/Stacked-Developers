// by R. Malope, T. Coutts, K. Lesese, H. Chagaluka, 2025

// This is a program develops a parser for Modula-2 declarations
// Modification of an original program written by P.D. Terry, Rhodes University, Modified by KL Bradshaw 2022

import java.util.*;
import library.*;

class Token {
  public int kind;
  public String val;

  public Token(int kind, String val) {
    this.kind = kind;
    this.val = val;
  }

} // Token

class Mod2decl1 {

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
  // and others like this

  // +++++++++++++++++++++++++++++ Character Handler ++++++++++++++++++++++++++

  static final char EOF = '\0';
  static boolean atEndOfFile = false;

  // Declaring ch as a global variable is done for expediency - global variables
  // are not always a good thing

  static char ch; // look ahead character for scanner

  static void getChar() {
    // Obtains next character ch from input, or CHR(0) if EOF reached
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
    // Scans for next sym from input
    while (ch > EOF && ch <= ' ')
      getChar();
    StringBuilder symLex = new StringBuilder();
    int symKind = noSym;

    if (ch == EOF) {
      symKind = EOFSym;
    }
    // Handle identifiers and keywords
    else if (Character.isLetter(ch)) {
      while (Character.isLetterOrDigit(ch)) {
        symLex.append(ch);
        getChar();
      }
      String ident = symLex.toString();
      // Check for keywords
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
    // Handle numbers
    else if (Character.isDigit(ch)) {
      while (Character.isDigit(ch)) {
        symLex.append(ch);
        getChar();
      }
      symKind = numberSym;
    }

    // Handle comments
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
        // Recursively call getSym to get next token after comment
        getSym();
        return;
      } else {
        // It's just a '(' symbol
        symLex.append('(');
        symKind = leftParenSym;
        getChar();
      }
    }
    // Handle other symbols
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

  /*
   * ++++ Commented out for the moment
   * 
   * // +++++++++++++++++++++++++++++++ Parser +++++++++++++++++++++++++++++++++++
   * 
   * static void accept(int wantedSym, String errorMessage) {
   * // Checks that lookahead token is wantedSym
   * if (sym.kind == wantedSym) getSym(); else abort(errorMessage);
   * } // accept
   * 
   * 
   * static void accept(IntSet allowedSet, String errorMessage) {
   * // Checks that lookahead token is in allowedSet
   * if (allowedSet.contains(sym.kind)) getSym(); else abort(errorMessage);
   * } // accept
   * ++++++
   */

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

    do {
      getSym(); // Lookahead symbol
      OutFile.StdOut.write(sym.kind, 3);
      OutFile.StdOut.writeLine(" " + sym.val);
    } while (sym.kind != EOFSym);

    /*
     * After the scanner is debugged, comment out lines 127 to 131 and uncomment
     * lines 135 to 138.
     * In other words, replace the code immediately above with this code:
     * 
     * getSym(); // Lookahead symbol
     * Mod2Decl(); // Start to parse from the goal symbol
     * // if we get back here everything must have been satisfactory
     * System.out.println("Parsed correctly");
     */
    output.close();
  } // main

} // Mod2decl
