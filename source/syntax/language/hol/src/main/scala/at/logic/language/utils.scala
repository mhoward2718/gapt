/*
 * Simple functions that operate on HOL-expressions
 *
 */

package at.logic.language.hol

import at.logic.language.lambda.{freeVariables => freeVariablesLambda, rename => renameLambda}
import at.logic.language.hol.logicSymbols._
import at.logic.language.lambda.types.TA
import scala.Some

object freeVariables {
  def apply(e: HOLExpression) : List[HOLVar] = freeVariablesLambda(e).asInstanceOf[List[HOLVar]]
}

// matches for consts and vars, but nothing else
object VarOrConst {
  def unapply(e:HOLExpression) : Option[(String, TA)]= e match {
    case HOLVar(name, et) => Some((name,et))
    case HOLConst(name, et) => Some((name,et))
    case _ => None
  }
}


// get a new variable/constant (similar to the current and) different from all 
// variables/constants in the blackList, returns this variable if this variable 
// is not in the blackList
object rename {
  def apply(v: HOLVar, blacklist: List[HOLVar]) : HOLVar = renameLambda(v, blacklist).asInstanceOf[HOLVar]
}

// Instantiates a term in a quantified formula (using the first quantifier).
object instantiate {
  def apply(f: HOLFormula, t: HOLExpression) : HOLFormula = f match {
    case AllVar(v, form) =>
      val sub = Substitution(v, t)
      sub(form)
    case ExVar(v, form) => 
      val sub = Substitution(v, t)
      sub(form)
    case _ => throw new Exception("ERROR: trying to replace variables in a formula without quantifier.") 
  }
}

object containsQuantifier {
  def apply(e: HOLExpression) : Boolean = e match {
    case HOLVar(x,tpe) => false
    case HOLConst(x,tpe) => false
    case Atom(x, args) => false
    case And(x,y) => containsQuantifier(x) || containsQuantifier(y)
    case Or(x,y) => containsQuantifier(x) || containsQuantifier(y)
    case Imp(x,y) => containsQuantifier(x) || containsQuantifier(y)
    case Neg(x) => containsQuantifier(x)
    case ExVar(x,f) => true
    case AllVar(x,f) => true
    // Is this really necessary?
    case HOLAbs(v, exp) => containsQuantifier(exp)
    case HOLApp(l, r) => containsQuantifier(l) || containsQuantifier(r)
    case _ => throw new Exception("Unrecognized symbol.")
  }
}

object isPrenex {
  def apply(e: HOLExpression) : Boolean = e match {
    case HOLVar(_,_) => true
    case HOLConst(_,_) => true
    case Atom(_,_) => true
    case Neg(f) => !containsQuantifier(f)
    case And(f1,f2) => !containsQuantifier(f1) && !containsQuantifier(f2)
    case Or(f1,f2) => !containsQuantifier(f1) && !containsQuantifier(f2)
    case Imp(f1,f2) => !containsQuantifier(f1) && !containsQuantifier(f2)
    case ExVar(v,f) => isPrenex(f)
    case AllVar(v,f) => isPrenex(f)
    case _ => throw new Exception("ERROR: Unknow operator encountered while checking for prenex formula: " + this)
  }
}

object isAtom {
  def apply(e: HOLExpression) : Boolean = e match {
    case Atom(_,_) => true
    case _ => false
  }
}

object subTerms {
  def apply(e: HOLExpression) : List[HOLExpression] = e match {
    case HOLVar(_,_) => List(e)
    case HOLConst(_,_) => List(e)
    case Atom(_, args) =>  e +: args.flatMap( a => subTerms(a) )
    case Function(_,args,_)  =>  e +: args.flatMap( a => subTerms(a) )
    case And(x,y) => e +: (subTerms(x) ++ subTerms(y))
    case Or(x,y) => e +: (subTerms(x) ++ subTerms(y))
    case Imp(x,y) => e +: (subTerms(x) ++ subTerms(y))
    case Neg(x) => e +: subTerms(x)
    case AllVar(_, x) => e +: subTerms(x)
    case ExVar(_, x) => e +: subTerms(x)
    case HOLAbs(_, x) => e +: subTerms(x)
    case HOLApp(x, y) => e +: (subTerms(x) ++ subTerms(y))
    case _ => throw new Exception("Unrecognized symbol.")
  }
}

object isLogicalSymbol {
  def apply(e: HOLExpression) : Boolean = e match {
    case x : HOLConst => x.sym.isInstanceOf[LogicalSymbolA]
    case _ => false
  }
}

/**
 * the logical complexity of this formula, i.e. the number of logical connectives and atoms
 * starting from the root of the formula. The inner structure of atoms is not counted.
 **/
object lcomp {
  def apply(formula: HOLFormula) : Int = formula match {
    case Atom(_, _) => 1
    case Neg(f) => lcomp(f) + 1
    case And(f,g) => lcomp(f) + lcomp(g) + 1
    case Or(f,g) => lcomp(f) + lcomp(g) + 1
    case Imp(f,g) => lcomp(f) + lcomp(g) + 1
    case ExVar(x,f) => lcomp(f) + 1
    case AllVar(x,f) => lcomp(f) + 1
  }
}

// Returns the quantifier free part of a prenex formula
object getMatrix {
  def apply(f: HOLFormula) : HOLFormula = {
    assert(isPrenex(f))
    f match {
      case HOLVar(_) |
           HOLConst(_) |
           Atom(_,_) |
           Imp(_,_) |
           And(_,_) |
           Or(_,_) |
           Neg(_) => f
      case ExVar(x,f0) => getMatrix(f0)
      case AllVar(x,f0) => getMatrix(f0)
      case _ => throw new Exception("ERROR: Unexpected case while extracting the matrix of a formula.")
    }
  }
}

object toAbbreviatedString {
  /**
   * This function takes a HOL construction and converts it to a abbreviated string version. The abbreviated string version is made
   * by replacing the code construction for logic symbols by string versions in the file language/hol/logicSymbols.scala.
   * Several recursive function calls will be transformed into an abbreviated form (e.g. f(f(f(x))) => f^3(x)).
   * Terms are also handled by the this function.
   * @param  e  The method has no parameters other then the object which is to be written as a string
   * @throws Exception This occurs when an unknown subformula is found when parsing the HOL construction
   * @return A String which contains the defined symbols in language/hol/logicSymbols.scala.
   *
   */
  def apply(e : HOLExpression) : String = {


    val p = pretty(e)

    val r : String = e match {
      case Function(x, args, tpe) => {
        if(p._1 != p._2 && p._2 != "tuple1")
          if(p._3 > 0)
            return p._2 + "^"+(p._3+1)+"("+p._1+") : " + tpe.toString()
          else
            return p._1
        else
          return p._1
      }
      case _ => return p._1
    }

    return r
  }


  private def pretty(exp : HOLExpression) : (String, String, Int) = {

    val s : (String, String, Int) = exp match {
      case null => ("null", "null", -2)
      case HOLVar(x, t) => (x.toString() + " : " + t.toString(), x.toString(), 0)
      case Atom(x, args) => {
        (x.toString() + "(" + (args.foldRight(""){  case (x,"") => "" + toAbbreviatedString(x)
        case (x,str) => toAbbreviatedString(x) + ", " + str }) + ")" + " : o", x.toString(), 0)
      }
      case Function(x, args, t) => {
        // if only 1 argument is provided
        // check if abbreviating of recursive function calls is possible
        if(args.length == 1)
        {
          val p = pretty(args.head)

          // current function is equal to first and ONLY argument
          if( p._2 == x.toString() )
          {
            // increment counter and return (<current-string>, <functionsymbol>, <counter>)
            return (p._1, x.toString(), p._3+1)
          }
          // function symbol has changed from next to this level
          else
          {

            // in case of multiple recursive function calls
            if(p._3 > 0)
            {
              return (p._2+"^"+p._3+"("+p._1+") : " + t.toString(), x.toString(), 0)
            }
            // otherwise
            else
            {
              return (p._1, x.toString(), 1)
            }
          }
        }
        else
        {
          return (x.toString()+"("+ (args.foldRight(""){   case (x,"") => toAbbreviatedString(x)
          case (x,s) => toAbbreviatedString(x) + ", " + s
          })+ ") : " + t.toString(), x.toString(), 0)
        }

      }
      case And(x,y) => ("(" + toAbbreviatedString(x) + " " + AndSymbol + " " + toAbbreviatedString(y) + ")", AndSymbol.toString(), 0)
      case Equation(x,y) => ("(" + toAbbreviatedString(x) + " " + EqSymbol + " " + toAbbreviatedString(y) + ")", EqSymbol.toString(), 0)
      case Or(x,y) => ("(" + toAbbreviatedString(x) + " " + OrSymbol + " " + toAbbreviatedString(y) + ")", OrSymbol.toString(), 0)
      case Imp(x,y) => ("(" + toAbbreviatedString(x) + " " + ImpSymbol + " " + toAbbreviatedString(y) + ")", ImpSymbol.toString(), 0)
      case Neg(x) => (NegSymbol + toAbbreviatedString(x), NegSymbol.toString(), 0)
      case ExVar(x,f) => (ExistsSymbol + toAbbreviatedString(x) + "." + toAbbreviatedString(f), ExistsSymbol.toString(), 0)
      case AllVar(x,f) => (ForallSymbol + toAbbreviatedString(x) + "." + toAbbreviatedString(f), ForallSymbol.toString(), 0)
      case HOLAbs(v, exp) => ("(λ" + toAbbreviatedString(v) + "." + toAbbreviatedString(exp) + ")", "λ", 0)
      case HOLApp(l,r) => ("(" + toAbbreviatedString(l) + ")(" + toAbbreviatedString(r) + ")", "()()", 0)
      case HOLConst(x) => (x.toString(),x.toString(), 0)
      case _ => throw new Exception("ERROR: Unknown HOL expression.");
    }
    return s

  }

}