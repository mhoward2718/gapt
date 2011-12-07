package at.logic.gui.prooftool.gui

/**
 * Created by IntelliJ IDEA.
 * User: mrukhaia
 * Date: 2/3/11
 * Time: 4:24 PM
 */

import scala.swing._
import BorderPanel._
import event._
import java.awt.Font._
import java.awt.{RenderingHints, BasicStroke}
import at.logic.calculi.treeProofs._
import ProoftoolSequentFormatter._
import java.awt.event.{MouseMotionListener, MouseEvent}
import at.logic.calculi.slk.SchemaProofLinkRule
import at.logic.calculi.lk.base.Sequent
import at.logic.calculi.occurrences.FormulaOccurrence


class DrawProof(val proof: TreeProof[_], private val fSize: Int, private var colored_occurrences : Set[FormulaOccurrence])
  extends BorderPanel with MouseMotionListener {
  background = white
  opaque = false
  private val blue = new Color(0,0,255)
  private val black = new Color(0,0,0)
  private val white = new Color(255,255,255)
  private val bd = Swing.EmptyBorder(0,fSize*3,0,fSize*3)
  private val ft = new Font(SANS_SERIF, PLAIN, fSize)
  private val labelFont = new Font(MONOSPACED, ITALIC, fSize-2)
  private val tx = proof.root match {
    case so: Sequent => //sequentToStringCutAnc(so, colored_occurrences) //modified by Cvetan
      val ds = DrawSequent(so, ft, colored_occurrences)
      ds.listenTo(mouse.moves, mouse.clicks, mouse.wheel)
      ds.reactions += {
        case e: MouseEntered => ds.contents.foreach(x => x.foreground = blue)
        case e: MouseExited => ds.contents.foreach(x => x.foreground = black)
        case e: MouseClicked => PopupMenu(proof, this, e.point.x, e.point.y)
      }
      ds
    case _ => new Label(proof.root.toString) { font = ft }
  }

  listenTo(mouse.moves, mouse.clicks, mouse.wheel)
  reactions += {
    case e: MouseDragged =>
      Main.body.cursor = new java.awt.Cursor(java.awt.Cursor.MOVE_CURSOR)
    case e: MouseReleased =>
      Main.body.cursor = java.awt.Cursor.getDefaultCursor
  }

  initialize

  // end of constructor
  def setColoredOccurrences(s : Set[FormulaOccurrence]) {
    colored_occurrences = s
    initialize
  }

  def initialize = proof match {
    case p: UnaryTreeProof[_] =>
      border = bd
      layout(new DrawProof(p.uProof.asInstanceOf[TreeProof[_]], fSize, colored_occurrences)) = Position.Center
      layout(/*new Label(tx) {
        font = ft
        listenTo(mouse.moves, mouse.clicks, mouse.wheel)
        reactions += {
          case e: MouseEntered => foreground = blue
          case e: MouseExited => foreground = black
          case e: MouseClicked => PopupMenu(proof, this, e.point.x, e.point.y)
        }
      }*/ tx) = Position.South
    case p: BinaryTreeProof[_] =>
      border = bd
      layout(new DrawProof(p.uProof1.asInstanceOf[TreeProof[_]], fSize, colored_occurrences)) = Position.West
      layout(new DrawProof(p.uProof2.asInstanceOf[TreeProof[_]], fSize, colored_occurrences)) = Position.East
      layout(/*new Label(tx) {
        font = ft
        listenTo(mouse.moves, mouse.clicks, mouse.wheel)
        reactions += {
          case e: MouseEntered => foreground = blue
          case e: MouseExited => foreground = black
          case e: MouseClicked => PopupMenu(proof, this, e.point.x, e.point.y)
        }
      }*/ tx) = Position.South
    case p: NullaryTreeProof[_] => p match {
      case SchemaProofLinkRule(_, link, indices) =>
        layout(new BoxPanel(Orientation.Vertical) {
          background = white
          contents += new Label("(" + link + ", " + formulaToString(indices.head) + ")") {
            font = ft
            xLayoutAlignment = 0.5
          }
          tx.xLayoutAlignment = 0.5
          contents += tx/*new Label(tx) {
            font = ft
            xLayoutAlignment = 0.5
          }             */
        }) = Position.South
      case _ =>
        tx.border = Swing.EmptyBorder(0,fSize,0,fSize)
        layout(/*new Label(tx) {
          border = Swing.EmptyBorder(0,fSize,0,fSize)
          font = ft
          listenTo(mouse.moves, mouse.clicks, mouse.wheel)
          reactions += {
            case e: MouseEntered => foreground = blue
            case e: MouseExited => foreground = black
            case e: MouseClicked => PopupMenu(proof, this, e.point.x, e.point.y)
          }
        }*/ tx) = Position.South
    }
  }

  def getSequentWidth = {
    var width = 0
    if (tx.isInstanceOf[Label]) width = tx.size.width
    else tx.asInstanceOf[FlowPanel].contents.foreach(x => width = width + x.size.width + 5)
    width
  }

  override def paintComponent(g: Graphics2D) = {
    import scala.math.max

    super.paintComponent(g)

    val metrics = g.getFontMetrics(ft)

    g.setFont(labelFont)
    // g.setStroke(new BasicStroke(fSize / 25))
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)

    proof match {
      case p: UnaryTreeProof[_] => {
        val center = this.layout.find(x => x._2 == Position.Center).get._1.asInstanceOf[DrawProof]
        val width = center.size.width + fSize*6
        val height = center.size.height
        val seqLength = max(center.getSequentWidth, getSequentWidth)

        /*p.root match {
          case so: Sequent =>
            max(metrics.stringWidth(sequentToString(p.uProof.root.asInstanceOf[Sequent])),
              metrics.stringWidth(sequentToString(so)))
          case _ =>
            max(metrics.stringWidth(p.uProof.root.toString),
              metrics.stringWidth(p.root.toString))
        }*/

        g.drawLine((width - seqLength) / 2, height, (width + seqLength) / 2, height)
        g.drawString(p.name, (fSize / 4 + width + seqLength) / 2, height + metrics.getMaxDescent)
      }
      case p: BinaryTreeProof[_] => {
        val left = this.layout.find(x => x._2 == Position.West).get._1.asInstanceOf[DrawProof]
        val leftWidth = left.size.width + fSize*6
        val right = this.layout.find(x => x._2 == Position.East).get._1.asInstanceOf[DrawProof]
        val rightWidth = right.size.width
        val height = max(left.size.height, right.size.height)
        val leftSeqLength = left.getSequentWidth
         /*p.uProof1.root match {
          case so: Sequent => metrics.stringWidth(sequentToString(so))
          case _ =>  metrics.stringWidth(p.uProof1.root.toString)
        }                     */
        val rightSeqLength = right.getSequentWidth
        /*p.uProof2.root match {
          case so: Sequent => metrics.stringWidth(sequentToString(so))
          case _ =>  metrics.stringWidth(p.uProof2.root.toString)
        }                      */

        val lineLength = right.location.x + (rightWidth + rightSeqLength) / 2

        g.drawLine((leftWidth - leftSeqLength) / 2, height, lineLength, height)
        g.drawString(p.name, lineLength + fSize / 4, height + metrics.getMaxDescent)
      }
      case _ =>
    }
  }

  this.peer.setAutoscrolls(true)
  this.peer.addMouseMotionListener(this)

  def mouseMoved(e: MouseEvent) {}
  def mouseDragged(e: MouseEvent) {
    //The user is dragging us, so scroll!
    val r = new Rectangle(e.getX(), e.getY(), 1, 1);
    this.peer.scrollRectToVisible(r);
  }
}
