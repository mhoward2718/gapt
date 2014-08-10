package at.logic.transformations.herbrandExtraction.lksk

import at.logic.transformations.herbrandExtraction.extractExpansionTrees
import at.logic.calculi.lk.base.LKProof
import at.logic.calculi.expansionTrees.{merge => mergeTree, Atom => AtomTree, _}
import at.logic.calculi.occurrences.FormulaOccurrence

import at.logic.calculi.lksk._
import scala.Tuple2
import at.logic.language.hol.{TopC, BottomC}
import at.logic.calculi.lk.{BinaryLKProof, CutRule, UnaryLKProof}

/**
 * Extends expansion tree extraction to lksk.
 */
object extractLKSKExpansionTrees extends extractLKSKExpansionTrees;
class extractLKSKExpansionTrees  extends extractExpansionTrees {
  override def apply(proof: LKProof): ExpansionSequent = {
    val map = extract(proof)
    mergeTree( (proof.root.antecedent.map(fo => map(fo)), proof.root.succedent.map(fo => map(fo))) )
  }

  def extract(proof: LKProof): Map[FormulaOccurrence,ExpansionTreeWithMerges] = proof match {
    case Axiom(r) =>
      handleAxiom(r)

    case WeakeningRightRule(parent, r, p) =>
      val map = extract(parent)
      val contextmap = getMapOfContext((r.antecedent ++ r.succedent).toSet - p, map)
      contextmap + ((p, AtomTree(TopC)))
    case WeakeningLeftRule(parent, r, p) =>
      val map = extract(parent)
      val contextmap = getMapOfContext((r.antecedent ++ r.succedent).toSet - p, map)
      contextmap + ((p, AtomTree(BottomC)))
    case ForallSkLeftRule(parent, r, a, p, t) =>
      val map = extract(parent)
      val contextmap = getMapOfContext((r.antecedent ++ r.succedent).toSet - p, map)
      contextmap + ((p, WeakQuantifier(p.formula, List(Tuple2(map(a), t)))))
    case ExistsSkRightRule(parent, r, a, p, t) =>
      val map = extract(parent)
      val contextmap = getMapOfContext((r.antecedent ++ r.succedent).toSet - p, map)
      contextmap + ((p, WeakQuantifier(p.formula, List(Tuple2(map(a), t)))))
    case ForallSkRightRule(parent, r, a, p, skt) =>
      val map = extract(parent)
      val contextmap = getMapOfContext((r.antecedent ++ r.succedent).toSet - p, map)
      contextmap + ((p, SkolemQuantifier(p.formula,  skt, map(a) )))
    case ExistsSkLeftRule(parent, r, a, p, skt) =>
      val map = extract(parent)
      val contextmap = getMapOfContext((r.antecedent ++ r.succedent).toSet - p, map)
      contextmap + ((p, SkolemQuantifier(p.formula,  skt, map(a) )))


    case UnaryLKProof(_,up,r,_,p) =>
      val map = extract(up)
      handleUnary(r, p, map, proof)

    case CutRule(up1,up2,r,_,_) =>
      getMapOfContext((r.antecedent ++ r.succedent).toSet, extract(up1) ++ extract(up2))

    case BinaryLKProof(_,up1,up2,r,a1,a2,Some(p)) =>
      val map = extract(up1) ++ extract(up2)
      handleBinary(r, map, proof, a1, a2, p)

  }


}
