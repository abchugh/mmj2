//********************************************************************/
//* Copyright (C) 2005-2011                                          */
//* MEL O'CAT  X178G243 (at) yahoo (dot) com                         */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/**
 *  MinProofWorksheet.java  0.01 11/01/2011
 *
 *  Version 0.01:
 *  Nov-01-2011: new.
 */

package mmj.gmff;

import java.io.*;
import java.util.ArrayList;

import mmj.lang.Messages;
import mmj.mmio.Tokenizer;
import mmj.pa.PaConstants;

/**
 *  <code>MinProofWorksheet</code> is a minimalist
 *  <code>ProofWorksheet</code> containing just enough
 *  parsed proof data to generate a Model A type export
 *  file via GMFF.
 *  <p>
 *  Initially, GMFF Model A (export type) is supported
 *  but additional Models could be supported. Some
 *  Models might require the "Full Monte" -- our old friend
 *  <code>ProofWorksheet</code>, which requires full structural
 *  validation edits for loading.
 */
public class MinProofWorksheet {

    /**
     *  <code>Messages</code> object stored here for convenience.
     */
    private final Messages messages;

    /**
     *  <code>ArrayList</code> of <code>MinProofWorkStmt</code>
     *  type objects.
     */
    private final ArrayList minProofWorkStmtList;

    /**
     *  <code>theoremLabel</code> extracted from
     *  <code>MinHeaderStmt</code>, stored here for convenience.
     *  <p>
     *  Default label set in case of <code>structuralErrors</code>
     *  for use in messages.
     */
    private String theoremLabel = PaConstants.DEFAULT_STMT_LABEL;

    /**
     *  <code>locAfter</code> statement label extracted from
     *  <code>MinHeaderStmt</code>, stored here for convenience.
     */
    private String locAfter;

    /**
     *  <code>lineCnt</code> is the number of text lines
     *  in the Proof Worksheet. It is accum'ed during the
     *  load() process and is used in error messages to
     *  determine error location.
     */
    private int lineCnt;

    /**
     *  "fatal" structural errors? Unable to obtain
     *  theorem label from a valid ProofWorkseet Header;
     *  Or if it contains invalid Metamath characters.
     */
    private boolean structuralErrors = true;

    /**
     *  Constructor for MinProofWorksheet.
     *
     *  Builds an empty MinProofWorksheet marked as
     *  invalid so that it can be cached in ProofWorksheetCache
     *  regardless of its validity and successful loading.
     *
     *  @param messages The <code>Messages</code> object.
     *
     */
    public MinProofWorksheet(final Messages messages) {
        this.messages = messages;
        minProofWorkStmtList = new ArrayList();
    }

    /**
     *  Loads the Proof Worksheet text into the MinProofWorksheet.
     *  <br>
     *  Does the following: <ul>
     *  <li> Uses Java's readLine() to break the
     *       proofText into lines and strip off newline characters.
     *  <li> Each line is "tokenized" into strings containing
     *       either Metamath whitespace or a single Metamath
     *       token Strings, and the collection of Strings for
     *       each line is stored in an ArrayList
     *  <li> A non-blank character in a line's first column indicates
     *       a new ProofWorksheet statement and triggers loading
     *       of the previous MinProofWorkStmt. (Blank in column
     *       one indicates a continuation of the previous
     *       statement.)
     *  <li> The start of the first line of a ProofWorksheet
     *       statement is used to determine the type of statement
     *       to be loaded, and the appropriate constructor is
     *       invoked via MinProofWorkStmt.constructStmt().
     *  <li> A severe error anywhere causes structuralErrors
     *       to be set to true, and corresponding error message(s)
     *       stored in messages.
     *
     *  @param proofText String data holding Proof Worksheet text.
     */
    public void load(final String proofText) {
        setStructuralErrors(false);

        final LineNumberReader lineReader = new LineNumberReader(
            new StringReader(proofText));

        final ArrayList<ArrayList<String>> lineList = new ArrayList<ArrayList<String>>();
        ArrayList<String> tokenList;
        String line;

        MinProofWorkStmt minProofWorkStmt = null;

        try {
            line = lineReader.readLine();
            if (line == null)
                triggerEmptyProofError();
            if (line.length() == 0 || line.charAt(0) == ' ')
                triggerBogusLine1Error();
            do {
                do {
                    tokenList = tokenize(line);
                    if (structuralErrors)
                        return;
                    lineList.add(tokenList);
                    ++lineCnt;
                } while ((line = lineReader.readLine()) != null
                    && (line.length() == 0 || line.charAt(0) == ' '));

                minProofWorkStmt = MinProofWorkStmt.constructStmt(this,
                    lineList);

                if (minProofWorkStmt == null)
                    setStructuralErrors(true);
                else {
                    minProofWorkStmtList.add(minProofWorkStmt);
                    lineList.clear();
                }
            } while (!structuralErrors && line != null);
        } catch (final IOException e) {
            setStructuralErrors(true);
            messages
                .accumErrorMessage(GMFFConstants.ERRMSG_LOAD_READLINE_IO_ERROR_1
                    + (lineCnt + 1)
                    + GMFFConstants.ERRMSG_LOAD_READLINE_IO_ERROR_2
                    + e.toString());
        }

    }

    /**
     *  Returns the list of Proof Worksheet statements.
     *
     *  @return minProofWorkStmtList.
     */
    public ArrayList getMinProofWorkStmtList() {
        return minProofWorkStmtList;
    }

    /**
     *  Returns the Proof Worksheet <code>structuralErrors</code>
     *  indicator.
     *
     *  @return structuralErrors indicator.
     */
    public boolean getStructuralErrors() {
        return structuralErrors;
    }

    /**
     *  Sets the Proof Worksheet <code>structuralErrors</code>
     *  indicator.
     *
     *  @param structuralErrors boolean indicator.
     */
    public void setStructuralErrors(final boolean structuralErrors) {
        this.structuralErrors = structuralErrors;
    }

    /**
     *  Returns the Proof Worksheet <code>theoremLabel</code>
     *  field.
     *
     *  @return theoremLabel field.
     */
    public String getTheoremLabel() {
        return theoremLabel;
    }

    /**
     *  Sets the Proof Worksheet <code>theoremLabel</code>
     *  field.
     *
     *  @param s Theorem Label field.
     */
    public void setTheoremLabel(final String s) {
        theoremLabel = s;
    }

    /**
     *  Returns the Proof Worksheet <code>locAfter</code>
     *  field.
     *
     *  @return locAfter field.
     */
    public String getLocAfter() {
        return locAfter;
    }

    /**
     *  Sets the Proof Worksheet <code>locAfter</code>
     *  field.
     *
     *  @param s locAfter field.
     */
    public void setLocAfter(final String s) {
        locAfter = s;
    }

    /**
     *  Returns the Proof Worksheet <code>lineCnt</code>
     *  field.
     *
     *  @return lineCnt field.
     */
    public int getLineCnt() {
        return lineCnt;
    }

    /**
     *  <code>tokenize</code> breaks up a line of text from a Proof
     *  Worksheet (from which cr/lf has already been removed), storing
     *  chunks of Metamath whitespace and Metamath tokens in
     *  <code>String</code>s which are accumulated into an output
     *  <code>ArrayList</code>.
     *  <p>
     *  An empty line is returned with a single empty String in
     *  the ArrayList.
     *  <p>
     *  If no whitespace precedes the first Metamath token
     *  on the line then the token is the first String output
     *  in the ArrayList.
     *  <p>
     *  If there is no whitespace following the last token on
     *  the line, then there will not be an empty
     *  String at the end of the ArrayList.
     *  <p>
     *  Likewise, between two Metamath tokens there will be a
     *  whitespace String of non-zero length.
     *  <p>
     *  @param line String containing one physical line of a
     *              Proof Worksheet.
     *  @return an <code>ArrayList</code> of <code>String</code>
     *              chunks of either Metamath whitespace or
     *              Metamath tokens, or just one empty <code>
     *              String</code> if the line is empty.
     */
    /* friendly */ArrayList<String> tokenize(final String line) {

        final ArrayList<String> tokenList = new ArrayList<String>();

        StringBuffer sb = new StringBuffer();

        int len;

        try {
            final Tokenizer tokenizer = new Tokenizer(new StringReader(line),
                "");

            len = tokenizer.getWhiteSpace(sb, 0);
            if (len < 0)
                tokenList.add("");
            else
                while (len >= 0) {
                    if (len > 0)
                        tokenList.add(sb.toString());
                    sb = new StringBuffer();
                    if ((len = tokenizer.getToken(sb, 0)) >= 0) {
                        tokenList.add(sb.toString());
                        sb = new StringBuffer();
                        len = tokenizer.getWhiteSpace(sb, 0);
                    }
                }
        } catch (final IOException e) {
            triggerTokenizerIO_Error(e.toString(), lineCnt);
        }
        return tokenList;
    }

    /* friendly */void triggerTokenizerIO_Error(final String e,
        final int lineNbr)
    {
        setStructuralErrors(true);
        messages.accumErrorMessage(GMFFConstants.ERRMSG_TOKENIZER_IO_ERROR_1
            + getTheoremLabel() + GMFFConstants.ERRMSG_TOKENIZER_IO_ERROR_1B
            + lineNbr + GMFFConstants.ERRMSG_TOKENIZER_IO_ERROR_2 + e);
    }

    /* friendly */void triggerEmptyProofError() {
        setStructuralErrors(true);
        messages.accumErrorMessage(GMFFConstants.ERRMSG_EMPTY_PROOF_1
            + getTheoremLabel() + GMFFConstants.ERRMSG_EMPTY_PROOF_1B);
    }

    /* friendly */void triggerBogusLine1Error() {
        setStructuralErrors(true);
        messages.accumErrorMessage(GMFFConstants.ERRMSG_BOGUS_LINE_1_1
            + getTheoremLabel() + GMFFConstants.ERRMSG_BOGUS_LINE_1_1B);
    }

    /* friendly */void triggerBogusStmtLineStart(final String chunk,
        final int lineNbr)
    {
        setStructuralErrors(true);
        messages
            .accumErrorMessage(GMFFConstants.ERRMSG_WORK_STMT_LINE_START_ERROR_1
                + getTheoremLabel()
                + GMFFConstants.ERRMSG_WORK_STMT_LINE_START_ERROR_1B
                + lineNbr
                + GMFFConstants.ERRMSG_WORK_STMT_LINE_START_ERROR_2
                + chunk
                + GMFFConstants.ERRMSG_WORK_STMT_LINE_START_ERROR_3);
    }

    /* friendly */void triggerConstructorError(final String e,
        final int lineNbr)
    {
        setStructuralErrors(true);
        messages
            .accumErrorMessage(GMFFConstants.ERRMSG_WORK_STMT_CONSTRUCTOR_ERROR_1
                + getTheoremLabel()
                + GMFFConstants.ERRMSG_WORK_STMT_CONSTRUCTOR_ERROR_1B
                + lineNbr
                + GMFFConstants.ERRMSG_WORK_STMT_CONSTRUCTOR_ERROR_2 + e);
    }

    /* friendly */void triggerInvalidTheoremLabel(final String theoremLabel,
        final int lineNbr)
    {
        setStructuralErrors(true);
        messages
            .accumErrorMessage(GMFFConstants.ERRMSG_INVALID_THEOREM_LABEL_ERROR_1
                + theoremLabel
                + GMFFConstants.ERRMSG_INVALID_THEOREM_LABEL_ERROR_2 + lineNbr);
    }

    /* friendly */void triggerInvalidHeaderConstants(final int lineNbr) {
        setStructuralErrors(true);
        messages
            .accumErrorMessage(GMFFConstants.ERRMSG_INVALID_HEADER_CONSTANTS_ERROR_1
                + getTheoremLabel()
                + GMFFConstants.ERRMSG_INVALID_HEADER_CONSTANTS_ERROR_1B
                + lineNbr);
    }
}
