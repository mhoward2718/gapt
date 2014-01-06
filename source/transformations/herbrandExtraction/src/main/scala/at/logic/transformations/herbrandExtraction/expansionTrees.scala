package at.logic.transformations.herbrandExtraction

import at.logic.calculi.lk.base._
import at.logic.calculi.lk.propositionalRules._
import at.logic.calculi.lk.quantificationRules._
import at.logic.calculi.lk.equationalRules._
import at.logic.language.hol._
import at.logic.calculi.expansionTrees.{WeakQuantifier => WQTree, StrongQuantifier => SQTree, And => AndTree, Or => OrTree, Imp => ImpTree, Neg => NotTree, Atom => AtomTree, MergeNode => MergeNodeTree, ExpansionSequent, ExpansionTreeWithMerges, ExpansionTree, merge => mergeTree, coerceFormulaToET}
import at.logic.calculi.lk.lkExtractors._
import at.logic.calculi.occurrences._

object extractExpansionTrees {

  def apply(proof: LKProof): Tuple2[Seq[ExpansionTree],Seq[ExpansionTree]] = {
    val map = extract(proof)
    mergeTree( new ExpansionSequent[ExpansionTreeWithMerges](proof.root.antecedent.map(fo => map(fo)), proof.root.succedent.map(fo => map(fo))) ).toTuple()
  }

  private def extract(proof: LKProof): Map[FormulaOccurrence,ExpansionTreeWithMerges] = proof match {
    case Axiom(r) => Map(r.antecedent.map(fo => (fo,AtomTree(fo.formula))) ++
                         r.succedent.map(fo => (fo, AtomTree(fo.formula))): _*)
    case UnaryLKProof(_,up,r,_,p) => {
      val map = extract(up)
      getMapOfContext((r.antecedent ++ r.succedent).toSet - p, map) + Pair(p, (proof match {
        case WeakeningRightRule(_,_,_) => coerceFormulaToET(p.formula, isAntecedent=true)
        case WeakeningLeftRule(_,_,_) => coerceFormulaToET(p.formula, isAntecedent=false)
        case ForallLeftRule(_,_,a,_,t) => WQTree(p.formula, List(Pair(map(a),t)))
        case ExistsRightRule(_,_,a,_,t) => WQTree(p.formula, List(Pair(map(a),t)))
        case ForallRightRule(_,_,a,_,v) => SQTree(p.formula, v, map(a))
        case ExistsLeftRule(_,_,a,_,v) => SQTree(p.formula, v, map(a))
        case ContractionLeftRule(_,_,a1,a2,_) => MergeNodeTree(map(a1),map(a2))
        case ContractionRightRule(_,_,a1,a2,_) => MergeNodeTree(map(a1),map(a2))
        case AndLeft1Rule(_,_,a,_) => {val And(_,f2) = p.formula; AndTree(map(a), AtomTree(f2))}
        case AndLeft2Rule(_,_,a,_) => {val And(f1,_) = p.formula; AndTree(AtomTree(f1),map(a))}
        case OrRight1Rule(_,_,a,_) => {val Or(_,f2) = p.formula; OrTree(map(a), AtomTree(f2))}
        case OrRight2Rule(_,_,a,_) => {val Or(f1,_) = p.formula; OrTree(AtomTree(f1),map(a))}
        case ImpRightRule(_,_,a1,a2,_) => ImpTree(map(a1),map(a2))
        case NegLeftRule(_,_,a,_) => NotTree(map(a))
        case NegRightRule(_,_,a,_) => NotTree(map(a))
      }))
    }
    case CutRule(up1,up2,r,_,_) => getMapOfContext((r.antecedent ++ r.succedent).toSet, extract(up1) ++ extract(up2))
    case BinaryLKProof(_,up1,up2,r,a1,a2,Some(p)) => {
      val map = extract(up1) ++ extract(up2)
      getMapOfContext((r.antecedent ++ r.succedent).toSet - p, map) + Pair(p, (proof match {
        case ImpLeftRule(_,_,_,_,_,_) => ImpTree(map(a1),map(a2))
        case OrLeftRule(_,_,_,_,_,_) => OrTree(map(a1),map(a2))
        case AndRightRule(_,_,_,_,_,_) => AndTree(map(a1),map(a2))
        case EquationLeft1Rule(_,_,_,_,_,_) => map(a2)
        case EquationLeft2Rule(_,_,_,_,_,_) => map(a2)
        case EquationRight1Rule(_,_,_,_,_,_) => map(a2)
        case EquationRight2Rule(_,_,_,_,_,_) => map(a2)
      }))
    }
    case _ => throw new IllegalArgumentException("unsupported proof rule: " + proof)
  }

  // the set of formula occurrences given to method must not contain any principal formula
  private def getMapOfContext(s: Set[FormulaOccurrence], map: Map[FormulaOccurrence,ExpansionTreeWithMerges]): Map[FormulaOccurrence,ExpansionTreeWithMerges] =
    Map(s.toList.map(fo => (fo, {
      require(fo.ancestors.size == 1)
      map(fo.ancestors.head)
    })): _*)


}
