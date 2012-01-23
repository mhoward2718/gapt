/*
 * UnificationAlgorithm.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.algorithms.unification

import at.logic.language.lambda.typedLambdaCalculus._
import at.logic.language.lambda.substitutions._


trait FinitaryUnification[Expression <: LambdaExpression] {
  def unify(term1:Expression, term2:Expression) : List[Substitution[Expression]]
}


trait UnificationAlgorithm[Expression <: LambdaExpression] extends FinitaryUnification[Expression]