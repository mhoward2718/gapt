package at.logic.algorithms.hlk

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.execute.Success
import at.logic.parsing.language.hlk.{HLKHOLParser, ast}
import at.logic.parsing.language.hlk.ast.LambdaAST
import java.io.File.separator
import at.logic.language.lambda.types.TA
import at.logic.language.hol.{HOLFormula, HOLConst, HOLVar, HOLExpression}
import at.logic.language.lambda.symbols.VariableStringSymbol
import at.logic.language.hol.logicSymbols.ConstantStringSymbol
import at.logic.calculi.lk.base.FSequent


/**
 * Created with IntelliJ IDEA.
 * User: marty
 * Date: 10/14/13
 * Time: 3:02 PM
 * To change this template use File | Settings | File Templates.
 */
class HybridLatexParserTest extends SpecificationWithJUnit {
  val p1 =
    """\AX{T,MON(h_1,\alpha)}{MON(h_1,\alpha) }
      |\AX{ NOCC(h_1,\alpha,\sigma)}{NOCC(h_1,\alpha,\sigma)}
      |\EXR{}{ NOCC(h_1,\alpha,\sigma)}{(exists s NOCC(h_1,\alpha,s))}
      |\ANDR{T,MON(h_1,\alpha), NOCC(h_1,\alpha,\sigma)}{MON(h_1,\alpha) & (exists s NOCC(h_1,\alpha,s))}
      |\EXR{}{T,MON(h_1,\alpha), NOCC(h_1,\alpha,\sigma)}{(exists h (MON(h,\alpha) & (exists s NOCC(h,\alpha,s))))}
      |\ANDL{T, MON(h_1,\sigma) & NOCC(h_1,\sigma,x)}{(exists h (MON(h,\alpha) & (exists s NOCC(h,\alpha,s))))}
      |\EXL{}{T, (exists h (MON(h,\sigma) & NOCC(h,\sigma,x)))}{(exists h (MON(h,\alpha) & (exists s NOCC(h,\alpha,s))))}
      |\ALLL{}{T, (all n exists h (MON(h,n) & NOCC(h,n,x)))}{(exists h (MON(h,\alpha) & (exists s NOCC(h,\alpha,s))))}
      |\DEF{T,A(\sigma)}{(exists h (MON(h,\alpha) & (exists s NOCC(h,\alpha,s))))}
      |\ALLR{}{T,A(\sigma)}{(all n exists h (MON(h,n) & (exists s NOCC(h,n,s))))}
      |\DEF{T,A(\sigma)}{C}
      |\CONTINUEWITH{\rho(\sigma)}
      |""".stripMargin

  def checkformula(f:String) = {
    HybridLatexParser.parseAll(HybridLatexParser.formula,f) match {
      case HybridLatexParser.Success(r,_) =>
        ok(r.toString)
      case HybridLatexParser.NoSuccess(msg, input) =>
        ko("Error at "+input.pos+": "+msg)
    }
  }


  "Hybrid Latex-GAPT" should {
    "correctly handle latex macros in formulas (1)" in {
      checkformula("\\benc{j_1<n+1}")
    }

    "correctly handle latex macros in formulas (2)" in {
      checkformula("\\ite{\\benc{j_1<n+1}}{h'(j_1)}{\\alpha}")
    }

    "correctly handle latex macros in formulas (3)" in {
      checkformula("\\ite{\\ienc{j_1<n+1}}{h'(j_1)}{\\alpha}")
    }

    "correctly handle latex macros in formulas (4)" in {
      checkformula("\\ite{\\benc{j_1<n+1}}{h'(j_1)}{\\alpha} = 0")
    }

    "accept the proof outline" in {
      //println(p1)
      HybridLatexParser.parseAll(HybridLatexParser.rules, p1) match {
        case HybridLatexParser.Success(r : List[Token] , _) =>
          //println(r)
          val lterms : List[LambdaAST] = r.flatMap(_ match {
            case RToken(_,_,a, s) => a++s
            case TToken(_,_,_) => Nil
          })


          println(lterms.flatMap(_.varnames).toSet)

          ok("successfully parsed "+r)
        case HybridLatexParser.NoSuccess(msg, input) =>
          ko("parsing error at "+input.pos +": "+msg)
      }
    }

    "accept the proof outline with the parse interface" in {
      try {
        val r = HybridLatexParser.parse(p1)
        ok
      } catch {
        case e:Exception =>
        ko("Parsing error: "+e.getMessage + " stacktrace: "+e.getStackTraceString)
      }
    }


    "load the simple example from file and parse it" in {
      //skipped("problem with autocontt")
      try {
        val r = HybridLatexParser.parseFile("target" + separator + "test-classes" + separator + "simple.llk")
        val p = HybridLatexParser.createLKProof(r)
        println(p)

        ok
      } catch {
        case e:Exception =>
          ko("Parsing error: "+e.getMessage + " stacktrace: "+e.getStackTraceString)
      }
    }

    "load the tape3 proof from file" in {
      skipped("lots of unimplemented rules")
      try {
        val r = HybridLatexParser.parseFile("target" + separator + "test-classes" + separator + "tape3.llk")
        val lterms : List[LambdaAST] = r.flatMap(_ match {
          case RToken(_,_,a, s) => a++s
          case TToken(_,_,_) => Nil
        })
//        println(lterms.flatMap(x => {val vn = x.varnames; if(vn.contains("lambda") ) println(x); vn}).toSet.toList.sorted)

        val p = HybridLatexParser.createLKProof(r)

        ok
      } catch {
        case e:Exception =>
          ko("Parsing error: "+e.getMessage + " stacktrace: "+e.getStackTraceString)
      }
    }


  }
}
