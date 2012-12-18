package at.logic.gui.prooftool.gui

/**
 * Created by IntelliJ IDEA.
 * User: mrukhaia
 * Date: 2/21/12
 * Time: 12:41 PM
 */

import at.logic.calculi.treeProofs.TreeProof
import at.logic.calculi.occurrences.FormulaOccurrence
import at.logic.calculi.lk.base.{Sequent, BinaryLKProof, UnaryLKProof, NullaryLKProof}
import at.logic.calculi.proofs.{BinaryProof, UnaryProof, Proof, NullaryProof}

object Search {

  def inTreeProof(str: String, proof: TreeProof[_]): Set[FormulaOccurrence] = proof match {
    case p: NullaryLKProof =>
      ( p.root.antecedent.filter( fo => DrawSequent.formulaToLatexString(fo.formula).contains(str)) ++
        p.root.succedent.filter( fo => DrawSequent.formulaToLatexString(fo.formula).contains(str)) ).toSet
    case p: UnaryLKProof =>
      inTreeProof(str, p.uProof.asInstanceOf[TreeProof[_]]) ++
        ( p.root.antecedent.filter( fo => DrawSequent.formulaToLatexString(fo.formula).contains(str)) ++
          p.root.succedent.filter( fo => DrawSequent.formulaToLatexString(fo.formula).contains(str)) ).toSet
    case p: BinaryLKProof =>
      inTreeProof(str, p.uProof1.asInstanceOf[TreeProof[_]]) ++
      inTreeProof(str, p.uProof2.asInstanceOf[TreeProof[_]]) ++
        ( p.root.antecedent.filter( fo => DrawSequent.formulaToLatexString(fo.formula).contains(str)) ++
          p.root.succedent.filter( fo => DrawSequent.formulaToLatexString(fo.formula).contains(str)) ).toSet
    case _ => throw new Exception("Cannot search in this object!")
  }

  def inResolutionProof(str: String, tree: Proof[_]): Set[FormulaOccurrence] = tree match {
    case p: NullaryProof[Sequent] =>
      ( p.root.antecedent.filter( fo => DrawSequent.formulaToLatexString(fo.formula).contains(str)) ++
        p.root.succedent.filter( fo => DrawSequent.formulaToLatexString(fo.formula).contains(str)) ).toSet
    case p: UnaryProof[Sequent] =>
      inResolutionProof(str, p.uProof) ++
        ( p.root.antecedent.filter( fo => DrawSequent.formulaToLatexString(fo.formula).contains(str)) ++
          p.root.succedent.filter( fo => DrawSequent.formulaToLatexString(fo.formula).contains(str)) ).toSet
    case p: BinaryProof[Sequent] =>
      inResolutionProof(str, p.uProof1) ++ inResolutionProof(str, p.uProof2) ++
        ( p.root.antecedent.filter( fo => DrawSequent.formulaToLatexString(fo.formula).contains(str)) ++
          p.root.succedent.filter( fo => DrawSequent.formulaToLatexString(fo.formula).contains(str)) ).toSet
    case _ => throw new Exception("Cannot search in this object!")
  }
}
