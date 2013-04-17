/*
 * quantificationRules.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.calculi.lk

import propositionalRules._
import at.logic.calculi.occurrences._
import at.logic.calculi.proofs._
import at.logic.language.lambda.BetaReduction._
import at.logic.language.lambda.BetaReduction.ImplicitStandardStrategy._
import at.logic.language.hol._
import at.logic.language.lambda.typedLambdaCalculus._
import at.logic.utils.ds.trees._
import scala.collection.immutable.Set
import scala.collection.mutable.HashMap
import base._

package quantificationRules {

import _root_.at.logic.utils.traits.Occurrence

  case class LKQuantifierException(root : Sequent,
                                   formula_occ : FormulaOccurrence,
                                   term : HOLExpression,
                                   calculated_formula : HOLFormula) extends Exception {
    override def getMessage = "Substituting the term "+term+"back into the given formula " + formula_occ.formula +" gives " + calculated_formula.toPrettyString + " instead of " + formula_occ.formula.toPrettyString+")"
  }

// Quantifier rules
  case object ForallLeftRuleType extends UnaryRuleTypeA
  case object ForallRightRuleType extends UnaryRuleTypeA
  case object ExistsLeftRuleType extends UnaryRuleTypeA
  case object ExistsRightRuleType extends UnaryRuleTypeA

  object ForallLeftRule extends WeakRuleHelper(false) {
    /**
     * Constructs a proof ending with a ForallLeft rule
     *
     * @param s1    subproof
     * @param aux   the auxiliary formula
     * @param main  the main formula
     * @param term  the term whose substitution into the main formula yields the auxiliary formula
     *
     * @return an LKProof ending with the constructed inference
     **/

    def apply(s1: LKProof, aux: HOLFormula, main: HOLFormula, term: HOLExpression) : LKProof = {
      s1.root.antecedent.filter( x => x.formula == aux ).toList match {
        case (x::_) => apply( s1, x, main, term )
        case _ => throw new LKRuleCreationException("No matching formula occurrence found for application of the rule with the given auxiliary formula.")
      }
    }


    def apply(s1: LKProof, term1oc: Occurrence, main: HOLFormula, term: HOLExpression) : LKProof = {
      val aux_fo = getTerms(s1.root, term1oc, main, term)
      val prinFormula = getPrinFormula(main, aux_fo)
      val sequent = getSequent(s1.root, aux_fo, prinFormula)

      new UnaryTree[Sequent](sequent, s1 )
      with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas with SubstitutionTerm {
        def rule = ForallLeftRuleType
        def aux = (aux_fo::Nil)::Nil
        def prin = prinFormula::Nil
        def subst = term
        override def name = "\u2200:l"
      }
    }

    def apply(s1: Sequent, term1oc: Occurrence, main: HOLFormula, term: HOLExpression) = {
      val aux_fo = getTerms(s1, term1oc, main, term)
      val prinFormula = getPrinFormula(main, aux_fo)
      getSequent(s1, aux_fo, prinFormula)
    }

    def unapply(proof: LKProof) = if (proof.rule == ForallLeftRuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas with SubstitutionTerm]
        val ((a1::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, a1, p1, r.subst))
      }
      else None
  }

  object ExistsRightRule extends WeakRuleHelper(true) {
    def apply(s1: LKProof, aux: HOLFormula, main: HOLFormula, term: HOLExpression) : LKProof = {
      s1.root.succedent.filter( x => x.formula == aux ).toList match {
        case (x::_) => apply( s1, x, main, term )
        case _ => throw new LKRuleCreationException("No matching formula occurrence found for application of the rule with the given auxiliary formula")
        }
    }


    def apply(s1: LKProof, term1oc: Occurrence, main: HOLFormula, term: HOLExpression) : LKProof = {
      val aux_fo = getTerms(s1.root, term1oc, main, term)
      val prinFormula = getPrinFormula(main, aux_fo)
      val sequent = getSequent(s1.root, aux_fo, prinFormula)

      new UnaryTree[Sequent](sequent, s1 )
      with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas with SubstitutionTerm {
        def rule = ExistsRightRuleType
        def aux = (aux_fo::Nil)::Nil
        def prin = prinFormula::Nil
        def subst = term
        override def name = "\u2203:r"
      }
    }
    def apply(s1: Sequent, term1oc: Occurrence, main: HOLFormula, term: HOLExpression) = {
      val aux_fo = getTerms(s1, term1oc, main, term)
      val prinFormula = getPrinFormula(main, aux_fo)
      getSequent(s1, aux_fo, prinFormula)
    }
    def unapply(proof: LKProof) = if (proof.rule == ExistsRightRuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas with SubstitutionTerm]
        val ((a1::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, a1, p1, r.subst))
      }
      else None
  }

  object ForallRightRule extends StrongRuleHelper(true) {
    def apply(s1: LKProof, aux: HOLFormula, main: HOLFormula, eigen_var: HOLVar) : LKProof =
      s1.root.succedent.filter( x => x.formula == aux ).toList match {
        case (x::_) => apply( s1, x, main, eigen_var )
        case _ => throw new LKRuleCreationException("No matching formula occurrence found for application of the rule with the given auxiliary formula")
      }

    def apply( s1: LKProof, term1oc: Occurrence, main: HOLFormula, eigen_var: HOLVar ) : LKProof = {
      val aux_fo = getTerms(s1.root, term1oc, main, eigen_var)
      val prinFormula = getPrinFormula(main, aux_fo)
      val sequent = getSequent(s1.root, aux_fo, prinFormula)

      new UnaryTree[Sequent](sequent, s1 )
        with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas with Eigenvariable {
          def rule = ForallRightRuleType
          def aux = (aux_fo::Nil)::Nil
          def prin = prinFormula::Nil
          def eigenvar = eigen_var
          override def name = "\u2200:r"
        }
    }
    def apply( s1: Sequent, term1oc: Occurrence, main: HOLFormula, eigen_var: HOLVar ) = {
      val aux_fo = getTerms(s1, term1oc, main, eigen_var)
      val prinFormula = getPrinFormula(main, aux_fo)
      getSequent(s1, aux_fo, prinFormula)
    }


    def unapply(proof: LKProof) = if (proof.rule == ForallRightRuleType) {
        val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas with Eigenvariable]
        val ((a1::Nil)::Nil) = r.aux
        val (p1::Nil) = r.prin
        Some((r.uProof, r.root, a1, p1, r.eigenvar))
      }
      else None
  }

  object ExistsLeftRule extends StrongRuleHelper(false) {
    def apply(s1: LKProof, aux: HOLFormula, main: HOLFormula, eigen_var: HOLVar) : LKProof =
      s1.root.antecedent.filter( x => x.formula == aux ).toList match {
        case (x::_) => apply( s1, x, main, eigen_var )
        case _ => throw new LKRuleCreationException("No matching formula occurrence found for application of the rule with the given auxiliary formula")
      }

    def apply( s1: LKProof, term1oc: Occurrence, main: HOLFormula, eigen_var: HOLVar ) : LKProof = {
      val aux_fo = getTerms(s1.root, term1oc, main, eigen_var)
      val prinFormula = getPrinFormula(main, aux_fo)
      val sequent = getSequent(s1.root, aux_fo, prinFormula)

      new UnaryTree[Sequent](sequent, s1)
      with UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas with Eigenvariable {
        def rule = ExistsLeftRuleType
        def aux = (aux_fo::Nil)::Nil
        def prin = prinFormula::Nil
        def eigenvar = eigen_var
        override def name = "\u2203:l"
      }
    }

    def apply( s1: Sequent, term1oc: Occurrence, main: HOLFormula, eigen_var: HOLVar ) = {
      val aux_fo = getTerms(s1, term1oc, main, eigen_var)
      val prinFormula = getPrinFormula(main, aux_fo)
      getSequent(s1, aux_fo, prinFormula)
    }

    def unapply(proof: LKProof) = if (proof.rule == ExistsLeftRuleType) {
      val r = proof.asInstanceOf[UnaryLKProof with AuxiliaryFormulas with PrincipalFormulas with Eigenvariable]
      val ((a1::Nil)::Nil) = r.aux
      val (p1::Nil) = r.prin
      Some((r.uProof, r.root, a1, p1, r.eigenvar))
    }
    else None
  }


  class QuantifierRuleHelper(polarity : Boolean) {
    def computeAux( main: HOLFormula, term: HOLExpression ) = main match {
      // TODO: make betaNormalize that respects closure of HOLFormula under normalization
      case All( sub, _ ) => betaNormalize( App( sub, term ) ).asInstanceOf[HOLFormula]
      case Ex( sub, _ ) => betaNormalize( App( sub, term ) ).asInstanceOf[HOLFormula]
      case _ => throw new LKRuleCreationException("Main formula of a quantifier rule must start with a strong quantfier.")
    }

    private[quantificationRules] def getPrinFormula(main: HOLFormula, aux_fo: FormulaOccurrence) = {
      aux_fo.factory.createFormulaOccurrence(main, aux_fo::Nil)
    }

    private[quantificationRules] def getSequent(s1: Sequent, aux_fo: FormulaOccurrence, prinFormula: FormulaOccurrence) = {
      if (polarity == false) {
        //working on antecedent {
        val ant = createContext(s1.antecedent.filterNot(_ == aux_fo))
        val antecedent = ant :+ prinFormula
        val succedent = createContext(s1.succedent)
        Sequent(antecedent, succedent)
      } else {
        //working on succedent
        val antecedent = createContext(s1.antecedent)
        val suc = createContext(s1.succedent.filterNot(_ == aux_fo))
        val succedent = suc :+ prinFormula
        Sequent(antecedent, succedent)
      }

    }
  }

  class StrongRuleHelper(polarity : Boolean) extends QuantifierRuleHelper(polarity) {
    private[quantificationRules] def getTerms(s1: Sequent, term1oc: Occurrence, main: HOLFormula, eigen_var: HOLVar) = {
      val foccs = if (polarity==false) s1.antecedent else s1.succedent
      foccs.find(_ == term1oc) match {
      case None => throw new LKRuleCreationException("Auxiliary formulas are not contained in the right part of the sequent")
      case Some(aux_fo) =>
        main match {
          case All( sub, _ ) =>
            // eigenvar condition
            assert( ( s1.antecedent ++ (s1.succedent.filterNot(_ == aux_fo)) ).forall( fo => !fo.formula.freeVariables.contains( eigen_var ) ),
              "Eigenvariable " + eigen_var.toStringSimple + " occurs in context " + s1.toStringSimple )
            //This check does the following: if we conclude exists x.A[x] from A[t] then A[x\t] must be A[t].
            //If it fails, you are doing something seriously wrong!
            //In any case do NOT remove it without telling everyone!
            assert( betaNormalize( App( sub, eigen_var ) ) == aux_fo.formula , "\n\nassert 2 in getTerms of String Quantifier Rule fails!\n\n")
            aux_fo

          case Ex( sub, _ ) =>
            // eigenvar condition
            assert( ( (s1.antecedent.filterNot(_ == aux_fo)) ++ s1.succedent ).forall( fo => !fo.formula.freeVariables.contains( eigen_var ) ),
              "Eigenvariable " + eigen_var.toStringSimple + " occurs in context " + s1.toStringSimple )
            //This check does the following: if we conclude exists x.A[x] from A[t] then A[x\t] must be A[t].
            //If it fails, you are doing something seriously wrong!
            //In any case do NOT remove it without telling everyone!
            assert( betaNormalize( App( sub, eigen_var ) ) == aux_fo.formula )
            aux_fo

          case _ => throw new LKRuleCreationException("Main formula of a quantifier rule must start with a strong quantfier.")
        }
      }
    }

  }

  class WeakRuleHelper(polarity : Boolean) extends QuantifierRuleHelper(polarity) {
    private[quantificationRules] def getTerms(s1: Sequent, term1oc: Occurrence, main: HOLFormula, term: HOLExpression) = {
      val foccs = if (polarity==false) s1.antecedent else s1.succedent
      foccs.find(_ == term1oc) match {
        case None => throw new LKRuleCreationException("Auxiliary formulas are not contained in the correct part of the sequent!")
        case Some(aux_fo) =>
          val comp_aux = computeAux( main, term )
          //This check does the following: if we conclude exists x.A[x] from A[t] then A[x\t] must be A[t].
          //If it fails, you are doing something seriously wrong!
          //In any case do NOT remove it without telling everyone!
          if (comp_aux !=  aux_fo.formula)
            throw new LKQuantifierException(s1, aux_fo, term, comp_aux)
          aux_fo
      }
    }
  }
}
