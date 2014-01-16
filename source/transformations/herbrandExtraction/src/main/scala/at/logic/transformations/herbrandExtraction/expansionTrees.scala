package at.logic.transformations.herbrandExtraction

import at.logic.calculi.lk.base._
import at.logic.calculi.lk.propositionalRules._
import at.logic.calculi.lk.quantificationRules._
import at.logic.calculi.lk.equationalRules._
import at.logic.language.hol._
import at.logic.calculi.expansionTrees.{WeakQuantifier => WQTree, StrongQuantifier => SQTree, And => AndTree, Or => OrTree, Imp => ImpTree, Neg => NotTree, Atom => AtomTree, MergeNode => MergeNodeTree, ExpansionSequent, ExpansionTreeWithMerges, ExpansionTree, merge => mergeTree}
import at.logic.calculi.lk.lkExtractors._
import at.logic.calculi.occurrences._

object extractExpansionTrees {

  def apply(proof: LKProof): ExpansionSequent = {
    val map = extract(proof)
    mergeTree( (proof.root.antecedent.map(fo => map(fo)), proof.root.succedent.map(fo => map(fo))) )
  }

  private def extract(proof: LKProof): Map[FormulaOccurrence,ExpansionTreeWithMerges] = proof match {
    case Axiom(r) => {
      // guess the axiom: must be an atom and appear left as well as right
      // can't use set intersection, but lists are small enough to do it manually
      val axiomCandidates = (r.antecedent.filter(elem => r.succedent.exists(elem2 => elem syntaxEquals elem2))).filter(_.formula.isAtom)


      if (axiomCandidates.size > 1) {
        println("Warning: Multiple candidates for axiom formula in expansion tree extraction, choosing first one of: "+axiomCandidates)
      }

      if (axiomCandidates.isEmpty) {
        println("Warning: No candidates for axiom formula in expansion tree extraction, treating it as list of formulas: " + r)
        // this behaviour is convenient for development, as it allows to work reasonably with invalid axioms
        Map(r.antecedent.map(fo => (fo, AtomTree(fo.formula) )) ++
             r.succedent.map(fo => (fo, AtomTree(fo.formula) )): _*)
      } else {
        val axiomFormula = axiomCandidates(0)

        Map(r.antecedent.map(fo => (fo, AtomTree(if (fo syntaxEquals axiomFormula) fo.formula else TopC) )) ++
             r.succedent.map(fo => (fo, AtomTree(if (fo syntaxEquals axiomFormula) fo.formula else BottomC) )): _*)
      }
    }
    case UnaryLKProof(_,up,r,_,p) => {
      val map = extract(up)
      getMapOfContext((r.antecedent ++ r.succedent).toSet - p, map) + Pair(p, (proof match {
        case WeakeningRightRule(_,_,_) => AtomTree(BottomC)
        case WeakeningLeftRule(_,_,_) => AtomTree(TopC)
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
