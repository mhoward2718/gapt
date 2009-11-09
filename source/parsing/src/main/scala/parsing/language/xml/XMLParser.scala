/**
 * Description: Parses the CERES XML format version 5.0
 * 
 * TODO: Support for 
 *  prooftool-related elements (variabledefinitions, ...)
 *  substitution element
 *  formulavariable element
 * 
 * All parsers expect the XML to conform to the DTD, no checking is done!
 */

package at.logic.parsing.language.xml

import scala.xml._
import scala.xml.Utility.trim
import at.logic.language.lambda.TypedLambdaCalculus._
import at.logic.parsing.ParsingException
import at.logic.parsing.readers.NodeReader
import at.logic.language.hol.HigherOrderLogic._
import at.logic.language.lambda.Symbols._
import at.logic.language.lambda.Types._
import at.logic.language.hol.LogicSymbols.{ConstantStringSymbol, VariableStringSymbol}

object XMLParser {

  trait XMLNodeParser {
    def getInput() : scala.xml.Node
  }
 /* 
  trait XMLSequentParser extends XMLNodeParser {
    def getSequent() : Sequent = getSequent( getInput() )
    def getSequentList() : List[Sequent] = getSequentList( getInput() )
    def getFormulaList() : List[Formula] = getFormulaList( getInput() )
    
    def getSequent(n: Node) : Sequent =
      trim(n) match {
        case <sequent>{ns @ _*}</sequent> => {
          new Sequent(getFormulaList(ns.first), getFormulaList(ns.last))
        }
        case _ => throw throw new ParsingException("Could not parse XML: " + n.toString)
      }
    
    def getSequentList(n: Node) =
      trim(n) match {
        case <sequentlist>{ns @ _*}</sequentlist> => {
          ns.map( n => getSequent(n) ).toList
        }
        case _ => throw throw new ParsingException("Could not parse XML: " + n.toString)
      }
    
    def getFormulaList(n: Node) =
      trim(n) match {
        case <formulalist>{ns @ _*}</formulalist> => {
          ns.map( n => (new NodeReader(n) with XMLFormulaParser).getFormula() ).toList
        }
        case _ => throw throw new ParsingException("Could not parse XML: " + n.toString)
      }
  }
  */
/*
  trait XMLProofParser extends XMLNodeParser {
    def getProof() : Rule = getProof( getInput() )
    
    def getProof(n : Node) : Rule =
      trim(n) match {
        case <proof>{ ns @ _* }</proof> => {
          // TODO: read symbol, calculus
          getProof( ns.first )
        }
        case <rule>{ ns @ _* }</rule> => {
          val type = n.attribute("type").get.first.text
          val conc = ( new NodeReader(ns.first) with XMLSequentParser ).getSequent()
          // TODO: according to DTD, last two items may be substitution
          // or lambdasubstitution! we _wrongly_ assume for the moment
          // that all further children are rules/prooflinks
          val prems = ns.toList.tail.map( n => getProof( n ) )
          // TODO: consider rule type
          new Rule( conc, prems )
        }
        //case <prooflink/> => {
          //n.attribute("symbol").get.first.text
          // TODO: create DAG Proof!?
        //  None
        //}
        case _ => throw throw new ParsingException("Could not parse XML: " + n.toString)
      }
  }
*/
  /*
  trait XMLDefinitionParser extends XMLNodeParser {
    def getDefinition() = getDefinition( getInput() )
    
    def getDefinition(n : Node) =
      n match {
        case <definitionlist>{_*}</definitionlist> =>
        case <formuladef>{_*}</formuladef> =>
        case <termdef>{_*}</termdef> =>
        case <indirecttermdef>{_*}</indirecttermdef> =>
      }
  }
  */
  
/*
This class parses the elements subsumed under the entity &formula;
 */
  trait XMLFormulaParser extends XMLNodeParser {
    def getFormula() : LambdaExpression[HOL] with Formula = getFormula( getInput() )

    def getFormula(n : Node) : LambdaExpression[HOL] with Formula =
      trim(n) match {
        case <constantatomformula>{ ns @ _* }</constantatomformula>
             => Atom(new ConstantStringSymbol( n.attribute("symbol").get.first.text ),
                     nodesToTerms(ns.toList))
        /*
        case <variableatomformula>{ ns @ _* }</variableatomformula>
             => {
                  createVariableAtomFormula( (new NodeReader( ns.first ) with XMLSOLTermParser).getTerm(),
                                             nodesToTerms( ns.toList.tail ) )
                }
        case <definedsetformula>{ ns @ _* }</definedsetformula>
          => 
        */
        case <conjunctiveformula>{ ns @ _* }</conjunctiveformula> 
             => createConjunctiveFormula(n.attribute("type").get.first.text,
                                         nodesToFormulas(ns.toList))
        case <quantifiedformula>{ ns @ _* }</quantifiedformula>
             => {
                  val variable = ( new NodeReader(ns.first) with XMLTermParser).getTerm().asInstanceOf[Var[HOL]]
                  val form = ( new NodeReader(ns.last) with XMLFormulaParser).getFormula() 
                  createQuantifiedFormula(n.attribute("type").get.first.text,
                                          variable, form)
                }
        /*
        case <secondorderquantifiedformula>{_*}</secondorderquantifiedformula>
          => */
        case _ => throw throw new ParsingException("Could not parse XML: " + n.toString)
      }

    def nodesToTerms(ns : List[Node]) : List[LambdaExpression[HOL]] =
      ns.map( c => (new NodeReader( c ) with XMLTermParser).getTerm() )
    
    def nodesToFormulas(ns : List[Node]) : List[LambdaExpression[HOL] with Formula] =
      ns.map( c => (new NodeReader( c ) with XMLFormulaParser).getFormula() )

    def createConjunctiveFormula(sym: String, formulas: List[LambdaExpression[HOL] with Formula]) : LambdaExpression[HOL] with Formula =
    {
      sym match {
        case "and" => And(formulas.first, formulas.last)
        case "or" => Or(formulas.first, formulas.last)
        case "impl" => Imp(formulas.first, formulas.last)
        case "neg" => Neg(formulas.first)
        case _ => throw new ParsingException("Could not parse conjunctiveformula type: " + sym)
      }
    }

    def createQuantifiedFormula(sym: String, variable: Var[HOL], formula: LambdaExpression[HOL] with Formula) : LambdaExpression[HOL] with Formula =
      sym match {
        case "all" => AllVar(variable, formula)
        case "exists" => ExVar(variable, formula)
        case _ => throw new ParsingException("Could not parse quantifiedformula type: " + sym)
      }
  }
  
/*
This class parses the elements subsumed under the entity &abstractterm;
 */
  trait XMLAbstractTermParser extends XMLNodeParser {
    def getAbstractTerm() : LambdaExpression[HOL] = getAbstractTerm( getInput() )
    
    def getAbstractTerm(n: Node) : LambdaExpression[HOL] =
      try {
        (new NodeReader(n) with XMLTermParser).getTerm()
      }
      catch {
        case e : ParsingException =>
          {
            (new NodeReader(n) with XMLSetTermParser).getSetTerm()
          }
      }
  }

/*
This class parses the elements subsumed under the entity &term;
 */
  trait XMLTermParser extends XMLNodeParser {
    def getTerm() : LambdaExpression[HOL] = getTerm(getInput())
    
    def getTerm(n: Node) : LambdaExpression[HOL] =
      trim(n) match {
        case <variable/> => Var[HOL](new VariableStringSymbol( n.attribute("symbol").get.first.text ), Ti())
        case <constant/> => Var[HOL](new ConstantStringSymbol( n.attribute("symbol").get.first.text ), Ti())
        case <function>{ ns @ _* }</function> => createFunction(n.attribute("symbol").get.first.text,
                                                             getTerms(ns.toList))
        case _ => throw new ParsingException("Could not parse XML: " + n.toString)
      }
    def getTerms(ns: List[Node]) : List[LambdaExpression[HOL]] = ns.map( n => getTerm( n ) )
    def createFunction( sym: String, args : List[LambdaExpression[HOL]] ) : LambdaExpression[HOL] = {
      val fType = ((args.map((x:Any) => i):List[TA]) :\ (i:TA))(->)  // computing the type of the function Var.
      val f = Var[HOL](new ConstantStringSymbol(sym), fType)         // creating the function Var.
      AppN(f, args)
    }
  }

/*
This class parses the elements subsumed under the entity &setterm;
 */
  trait XMLSetTermParser extends XMLNodeParser {
    def getSetTerm() : LambdaExpression[HOL] = getSetTerm(getInput())
    
    def getSetTerm(n: Node) : LambdaExpression[HOL] =
      trim(n) match {
        // TODO: how do we determine the arity of the secondordervariable?
        // case <secondordervariable/> => { /* TODO */}
        case <lambdasubstitution>{ ns @ _* }</lambdasubstitution> => {
          AbsN( (new NodeReader(ns.first) with XMLVariableListParser).getVariableList(),
                (new NodeReader(ns.last) with XMLFormulaParser).getFormula() )
        }
        //case <definedset>{_*}</definedset> => { /* TODO */}
        case _ => throw new ParsingException("Could not parse XML: " + n.toString)
      }
  }
  
  trait XMLVariableListParser extends XMLNodeParser {
    def getVariableList() : List[Var[HOL]] = getVariableList(getInput())
    
    def getVariableList(n: Node) : List[Var[HOL]] =
      trim(n) match {
        case <variablelist>{ns @ _*}</variablelist> => {
          ns.map( n => (new NodeReader(n) with XMLTermParser).getTerm().asInstanceOf[Var[HOL]] ).toList
        }
        case _ => throw new ParsingException("Could not parse XML: " + n.toString)
      }
  }
}
