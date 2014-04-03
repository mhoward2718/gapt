package at.logic.algorithms.lk

import at.logic.calculi.lk.base._
import at.logic.calculi.occurrences.FormulaOccurrence
import at.logic.calculi.lk.propositionalRules._
import at.logic.language.hol._
import at.logic.language.schema.{And => AndS, Or => OrS, SchemaFormula}
import at.logic.calculi.slk._
import at.logic.calculi.lk.equationalRules.{EquationLeft2Rule, EquationRight1Rule, EquationRight2Rule, EquationLeft1Rule}
import at.logic.calculi.lk.definitionRules.{DefinitionRightRule, DefinitionLeftRule}
import at.logic.calculi.lk.quantificationRules.{ExistsRightRule, ExistsLeftRule, ForallRightRule, ForallLeftRule}

/**
 * Removes the redundant weakenings and contractions.
 * Linear algorithm. Traverses the proof top down, keeping track of the
 * weakened formulas. Checks if the auxiliary formulas of each rule are weakened
 * or not and treats it appropriately.
 * TODO: make it tail-recursive.
 */
object CleanStructuralRules {

  def apply(p: LKProof) : LKProof = {
    val (proof, ws) = cleanStructuralRules(p)
    assert(ws.forall(f => (p.root.antecedent ++ p.root.succedent).map(_.formula).contains(f)))
    addWeakenings(proof, p.root.toFSequent)
  }

  private def cleanStructuralRules(pr: LKProof) : (LKProof, List[HOLFormula]) = pr match {
    // Base case: axiom
    case Axiom(s) => ( pr, Nil )

    // Structural rules:
    
    case WeakeningLeftRule(p, _, m) => 
      val (proof, ws) = cleanStructuralRules(p)
      ( proof, ws :+ m.formula )
    
    case WeakeningRightRule(p, _, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      ( proof, ws :+ m.formula )

    case ContractionLeftRule(p, _, a1, a2, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      ws.count(f => f == a1.formula) match {
        case n if n >= 2 => ( proof, ws.diff(List(a1.formula, a2.formula)) :+ m.formula ) 
        case n if n == 1 =>
          require(proof.root.antecedent.exists(fo => fo.formula == a1.formula))
          ( proof, ws.diff(List(a1.formula)) )
        case n if n == 0 => ( ContractionLeftRule(proof, a1.formula), ws )
      }

    case ContractionRightRule(p, _, a1, a2, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      ws.count(f => f == a1.formula) match {
        case n if n >= 2 => ( proof, ws.diff(List(a1.formula, a2.formula)) :+ m.formula ) 
        case n if n == 1 => 
          require(proof.root.succedent.exists(fo => fo.formula == a1.formula))
          ( proof, ws.diff(List(a1.formula)) )
        case n if n == 0 => ( ContractionRightRule(proof, a1.formula), ws )
      }
 
    case CutRule(p1, p2, _, a1, a2) =>
      val (proof1, ws1) = cleanStructuralRules(p1)
      val (proof2, ws2) = cleanStructuralRules(p2)
      (ws1.contains(a1.formula) && !proof1.root.succedent.exists(fo => fo.formula == a1.formula), 
       ws2.contains(a2.formula) && !proof2.root.antecedent.exists(fo => fo.formula == a2.formula)) match {
        case (true, true) => 
          val ws_ = ws1.diff(List(a1.formula)) ++ ws2.diff(List(a2.formula))
          (proof1, ws_) // The choice for proof1 is arbitrary
        case (true, false) =>
          val ws_ = ws1.diff(List(a1.formula)) ++ ws2
          val p = WeakeningRightRule(proof1, a1.formula)
          ( CutRule(p, proof2, a1.formula), ws_ )
        case (false, true) =>
          val ws_ = ws1 ++ ws2.diff(List(a2.formula))
          val p = WeakeningLeftRule(proof2, a2.formula)
          ( CutRule(proof1, p, a1.formula), ws_ )
        case (false, false) =>
          val ws_ = ws1 ++ ws2
          ( CutRule(proof1, proof2, a1.formula), ws_ )
      }

    // Unary rules, one aux formula:

    case NegLeftRule(p, _, a, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_right(proof, ws, a.formula, m.formula, {(p, a, m) => NegLeftRule(p, a)} )
  
    case NegRightRule(p, _, a, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_left(proof, ws, a.formula, m.formula, {(p, a, m) => NegRightRule(p, a)} )
 
    case AndLeft1Rule(p, _, a, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_left(proof, ws, a.formula, m.formula, { (p, a, m) =>
        val a2 = m match {case And(_, r) => r}; AndLeft1Rule(p, a, a2) } )
    
    case AndLeft2Rule(p, _, a, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_left(proof, ws, a.formula, m.formula, { (p, a, m) =>
        val a2 = m match {case And(l, _) => l}; AndLeft2Rule(p, a2, a) } )
    
    case OrRight1Rule(p, _, a, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_right(proof, ws, a.formula, m.formula, { (p, a, m) =>
        val a2 = m match {case Or(_, r) => r}; OrRight1Rule(p, a, a2) } )
    
    case OrRight2Rule(p, _, a, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_right(proof, ws, a.formula, m.formula, { (p, a, m) =>
        val a2 = m match {case Or(l, _) => l}; OrRight2Rule(p, a2, a) } )
 
    case ForallLeftRule(p, _, a, m, t) =>
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_left(proof, ws, a.formula, m.formula, { (p, a, m) => ForallLeftRule(p, a, m, t) } )

    case ForallRightRule(p, _, a, m, t) => 
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_right(proof, ws, a.formula, m.formula, { (p, a, m) => ForallRightRule(p, a, m, t) } )

    case ExistsLeftRule(p, _, a, m, t) => 
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_left(proof, ws, a.formula, m.formula, { (p, a, m) => ExistsLeftRule(p, a, m, t) } )

    case ExistsRightRule(p, _, a, m, t) => 
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_right(proof, ws, a.formula, m.formula, { (p, a, m) => ExistsRightRule(p, a, m, t) } )

    // Schema rules (all unary with one aux formula):
    case AndLeftEquivalenceRule1(p, _, a, m) => 
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_left(proof, ws, a.formula, m.formula, { (p, a, m) => AndLeftEquivalenceRule1(p, a, m) } )

    case AndRightEquivalenceRule1(p, _, a, m) => 
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_right(proof, ws, a.formula, m.formula, { (p, a, m) => AndRightEquivalenceRule1(p, a, m) } )
    
    case OrLeftEquivalenceRule1(p, _, a, m) => 
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_left(proof, ws, a.formula, m.formula, { (p, a, m) => OrLeftEquivalenceRule1(p, a, m) } )
    
    case OrRightEquivalenceRule1(p, _, a, m) => 
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_right(proof, ws, a.formula, m.formula, { (p, a, m) => OrRightEquivalenceRule1(p, a, m) } )
    
    case AndLeftEquivalenceRule3(p, _, a, m) => 
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_left(proof, ws, a.formula, m.formula, { (p, a, m) => AndLeftEquivalenceRule3(p, a, m) } )
    
    case AndRightEquivalenceRule3(p, _, a, m) => 
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_right(proof, ws, a.formula, m.formula, { (p, a, m) => AndRightEquivalenceRule3(p, a, m) } )
    
    case OrLeftEquivalenceRule3(p, _, a, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_left(proof, ws, a.formula, m.formula, { (p, a, m) => OrLeftEquivalenceRule3(p, a, m) } )
    
    case OrRightEquivalenceRule3(p, _, a, m) => 
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_right(proof, ws, a.formula, m.formula, { (p, a, m) => OrRightEquivalenceRule3(p, a, m) } )

    // Definition rules (all unary with one aux formula):
    case DefinitionLeftRule(p, _, a, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_left(proof, ws, a.formula, m.formula, { (p, a, m) => DefinitionLeftRule(p, a, m) } )

    case DefinitionRightRule(p, _, a, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      handle_unary_one_aux_right(proof, ws, a.formula, m.formula, { (p, a, m) => DefinitionRightRule(p, a, m) } )

    // Unary rules, two aux formulas:

    case ImpRightRule(p, _, a1, a2, m) =>
      val (proof, ws) = cleanStructuralRules(p)
      (ws.contains(a1.formula) && !proof.root.antecedent.exists(fo => fo.formula == a1.formula), 
       ws.contains(a2.formula) && !proof.root.succedent.exists(fo => fo.formula == a2.formula)) match {
        case (true, true) => ( proof, ws.diff(List(a1.formula, a2.formula)) :+ m.formula ) 
        case (true, false) => 
          val p1 = WeakeningLeftRule(proof, a1.formula)
          val p2 = ImpRightRule(p1, a1.formula, a2.formula)
          ( p2, ws.diff(List(a1.formula)) )
        case (false, true) => 
          val p1 = WeakeningRightRule(proof, a2.formula)
          val p2 = ImpRightRule(p1, a1.formula, a2.formula)
          ( p2, ws.diff(List(a2.formula)) )
        case (false, false) => ( ImpRightRule(proof, a1.formula, a2.formula), ws )
      }

    // Binary rules: TODO: refactor the binary rules (code is duplicated)

    case OrLeftRule(p1, p2, _, a1, a2, m) =>
      val (proof1, ws1) = cleanStructuralRules(p1)
      val (proof2, ws2) = cleanStructuralRules(p2)
      (ws1.contains(a1.formula) && !proof1.root.antecedent.exists(fo => fo.formula == a1.formula), 
       ws2.contains(a2.formula) && !proof2.root.antecedent.exists(fo => fo.formula == a2.formula)) match {
        case (true, true) => 
          val ws_ = ( ws1.diff(List(a1.formula)) ++ ws2.diff(List(a2.formula)) ) :+ m.formula
          (proof1, ws_) // The choice for proof1 is arbitrary
        case (true, false) =>
          val ws_ = ws1.diff(List(a1.formula)) ++ ws2
          val p = WeakeningLeftRule(proof1, a1.formula)
          ( OrLeftRule(p, proof2, a1.formula, a2.formula), ws_ )
        case (false, true) =>
          val ws_ = ws1 ++ ws2.diff(List(a2.formula))
          val p = WeakeningLeftRule(proof2, a2.formula)
          ( OrLeftRule(proof1, p, a1.formula, a2.formula), ws_ )
        case (false, false) =>
          val ws_ = ws1 ++ ws2
          ( OrLeftRule(proof1, proof2, a1.formula, a2.formula), ws_ )
      }

    case AndRightRule(p1, p2, _, a1, a2, m) =>
      val (proof1, ws1) = cleanStructuralRules(p1)
      val (proof2, ws2) = cleanStructuralRules(p2)
      (ws1.contains(a1.formula) && !proof1.root.succedent.exists(fo => fo.formula == a1.formula), 
       ws2.contains(a2.formula) && !proof2.root.succedent.exists(fo => fo.formula == a2.formula)) match {
        case (true, true) => 
          val ws_ = (ws1.diff(List(a1.formula)) ++ ws2.diff(List(a2.formula))) :+ m.formula
          (proof1, ws_) // The choice for proof1 is arbitrary
        case (true, false) =>
          val ws_ = ws1.diff(List(a1.formula)) ++ ws2
          val p = WeakeningRightRule(proof1, a1.formula)
          ( AndRightRule(p, proof2, a1.formula, a2.formula), ws_ )
        case (false, true) =>
          val ws_ = ws1 ++ ws2.diff(List(a2.formula))
          val p = WeakeningRightRule(proof2, a2.formula)
          ( AndRightRule(proof1, p, a1.formula, a2.formula), ws_ )
        case (false, false) =>
          val ws_ = ws1 ++ ws2
          ( AndRightRule(proof1, proof2, a1.formula, a2.formula), ws_ )
      }
      
    case ImpLeftRule(p1, p2, _, a1, a2, m) =>
      val (proof1, ws1) = cleanStructuralRules(p1)
      val (proof2, ws2) = cleanStructuralRules(p2)
      (ws1.contains(a1.formula) && !proof1.root.succedent.exists(fo => fo.formula == a1.formula), 
       ws2.contains(a2.formula) && !proof2.root.antecedent.exists(fo => fo.formula == a2.formula)) match {
        case (true, true) => 
          val ws_ = (ws1.diff(List(a1.formula)) ++ ws2.diff(List(a2.formula))) :+ m.formula
          (proof1, ws_) // The choice for proof1 is arbitrary
        case (true, false) =>
          val ws_ = ws1.diff(List(a1.formula)) ++ ws2
          val p = WeakeningRightRule(proof1, a1.formula)
          ( ImpLeftRule(p, proof2, a1.formula, a2.formula), ws_ )
        case (false, true) =>
          val ws_ = ws1 ++ ws2.diff(List(a2.formula))
          val p = WeakeningLeftRule(proof2, a2.formula)
          ( ImpLeftRule(proof1, p, a1.formula, a2.formula), ws_ )
        case (false, false) =>
          val ws_ = ws1 ++ ws2
          ( ImpLeftRule(proof1, proof2, a1.formula, a2.formula), ws_ )
      }
   
    // Equation rules (all binary):
    case EquationLeft1Rule(p1, p2, _, a1, a2, m) =>
      val (proof1, ws1) = cleanStructuralRules(p1)
      val (proof2, ws2) = cleanStructuralRules(p2)
      (ws1.contains(a1.formula) && !proof1.root.succedent.exists(fo => fo.formula == a1.formula), 
       ws2.contains(a2.formula) && !proof2.root.antecedent.exists(fo => fo.formula == a2.formula)) match {
        case (true, true) => 
          val ws_ = (ws1.diff(List(a1.formula)) ++ ws2.diff(List(a2.formula))) :+ m.formula
          (proof1, ws_) // The choice for proof1 is arbitrary
        case (true, false) =>
          val ws_ = ws1.diff(List(a1.formula)) ++ ws2
          val p = WeakeningRightRule(proof1, a1.formula)
          ( EquationLeft1Rule(p, proof2, a1.formula, a2.formula, m.formula), ws_ )
        case (false, true) =>
          val ws_ = ws1 ++ ws2.diff(List(a2.formula))
          val p = WeakeningLeftRule(proof2, a2.formula)
          ( EquationLeft1Rule(proof1, p, a1.formula, a2.formula, m.formula), ws_ )
        case (false, false) =>
          val ws_ = ws1 ++ ws2
          ( EquationLeft1Rule(proof1, proof2, a1.formula, a2.formula, m.formula), ws_ )
      }

    case EquationLeft2Rule(p1, p2, _, a1, a2, m) =>
      val (proof1, ws1) = cleanStructuralRules(p1)
      val (proof2, ws2) = cleanStructuralRules(p2)
      (ws1.contains(a1.formula) && !proof1.root.succedent.exists(fo => fo.formula == a1.formula), 
       ws2.contains(a2.formula) && !proof2.root.antecedent.exists(fo => fo.formula == a2.formula)) match {
        case (true, true) => 
          val ws_ = (ws1.diff(List(a1.formula)) ++ ws2.diff(List(a2.formula))) :+ m.formula
          (proof1, ws_) // The choice for proof1 is arbitrary
        case (true, false) =>
          val ws_ = ws1.diff(List(a1.formula)) ++ ws2
          val p = WeakeningRightRule(proof1, a1.formula)
          ( EquationLeft2Rule(p, proof2, a1.formula, a2.formula, m.formula), ws_ )
        case (false, true) =>
          val ws_ = ws1 ++ ws2.diff(List(a2.formula))
          val p = WeakeningLeftRule(proof2, a2.formula)
          ( EquationLeft2Rule(proof1, p, a1.formula, a2.formula, m.formula), ws_ )
        case (false, false) =>
          val ws_ = ws1 ++ ws2
          ( EquationLeft2Rule(proof1, proof2, a1.formula, a2.formula, m.formula), ws_ )
      }

    case EquationRight1Rule(p1, p2, _, a1, a2, m) =>
      val (proof1, ws1) = cleanStructuralRules(p1)
      val (proof2, ws2) = cleanStructuralRules(p2)
      (ws1.contains(a1.formula) && !proof1.root.succedent.exists(fo => fo.formula == a1.formula), 
       ws2.contains(a2.formula) && !proof2.root.succedent.exists(fo => fo.formula == a2.formula)) match {
        case (true, true) => 
          val ws_ = (ws1.diff(List(a1.formula)) ++ ws2.diff(List(a2.formula))) :+ m.formula
          (proof1, ws_) // The choice for proof1 is arbitrary
        case (true, false) =>
          val ws_ = ws1.diff(List(a1.formula)) ++ ws2
          val p = WeakeningRightRule(proof1, a1.formula)
          ( EquationRight1Rule(p, proof2, a1.formula, a2.formula, m.formula), ws_ )
        case (false, true) =>
          val ws_ = ws1 ++ ws2.diff(List(a2.formula))
          val p = WeakeningRightRule(proof2, a2.formula)
          ( EquationRight1Rule(proof1, p, a1.formula, a2.formula, m.formula), ws_ )
        case (false, false) =>
          val ws_ = ws1 ++ ws2
          ( EquationRight1Rule(proof1, proof2, a1.formula, a2.formula, m.formula), ws_ )
      }

    case EquationRight2Rule(p1, p2, _, a1, a2, m) =>
      val (proof1, ws1) = cleanStructuralRules(p1)
      val (proof2, ws2) = cleanStructuralRules(p2)
      (ws1.contains(a1.formula) && !proof1.root.succedent.exists(fo => fo.formula == a1.formula), 
       ws2.contains(a2.formula) && !proof2.root.succedent.exists(fo => fo.formula == a2.formula)) match {
        case (true, true) => 
          val ws_ = (ws1.diff(List(a1.formula)) ++ ws2.diff(List(a2.formula))) :+ m.formula
          (proof1, ws_) // The choice for proof1 is arbitrary
        case (true, false) =>
          val ws_ = ws1.diff(List(a1.formula)) ++ ws2
          val p = WeakeningRightRule(proof1, a1.formula)
          ( EquationRight2Rule(p, proof2, a1.formula, a2.formula, m.formula), ws_ )
        case (false, true) =>
          val ws_ = ws1 ++ ws2.diff(List(a2.formula))
          val p = WeakeningRightRule(proof2, a2.formula)
          ( EquationRight2Rule(proof1, p, a1.formula, a2.formula, m.formula), ws_ )
        case (false, false) =>
          val ws_ = ws1 ++ ws2
          ( EquationRight2Rule(proof1, proof2, a1.formula, a2.formula, m.formula), ws_ )
      }

    case _ => throw new Exception("ERROR: Unexpected case while cleaning redundant structural rules.")
  }
  
  
  private def handle_unary_one_aux_left (proof: LKProof, 
                                    ws: List[HOLFormula], 
                                    aux: HOLFormula, 
                                    m: HOLFormula,
                                    rule: ((LKProof, HOLFormula, HOLFormula) => LKProof) ) 
  : (LKProof, List[HOLFormula]) = 
    ws.contains(aux) && !proof.root.antecedent.exists(fo => fo.formula == aux) match {
      case true => (proof, ws.diff(List(aux)) :+ m)
      case false => (rule(proof, aux, m), ws)
    }

  private def handle_unary_one_aux_right (proof: LKProof, 
                                    ws: List[HOLFormula], 
                                    aux: HOLFormula, 
                                    m: HOLFormula,
                                    rule: ((LKProof, HOLFormula, HOLFormula) => LKProof) ) 
  : (LKProof, List[HOLFormula]) = 
    ws.contains(aux) && !proof.root.succedent.exists(fo => fo.formula == aux) match {
      case true => (proof, ws.diff(List(aux)) :+ m)
      case false => (rule(proof, aux, m), ws)
    }
}
