/* Description: Tests for the base prover
**/

package at.logic.provers.atp

import org.specs._
import org.specs.runner._
import org.specs.mock.Mockito
import org.mockito.Matchers._  // to use matchers like anyInt()
import at.logic.parsing.calculi.simple.SimpleResolutionParserFOL
import at.logic.parsing.readers.StringReader
import at.logic.calculi.resolution.base._
import refinements.SimpleRefinement
import commands._
import commandsParsers.FOLResolutionCommandsParser

private class MyParser(str: String) extends StringReader(str) with SimpleResolutionParserFOL
private object MyProver extends Prover

class ProverTest extends SpecificationWithJUnit {
  "Prover" should {
    "in case it has only one clause return it if it is the empty clause" in {
      MyProver.refute(simpleAutoStream(".")).head must beLike {
        case a: ResolutionProof if a.root.formulaEquivalece(theEmptyClause().root) => true
      }
    }
    "in case it has an empty clause set return None" in {
      MyProver.refute(simpleAutoStream("")) must beEqual (Stream.empty)
    }
    "in case it has only one clause return None if it is not the empty clause" in {
      MyProver.refute(simpleAutoStream("P(x).")) must beEqual (Stream.empty)
    }
    "refute the following clauses" in {
      "p(a). -p(x) | p(f(x)). -p(f(f(a)))" in {
        MyProver.refute(simpleAutoStream("P(a). -P(x) | P(f(x)). -P(f(f(a))).")).head must beLike {
          case a: ResolutionProof if a.root.formulaEquivalece(theEmptyClause().root) => true
        }
      }
      "requiring factoring" in {
        "p(a). -p(x) | -p(x) | p(f(x)) | p(f(x)). -p(f(f(a)))" in {
          MyProver.refute(simpleAutoStream("P(a). -P(x) | -P(x) | P(f(x)) | P(f(x)). -P(f(f(a))).")).head must beLike {
            case a: ResolutionProof if a.root.formulaEquivalece(theEmptyClause().root) => true
          }
        }
      }
      "requiring paramodulation" in {
        "P(a). -P(b). =(a,b)." in {
          MyProver.refute(simpleAutoStream("P(a). -P(b). =(a,b).")).head must beLike {
            case a: ResolutionProof if a.root.formulaEquivalece(theEmptyClause().root) => true
          }
        }
      }
    }
    /*"When there is a refutation the proof should be correct (clauses from the set as initials and using only the rules in a correct way" in {
      "ex1"
    }*/
    // test with a different target clause than the empty
  }
  " Prover with unit refinement" should {
    "refute the following clauses" in {
      "p(a). -p(x) | p(f(x)). -p(f(f(a)))" in {
        MyProver.refute(unitAutoStream("P(a). -P(x) | P(f(x)). -P(f(f(a))).")).head must beLike {
          case a: ResolutionProof if a.root.formulaEquivalece(theEmptyClause().root) => true
        }
      }
      "p(a). -p(x) | -p(x) | p(f(x)) | p(f(x)). -p(f(f(a)))" in {
        MyProver.refute(unitAutoStream("P(a). -P(x) | -P(x) | P(f(x)) | P(f(x)). -P(f(f(a))).")).head must beLike {
          case a: ResolutionProof if a.root.formulaEquivalece(theEmptyClause().root) => true
        }
      }
    }
  }
  def simpleAutoStream(cl: String) = {
    val pb = new at.logic.utils.ds.PublishingBuffer[Clause]
    val cls = new MyParser(cl).getClauseList
    pb.insertAll(0,cls)
    val subsumMng = new at.logic.algorithms.subsumption.managers.SimpleManager(pb.asInstanceOf[at.logic.utils.ds.PublishingBuffer[at.logic.calculi.lk.base.Sequent]],
        new at.logic.algorithms.subsumption.StillmanSubsumptionAlgorithm{val matchAlg = at.logic.algorithms.matching.fol.FOLMatchingAlgorithm})
    AutomatedFOLStream(-1, new at.logic.provers.atp.refinements.SimpleRefinement(pb), subsumMng)
  }
  def unitAutoStream(cl: String) = {
    val pb = new at.logic.utils.ds.PublishingBuffer[Clause]
    val cls = new MyParser(cl).getClauseList
    pb.insertAll(0,cls)
    val subsumMng = new at.logic.algorithms.subsumption.managers.SimpleManager(pb.asInstanceOf[at.logic.utils.ds.PublishingBuffer[at.logic.calculi.lk.base.Sequent]],
        new at.logic.algorithms.subsumption.StillmanSubsumptionAlgorithm{val matchAlg = at.logic.algorithms.matching.fol.FOLMatchingAlgorithm})
    AutomatedFOLStream(-1, new at.logic.provers.atp.refinements.UnitRefinement(pb), subsumMng)
  }
}
