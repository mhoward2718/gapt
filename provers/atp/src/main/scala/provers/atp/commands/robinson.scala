package at.logic.provers.atp.commands

import at.logic.provers.atp.commands.base.DataCommand
import at.logic.provers.atp.commands.sequents.SetSequentsCommand
import at.logic.calculi.resolution.robinson.{InitialClause, Clause}
import at.logic.provers.atp.Definitions._
import at.logic.calculi.resolution.base.ResolutionProof
import at.logic.calculi.occurrences._
import at.logic.utils.ds.PublishingBuffer
import at.logic.calculi.resolution.robinson.{Resolution, Variant, Factor}
import at.logic.algorithms.unification.UnificationAlgorithm
import at.logic.calculi.occurrences.FormulaOccurrence
import at.logic.language.fol.{FOLExpression, Equation}
import at.logic.language.hol.logicSymbols.ConstantStringSymbol
import at.logic.language.lambda.substitutions.Substitution
import at.logic.calculi.resolution.robinson.Paramodulation
import at.logic.language.fol.FOLFormula
import at.logic.language.hol.replacements.{getAllPositions, Replacement}
import at.logic.calculi.lk.base.types.FSequent

/**
 * Created by IntelliJ IDEA.
 * User: shaolin
 * Date: Dec 13, 2010
 * Time: 1:00:51 PM
 * To change this template use File | Settings | File Templates.
 */

package robinson {

import _root_.at.logic.language.hol.replacements.getAtPosition
import _root_.at.logic.provers.atp.ProverException

// adds to the state the initial set of resolution proofs, made from the input clauses
  case class SetClausesCommand(override val clauses: Iterable[FSequent]) extends SetSequentsCommand[Clause](clauses) {
    def apply(state: State, data: Any) = {
      val pb = new PublishingBuffer[ResolutionProof[Clause]]
      clauses.foreach(x => pb += InitialClause(x._1, x._2)(defaultFormulaOccurrenceFactory))
      List((state += new Tuple2("clauses", pb), data))
    }
  }

  // this should also work with subsumption but as we replace the pb we need to remove subsumption managers if there are any in the state
  case object SetClausesFromDataCommand extends DataCommand[Clause] {
    def apply(state: State, data: Any) = {
      state.remove("simpleSubsumManager")
      // we need a better way to reset things that are connected to the pb such as a specific
      // command which somehow does it without knowing the implementations
      val pb = new PublishingBuffer[ResolutionProof[Clause]]
      val clauses = data.asInstanceOf[Iterable[ResolutionProof[Clause]]]
      clauses.foreach(x => pb += x)
      List((state += new Tuple2("clauses", pb), data))
    }
  }


  // create variants to a pair of two clauses
  case object VariantsCommand extends DataCommand[Clause] {
    def apply(state: State, data: Any) = {
      val p = data.asInstanceOf[Tuple2[ResolutionProof[Clause],ResolutionProof[Clause]]]
      List((state, (Variant(p._1),Variant(p._2))))
    }
  }

case class ResolveCommand(alg: UnificationAlgorithm[FOLExpression]) extends DataCommand[Clause] {
    def apply(state: State, data: Any) = {
      val ((p1,(lit1,b1))::(p2,(lit2,b2))::Nil) = data.asInstanceOf[Iterable[Pair[ResolutionProof[Clause],Pair[FormulaOccurrence,Boolean]]]].toList
      val mgus = alg.unify(lit1.formula.asInstanceOf[FOLExpression], lit2.formula.asInstanceOf[FOLExpression])
      require(mgus.size < 2) // as it is first order it must have at most one mgu
      mgus.map(x => (state, Resolution(p1,p2,lit1,lit2,x.asInstanceOf[Substitution[FOLExpression]])))
    }
  }

  case class FactorCommand(alg: UnificationAlgorithm[FOLExpression]) extends DataCommand[Clause] {
    def apply(state: State, data: Any) = {
      val res@ Resolution(cls, pr1, pr2, occ1, occ2, sub) = data.asInstanceOf[ResolutionProof[Clause]]
      val factors1 = computeFactors(alg, pr1.root.succedent, pr1.root.succedent.filterNot(_ == occ1).toList, occ1, Substitution[FOLExpression]()/*sub.asInstanceOf[Substitution[FOLExpression]]*/, Nil)
      val factors2 = computeFactors(alg, pr2.root.antecedent, pr2.root.antecedent.filterNot(_ == occ2).toList, occ2, Substitution[FOLExpression]()/*sub.asInstanceOf[Substitution[FOLExpression]]*/, Nil)
      (state, res) :: ((for {
          (ls1,sub1) <- (Nil,Substitution[FOLExpression]())::factors1
          (ls2,sub2) <- (Nil,Substitution[FOLExpression]())::factors2
          if !(ls1.isEmpty && ls2.isEmpty)
        } yield {
          // we need to get the new occurrences from factor to be used in Resolution
          val (pr11,occ11) = if (ls1.isEmpty) (pr1, occ1) else {
            val factor1 = Factor(pr1, occ1, ls1, sub1.asInstanceOf[Substitution[FOLExpression]])
            (factor1, factor1.root.getChildOf(occ1).get)
          }
          val (pr21,occ21) = if (ls2.isEmpty) (pr2, occ2) else {
            val factor2 = Factor(pr2, occ2, ls2, sub2.asInstanceOf[Substitution[FOLExpression]])
            (factor2, factor2.root.getChildOf(occ2).get)
          }
          List((pr11,(occ11,true)),(pr21,(occ21,false)))
          //Resolution(pr11, pr21, occ11, occ21, sub)
        }
      ).flatMap(x => new ResolveCommand(alg).apply(state,x)))
    }

    // computes factors, calling recursively to smaller sets
    // it is assumed in each call that the sub from the previous round is already applied to the formulas
    private def computeFactors(alg: UnificationAlgorithm[FOLExpression], lits: Seq[FormulaOccurrence], indices: List[FormulaOccurrence], formOcc: FormulaOccurrence,
                               sub: Substitution[FOLExpression], usedOccurrences: List[FormulaOccurrence]): List[Tuple2[List[FormulaOccurrence], Substitution[FOLExpression]]] =
      indices match {
        case Nil => Nil
        case x::Nil =>
          val mgus = alg.unify(sub(x.formula.asInstanceOf[FOLExpression]), sub(formOcc.formula.asInstanceOf[FOLExpression]))
          mgus match {
            case Nil => Nil
            case List(sub2 : Substitution[_]) => {
              val subst : Substitution[FOLExpression] = (sub2 compose sub)
              List( (x::usedOccurrences, subst) )
            }
          }
        case x::ls => {
            val facts = computeFactors(alg, lits, ls, formOcc, sub, usedOccurrences)
            facts.foldLeft(Nil: List[Tuple2[List[FormulaOccurrence], Substitution[FOLExpression]]])((ls,a) => ls
                ++ computeFactors(alg, lits, x::Nil, formOcc, a._2, a._1)) ++ facts ++ computeFactors(alg, lits, x::Nil, formOcc, sub, usedOccurrences)
        }
      }
  }

  case class ParamodulationCommand(alg: UnificationAlgorithm[FOLExpression]) extends DataCommand[Clause] {
    def apply(state: State, data: Any) = {
      val (p1,p2) = data.asInstanceOf[Tuple2[ResolutionProof[Clause],ResolutionProof[Clause]]]
      ((for {
          l1 <- p1.root.succedent
          l2 <- p2.root.antecedent ++ p2.root.succedent
          subTerm <- getAllPositions(l2.formula) // except var positions and only on positions of the same type as a or b
        } yield l1.formula match {
          case Equation(a,b) => {
              val mgus1 = if (a.exptype == subTerm._2.exptype) alg.unify(a, subTerm._2.asInstanceOf[FOLExpression]) else Nil
              require(mgus1.size < 2)
              val mgus2 = if (b.exptype == subTerm._2.exptype) alg.unify(b, subTerm._2.asInstanceOf[FOLExpression]) else Nil
              require(mgus2.size < 2)
              if (!mgus1.isEmpty)
                if (!mgus2.isEmpty)
                  List(Paramodulation(p1, p2, l1, l2, Replacement(subTerm._1, b).apply(l2.formula).asInstanceOf[FOLFormula], mgus1.head),
                    Paramodulation(p1, p2, l1, l2, Replacement(subTerm._1, a).apply(l2.formula).asInstanceOf[FOLFormula], mgus2.head))
                else List(Paramodulation(p1, p2, l1, l2, Replacement(subTerm._1, b).apply(l2.formula).asInstanceOf[FOLFormula], mgus1.head))
              else if (!mgus2.isEmpty)
                List(Paramodulation(p1, p2, l1, l2, Replacement(subTerm._1, a).apply(l2.formula).asInstanceOf[FOLFormula], mgus2.head))
              else List()
            }
            case _ => List()
        }) ++
        (for {
          l1 <- p2.root.succedent
          l2 <- p1.root.antecedent ++ p1.root.succedent
          subTerm <- getAllPositions(l2.formula) // except variable positions
        } yield l1.formula match {
            case Equation(a,b) => {
              val mgus1 = if (a.exptype == subTerm._2.exptype) alg.unify(a, subTerm._2.asInstanceOf[FOLExpression]) else Nil
              require(mgus1.size < 2)
              val mgus2 = if (b.exptype == subTerm._2.exptype) alg.unify(b, subTerm._2.asInstanceOf[FOLExpression]) else Nil
              require(mgus2.size < 2)

              if (!mgus1.isEmpty)
                if (!mgus2.isEmpty)
                  List(Paramodulation(p2, p1, l1, l2, Replacement(subTerm._1, b).apply(l2.formula).asInstanceOf[FOLFormula], mgus1.head),
                    Paramodulation(p2, p1, l1, l2, Replacement(subTerm._1, a).apply(l2.formula).asInstanceOf[FOLFormula], mgus2.head))
                else List(Paramodulation(p2, p1, l1, l2, Replacement(subTerm._1, b).apply(l2.formula).asInstanceOf[FOLFormula], mgus1.head))
              else if (!mgus2.isEmpty)
                List(Paramodulation(p2, p1, l1, l2, Replacement(subTerm._1, a).apply(l2.formula).asInstanceOf[FOLFormula], mgus2.head))
              else List()
            }
            case _ => List()
        })).flatMap((x => x.map(y => (state, y))))
    }
  }

  // create variants to a pair of two clauses and propagate the literal and position information
  case object VariantLiteralPositionCommand extends DataCommand[Clause] {
    def apply(state: State, data: Any) = {
      val ((p1,occ1,pos1)::(p2,occ2,pos2)::Nil) = data.asInstanceOf[Iterable[Tuple3[ResolutionProof[Clause],Pair[FormulaOccurrence,Boolean],Iterable[Int]]]].toList
      val v1 = Variant(p1)
      val v2 = Variant(p2)
      List((state, List((v1,(v1.root.getChildOf(occ1._1).get,occ1._2),pos1),(v2,(v2.root.getChildOf(occ2._1).get,occ2._2),pos2))))
    }
  }

   // create variants to a pair of two clauses and propagate the literal information
  case object VariantLiteralCommand extends DataCommand[Clause] {
    def apply(state: State, data: Any) = {
      val ((p1,occ1)::(p2,occ2)::Nil) = data.asInstanceOf[Iterable[Tuple2[ResolutionProof[Clause],Pair[FormulaOccurrence,Boolean]]]].toList
      val v1 = Variant(p1)
      val v2 = Variant(p2)
      List((state, List((v1,(v1.root.getChildOf(occ1._1).get,occ1._2)),(v2,(v2.root.getChildOf(occ2._1).get,occ2._2)))))
    }
  }

  // paramodulation where we get in addition to the two clauses, also the literals and the position in the literals
  // lit1 must always be the equation
  case class ParamodulationLiteralPositionCommand(alg: UnificationAlgorithm[FOLExpression]) extends DataCommand[Clause] {
    def apply(state: State, data: Any) = {
      val ((p1,occ1,pos1s)::(p2,occ2,pos2s)::Nil) = data.asInstanceOf[Iterable[Tuple3[ResolutionProof[Clause],Pair[FormulaOccurrence,Boolean],Iterable[Int]]]].toList
      val pos1 = pos1s.head
      val pos2 = pos2s.toList // because bad interface in syntax should be Iterable in Replacement
      // we need to require that lit1 is an equation
      val lit1 = occ1._1
      val lit2 = occ2._1
      val Equation(l,r) = lit1.formula
      val subTerm = getAtPosition(lit2.formula, pos2)
      if (pos1 == 1) {
        val mgu = if (l.exptype == subTerm.exptype) alg.unify(l, subTerm.asInstanceOf[FOLExpression]) else throw new ProverException("Paramodulation on " + lit1 + " and " + lit2 + " at position " + pos2 + " is not possible due to different types")
        require(mgu.size < 2)
        if (mgu.isEmpty) throw new ProverException("Paramodulation on " + lit1.formula + " at position " + pos1 + " and " + lit2.formula + " at position " + pos2 + " is not possible due to non-unifiable subterms")
        List((state, Paramodulation(p1, p2, lit1, lit2, Replacement(pos2, r).apply(lit2.formula).asInstanceOf[FOLFormula], mgu.head)))
      } else if (pos1 == 2) {
        val mgu = if (r.exptype == subTerm.exptype) alg.unify(r, subTerm.asInstanceOf[FOLExpression]) else throw new ProverException("Paramodulation on " + lit1 + " and " + lit2 + " at position " + pos2 + " is not possible due to different types")
        require(mgu.size < 2)
        if (mgu.isEmpty) throw new ProverException("Paramodulation on " + lit1.formula + " at position " + pos1 + " and " + lit2.formula + " at position " + pos2 + " is not possible due to non-unifiable subterms")
        List((state, Paramodulation(p1, p2, lit1, lit2, Replacement(pos2, l).apply(lit2.formula).asInstanceOf[FOLFormula], mgu.head)))
      } else throw new ProverException("Equation's position: " + pos1 + " is greater than 2 or smaller than 1")
    }
  }
}
