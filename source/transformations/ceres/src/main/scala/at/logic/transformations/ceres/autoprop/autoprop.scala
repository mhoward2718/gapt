package at.logic.transformations.ceres.autoprop

import at.logic.algorithms.lk.getAncestors
import at.logic.calculi.lk.base._
import at.logic.calculi.lk.base.types.FSequent
import at.logic.calculi.lk.lkExtractors.{UnaryLKProof, BinaryLKProof}
import at.logic.calculi.lk.macroRules._
import at.logic.calculi.lk.propositionalRules._
import at.logic.calculi.occurrences.{FormulaOccurrence, defaultFormulaOccurrenceFactory}
import at.logic.calculi.slk._
import at.logic.calculi.slk.AndEquivalenceRule1._
import at.logic.language.hol.logicSymbols.ConstantStringSymbol
import at.logic.language.hol.{Atom, HOLExpression, HOLFormula}
import at.logic.language.lambda.symbols.VariableStringSymbol
import at.logic.language.lambda.typedLambdaCalculus.Var
import at.logic.language.schema._
import at.logic.transformations.ceres.projections.printSchemaProof
import at.logic.transformations.ceres.unfolding.{StepMinusOne, SchemaSubstitution1}
import at.logic.utils.ds.trees.LeafTree
import collection.immutable
import at.logic.parsing.language.simple.SHLK

// continue autopropositional
object Autoprop {
  // This method is used in prooftool to test autopropositional feature.
  def apply(s: String): List[LKProof] = if (s.isEmpty) {
    val auto1 = apply1(test.apply())
    val auto2 = StructuralOptimizationAfterAutoprop(auto1)
//    val auto3 = StructuralOptimizationAfterAutoprop(auto2)
//    val auto = apply(test.apply())
    List(auto1,auto2)//,auto3,auto)
  } else {
    val seq = SHLK.parseSequent(s)
    apply( seq ) :: Nil
  }

  def apply(seq: FSequent): LKProof = {
    var p = apply1(seq)
    while (rulesNumber(p) != rulesNumber(StructuralOptimizationAfterAutoprop(p)))
      p = StructuralOptimizationAfterAutoprop(p)
    p
  }

  def apply1(seq: FSequent): LKProof = {
    if (isSeqTautology(seq)) {
      val (f, rest) = getAxiomfromSeq(seq)
      return WeakeningRuleN(Axiom(f::Nil, f::Nil), rest)
    }
    if (getNonAtomicFAnt(seq) != None) {
      val f = getNonAtomicFAnt(seq).get._1
//      println("\nant f = "+printSchemaProof.formulaToString(f) )
      val rest = getNonAtomicFAnt(seq).get._2
  //    println("\nrest = "+rest )
      f match {
        case Neg(f1) => return NegLeftRule(apply1(new FSequent(rest.antecedent, f1.asInstanceOf[HOLFormula] +: rest.succedent)), f1.asInstanceOf[HOLFormula])
        case Imp(f1, f2)=> {
          return ImpLeftRule(apply1(new FSequent(rest.antecedent, f1.asInstanceOf[HOLFormula] +: rest.succedent)), apply1(new FSequent(f2.asInstanceOf[HOLFormula] +: rest.antecedent, rest.succedent)), f1.asInstanceOf[HOLFormula], f2.asInstanceOf[HOLFormula])
        }
        case And(f1, f2) => {
          val up1 = AndLeft1Rule(apply1(new FSequent(f1 +: f2 +: rest.antecedent, rest.succedent)), f1, f2)
          val up2 = AndLeft2Rule(up1, f1, f2)
          return ContractionLeftRule(up2, f)
        }
        case Or(f1, f2) => {
          val t1 = apply1(new FSequent(f1.asInstanceOf[HOLFormula] +: rest.antecedent, rest.succedent))
          val t2 = apply1(new FSequent(f2.asInstanceOf[HOLFormula] +: rest.antecedent, rest.succedent))
          val up = OrLeftRule(t1, t2, f1.asInstanceOf[HOLFormula], f2.asInstanceOf[HOLFormula])
          return ContractionRuleN(up, rest)
        }
        case BigAnd(i, iter, from, to) => {
          val i = IntVar(new VariableStringSymbol("i"))
          if (from == to) {
            val new_map = scala.collection.immutable.Map[Var, HOLExpression]() + Pair(i, to)
            val subst = new SchemaSubstitution1[HOLExpression](new_map)
            return AndLeftEquivalenceRule3(apply1(new FSequent(subst(iter).asInstanceOf[SchemaFormula] +: rest.antecedent, rest.succedent)), subst(iter).asInstanceOf[SchemaFormula], f.asInstanceOf[SchemaFormula])
          }
          else {
            val new_map = scala.collection.immutable.Map[Var, HOLExpression]() + Pair(i, to)
            val subst = new SchemaSubstitution1[HOLExpression](new_map)
            val up = AndLeftRule(apply1(new FSequent(BigAnd(i, iter, from, Pred(to)) +: subst(iter).asInstanceOf[HOLFormula] +:  rest.antecedent, rest.succedent)), BigAnd(i, iter, from, Pred(to)), subst(iter).asInstanceOf[HOLFormula])
            return AndLeftEquivalenceRule1(up, And(BigAnd(i, iter, from, Pred(to)), subst(iter).asInstanceOf[SchemaFormula]), BigAnd(i, iter, from, to))
          }
        }
        case BigOr(i, iter, from, to) => {
          val i = IntVar(new VariableStringSymbol("i"))
          if (from == to){
            val new_map = scala.collection.immutable.Map[Var, HOLExpression]() + Pair(i, to)
            val subst = new SchemaSubstitution1[HOLExpression](new_map)
            return OrLeftEquivalenceRule3(apply1(new FSequent(subst(iter).asInstanceOf[SchemaFormula] +: rest.antecedent, rest.succedent)), subst(iter).asInstanceOf[SchemaFormula], f.asInstanceOf[SchemaFormula])
          }
          else {
            val new_map = scala.collection.immutable.Map[Var, HOLExpression]() + Pair(i, to)
            val subst = new SchemaSubstitution1[HOLExpression](new_map)
            val up = OrLeftRule(apply1(new FSequent(BigOr(i, iter, from, Pred(to)) +:  rest.antecedent, rest.succedent)), apply1(new FSequent(subst(iter).asInstanceOf[HOLFormula] +:  rest.antecedent, rest.succedent)), BigOr(i, iter, from, Pred(to)), subst(iter).asInstanceOf[HOLFormula])
            val up1 = ContractionRuleN(up, rest)
            return OrLeftEquivalenceRule1(up1, Or(BigOr(i, iter, from, Pred(to)), subst(iter).asInstanceOf[SchemaFormula]), BigOr(i, iter, from, to))
          }
        }
        case _ => throw new Exception("Error in ANT-case in Autoprop.apply1 !\n")
      }
    }

    if (getNonAtomicFSucc(seq) == None)
      throw new Exception("\nError in Autoprop SUCC !\n")
    val f = getNonAtomicFSucc(seq).get._1
//    println("\nsucc f = "+printSchemaProof.formulaToString(f) )
    val rest = getNonAtomicFSucc(seq).get._2
    f match {
      case Neg(f1) => return NegRightRule(apply1(new FSequent(f1.asInstanceOf[HOLFormula] +: rest.antecedent, rest.succedent)), f1.asInstanceOf[HOLFormula])
      case Imp(f1, f2)=> {
        return ImpRightRule(apply1(new FSequent(f1.asInstanceOf[HOLFormula] +: rest.antecedent, f2.asInstanceOf[HOLFormula] +: rest.succedent)), f1.asInstanceOf[HOLFormula], f2.asInstanceOf[HOLFormula])
      }
      case Or(f1, f2) => {
        val up1 = OrRight1Rule(apply1(new FSequent(rest.antecedent, f1.asInstanceOf[HOLFormula] +: f2.asInstanceOf[HOLFormula] +: rest.succedent)), f1.asInstanceOf[HOLFormula], f2.asInstanceOf[HOLFormula])
        val up2 = OrRight2Rule(up1, f1.asInstanceOf[HOLFormula], f2.asInstanceOf[HOLFormula])
        return ContractionRightRule(up2, f)
      }
      case And(f1, f2) => {
        val t1 = apply1(new FSequent(rest.antecedent, f1 +: rest.succedent))
        val t2 = apply1(new FSequent(rest.antecedent, f2 +: rest.succedent))
        val up = AndRightRule(t1, t2, f1, f2)
//        print("\nsucc And(f1, f2) = ")
//        println (printSchemaProof.formulaToString(f))
//        println(printSchemaProof.sequentToString(up.root))
        return ContractionRuleN(up, rest)
      }
      case BigAnd(i, iter, from, to) => {
        val i = IntVar(new VariableStringSymbol("i"))
        if (from == to) {
          val new_map = scala.collection.immutable.Map[Var, HOLExpression]() + Pair(i, to)
          val subst = new SchemaSubstitution1[HOLExpression](new_map)
          return AndRightEquivalenceRule3(apply1(new FSequent(rest.antecedent, subst(iter).asInstanceOf[SchemaFormula] +: rest.succedent)), subst(iter).asInstanceOf[SchemaFormula], f.asInstanceOf[SchemaFormula])
        }
        else {
          val new_map = scala.collection.immutable.Map[Var, HOLExpression]() + Pair(i, to)
          val subst = new SchemaSubstitution1[HOLExpression](new_map)
          val up = AndRightRule(apply1(new FSequent(rest.antecedent, BigAnd(i, iter, from, Pred(to)) +: rest.succedent)), apply1(new FSequent(rest.antecedent, subst(iter).asInstanceOf[HOLFormula] +: rest.succedent)), BigAnd(i, iter, from, Pred(to)), subst(iter).asInstanceOf[HOLFormula])
          val up1 = ContractionRuleN(up, rest)
          return AndRightEquivalenceRule1(up1, And(BigAnd(i, iter, from, Pred(to)), subst(iter).asInstanceOf[SchemaFormula]), BigAnd(i, iter, from, to))
        }
      }
      case BigOr(i, iter, from, to) => {
        val i = IntVar(new VariableStringSymbol("i"))
        if (from == to){
          val new_map = scala.collection.immutable.Map[Var, HOLExpression]() + Pair(i, to)
          val subst = new SchemaSubstitution1[HOLExpression](new_map)
          return OrRightEquivalenceRule3(apply1(new FSequent(subst(iter).asInstanceOf[SchemaFormula] +: rest.antecedent, rest.succedent)), subst(iter).asInstanceOf[SchemaFormula], f.asInstanceOf[SchemaFormula])
        }
        else {
          val new_map = scala.collection.immutable.Map[Var, HOLExpression]() + Pair(i, to)
          val subst = new SchemaSubstitution1[HOLExpression](new_map)
          val up = OrRightRule(apply1(new FSequent(rest.antecedent, BigOr(i, iter, from, Pred(to)) +: subst(iter).asInstanceOf[HOLFormula] +: rest.succedent)), BigOr(i, iter, from, Pred(to)), subst(iter).asInstanceOf[HOLFormula])
          return OrRightEquivalenceRule1(up, Or(BigOr(i, iter, from, Pred(to)), subst(iter).asInstanceOf[SchemaFormula]), BigOr(i, iter, from, to))
        }
      }
      case _ => throw new Exception("Error in SUCC-case in Autoprop.apply1 !\n")
    }
    throw new Exception("Error in Autoprop - missing case !")
  }

//  def ContractionRuleN(p : LKProof, seq: FSequent) : LKProof = {
////    println("\nContrN proof:\n"+printSchemaProof(p))
////    val up = seq.antecedent.foldLeft(p)((res, f) => ContractionLeftRule(res, f))
////    seq.succedent.foldLeft(up)((res, f) => {
////      println("contracted f-la right = "+printSchemaProof.formulaToString (f))
////      ContractionRightRule(res, f)
////    })
//  }

    def getListOfFormulasToContractAnt(seq: FSequent): Set[HOLFormula] = {
      seq.antecedent.filter(f => seq.antecedent.count(x => x == f) > 1).toSet
    }
    def getListOfFormulasToContractSucc(seq: FSequent): Set[HOLFormula] = {
      seq.succedent.filter(f => seq.succedent.count(x => x == f) > 1).toSet
    }

    def ContractionRuleN(p : LKProof, seq: FSequent) : LKProof = {
//      println("\nContrN proof:\n"+printSchemaProof(p))
      var l1 = getListOfFormulasToContractAnt(p.root.toFSequent()).toList
      var up = p
      while(l1.length > 0) {
//        println("\n\n ANT\n\n")
        up = l1.foldLeft(up)((res, f) => ContractionLeftRule(res, f))
        l1 = getListOfFormulasToContractAnt(up.root.toFSequent()).toList
      }
      var l2 = getListOfFormulasToContractSucc(p.root.toFSequent()).toList
//      println("\n\n\n\n\n")

//      seq.succedent.foreach(f => println("fseq = "+printSchemaProof.formulaToString(f) ))
      var up2 = up
      var i=1
      while(l2.length > 0) {
//        println("\n\n i = "+i)
        i = i+1
//        l2.foreach(f => println(printSchemaProof.formulaToString(f)))
//        print("seq = ")
//        println(printSchemaProof.sequentToString(up2.root))
        up2 = l2.foldLeft(up2)((res, f) => {
//          println("\napply contrr to = " +printSchemaProof.sequentToString(up2.root) )
//          println("\nformula = "+printSchemaProof.formulaToString(f) )
          ContractionRightRule(res, f)
        })
        l2 = getListOfFormulasToContractSucc(up2.root.toFSequent()).toList
      }
      up2
    }

//    def ContractionRuleN(p : LKProof, seq: FSequent) : LKProof = {
//      println("\nContrN proof:\n"+printSchemaProof(p))
//      val up = seq.antecedent.foldLeft(p)((res, f) => ContractionLeftRule(res, f))
//      seq.succedent.foldLeft(up)((res, f) => ContractionRightRule(res, f))
//    }

  def WeakeningRuleN(p : LKProof, seq: FSequent) : LKProof = {
    val up = seq.antecedent.foldLeft(p)((res, f) => WeakeningLeftRule(res, f))
    seq.succedent.foldLeft(up)((res, f) => WeakeningRightRule(res, f))
  }

  //return the first non Atomic f-la and the subsequent without that f-la
  def getNonAtomicFAnt(seq: FSequent) : Option[(HOLFormula, FSequent)] = {
    seq.antecedent.foreach(f => f match {
      case IndexedPredicate(_, _) => {}
//      case Atom(_, _) => {}
      case _ => return Some(f, removeFfromSeqAnt(seq, f))
    })
    None
  }

  def getNonAtomicFSucc(seq: FSequent) : Option[(HOLFormula, FSequent)] = {
    seq.succedent.foreach(f => f match {
      case IndexedPredicate(_, _) => {}
//      case Atom(_, _) => {}
      case _ => return Some(f, removeFfromSeqSucc(seq, f))
    })
    None
  }
  
  def isAtom(f: HOLFormula): Boolean = f match {
    case IndexedPredicate(_, _) => true
    case Atom(_, _) => true
    case _ => false
  }
  
  def isSeqTautology(seq: FSequent): Boolean = {
    seq.antecedent.foreach(f => seq.succedent.foreach(f2 =>  if(f == f2 && isAtom(f)) return true))
      return false
  }
  
  def removeFfromSeqAnt(seq: FSequent, f : HOLFormula) : FSequent = {
    new FSequent(seq.antecedent.filter(x => x != f) , seq.succedent)
  }

  def removeFfromSeqSucc(seq: FSequent, f : HOLFormula) : FSequent = {
    new FSequent(seq.antecedent, seq.succedent.filter(x => x != f))
  }

  def removeFfromSeqAnt(seq: FSequent, flist : List[HOLFormula]) : FSequent = {
    new FSequent(seq.antecedent.filter(x => !flist.contains(x)) , seq.succedent)
  }

  def removeFfromSeqSucc(seq: FSequent, flist : List[HOLFormula]) : FSequent = {
    new FSequent(seq.antecedent, seq.succedent.filter(x => !flist.contains(x)))
  }
  
  def getAxiomfromSeq(seq: FSequent) : (HOLFormula, FSequent) = {
    if (isSeqTautology(seq)) {
      seq.antecedent.foreach(f => if (seq.succedent.contains(f)){
        return (f, removeFfromSeqAnt(removeFfromSeqSucc(seq, f), f))
      })
      throw new Exception("\nError in if-autoprop.getAxiomfromSeq !\n")
    }
    else throw new Exception("\nError in else-autoprop.getAxiomfromSeq !\n")
  }
}



//delete from an SLKProof those weakening inefernces whose aux. f-las go to a contraction inference
object StructuralOptimizationAfterAutoprop {
  def apply(p: LKProof): LKProof = apply(p, p)
  
  def removeNonWeakDesc(anc: Set[FormulaOccurrence], ws: Set[FormulaOccurrence]): Set[FormulaOccurrence] = {
    anc.filter(fo => !(getAncestors(fo).intersect(ws)).isEmpty)
  }

  def apply(p : LKProof, p_old : LKProof): LKProof = p match {
    case ax: NullaryLKProof => p
    case ContractionLeftRule(up, _, a1, a2, _)  => {
      val anc1 = getAncestors(a1);val anc2 = getAncestors(a2)
      val b1 = isDescentanfOfAuxFOccOfWeakRule(anc1, p_old)
      val b2 = isDescentanfOfAuxFOccOfWeakRule(anc2, p_old)
      val wfo = getWeakFOccs(up)
      if ((b1 || b2) && isUpperMostContr(up)) {
        val p1 = delSuperfluousRules(removeNonWeakDesc(anc1 ++ anc2, wfo), up)

//        println("\na1 = "+printSchemaProof.formulaToString (a1))
//        println("\n1\n")
//        println("\np1 = \n")
//        println(printSchemaProof(p1))
        p1
      }
      else  {
        val p2 = apply(up, p_old)
//        println("\n2 = \n")
//        println("\na1 = "+printSchemaProof.formulaToString (a1.formula))

//        println(printSchemaProof(p2))

        if (p2.root.antecedent.filter(fo => fo.formula == a1.formula).size < 2)
          return p2
        return ContractionLeftRule(p2, a1.formula)
      }
//
//      else
//        if (b1) {
//                    println("\n2 left\n")
//          return delSuperfluousRules(removeNonWeakDesc(anc1, wfo), up)
//        }
//        else
//          if (b2) {
//                        println("\n3 left\n")
//            delSuperfluousRules(removeNonWeakDesc(anc2, wfo), up)
//          }
//          else {
//                        println("\n4 left\n")
//            ContractionLeftRule(apply(up, p_old), a1.formula)
//          }
    }
    case ContractionRightRule(up, _, a1, a2, _) => {
      val anc1 = getAncestors(a1);val anc2 = getAncestors(a2)
      val b1 = isDescentanfOfAuxFOccOfWeakRule(anc1, p_old)
      val b2 = isDescentanfOfAuxFOccOfWeakRule(anc2, p_old)
      val wfo = getWeakFOccs(p_old)
      if ((b1 || b2) && isUpperMostContr(up)) {
//        println("\n1\n")
        val p1 = delSuperfluousRules(removeNonWeakDesc(anc1 ++ anc2, wfo), up)
//        if (!removeNonWeakDesc(getAncestors(a1), wfo).isEmpty) {
//          println("\n1 right if\n")
        p1
      }
      else {
        val p2 = apply(up, p_old)
        if (p2.root.succedent.filter(fo => fo.formula == a1.formula).size < 2)
          return p2
        return ContractionRightRule(p2, a1.formula)
      }
    }
    case AndLeftEquivalenceRule1(p, s, a, m) => {
      //            println("\nAndLeftEquivalenceRule1   YESSSSSSSSSSS \n")
      val new_p = apply(p, p_old)
      AndLeftEquivalenceRule1(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
    }
    case AndRightEquivalenceRule1(p, s, a, m) => {
      // println("\nAndRightEquivalenceRule1\n")
      val new_p = apply(p, p_old)
      AndRightEquivalenceRule1(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
    }
    case OrLeftEquivalenceRule1(p, s, a, m) => {
      val new_p = apply(p, p_old)
      OrLeftEquivalenceRule1(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
    }
    case OrRightEquivalenceRule1(p, s, a, m) => {
      // println("\nOrRightEquivalenceRule1\n")
      val new_p = apply(p, p_old)
      OrRightEquivalenceRule1(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
    }
    case AndLeftEquivalenceRule3(p, s, a, m) => {
      // println("\nAndLeftEquivalenceRule3\n")
      val new_p = apply(p, p_old)
      AndLeftEquivalenceRule3(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
    }
    case AndRightEquivalenceRule3(p, s, a, m) => {
      // println("\nAndRightEquivalenceRule3\n")
      val new_p = apply(p, p_old)
      AndRightEquivalenceRule3(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
    }
    case OrLeftEquivalenceRule3(p, s, a, m) => {
      val new_p = apply(p, p_old)
      OrLeftEquivalenceRule3(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
    }
    case OrRightEquivalenceRule3(p, s, a, m) => {
      //println("\nOrRightEquivalenceRule3\n")
      val new_p = apply(p, p_old)
      OrRightEquivalenceRule3(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
    }
    case WeakeningLeftRule(p, _, m) => {
      val new_p = apply(p, p_old)
      implicit val factory = defaultFormulaOccurrenceFactory
      WeakeningLeftRule( new_p, m.formula )
    }
    case WeakeningRightRule(p, _, m) => {
      val new_p = apply(p, p_old)
      implicit val factory = defaultFormulaOccurrenceFactory
      WeakeningRightRule( new_p, m.formula )
    }
    case OrLeftRule(p1, p2, _, a1, a2, m) => {
      val new_p1 = apply(p1, p_old)
      val new_p2 = apply(p2, p_old)
      OrLeftRule(new_p1, new_p2, a1.formula, a2.formula)
    }
    case AndRightRule(p1, p2, _, a1, a2, m) => {
      val new_p1 = apply(p1, p_old)
      val new_p2 = apply(p2, p_old)
      AndRightRule(new_p1, new_p2, a1.formula, a2.formula)
    }
    case NegLeftRule( p, _, a, m ) => {
      val new_p = apply(p, p_old)
      NegLeftRule( new_p, a.formula )
    }
    case AndLeft1Rule(p, r, a, m) =>  {
      val new_p = apply(p, p_old)
      val a2 = m.formula  match { case And(l, right) => right }
      //      println("AndLeft1Rule : "+printSchemaProof.sequentToString(new_p.root))
      //     println("aux : \n"+printSchemaProof.formulaToString(a.formula))
      //    println(printSchemaProof.formulaToString(a2))
      AndLeft1Rule( new_p, a.formula, a2)
    }
    case AndLeft2Rule(p, r, a, m) =>  {
      val new_p = apply(p, p_old)
      val a2 = m.formula  match { case And(l, _) => l }
      //     println("AndLeft2Rule : "+printSchemaProof.sequentToString(new_p.root))
      //     println("aux : \n"+printSchemaProof.formulaToString(a.formula))
      //     println(printSchemaProof.formulaToString(a2))
      AndLeft2Rule( new_p, a2, a.formula )
    }
    case OrRight1Rule(p, r, a, m) =>  {
      val new_p = apply(p, p_old)
      val a2 = m.formula  match { case Or(_, r) => r }
      //            println("\np or:r1 = "+p.root)
      //            println("\nnew_p or:r1 = "+new_p.root)
      //            println("\nor:r1 a = "+a.formula)
      //            println("\nor:r1 m = "+m.formula)
      OrRight1Rule( new_p, a.formula, a2.asInstanceOf[HOLFormula])
    }
    case OrRight2Rule(p, r, a, m) =>  {
      val new_p = apply(p, p_old)
      val a2 = m.formula  match { case Or(l, _) => l }
      //            println("\np or:r2 = "+p.root)
      //            println("\nnew_p or:r2 = "+new_p.root)
      //          println("\nor:r2 a = "+a.formula)
      //            println("\nor:r2 m = "+m.formula)
      OrRight2Rule( new_p, a2.asInstanceOf[HOLFormula], a.formula)
    }
    case NegRightRule( p, _, a, m ) => {
      val new_p = apply(p, p_old)
      NegRightRule( new_p, a.formula )
    }
    case ImpLeftRule(p1, p2, seq, a1, a2, _) =>{
      val new_p1 = apply(p1, p_old)
      val new_p2 = apply(p2, p_old)
      ImpLeftRule(new_p1, new_p2, a1.formula, a2.formula)
    }
    case ImpRightRule(p, _, a1, a2, m ) => {
      val new_p = apply(p, p_old)
      ImpRightRule(new_p, a1.formula, a2.formula )
    }
    case _ => { println("ERROR in StructuralOptimizationAfterAutoprop : missing rule!");throw new Exception("ERROR in autoprop: StructuralOptimizationAfterAutoprop") }
  }   
}


//**************************************************************************

  //getDescOfAuxFOccOfWeakRule
object getWeakFOccs {
  def apply(p: LKProof): Set[FormulaOccurrence] = {
    p match {
      case ax: NullaryLKProof => return Set.empty[FormulaOccurrence]
      case WeakeningLeftRule(up, _, m) => {
        return apply(up) + m
      }
      case WeakeningRightRule(up, _, m) => {
        return apply(up) + m
      }
      case UnaryLKProof(_, up, _, _, _) => return apply(up)
      case BinaryLKProof(_, up1, up2, _, _, _, _) => return apply(up1) ++ apply (up2)
      case AndEquivalenceRule1(up, _, _, _) => return apply(up)
      case OrEquivalenceRule1(up, _, _, _) => return apply(up)
      case AndEquivalenceRule3(up, _, _, _) => return apply(up)
      case OrEquivalenceRule3(up, _, _, _) => return apply(up)
      case _ => { println("ERROR in getWeakFOccs : missing rule!");throw new Exception("ERROR in autoprop: getWeakFOccs") }
    }
  }
}

object isUpperMostContr {
  def apply(p: LKProof):Boolean = (contrNumber(p) == 0)
  
  def contrNumber(p: LKProof): Int = p match {
    case ax: NullaryLKProof => return 0
    case ContractionLeftRule(up, _, _, _, _) => return contrNumber(up) + 1
    case ContractionRightRule(up, _, _, _, _) =>return contrNumber(up) + 1
    case UnaryLKProof(_, up, _, _, _) => return contrNumber(up)
    case BinaryLKProof(_, up1, up2, _, _, _, _) => return contrNumber(up1) + contrNumber (up2)
    case AndEquivalenceRule1(up, _, _, _) => return contrNumber(up)
    case OrEquivalenceRule1(up, _, _, _) => return contrNumber(up)
    case AndEquivalenceRule3(up, _, _, _) => return contrNumber(up)
    case OrEquivalenceRule3(up, _, _, _) => return contrNumber(up)
    case _ => { println("ERROR in getWeakFOccs : missing rule!");throw new Exception("ERROR in autoprop: getWeakFOccs") }
  }
}


object isDescentanfOfAuxFOccOfWeakRule {
  def apply(s: Set[FormulaOccurrence], p:LKProof): Boolean = {
//    println("\nrule = "+p.name)
    p match {
      case ax: NullaryLKProof => false
      case WeakeningLeftRule(up, _, m) => {
        if (s.contains(m))
          true
        else
          apply(s, up)
      }
      case WeakeningRightRule(up, _, m) => {
        if (s.contains(m))
          true
        else
          apply(s, up)
      }
      case UnaryLKProof(_, up, _, _, _) => apply(s, up)
      case BinaryLKProof(_, up1, up2, _, _, _, _) => apply(s, up1) || apply (s, up2)
      case AndEquivalenceRule1(up, _, _, _) => apply(s, up)
      case OrEquivalenceRule1(up, _, _, _) => apply(s, up)
      case AndEquivalenceRule3(up, _, _, _) => apply(s, up)
      case OrEquivalenceRule3(up, _, _, _) => apply(s, up)
      case _ => { println("ERROR in isDescentanfOfAuxFOccOfWeakRule : missing rule!");throw new Exception("ERROR in autoprop: StructuralOptimizationAfterAutoprop") }
    }
  }
}


// *************************************************************************
object delSuperfluousRules {
  def apply(set: Set[FormulaOccurrence], p_old: LKProof): LKProof = {
//    println("\n\ndelSuperfluousWeakening  -  "+p_old.name)
    p_old match {
      case ax: NullaryLKProof => p_old
      case AndLeftEquivalenceRule1(p, s, a, m) => {
        val new_p = apply(set, p)
        AndLeftEquivalenceRule1(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
      }
      case AndRightEquivalenceRule1(p, s, a, m) => {
        // println("\nAndRightEquivalenceRule1\n")
        val new_p = apply(set, p)
        AndRightEquivalenceRule1(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
      }
      case OrRightEquivalenceRule1(p, s, a, m) => {
        // println("\nOrRightEquivalenceRule1\n")
        val new_p = apply(set, p)
        OrRightEquivalenceRule1(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
      }
      case OrLeftEquivalenceRule1(p, s, a, m) => {
        val new_p = apply(set, p)
        OrLeftEquivalenceRule1(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
      }
      case AndLeftEquivalenceRule3(p, s, a, m) => {
        // println("\nAndLeftEquivalenceRule3\n")
        val new_p = apply(set, p)
        AndLeftEquivalenceRule3(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
      }
      case AndRightEquivalenceRule3(p, s, a, m) => {
        // println("\nAndRightEquivalenceRule3\n")
        val new_p = apply(set, p)
        AndRightEquivalenceRule3(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
      }
      case OrLeftEquivalenceRule3(p, s, a, m) => {
        //println("\nOrLeftEquivalenceRule3\n")
        val new_p = apply(set, p)
        OrLeftEquivalenceRule3(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
      }
      case OrRightEquivalenceRule3(p, s, a, m) => {
        //println("\nOrRightEquivalenceRule3\n")
        val new_p = apply(set, p)
        OrRightEquivalenceRule3(new_p, a.formula.asInstanceOf[SchemaFormula], m.formula.asInstanceOf[SchemaFormula])
      }
      case WeakeningLeftRule(p, _, m) => {
        if (set.contains(m))
          return apply(set, p)
        val new_p = apply(set, p)
        implicit val factory = defaultFormulaOccurrenceFactory
        WeakeningLeftRule( new_p, m.formula )
      }
      case WeakeningRightRule(p, _, m) => {
        if (set.contains(m))
          return apply(set, p)
        val new_p = apply(set, p)
        implicit val factory = defaultFormulaOccurrenceFactory
        WeakeningRightRule( new_p, m.formula )
      }
      case OrLeftRule(p1, p2, _, a1, a2, m) => {
        if (set.contains(a1))
          return apply(set, p2)
        if (set.contains(a2))
          return apply(set, p1)
        val new_p1 = apply(set, p1)
        val new_p2 = apply(set, p2)
        OrLeftRule(new_p1, new_p2, a1.formula, a2.formula)
      }
      case AndRightRule(p1, p2, _, a1, a2, m) => {
        if (set.contains(a1))
          return apply(set, p2)
        if (set.contains(a2))
          return apply(set, p1)
        val new_p1 = apply(set, p1)
        val new_p2 = apply(set, p2)
        AndRightRule(new_p1, new_p2, a1.formula, a2.formula)
      }
      case NegLeftRule( p, _, a, m ) => {
        if (set.contains(a))
          return apply(set, p)
        val new_p = apply(set, p)
        NegLeftRule( new_p, a.formula )
      }
      case AndLeft1Rule(p, r, a, m) =>  {
        if (set.contains(a))
          return apply(set, p)
        val new_p = apply(set, p)
        val a2 = m.formula  match { case And(l, right) => right }
        //      println("AndLeft1Rule : "+printSchemaProof.sequentToString(new_p.root))
        //     println("aux : \n"+printSchemaProof.formulaToString(a.formula))
        //    println(printSchemaProof.formulaToString(a2))
        AndLeft1Rule( new_p, a.formula, a2)
      }
      case AndLeft2Rule(p, r, a, m) =>  {
        if (set.contains(m))
          return apply(set, p)
        val new_p = apply(set, p)
        val a2 = m.formula  match { case And(l, _) => l }
        //     println("AndLeft2Rule : "+printSchemaProof.sequentToString(new_p.root))
        //     println("aux : \n"+printSchemaProof.formulaToString(a.formula))
        //     println(printSchemaProof.formulaToString(a2))
        AndLeft2Rule( new_p, a2, a.formula )
      }
      case OrRight1Rule(p, r, a, m) =>  {
        if (set.contains(m))
          return apply(set, p)
        val new_p = apply(set, p)
        val a2 = m.formula  match { case Or(_, r) => r }
        //            println("\np or:r1 = "+p.root)
        //            println("\nnew_p or:r1 = "+new_p.root)
        //            println("\nor:r1 a = "+a.formula)
        //            println("\nor:r1 m = "+m.formula)
        OrRight1Rule( new_p, a.formula, a2.asInstanceOf[HOLFormula])
      }
      case OrRight2Rule(p, r, a, m) =>  {
        if (set.contains(m))
          return apply(set, p)
        val new_p = apply(set, p)
        val a2 = m.formula  match { case Or(l, _) => l }
        //            println("\np or:r2 = "+p.root)
        //            println("\nnew_p or:r2 = "+new_p.root)
        //          println("\nor:r2 a = "+a.formula)
        //            println("\nor:r2 m = "+m.formula)
        OrRight2Rule( new_p, a2.asInstanceOf[HOLFormula], a.formula)
      }
      case NegRightRule( p, _, a, m ) => {
        if (set.contains(a))
          return apply(set, p)
        val new_p = apply(set, p)
        NegRightRule( new_p, a.formula )
      }
      case ContractionLeftRule(p, _, a1, a2, m) => {
        if (set.contains(m))
          return apply(set, p)
        val new_p = apply(set, p)
        ContractionLeftRule( new_p, a1.formula )
      }
      case ContractionRightRule(p, _, a1, a2, m) => {
        if (set.contains(m))
          return apply(set, p)
        val new_p = apply(set, p)
        //            println("\nc:r = "+new_p.root)
        ContractionRightRule( new_p, a1.formula )
      }
      case ImpLeftRule(p1, p2, seq, a1, a2, _) =>{
        if (set.contains(a1))
          return apply(set, p2)
        if (set.contains(a2))
          return apply(set, p1)
        val new_p1 = apply(set, p1)
        val new_p2 = apply(set, p2)
        ImpLeftRule(new_p1, new_p2, a1.formula, a2.formula)
      }
      case ImpRightRule(p, _, a1, a2, m ) => {
        if (set.contains(m))
          return apply(set, p)
        val new_p = apply(set, p)
        ImpRightRule(new_p, a1.formula, a2.formula )
      }
      case _ => { println("ERROR in delSuperfluousWeakening : missing rule!");throw new Exception("ERROR in delSuperfluousWeakening") }
    }
  }
}

//**************************************************************************
object rulesNumber {
  def apply(p: LKProof) : Int = p match {
    case ax: NullaryLKProof  => 0
    case BinaryLKProof(_, p1, p2, _, _, _, _) => apply(p1) + apply(p2) + 1
    case UnaryLKProof(_, p, _, _, _) => apply(p) + 1
    case AndEquivalenceRule1(up, _, _, _) => apply(up) + 1
    case OrEquivalenceRule1(up, _, _, _) => apply(up) + 1
    case AndEquivalenceRule3(up, _, _, _) => apply(up) + 1
    case OrEquivalenceRule3(up, _, _, _) => apply(up) + 1
    case _ => { println("ERROR in delSuperfluousWeakening : missing rule!");throw new Exception("ERROR in rulesNumber") }
  }
}

object test {
  def apply(): FSequent = {
    val k = IntVar(new VariableStringSymbol("k"))
    val real_n = IntVar(new VariableStringSymbol("n"))
    val n = k
    val n1 = Succ(k); val n2 = Succ(n1); val n3 = Succ(n2)
    val k1 = Succ(k); val k2 = Succ(n1); val k3 = Succ(n2)
    val s = Set[FormulaOccurrence]()

    val i = IntVar(new VariableStringSymbol("i"))
    val A = IndexedPredicate(new ConstantStringSymbol("A"), i)
    val B = IndexedPredicate(new ConstantStringSymbol("B"), i)
    val C = IndexedPredicate(new ConstantStringSymbol("C"), i)
    val zero = IntZero(); val one = Succ(IntZero()); val two = Succ(Succ(IntZero())); val three = Succ(Succ(Succ(IntZero())))
    val four = Succ(three);val five = Succ(four); val six = Succ(Succ(four));val seven = Succ(Succ(five));       val A0 = IndexedPredicate(new ConstantStringSymbol("A"), IntZero())
    val A1 = IndexedPredicate(new ConstantStringSymbol("A"), one)
    val A2 = IndexedPredicate(new ConstantStringSymbol("A"), two)
    val A3 = IndexedPredicate(new ConstantStringSymbol("A"), three)

    val B0 = IndexedPredicate(new ConstantStringSymbol("B"), IntZero())

    val Ak = IndexedPredicate(new ConstantStringSymbol("A"), k)
    val Ai = IndexedPredicate(new ConstantStringSymbol("A"), i)
    val Ai1 = IndexedPredicate(new ConstantStringSymbol("A"), Succ(i))
    val orneg = at.logic.language.schema.Or(at.logic.language.schema.Neg(Ai).asInstanceOf[SchemaFormula], Ai1.asInstanceOf[SchemaFormula]).asInstanceOf[SchemaFormula]

    val Ak1 = IndexedPredicate(new ConstantStringSymbol("A"), Succ(k))
    val An = IndexedPredicate(new ConstantStringSymbol("A"), k)
    val An1 = IndexedPredicate(new ConstantStringSymbol("A"), n1)
    val An2 = IndexedPredicate(new ConstantStringSymbol("A"), n2)
    val An3 = IndexedPredicate(new ConstantStringSymbol("A"), n3)
    //             println("\n\n START \n\n")

    //      val fseq = FSequent(A0 :: Nil, A0 :: Nil)
    //      val fseq = FSequent(A0 :: Neg(A0) :: Nil, Nil)
    val biga = BigAnd(i, A, zero, two)
    val bigo = BigOr(i, A, zero, one)
    val biga2 = BigAnd(i, A, zero, two)
    val bigo2 = BigOr(i, A, zero, two)

    //      val fseq = FSequent(bigo :: Nil, A0 :: A1 :: Nil )
    //      val fseq = FSequent(biga :: Nil, A0 :: A1 :: Nil )
    //      val fseq = FSequent(biga :: Nil, A0 :: A1 :: A2 :: Nil )
    //      val fseq = FSequent(A :: B :: Nil, And(A, B) :: Nil)
    val fseq = FSequent(A :: B :: C :: Nil, And(And(A, B), C) :: Nil)
//    val fseq = FSequent(bigo2 :: Nil, A0 :: A1 :: A2 :: Nil)
//    val fseq = FSequent(A0 :: A1 :: A2 :: Nil, biga2 :: Nil)
//    val fseq = FSequent(A0 :: A1 :: A2 :: Nil, biga :: Nil)
    //      val fseq = FSequent(A0 :: A1 :: Nil, bigo :: Nil)
    fseq
  }
}
