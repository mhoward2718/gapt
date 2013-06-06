/*
 * TPTPFOLParser.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.parsing.language.tptp

import _root_.at.logic.language.fol._
import at.logic.language.hol.{Neg => HOLNEG, HOLFormula, Or => HOLOR}
import at.logic.language.hol.logicSymbols._
import at.logic.calculi.lk.base.types.FSequent
import scala.collection.immutable.HashMap
import at.logic.algorithms.fol.hol2fol._
import collection.immutable.List._

object TPTPFOLExporter {
  // FIXME: this should not be here!
  def hol2fol(f: HOLFormula) : FOLFormula = 
  {
    val imap = scala.collection.mutable.Map[at.logic.language.lambda.typedLambdaCalculus.LambdaExpression, at.logic.language.hol.logicSymbols.ConstantStringSymbol]()
    val iid = new {var idd = 0; def nextId = {idd = idd+1; idd}}
    reduceHolToFol(f, imap, iid )
  } 

  def toFormula(s: FSequent): HOLFormula =  HOLOR( s._1.toList.map( f => HOLNEG( f ) ) ++ s._2 )

  // TODO: have to give a different name because of erasure :-(
  def tptp_problem_named( ss: List[Pair[String, FSequent]] ) =
    ss.foldLeft("")( (s, p) => s + sequentToProblem( p._2, p._1 ) + "\n")

  def tptp_problem( ss: List[FSequent] ) =
    tptp_problem_named( ss.zipWithIndex.map( p => ( "sequent" + p._2, p._1 ) ) )

  def sequentToProblem( s: FSequent, n: String ) =
    "cnf( " + n + ",axiom," + export( s ) + ")."

  // TODO: would like to have FOLSequent here --- instead, we convert
  // we export it as a disjunction
  def export( s: FSequent ) = {
    val f = hol2fol(toFormula(s))
    val map = getFreeVarRenaming( f )
    tptp( f )( map )
  }

  def getFreeVarRenaming( f: FOLFormula ) = {
    getFreeVariablesFOL( f ).toList.zipWithIndex.foldLeft( new HashMap[FOLVar, String] )( (m, p) =>
      m + (p._1 -> ("X" + p._2.toString) )
    )
  }

  def tptp( e: FOLExpression )(implicit s_map: Map[FOLVar, String]) : String = e match {
    case f: FOLFormula => tptp( f )
    case t: FOLTerm => tptp( t )
  }

  // To be able to deal with theorem provers that implement only
  // the parsing of clauses (i.e. they assume associativity of |
  // and dislike parentheses), we only export clauses at the moment.
  def tptp( f: FOLFormula )(implicit s_map: Map[FOLVar, String]) : String = f match {
    case Atom(x, args) => handleAtom( x, args )
    case Or(x,y) => tptp( x ) + " | " + tptp( y )
    case Neg(x) => "~" + tptp( x )
  }

  def tptp( t: FOLTerm )(implicit s_map: Map[FOLVar, String]) : String = t match {
    case FOLConst(c) => single_quote( c.toString )
    case FOLVar(x) => s_map( t.asInstanceOf[FOLVar] )
    case Function(x, args) => handleAtom( x, args )
  }

  // todo: introduce special constant for equality and use it here!
  def handleAtom( x: ConstantSymbolA, args: List[FOLTerm] )(implicit s_map: Map[FOLVar, String]) =
    if ( x.toString.equals("=") )
      tptp( args.head ) + " = " + tptp( args.last )
    else
      single_quote( x.toString ) + (
      if (args.size == 0)
        ""
      else
        "(" + tptp( args.head ) + 
        args.tail.foldLeft("")((s,a) => s + ", " + tptp( a ) )
        + ")" )

  def single_quote( s: String ) = "'" + s + "'"
}

object TPTPfofExporter {
  def apply(conjectures: Seq[FOLFormula]) = generate_file(Nil, conjectures)
  def apply(axioms : Seq[FOLFormula], conjectures: Seq[FOLFormula]) = generate_file(axioms, conjectures)

  def generate_file(axioms : Seq[FOLFormula], conjectures : Seq[FOLFormula]) = {
    val builder = new StringBuilder()

    var count = 0
    for (formula <- axioms) {
      builder append ("fof(axiom")
      builder append (count)
      builder append (", axiom, ")
      //builder append (Renaming.fol_as_tptp(formula) )
      builder append (").\n\n")

      count = count + 1
    }

    for (formula <- conjectures) {
      builder append ("fof(formula")
      builder append (count)
      builder append (", conjecture, ")
      //builder append (Renaming.fol_as_tptp(formula) )
      builder append (").\n\n")

      count = count + 1
    }
    builder.toString()
  }

}

