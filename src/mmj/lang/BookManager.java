//********************************************************************/
//* Copyright (C) 2008                                               */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 *  BookManager.java  0.01 08/01/2008
 *
 *  Aug-1-2008:
 *      --> new!
 *
 */

package mmj.lang;

import java.util.*;

import mmj.mmio.MMIOConstants;
import mmj.tl.*;

/**
 *  BookManager is a "helper" class that is used to
 *  keep track of Chapter and Section definitions for
 *  an input Metamath database and its objects (called
 *  "MObj"s herein.)
 *  <p>
 *  BookManager keeps track of input Chapters and Sections
 *  from a .mm database as the data is input, while LogicalSystem
 *  controls assigning Section and MObjNbr data items
 *  to BookManager's Chapters and Sections. This is an
 *  important point: BookManager is called during the FileLoad
 *  process as each MObj is created so that the BookManager
 *  can assign SectionMObjNbrs in the correct order.
 *  <p.
 *  By "Chapter" we refer to a portion of a Metamath .mm
 *  file beginning with a Metamath Comment statement ("$(")
 *  whose first token (after "$(") begins with "#*#*".
 *  The "title" is extracted from the Metamath comment
 *  by stringing together the non-whitespace,
 *  non-"#*#*"-prefixed tokens. (Norm refers to these
 *  Chapters as "Sections" and our Sections and "Sub-Sections").
 *  <p>
 *  A "Section" is similar to Chapter except that it
 *  is contained within a Chapter and the identifying
 *  token prefix string is "=-=-".
 *  <p>
 *  In some cases there may not be a Section header within
 *  a Chapter, and in that case the Chapter title is used for
 *  the Section title and a default-Section must be
 *  automatically generated by the system.
 *  <p>
 *  Likewise, if a valid Chapter comment statement
 *  has not been found before Metamath objects or a Section
 *  comment statement are input, then a default Chapter
 *  with a default title is automatically generated by the
 *  system.
 *
 *  It is possible that neither Chapters or
 *  Sections are used by an input .mm database. In this case
 *  everything is loaded into a single default Chapter
 *  with four default Sections.
 *  <p>
 *  <code>
 *  Note: Input Sections are physically split and assigned
 *        sequential numbers based on the MObj content type
 *        as shown below. The Section numbers are multiples of
 *        1, 2, 3, or 4 as follows:<br>
 *        <br>
 *        1 = Cnst or Var symbols<br>
 *        2 = VarHyp<br>
 *        3 = Syntax Axioms<br>
 *        4 = Theorems, Logic Axioms and LogHyps.<br>
 *        <br>
 *        Thus, the first input Section is assigned
 *        Section numbers 1, 2, 3, and 4. The 2nd input
 *        Section is assigned Section numbers 5, 6, 7
 *        and 8. And so on. These numbers are assigned
 *        across Chapter boundaries -- meaning that
 *        Section numbers do not reset to 1 at the
 *        beginning of each chapter.
 *
 */
public class BookManager implements TheoremLoaderCommitListener {

    private final boolean enabled;
    private final String provableLogicStmtTypeParm;

    private ArrayList chapterList;
    private ArrayList sectionList;

    private Chapter currChapter;

    private Section currSymSection;
    private Section currVarHypSection;
    private Section currSyntaxSection;
    private Section currLogicSection;

    private int inputSectionCounter;

    private int totalNbrMObjs;

    private String nextChapterTitle = MMIOConstants.DEFAULT_TITLE;
    private String nextSectionTitle = MMIOConstants.DEFAULT_TITLE;

    /**
     *  Sole constructor for BookManager.
     *  <p>
     *  @param enabled Book Manager enabled? If not enabled
     *         then zero Chapter and Section numbers are
     *         assigned and no data is retained.
     *  @param provableLogicStmtTypeParm String identifying
     *         theorems, logic axioms and logical hypotheses
     *         (normally = "|-" in Metamath, matched against
     *         the first symbol of the object's formula).
     */
    public BookManager(final boolean enabled,
        final String provableLogicStmtTypeParm)
    {

        this.enabled = enabled;

        this.provableLogicStmtTypeParm = provableLogicStmtTypeParm;

        if (enabled) {
            chapterList = new ArrayList(
                LangConstants.ALLOC_NBR_BOOK_CHAPTERS_INITIAL);
            sectionList = new ArrayList(
                LangConstants.ALLOC_NBR_BOOK_SECTIONS_INITIAL);
        }
        else {
            chapterList = new ArrayList(1);
            sectionList = new ArrayList(1);
        }
    }

    /**
     *   Stores new MObj's from the TheoremLoader as part
     *   of a load operation.
     *   <p>
     *   BookManager is called to perform its updates en masse
     *   at the end of the TheoremLoader update. A failure
     *   of BookManager to complete the updates is deemed
     *   irreversible and severe, warranting a message to the
     *   user to manually restart mmj2.
     *   <p>
     *   @param mmtTheoremSet the set of MMTTheoremFile object
     *          added or updated by TheoremLoader.
     */
    @Override
    public void commit(final MMTTheoremSet mmtTheoremSet) {
        if (!enabled)
            return;

        final List addList = mmtTheoremSet
            .buildSortedListOfAdds(TheoremStmtGroup.SEQ);

        if (addList.size() == 0)
            return;

        TheoremStmtGroup t;
        int insertSectionNbr;

        final Iterator iterator = addList.iterator();
        while (iterator.hasNext()) {
            t = (TheoremStmtGroup)iterator.next();

            /* ok, if object inserted then section is the
               section of the thing it was inserted after
               (converted for stmt type appropriate section nbr).
               otherwise, if appended add to final section.
             */
            insertSectionNbr = t.getInsertSectionNbr();
            if (insertSectionNbr > 0)
                commitInsertTheoremStmtGroup(t, insertSectionNbr);
            else
                commitAppendTheoremStmtGroup(t);
        }
    }

    /**
     *  Returns BookManager enabled flag, which indicates
     *  whether or not the BookManager is in use within
     *  the currently system.
     *  <p>
     *  @return BookManager enabled flag.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     *  Returns the Chapter corresponding to a given
     *  Chapter Nbr.
     *  <p>
     *  @param chapterNbr Chapter number.
     *  @return Chapter or null if no such chapter exists.
     */
    public Chapter getChapter(final int chapterNbr) {
        if (!enabled)
            return null;
        Chapter chapter = null;
        try {
            chapter = (Chapter)chapterList.get(chapterNbr - 1);
        } catch (final IndexOutOfBoundsException e) {}
        return chapter;
    }

    /**
     *  Returns the Chapter corresponding to a given
     *  Section Nbr.
     *  <p>
     *  @param sectionNbr Section number.
     *  @return Chapter or null if no such section exists.
     */
    public Chapter getChapterForSectionNbr(final int sectionNbr) {
        if (!enabled)
            return null;
        Chapter chapter = null;
        final Section section = getSection(sectionNbr);
        if (section != null)
            chapter = section.getSectionChapter();
        return chapter;
    }

    /**
     *  Returns the Section corresponding to a given
     *  Section Nbr.
     *  <p>
     *  Note: no processing occurs if BookManager is not enabled.
     *  <p>
     *  @param sectionNbr Section number.
     *  @return Chapter or null if no such section exists or
     *          BookManager is not enabled.
     */
    public Section getSection(final int sectionNbr) {
        if (!enabled)
            return null;
        Section section = null;
        try {
            section = (Section)sectionList.get(sectionNbr - 1);
        } catch (final IndexOutOfBoundsException e) {}
        return section;
    }

    /**
     *  Adds a new Chapter to the BookManager's collection.
     *  <p>
     *  Note: no processing occurs if BookManager is not enabled.
     *  <p>
     *  @param chapterTitle Chapter Title or descriptive String.
     */
    public void addNewChapter(final String chapterTitle) {
        if (!enabled)
            return;
        nextChapterTitle = chapterTitle;
        nextSectionTitle = null;
    }

    /**
     *  Adds a new Section to the current Chapter in the
     *  BookManager's collection
     *  <p>
     *  If no prior Chapter has been input a default Chapter
     *  is automatically created.
     *  <p>
     *  Note that one call to addNewSection() actually
     *  creates the 4 Sections (Symbols, VarHyps, Syntax
     *  and Logic) that correspond to one input .mm database
     *  section.
     *  <p>
     *  Note: no processing occurs if BookManager is not enabled.
     *  <p>
     *  @param sectionTitle or descriptive String.
     */
    public void addNewSection(final String sectionTitle) {
        if (!enabled)
            return;
        nextSectionTitle = sectionTitle;
    }

    /**
     *  Assigns Chapter and SectionNbrs to an axiom.
     *  <p>
     *  If the Type Code of the Axiom is equal to
     *  the Provable Logic Statement Type code parameter
     *  then the input Axiom is assigned to the current
     *  "Logic" Section. Otherwise it is considered to be
     *  "Syntax".
     *  <p>
     *  This function is provided for use by the LogicalSystem
     *  during initial load of an input .mm database. If
     *  the MObj has already been assigned SectionMObjNbr
     *  then no update is performed (this is significant
     *  normally only for re-declared Vars because only
     *  Metamath Vars can be validly re-declared -- this
     *  happens with in-scope local Var declarations.)
     *  no update
     *  <p>
     *  Note: no processing occurs if BookManager is not enabled.
     *  <p>
     *  @param axiom newly created Axiom.
     */
    public void assignChapterSectionNbrs(final Axiom axiom) {
        if (enabled) {

            prepareChapterSectionForMObj();

            if (axiom.getTyp().getId().equals(provableLogicStmtTypeParm)) {

                if (currLogicSection.assignChapterSectionNbrs(axiom))
                    ++totalNbrMObjs;
            }
            else if (currSyntaxSection.assignChapterSectionNbrs(axiom))
                ++totalNbrMObjs;
        }
    }

    /**
     *  Assigns Chapter and SectionNbrs to a theorem.
     *  <p>
     *  Note: "syntax theorems" are assigned to the
     *  current "Logic" section, not "Syntax".
     *  <p>
     *  This function is provided for use by the LogicalSystem
     *  during initial load of an input .mm database. If
     *  the MObj has already been assigned SectionMObjNbr
     *  then no update is performed (this is significant
     *  normally only for re-declared Vars because only
     *  Metamath Vars can be validly re-declared -- this
     *  happens with in-scope local Var declarations.)
     *  no update
     *  <p>
     *  Note: no processing occurs if BookManager is not enabled.
     *  <p>
     *  @param theorem newly created Theorem.
     */
    public void assignChapterSectionNbrs(final Theorem theorem) {
        if (enabled) {

            prepareChapterSectionForMObj();

            if (currLogicSection.assignChapterSectionNbrs(theorem))
                ++totalNbrMObjs;
        }
    }

    /**
     *  Assigns Chapter and SectionNbrs to a logical
     *  hypothesis.
     *  <p>
     *  This function is provided for use by the LogicalSystem
     *  during initial load of an input .mm database. If
     *  the MObj has already been assigned SectionMObjNbr
     *  then no update is performed (this is significant
     *  normally only for re-declared Vars because only
     *  Metamath Vars can be validly re-declared -- this
     *  happens with in-scope local Var declarations.)
     *  no update
     *  <p>
     *  Note: no processing occurs if BookManager is not enabled.
     *  <p>
     *  @param logHyp newly created LogHyp.
     */
    public void assignChapterSectionNbrs(final LogHyp logHyp) {
        if (enabled) {

            prepareChapterSectionForMObj();

            if (currLogicSection.assignChapterSectionNbrs(logHyp))
                ++totalNbrMObjs;
        }
    }

    /**
     *  Assigns Chapter and SectionNbrs to a VarHyp.
     *  <p>
     *  This function is provided for use by the LogicalSystem
     *  during initial load of an input .mm database. If
     *  the MObj has already been assigned SectionMObjNbr
     *  then no update is performed (this is significant
     *  normally only for re-declared Vars because only
     *  Metamath Vars can be validly re-declared -- this
     *  happens with in-scope local Var declarations.)
     *  no update
     *  <p>
     *  Note: no processing occurs if BookManager is not enabled.
     *  <p>
     *  @param varHyp newly created VarHyp
     */
    public void assignChapterSectionNbrs(final VarHyp varHyp) {
        if (enabled) {

            prepareChapterSectionForMObj();

            if (currVarHypSection.assignChapterSectionNbrs(varHyp))
                ++totalNbrMObjs;
        }
    }

    /**
     *  Assigns Chapter and SectionNbrs to either a Cnst
     *  or a Var.
     *  <p>
     *  This function is provided for use by the LogicalSystem
     *  during initial load of an input .mm database. If
     *  the MObj has already been assigned SectionMObjNbr
     *  then no update is performed (this is significant
     *  normally only for re-declared Vars because only
     *  Metamath Vars can be validly re-declared -- this
     *  happens with in-scope local Var declarations.)
     *  no update
     *  <p>
     *  Note: no processing occurs if BookManager is not enabled.
     *  <p>
     *  @param sym newly created Sym.
     */
    public void assignChapterSectionNbrs(final Sym sym) {
        if (enabled) {

            prepareChapterSectionForMObj();

            if (currSymSection.assignChapterSectionNbrs(sym))
                ++totalNbrMObjs;
        }
    }

    /**
     *  Returns the count of all MObj objects assigned to
     *  Sections within the BookManager.
     *  <p>
     *  @return total number of MObjs added so far.
     */
    public int getTotalNbrMObjs() {
        return totalNbrMObjs;
    }

    /**
     *  Returns the List of Chapters in the BookManager.
     *  <p>
     *  Note: if BookManager is not enabled, the List
     *  returned will not be null, it will be empty.
     *  <p>
     *  @return ArrayList of Chapters.
     */
    public ArrayList getChapterList() {
        return chapterList;
    }

    /**
     *  Returns the List of Sections in the BookManager.
     *  <p>
     *  Note: if BookManager is not enabled, the List
     *  returned will not be null, it will be empty.
     *  <p>
     *  @return ArrayList of Sections.
     */
    public ArrayList getSectionList() {
        return sectionList;
    }

    /**
     *  Returns an Iterator over all of the MObjs assigned
     *  to Sections.
     *  <p>
     *  Note: if BookManager is not enabled, the Iterator
     *  returned will not be null, it will be empty.
     *  <p>
     *  @param logicalSystem the mmj2 LogicalSystem object.
     *  @return ArrayList of Sections.
     */
    public Iterator getSectionMObjIterator(final LogicalSystem logicalSystem) {
        final MObj[][] sectionArray = getSectionMObjArray(logicalSystem);

        return new SectionMObjIterator(sectionArray);
    }

    /**
     *  Returns a two-dimensional array of MObjs by Section
     *  and MObjNbr within Section.
     *  <p>
     *  Note: if BookManager is not enabled, the array is
     *  empty and is allocated as <code>new MObj[0][]</code>.
     *  <p>
     *  @param logicalSystem the mmj2 LogicalSystem object.
     *  @return two-dimensional array of MObjs by Section
     *          and MObjNbr within Section.
     */
    public MObj[][] getSectionMObjArray(final LogicalSystem logicalSystem) {

        MObj[][] sectionArray;

        if (!enabled) {
            sectionArray = new MObj[0][];
            return sectionArray;
        }

        sectionArray = new MObj[sectionList.size()][];

        Iterator iterator = sectionList.iterator();
        int i = 0;
        while (iterator.hasNext())
            sectionArray[i++] = new MObj[((Section)iterator.next())
                .getLastMObjNbr()];

        iterator = logicalSystem.getSymTbl().values().iterator();
        loadSectionArrayEntry(sectionArray, iterator);

        iterator = logicalSystem.getStmtTbl().values().iterator();
        loadSectionArrayEntry(sectionArray, iterator);

        return sectionArray;
    }

    /**
     *  Nested class which implements Iterator for a two-dimensional
     *  array of MObjs by Section and MObjNbr within Section.
     */
    public class SectionMObjIterator implements Iterator {
        private int prevI;
        private int prevJ;
        private int nextI;
        private int nextJ;
        private final MObj[][] mArray;

        /**
         *  Sole Constructor.
         *  <p>
         *  Note: the input array must not contain any null
         *        (empty) array entries. It is assumed to
         *        be completely full (though the idea of
         *        arrays with padding was considered, it was
         *        rejected ... for now.)
         *  <p>
         *  @param s two-dimensional array of MObjs by Section
         *         and MObjNbr within Section.
         */
        public SectionMObjIterator(final MObj[][] s) {
            mArray = s;
            prevI = 0;
            prevJ = -1;
        }

        /**
         *  Returns the next MObj within the two-dimensional array.
         *  <p>
         *  @return the next MObj within the two-dimensional array.
         *  @throws NoSuchElementException if there are no more
         *          MObjs to return.
         */
        @Override
        public Object next() {
            if (!hasNext())
                throw new NoSuchElementException();
            prevI = nextI;
            prevJ = nextJ;
            return mArray[prevI][prevJ];
        }

        /**
         *  Returns true if there is another MObj to return
         *  within the two-dimensional array.
         *  <p>
         *  @return true if there is a next() MObj within
         *          the two-dimensional array.
         */
        @Override
        public boolean hasNext() {
            nextI = prevI;
            nextJ = prevJ + 1;
            while (nextI < mArray.length) {
                if (nextJ < mArray[nextI].length)
                    return true;
                ++nextI;
                nextJ = 0;
            }
            return false;
        }

        /**
         *  Not supported.
         *  <p>
         *  @throws UnsupportedOperationException if called.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private void prepareChapterSectionForMObj() {
        if (nextChapterTitle != null) {

            if (nextSectionTitle == null)
                nextSectionTitle = nextChapterTitle;

            constructNewChapter();
            nextChapterTitle = null;
            nextSectionTitle = null;
        }
        else if (nextSectionTitle != null) {

            constructNewSection();
            nextSectionTitle = null;
        }
    }

    private void constructNewChapter() {

        currChapter = new Chapter(nextChapterNbr(), nextChapterTitle);
        chapterList.add(currChapter);

        constructNewSection();
    }

    private void constructNewSection() {

        final int n = inputSectionCounter++
            * LangConstants.SECTION_NBR_CATEGORIES;

        currSymSection = new Section(currChapter, n
            + LangConstants.SECTION_SYM_CD, nextSectionTitle);
        sectionList.add(currSymSection);

        currVarHypSection = new Section(currChapter, n
            + LangConstants.SECTION_VAR_HYP_CD, nextSectionTitle);
        sectionList.add(currVarHypSection);

        currSyntaxSection = new Section(currChapter, n
            + LangConstants.SECTION_SYNTAX_CD, nextSectionTitle);
        sectionList.add(currSyntaxSection);

        currLogicSection = new Section(currChapter, n
            + LangConstants.SECTION_LOGIC_CD, nextSectionTitle);
        sectionList.add(currLogicSection);
    }

    private int nextChapterNbr() {
        int chapterNbr;
        if (currChapter == null)
            chapterNbr = 1;
        else
            chapterNbr = currChapter.getChapterNbr() + 1;
        return chapterNbr;
    }

    private void loadSectionArrayEntry(final MObj[][] sectionArray,
        final Iterator iterator)
    {
        MObj mObj;
        while (iterator.hasNext()) {
            mObj = (MObj)iterator.next();
            sectionArray[mObj.sectionNbr - 1][mObj.sectionMObjNbr - 1] = mObj;
        }
    }

    private void commitAppendTheoremStmtGroup(final TheoremStmtGroup t) {

        final LogHyp[] logHypArray = t.getLogHypArray();
        for (final LogHyp element : logHypArray)
            assignChapterSectionNbrs(element);
        assignChapterSectionNbrs(t.getTheorem());
    }

    private void commitInsertTheoremStmtGroup(final TheoremStmtGroup t,
        final int insertSectionNbr)
    {

        // 'x' is the actual section number to be used for
        // insertions. Note that only 'logic' theorems can
        // be added using Theorem Loader! The input section
        // number could be any category, so we reverse out
        // the input category code and add back in "logic".
        final int x = insertSectionNbr - Section.getSectionCategoryCd( // old
                                                                       // category
                                                                       // cd
            insertSectionNbr) + LangConstants.SECTION_LOGIC_CD; // new category
                                                                // cd

        final Section insertSection = getSection(x);
        if (insertSection == null)
            throw new IllegalArgumentException(
                LangConstants.ERRMSG_BM_UPDATE_W_MMT_SECTION_NOTFND_1 + x
                    + LangConstants.ERRMSG_BM_UPDATE_W_MMT_SECTION_NOTFND_2
                    + t.getTheoremLabel()
                    + LangConstants.ERRMSG_BM_UPDATE_W_MMT_SECTION_NOTFND_3);

        final LogHyp[] logHypArray = t.getLogHypArray();
        for (final LogHyp element : logHypArray)
            if (insertSection.assignChapterSectionNbrs(element))
                ++totalNbrMObjs;
        if (insertSection.assignChapterSectionNbrs(t.getTheorem()))
            ++totalNbrMObjs;
    }
}
