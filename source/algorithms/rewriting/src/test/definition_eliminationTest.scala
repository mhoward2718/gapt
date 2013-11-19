package at.logic.algorithms.rewriting

import org.junit.runner.RunWith
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.runner.JUnitRunner
import at.logic.language.fol._
import at.logic.language.hol.logicSymbols.ConstantStringSymbol
import at.logic.language.lambda.symbols.VariableStringSymbol
import at.logic.calculi.lk.propositionalRules.{InitialRuleType, AndRightRule, Axiom}
import at.logic.calculi.lk.quantificationRules.{ExistsRightRule, ForallRightRule, ForallLeftRule}
import at.logic.calculi.lk.definitionRules.{DefinitionLeftRule, DefinitionRightRule}
import at.logic.language.hol.HOLExpression
import at.logic.calculi.lk.base.{Sequent, LKProof}
import at.logic.calculi.lk.lkExtractors.{BinaryLKProof, UnaryLKProof}
import at.logic.calculi.proofs.NullaryProof

@RunWith(classOf[JUnitRunner])
class definition_eliminationTest extends SpecificationWithJUnit {
  object proof1 {
    val List(alphasym, betasym, xsym, ysym) = List("\\alpha","\\beta","x","y") map VariableStringSymbol
    val List(p,q,a,b,tsym) = List("P","Q","A","B","t") map ConstantStringSymbol
    val List(t) = List(tsym) map ((x:ConstantStringSymbol) => FOLConst(x))
    val List(alpha,beta,x,y) = List(alphasym, betasym, xsym, ysym).map( (x : VariableStringSymbol) => FOLVar(x))
    val qa = Atom(q, alpha::Nil)
    val qx = Atom(q, x::Nil)
    val pab = Atom(p, List(alpha,beta))
    val pay = Atom(p, List(alpha,y))
    val pty = Atom(p, List(t,y))
    val pxy = Atom(p, List(x,y))
    val ax =  Atom(a,x::Nil)
    val aa =  Atom(a,alpha::Nil)
    val bx = Atom(b,x::Nil)
    val allypay = AllVar(y,pay)
    val allypty = AllVar(y,pty)
    val allypxy = AllVar(y, pxy)
    val allxypxy = AllVar(x, allypxy )
    val allxax = AllVar(x, ax)
    val exformula = ExVar(x, And(qx, ax))

    val i1 = Axiom(List(qa), List(qa))
    val i2 = ForallLeftRule(i1, i1.root.antecedent(0), AllVar(x,qx), alpha)

    val i3 = Axiom(List(pab),List(pab))
    val i4 = ForallLeftRule(i3, i3.root.antecedent(0), allypay, beta)
    val i5 = ForallRightRule(i4, i4.root.succedent(0), allypay, beta)
    val i6 = DefinitionRightRule(i5, i5.root.succedent(0), aa)
    val i7 = ForallLeftRule(i6, i6.root.antecedent(0), allxypxy , alpha)
    val i8 = DefinitionLeftRule(i7, i7.root.antecedent(0), allxax )
    val i9 = AndRightRule(i2, i8, i2.root.succedent(0), i8.root.succedent(0))
    val i10 = ExistsRightRule(i9, i9.root.succedent(0), exformula , alpha)
    val i11 = DefinitionRightRule(i10, i10.root.succedent(0), ExVar(x, bx))
    getoccids(i11, Nil) map println

    val def1 = (ax, AllVar(y, pxy))
    val def2 = (bx, And(qx,ax))
    val dmap = Map[HOLExpression, HOLExpression]() + def1 +def2

    def getoccids(p:LKProof, l : List[String]) : List[String] = p match {
      case r:NullaryProof[Sequent] =>
        val line = r.rule +": "+  r.root.antecedent.map(_.id).mkString(",") + " :- " + (r.root.succedent.map(_.id).mkString(","))
        line::Nil
      case r@UnaryLKProof(_, p1, root, _, _) =>
        val line = r.rule +": "+ root.antecedent.map(_.id).mkString(",") + " :- " + (root.succedent.map(_.id).mkString(","))
        getoccids(p1, line::l) :+ line
      case r@BinaryLKProof(_, p1, p2, root, _, _,  _) =>
        val line = r.rule +": "+ root.antecedent.map(_.id).mkString(",") + " :- " + (root.succedent.map(_.id).mkString(","))
        val rec1 = getoccids(p1, line::l)
        val rec2 = getoccids(p2, rec1)
        (rec1 ++ rec2) :+ line

    }

  }

  "Definition elimination" should {
    "work on formulas" in {
      val f = And(proof1.ax,Or(Atom(proof1.a,proof1.t::Nil), proof1.bx))
      val expdmap = definition_elimination.expand_dmap(proof1.dmap)
      println(expdmap)
      val f_ = definition_elimination.replaceAll_in(expdmap,f)
      println(f_)
      val correct_f = And(proof1.allypxy,Or(proof1.allypty, And(proof1.qx, proof1.allypxy)))
      f_ mustEqual(correct_f)
    }

    "work on a simple proof" in {
      val expdmap = definition_elimination.expand_dmap(proof1.dmap)
      val elp = DefinitionElimination.eliminate_in_proof_( definition_elimination.replaceAll_in(expdmap, _), proof1.i11 )
      println(elp)
      ok
    }
  }

}
