package at.logic.algorithms.resolution

import at.logic.language.hol._
import at.logic.calculi.resolution.base.FClause
import at.logic.calculi.lk.base.FSequent

/**
 * * Formulas must be regular and weakly quantified
 * (see Issue 196)
 */

  /**
   * computes the set CNF^+
   */
  object CNFp {
    def apply(f: HOLFormula): Set[FClause] = f match {
      case BottomC => Set(FClause(List(), List()))
      case TopC => Set()
      case Atom(_,_) => Set(FClause(List(), List(f)))
      case Neg(f2) => CNFn(f2)
      case And(f1,f2) => CNFp(f1) union CNFp(f2)
      case Or(f1,f2) => times(CNFp(f1),CNFp(f2))
      case Imp(f1,f2) => times(CNFn(f1),CNFp(f2))
      case AllVar(_,f2) => CNFp(f2)
      case _ => throw new IllegalArgumentException("unknown head of formula: " + f.toString)
    }
  }

  /**
   * computes the set CNF^-
   */
  object CNFn {
    def apply(f: HOLFormula): Set[FClause] = f match {
      case BottomC => Set()
      case TopC => Set(FClause(List(), List()))
      case Atom(_,_) => Set(FClause(List(f), List()))
      case Neg(f2) => CNFp(f2)
      case And(f1,f2) => times(CNFn(f1),CNFn(f2))
      case Or(f1,f2) => CNFn(f1) union CNFn(f2)
      case Imp(f1,f2) => CNFp(f1) union CNFn(f2)
      case ExVar(_,f2) => CNFn(f2)
      case _ => throw new IllegalArgumentException("unknown head of formula: " + f.toString)
    }
  }

  object times {
    def apply(s1: Set[FClause], s2: Set[FClause]): Set[FClause] = {
      s1.flatMap(c1 => s2.map(c2 => setCompose(c1, c2)))
    }

    //as FClause.compose, but does not duplicate literals occurring in both fc1 and fc2
    def setCompose(fc1 : FClause, fc2: FClause) = {
      FClause((fc1.neg.toSet ++ fc2.neg).toList, (fc1.pos.toSet ++ fc2.pos).toList)
    }
  }

