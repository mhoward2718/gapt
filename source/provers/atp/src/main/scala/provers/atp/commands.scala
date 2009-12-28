/*
 * commands.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.provers.atp

import at.logic.calculi.resolution.base._

package commands {
  sealed abstract class Command
  case object EmptyCom extends Command
  case class InsertClausesCom(clauses: List[Clause]) extends Command
  case object GetClausesCom extends Command
  case object FailureCom extends Command
  case class ApplyOnClausesCom(clauses: Tuple2[ResolutionProof, ResolutionProof]) extends Command
  case object FactorCom extends Command
  case object ResolveCom extends Command
  case class ResolventCom(resolvent: ResolutionProof) extends Command
  case object InsertCom extends Command
  case class CorrectResolventFound(res: ResolutionProof) extends Command

  // default commands streams
  object AutomatedFOLStream {
    def apply(clauses: List[Clause]) = Stream.cons(InsertClausesCom(clauses),rest)
    def rest: Stream[Command] = Stream(
      GetClausesCom,
      ResolveCom,
      InsertCom
    ).append(rest)
  }
}
