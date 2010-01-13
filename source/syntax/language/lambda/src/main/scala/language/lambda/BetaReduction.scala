/*
 * BetaReduction.scala
 *
 */

package at.logic.language.lambda

import symbols._
import typedLambdaCalculus._

/* The BetaReduction object encapsulates two functions:
 * 1) betaNormalize, which transforms a lambda expression to beta normal form.
 * 2) betaReduce, which applies only one rewrite step to a lambda expression.
 *
 * These functions take additional arguments specifying which strategy should
 * be used for reduction. The chosen strategies determine which redex is
 * selected for reduction first. The strategy options are implemented as
 * an Enumeration and are:
 * 1) Outermost (call-by-name) or Innermost (call-by-value)
 * 2) Leftmost or Rightmost [these are only relevant for betaReduce]
 *
 * In principle, betaNormalize could have been implemented as a while loop
 * iterating betaReduce. However, for efficiency reasons, this has not been done.
 *
 */
object BetaReduction {

  class ReductionException(msg: String) extends Exception(msg)
  
  abstract class Strategy extends Enumeration
  object StrategyOuterInner extends Strategy {
    val Outermost = Value
    val Innermost = Value
  }
  object StrategyLeftRight extends Strategy {
    val Leftmost = Value
    val Rightmost = Value
  }

  object ImplicitStandardStrategy {
    implicit val implicitOuter = StrategyOuterInner.Outermost
    implicit val implicitLeft = StrategyLeftRight.Leftmost
  }

  def betaNormalize(expression: LambdaExpression)(implicit strategy: StrategyOuterInner.Value):LambdaExpression = expression match {
    case App(AbsInScope(x,body),arg) => {
      strategy match {
        case StrategyOuterInner.Outermost => betaNormalize(replace(x, arg, body))(strategy)  // If it is outermost strategy, we first reduce the current redex by applying sigma, and then we call betaNormalize recursively on the result.
        case StrategyOuterInner.Innermost => replace(x, betaNormalize(arg), betaNormalize(body))
      }
    }
    case App(m,n) => App(betaNormalize(m)(strategy),betaNormalize(n)(strategy))
    case Abs(x,m) => Abs(x,betaNormalize(m)(strategy))
    case x: Var => x
  }

  // replace bound variables, needs to recompute db indices
  private def replace(bvar: Var, withTerm: LambdaExpression, inTerm: LambdaExpression)(implicit strategy: StrategyOuterInner.Value): LambdaExpression =
    if (bvar.isFree) throw new ReductionException("Error in beta reduction: Malformed Abs term, bounded variable has no db index and therefore is not bound")
    else inTerm match {
      case v: Var if v == bvar => withTerm
      case v: Var => v
      case App(a, b) => App(replace(bvar, withTerm, a), replace(bvar, withTerm, b))
      case abs: Abs if abs.variableInScope != bvar => Abs(abs.variable, replace(bvar, withTerm, abs.expressionInScope))
      case _ => throw new ReductionException("Error in beta reduction: the same bound variable (with the same db index) appears inside the other one scope")
    }
  
  def betaReduce(expression: LambdaExpression)(implicit strategyOI: StrategyOuterInner.Value, strategyLR: StrategyLeftRight.Value): LambdaExpression = expression match {
    case App(abs: Abs,arg) => {
      strategyOI match {
        case StrategyOuterInner.Outermost => replace(abs.variableInScope, arg, abs.expressionInScope)
        case StrategyOuterInner.Innermost => {
          strategyLR match {
            case StrategyLeftRight.Rightmost => {
              val argr = betaReduce(arg)(strategyOI,strategyLR)    // Since it is innerrightmost redex strategy, we try first to reduce the argument.
              if (argr != arg) App(abs, argr)            // If it succeeds, great!
              else {                                              // If it doesn't, then we try to find an innermost redex in the left side, i.e. in the body.
                val bodyr = betaReduce(abs.expression)(strategyOI,strategyLR)
                if (bodyr != abs.expression) App(Abs(abs.variable, bodyr), arg)      // If it succeeds, great!
                else replace(abs.variableInScope, arg, abs.expressionInScope)
              }
            }
            case StrategyLeftRight.Leftmost => {                    // Analogous to the previous case, but giving priority to the left side (body) instead of the ride side (arg)
              val bodyr = betaReduce(abs.expression)(strategyOI,strategyLR)
              if (bodyr != abs.expression) App(Abs(abs.variable, bodyr), arg)
              else {
                val argr = betaReduce(arg)(strategyOI,strategyLR)
                if (argr != arg) App(abs, argr)
                else replace(abs.variableInScope, arg, abs.expressionInScope)
              }
            }
          }
        }
      }
    }
    case App(m,n) => {
      strategyLR match {
        case StrategyLeftRight.Leftmost => {
          val mr = betaReduce(m)(strategyOI,strategyLR)        // Since it is leftmost redex strategy, we try first to reduce the left side (m).
          if (mr != m) App(mr,n)                              // If it succeeds, great!
          else App(m, betaReduce(n)(strategyOI,strategyLR))    // If it doesn't, then we try to find and reduce a redex in the right side (n)
        }
        case StrategyLeftRight.Rightmost => {
          val nr = betaReduce(n)(strategyOI,strategyLR)        // Since it is rightmost redex strategy, we try first to reduce the right side (n).
          if (nr != n) App(m,nr)                              // If it succeeds, great!
          else App(betaReduce(m)(strategyOI,strategyLR), n )   // If it doesn't, then we try to find and reduce a redex in the left side (m)
        }
      }
    }
    case Abs(x,m) => Abs(x,betaReduce(m)(strategyOI,strategyLR))
    case x: Var => x
  }
}
