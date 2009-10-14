/*
 * typedLambdaCalculus.scala
 *
 */

package at.logic.language

import at.logic.language.Symbols._

object TypedLambdaCalculus {

    import at.logic.language.Types._
    
    
    abstract class LambdaExpression {
        def exptype: TA
    }


    case class Var(name: SymbolA, exptype: TA )
        extends LambdaExpression

    case class Abs(variable: Var, expression: LambdaExpression)
        extends LambdaExpression {
            def exptype: TA = ->(variable.exptype,expression.exptype)
        }

    case class App(function: LambdaExpression, argument: LambdaExpression )
        extends LambdaExpression {
            require({
                function.exptype match {
                    case ->(in,out) => {if (in == argument.exptype) true
                                        else false}
                    case _          => false
                }
            })
            def exptype: TA = {
                function.exptype match {
                    case ->(in,out) => out
                }
            }
        }



    def AbsN(variables: List[Var], expression: LambdaExpression):LambdaExpression =
        if (!variables.isEmpty) (variables :\ expression)(Abs)
        else expression

    def AppN(function: LambdaExpression, arguments: List[LambdaExpression]):LambdaExpression =
        if (!arguments.isEmpty) (function /: arguments)(App)
        else function

    def exportLambdaExpressionToString(expression: LambdaExpression):String = expression match {
        case Var(name,exptype) => name.toString
        case Abs(variable, exp) => "\\" + exportLambdaExpressionToString(variable) + "." + exportLambdaExpressionToString(exp)
        case App(function, argument) => "(" + exportLambdaExpressionToString(function) + " " + exportLambdaExpressionToString(argument)  + ")"
    }

    def exportLambdaExpressionToStringWithTypes(expression: LambdaExpression):String = expression match {
        case Abs(variable, exp) => "\\" + exportLambdaExpressionToString(variable) + "." + exportLambdaExpressionToString(exp)
        case App(function, argument) => "(" + exportLambdaExpressionToString(function) + " " + exportLambdaExpressionToString(argument)  + ")"
        case Var(name,exptype) => {
            name.toString +
                "ToDo"

        }
    }
}