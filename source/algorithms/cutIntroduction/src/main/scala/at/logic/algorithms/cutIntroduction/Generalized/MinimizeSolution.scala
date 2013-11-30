/*
 * MinimizeSolution.scala
 *
 * Code related to the improvement of the solution for the cut-introduction problem.
 * Takes an extended Herbrand sequent with an associated solution and returns another one with an improved solution.
 */

package at.logic.algorithms.cutIntroduction.Generalized

import at.logic.language.fol._
import at.logic.calculi.resolution.base.FClause
import at.logic.language.fol.Utils._
import at.logic.provers.minisat.MiniSAT
import at.logic.provers.Prover
import at.logic.utils.dssupport.ListSupport.mapAccumL
import at.logic.algorithms.resolution._
import at.logic.utils.executionModels.searchAlgorithms.SearchAlgorithms.DFS
import at.logic.utils.executionModels.searchAlgorithms.SearchAlgorithms.setSearch
import at.logic.utils.executionModels.searchAlgorithms.SetNode
import at.logic.algorithms.lk.solvePropositional._
import at.logic.calculi.lk.base._
import at.logic.algorithms.cutIntroduction.{Grammar => BaseGrammar, ExtendedHerbrandSequent => BaseExtendedHerbrandSequent,
                                            CutIntroduction => BaseCutIntroduction, DeltaTable => BaseDeltaTable,
                                            DefaultProver, CutIntroUncompressibleException, CutIntroEHSUnprovableException,
                                            CutIntroException, TermsExtraction, FlatTermSet, DeltaTableException}

object MinimizeSolution {

  def apply(ehs: ExtendedHerbrandSequent, prover: Prover) = {
    val minSol = improveSolution(ehs, prover).sortWith((r1,r2) => r1.numOfAtoms < r2.numOfAtoms).head
    new ExtendedHerbrandSequent(ehs.endSequent, ehs.grammar, minSol)
  }

  //---------------------------------------------------------------------------
  // New variant of improveSolution
  //---------------------------------------------------------------------------

  //Helper functions.

  /** Returns the Cartesian product of two sets.
    * e.g. choose2([1,2],[3,4]) = [(1,2),(1,3),(1,4),(1,5)]
    */
  def cartesianProduct[A,B](xs:List[A], ys:List[B]) = {
    xs.flatMap((x) => ys.map((y) => (x,y)))
  }

  /** Give each atom in a formula an index. Multiple occurrences of the same atom get different indices.
    * @param formula A list of clauses.
    * @return Formula, but with each atom turned into a tuple. The 2nd component is the atom's index.
    */
  def numberAtoms(formula:List[MyFClause[FOLFormula]]) =
    mapAccumL((c:Int,cl:MyFClause[FOLFormula]) => (c + cl.neg.length + cl.pos.length,
                                                   new MyFClause(cl.neg zip (Stream from c), cl.pos zip (Stream from (c + cl.neg.length)))),
              0,formula)._2

  /** Tries to minimize the canonical solution by removing as many atoms as
    * as possible through forgetful resolution.
    *
    * The original variant did a DFS, with the successor-nodes of a formula being
    * all possible resolutions of a single pair of atoms. If we identify every
    * pair of atoms (say, the pairs a,b,c in a formula F with n atoms), then this creates
    * on the order of O((n²)!) redudant paths in the search tree, since
    * the application of the resolution to pairs [a,b] is identical to applying
    * it to pairs [b,a].
    *
    * This variant of improveSolution uses the following strategy:
    * <pre>
    * 1) assign a number to every atom in F.
    * 2) gather the positive and negative occurrences of every variable v into sets v+ and v-.
    * 3) for every variable v, generate every (v1 in v+, v2 in v-) and number all of the resultant pairs.
    *    Let this set of pairs be called PAIRS.
    * 4) let each node of the DFS be (R,V,F'), where R is the set of resolved pairs, V is the set of resolved atoms, and F' the resulting formula.
    * 4.1) let the root be ({},{},F).
    * 4.2) let the successor function be succ((R,V,F)) = {(R U r,V,F'') | r in (PAIRS - R),
    *                                                                     r intersect V =  {},
    *                                                                     r > max{R}, F'' = r applied to F',
    *                                                                     F'' is still valid}
    *      (if a node has no valid successors, it is considered an end node and added to the list of solutions.)
    * </pre>
    *
    * Due to the ordering of the pairs, no node will have descendants in which lower elements entered its R and each set of resolvents will
    * only be generated once.
    *
    * @param form The canonical solution to be improved (doesn't have to be in CNF).
    * @return The list of minimal-size solutions (=the set of end nodes as described in 4.2).
    */
   private def improveSolution(ehs: ExtendedHerbrandSequent, prover: Prover) : List[FOLFormula] = {
      val (xs, form2) = removeQuantifiers(ehs.cutFormula)

      if (xs.length == 0) { throw new CutIntroException("ERROR: Canonical solution is not quantified.") }

      //0. Convert to a clause set where each clause is a list of positive and negative atoms.
      //1. assign a number to every atom in F.
      val fNumbered = numberAtoms(CNFp(form2.toCNF).map(c => toMyFClause(c)).toList)

      //2. gather the positive and negative occurrences o every variable v into sets v+ and v-.
      val posNegSets = fNumbered.foldLeft(Map[FOLFormula, (Set[Int], Set[Int])]()) {(m, clause) =>
        val neg = clause.neg
        val pos = clause.pos

        //Add the negative atoms of the clause to the negative set.
        val m2 = neg.foldLeft(m) {(m, pair) => {
            val (k,v) = pair
            val (neg, pos) = m.get(k) match {
                case None => (Set[Int](),Set[Int]())
                case Some (p) => p
              }
            m + Tuple2(k, Tuple2(neg + v, pos))
          }}

        //Add the positive atoms to the positive set.
        pos.foldLeft(m2) {(m, pair) => {
            val (k,v) = pair
            val (neg, pos) = m.get(k) match {
                case None => (Set[Int](),Set[Int]())
                case Some (p) => p
              }
            m + Tuple2(k, Tuple2(neg, pos + v))
          }}
      }

      //3. for every variable v, generate every (v1 in v+, v2 in v-) and number all of the resultant pairs.
      val pairs = posNegSets.map((v) => {val (_,(n,p)) = v; cartesianProduct(n.toList,p.toList)}).flatten.zipWithIndex.toList

      //-----------------------------------------------------------------------
      //DFS starts here
      //-----------------------------------------------------------------------

      // 4) let each node of the DFS be (R,V, F'), where R is the set of resolved pairs, V is the set of resolved atoms, and F' the resulting formula.
      class ResNode(val appliedPairs:List[((Int,Int),Int)],
                    val remainingPairs:List[((Int,Int),Int)],
                    val resolvedVars:Set[Int],
                    val currentFormula: List[MyFClause[(FOLFormula, Int)]]) extends SetNode[(Int,Int)] {

        def includedElements: List[((Int, Int),Int)] = appliedPairs
        def remainingElements: List[((Int, Int),Int)] = remainingPairs
        def largerElements: List[((Int, Int),Int)] = {
          if (appliedPairs.size == 0) { remainingPairs }
          else {
            val maxIncluded = appliedPairs.map(p => p._2).max
            remainingPairs.filter(p => p._2 > maxIncluded)
          }
        }

        override def addElem(p:((Int,Int),Int)): ResNode = {
          val (pair,index) = p
          new ResNode(p::appliedPairs, remainingPairs.filter(x => x._2 != index),
                      resolvedVars + (pair._1,pair._2) , forgetfulResolve(currentFormula, pair))
        }
      }

      // 4.1) let the root be ({},{},F).
      val rootNode = new ResNode(List[((Int,Int),Int)](), pairs, Set[Int](), fNumbered)

      var satCount = 0

      // 4.2) let the successor function be succ((R,V,F)) = {(R U r,V,F'') | r in (PAIRS - R),
      //                                                                     r intersect V =  {},
      //                                                                     r > max{R}, F'' = r applied to F',
      //                                                                     F'' is still valid}
      //      (if a node has no valid successors, it is considered an end node and added to the list of solutions.)
      def elemFilter(node: ResNode, elem:((Int,Int),Int)) : Boolean = {
        //trace("elemfilter: node.appliedPairs:   " + node.appliedPairs)
        //trace("            node.remainingPairs: " + node.remainingPairs)
        //trace("            node.resolvedVars:   " + node.resolvedVars)
        //trace("            node.largerElements: " + node.largerElements)

        val ret = (!node.resolvedVars.contains(elem._1._1) && !node.resolvedVars.contains(elem._1._2))
        //trace("            RETURN: " + ret)
        ret
      }

      //node-filter which checks for validity using miniSAT
      def nodeFilter(node: ResNode) : Boolean = {
        satCount = satCount + 1
        isValidWith(ehs, prover, addQuantifiers(NumberedCNFtoFormula(node.currentFormula), xs))
      }

      //Perform the DFS
      val solutions = DFS[ResNode](rootNode, (setSearch[(Int,Int),ResNode](elemFilter, nodeFilter, _:ResNode)))

      //All-quantify the found solutions.
      //debug("IMPROVESOLUTION 2 - # of sets examined: " + satCount + ".finished")
      solutions.map(n => NumberedCNFtoFormula(n.currentFormula)).map(s => addQuantifiers(s, xs))
   }

  /** Checks if the sequent is a tautology using f as the cut formula.
    * 
    * @param prover A prover that performs the validity check.
    * @param f The formula to be checked. It will be instantiated with the
    *          eigenvariable of the solution's grammar.
    *          For details, see introqcuts.pdf, Chapter 5, Prop. 4, Example 6.
    * @return True iff f still represents a valid solution.
    */
  def isValidWith(ehs: ExtendedHerbrandSequent, prover: Prover, f: FOLFormula) : Boolean = {

    //Instantiate with the eigenvariables.
    val body = ehs.grammar.eigenvariables.foldLeft(f)((f,ev) => f.instantiate(ev))

    //Instantiate with all the values in s.
    val as = ehs.grammar.s.transpose.foldLeft(List[FOLFormula]()) {case (acc, t) =>
      (t.foldLeft(f){case (f, sval) => f.instantiate(sval)}) :: acc
    }

    val head = andN(as)

    val impl = Imp(body, head)

    val antecedent = ehs.prop_l ++ ehs.inst_l :+ impl
    val succedent = ehs.prop_r ++ ehs.inst_r

    //isTautology(FSequent(antecedent, succedent))
    //trace( "calling SAT-solver" )
    val r = prover.isValid(Imp(andN(antecedent), orN(succedent)))
    //trace( "finished call to SAT-solver" )

    r
  }
  
  //------------------------ FORGETFUL RESOLUTION -------------------------//
  // TODO: this should go somewhere else.

  class MyFClause[A](val neg: List[A], val pos: List[A])
 
  def toMyFClause(c: FClause) = {
    val neg = c.neg.toList.map(x => x.asInstanceOf[FOLFormula])
    val pos = c.pos.toList.map(x => x.asInstanceOf[FOLFormula])
    new MyFClause[FOLFormula](neg, pos)
  }

  // We assume f is in CNF. Maybe it works also for f not
  // in CNF (since CNFp transforms f to CNF?).
  //
  // Implements forgetful resolution.
  def ForgetfulResolve(f: FOLFormula) : List[FOLFormula] =
  {
    val clauses = CNFp(f).map(c => toMyFClause(c))
    clauses.foldLeft(List[FOLFormula]())( (list, c1) => 
      list ::: clauses.dropWhile( _ != c1).foldLeft(List[FOLFormula]())( (list2, c2) => 
        if (resolvable(c1, c2))
          CNFtoFormula( (clauses.filterNot(c => c == c1 || c == c2 ) + resolve(c1, c2)).toList )::list2
        else
          list2
      )
    )
  }

  /** Converts a CNF back into a FOL formula.
    */
  def CNFtoFormula( cls : List[MyFClause[FOLFormula]] ) : FOLFormula =
  {
    val nonEmptyClauses = cls.filter(c => c.neg.length > 0 || c.pos.length > 0).toList

    if (nonEmptyClauses.length == 0) { TopC }
    else { And(nonEmptyClauses.map( c => Or(c.pos ++ c.neg.map( l => Neg(l) )) )) }
  }

  /** Converts a numbered CNF back into a FOL formula.
    */
  def NumberedCNFtoFormula( cls : List[MyFClause[(FOLFormula, Int)]] ) : FOLFormula = {
    val nonEmptyClauses = cls.filter(c => c.neg.length > 0 || c.pos.length > 0).toList

    if (nonEmptyClauses.length == 0) { TopC }
    else { And(nonEmptyClauses.map( c => Or(c.pos.map(l => l._1) ++ c.neg.map( l => Neg(l._1) )) )) }
  }

  // Checks if complementary literals exist.
  def resolvable(l: MyFClause[FOLFormula], r: MyFClause[FOLFormula]) =
    l.pos.exists( f => r.neg.contains(f) ) || l.neg.exists(f => r.pos.contains(f))

  // Assumes that resolvable(l, r). Does propositional resolution.
  // TODO: incorporate contraction.
  def resolve(l: MyFClause[FOLFormula], r: MyFClause[FOLFormula]) : MyFClause[FOLFormula] =
  {
    val cl = l.pos.find( f => r.neg.contains(f) )
    if (cl != None)
      //new MyFClause[FOLFormula]( l.neg ++ (r.neg - cl.get) , (l.pos - cl.get) ++ r.pos )
      // Using diff to remove only one copy of cl.get (the - operator is deprecated)
      new MyFClause[FOLFormula]( l.neg ++ ( r.neg.diff(List(cl.get)) ) , ( l.pos.diff(List(cl.get)) ) ++ r.pos )
    else
    {
      val cr = l.neg.find( f => r.pos.contains(f) ).get
      //new MyFClause[FOLFormula]( (l.neg - cr) ++ r.neg, l.pos ++ (r.pos - cr) )
      // Using diff to remove only one copy of cr (the - operator is deprecated)
      new MyFClause[FOLFormula]( ( l.neg.diff(List(cr)) ) ++ r.neg, l.pos ++ ( r.pos.diff(List(cr)) ) )
    }
  }

  /** Given a formula and a pair of indices (i,j), resolves the two clauses which contain i & j.
    * The original two clauses are deleted and the new, merged clauses is added to the formula.
    *
    * The order of the clauses is NOT preserved!
    *
    * @param cls The formula in numbered clause form: each atom is tuple of the atom itself and its index.
    * @param pair The two atom indices indicating the atoms to be resolved.
    * @return The original formula, with the two resolved clauses deleted and the new, resolved clause added.
    */
  def forgetfulResolve(cls: List[MyFClause[(FOLFormula, Int)]], pair:(Int, Int)) : List[MyFClause[(FOLFormula, Int)]] = {

    /** If either component of pair is present in clause, (clause',True)
      * is returned, where clause' is clause, with the occurring atoms deleted.
      * Otherwise, (clause,False) is returned.
      */
    def resolveClause(clause:MyFClause[(FOLFormula, Int)], pair: (Int, Int)) = {
      val neg = clause.neg.filter(a => a._2 != pair._1 && a._2 != pair._2)
      val pos = clause.pos.filter(a => a._2 != pair._1 && a._2 != pair._2)

      (new MyFClause(neg, pos), neg.length != clause.neg.length || pos.length != clause.pos.length)
    }

    val emptyClause = new MyFClause[(FOLFormula, Int)](Nil, Nil)

    def mergeClauses(clauses:List[MyFClause[(FOLFormula, Int)]]) : MyFClause[(FOLFormula, Int)] = {
      clauses.foldLeft(emptyClause)((c1, c2) => new MyFClause(c1.neg ++ c2.neg, c1.pos ++ c2.pos))
    }

    val startVal = (List[MyFClause[(FOLFormula, Int)]](), List[MyFClause[(FOLFormula, Int)]]())

    //Goes through all clauses with fold, trying to delete the atoms given by pair.
    val (f, rest) = cls.foldLeft(startVal)((x:(List[MyFClause[(FOLFormula, Int)]], List[MyFClause[(FOLFormula, Int)]]), clause:MyFClause[(FOLFormula,Int)]) => {
        val (formula, mergingClause) = x
        val (clause2,resolved) = resolveClause(clause, pair)

        //The first clause was resolved => add it to the temporary mergingClause instead of formula.
        if (resolved && mergingClause.length == 0) { (formula, clause2::Nil) }
        //The 2nd clause was resolved => merge the two clauses and add the result to formula.
        else if (resolved) { (mergeClauses(clause2::mergingClause)::formula, Nil) }
        //No clause was resolved => add the clause as is to the formula and continue.
        else {(clause::formula, mergingClause)}
      })

    //If both atoms were part of the same clause, rest is non-empty. In this case, add rest's 1 clause again.
    if (rest.length > 0) { (rest.head)::f } else { f }
  }
  
  //-----------------------------------------------------------------------//


}
