package at.logic.algorithms.hlk

import at.logic.calculi.lk.base.LKProof
import at.logic.language.lambda.typedLambdaCalculus.{Abs, App, Var, LambdaExpression}
import at.logic.language.lambda.symbols._
import at.logic.language.lambda.types._
import at.logic.language.hol.logicSymbols.{LogicalSymbolsA, ConstantSymbolA}
import at.logic.language.hol._
import at.logic.calculi.lk.propositionalRules._
import at.logic.calculi.lk.base.types.FSequent
import at.logic.calculi.lk.quantificationRules._
import at.logic.calculi.lk.equationalRules._
import at.logic.calculi.lk.definitionRules.{DefinitionRightRule, DefinitionLeftRule}
import sun.management.ConnectorAddressLink

object LatexProofExporter extends HybridLatexExporter(true)
object HybridLatexExporter extends HybridLatexExporter(false)

class HybridLatexExporter(val expandTex : Boolean) {
  def nameToLatexString(s:String, escapebrack : Boolean = true) : String = {
    val s1 = at.logic.utils.latex.nameToLatexString(s)
    //val s2 = if (escapebrack) s1.replaceAll("\\[","(").replaceAll("\\]",")") else s1
    val s2 = if (s == "!=") "\\neq" else s1
    val s3 = if (s2 != "-") s2.replaceAll("-","") else s2
    s3
  }


  def apply(lkp : LKProof, escape_latex : Boolean =false) = {
    val declarations = generateDeclarations(lkp)
    val proofs = generateProof(lkp, "", escape_latex)

    declarations + "\n\\CONSTDEC{THEPROOF}{o}\n\n" + proofs + "\\CONTINUEWITH{THEPROOF}"
  }

  def generateDeclarations(p: LKProof) : String= {
    val formulas = p.nodes.flatMap(_.asInstanceOf[LKProof].root.toFSequent.formulas)
    val types = formulas.foldLeft(Map[SymbolA,TA]())((map,f) => getTypes(f, map)).filterNot(_._1.isInstanceOf[LogicalSymbolsA])
    val (vars,   rest1) = types.partition( p => p._1.isInstanceOf[VariableSymbolA])
    //val (defs, rest2) = rest1.partition( p => p._1.isInstanceOf[ConstantSymbolA] && p._1.toString.contains("["))
    val (consts, rest3) = rest1.partition( p => p._1.isInstanceOf[ConstantSymbolA])
    require(rest3.isEmpty, "There are symbols which are neither variables nor constants in the formulas: "+rest3)

    val svars = vars.foldLeft(Map[String, String]())((map, p) => {
      val vname = nameToLatexString(p._1.toString)
      if (map contains vname) throw new Exception("Two different kinds of symbol share the same name!")
      map + ((vname, getTypeString(p._2)))
    })
    val sconsts = consts.foldLeft(Map[String, String]())((map, p) => {
      val vname = nameToLatexString(p._1.toString)
      if (map contains vname) throw new Exception("Two different kinds of symbol share the same name!")
      map + ((vname, getTypeString(p._2)))
    }).filterNot(_._1 == "=")
    /*
    val sdefs = defs.foldLeft(Map[String, String]())((map, p) => {
      val w = "[a-zA-Z0-9]+"
      val re= ("("+w+")\\[("+w+"(,"+w+")*"+")\\]").r
      val vname = nameToLatexString(p._1.toString, false)
      if (map contains vname) throw new Exception("Two different kinds of symbol share the same name!")
      map + ((vname, getTypeString(p._2)))
    })*/


    val rvmap = svars.foldLeft(Map[String,List[String]]())((map,p) => {
      val (name, expt) = p
      if (map contains expt)
        map + ((expt, name::map(expt)))
      else
        map + ((expt, name::Nil))
    })

    val rcmap = sconsts.foldLeft(Map[String,List[String]]())((map,p) => {
      val (name, expt) = p
      if (map contains expt)
        map + ((expt, name::map(expt)))
      else
        map + ((expt, name::Nil))
    })

    val sv = rvmap.map( x => "\\VARDEC{"+x._2.mkString(", ")+"}{"+x._1+"}")
    val sc = rcmap.map( x => "\\CONSTDEC{"+x._2.mkString(", ")+"}{"+x._1+"}")
    sv.mkString("\n") +"\n" + sc.mkString("\n")
  }

  def getTypes(exp : LambdaExpression, map : Map[SymbolA, TA]) : Map[SymbolA, TA] = exp match {
    case Var(name:LogicalSymbolsA, exptype) => map
    case Var(name, exptype) =>
      if (map.contains(name)) {
        if (map(name) != exptype) throw new Exception("Symbol clash for "+name+" "+map(name)+" != "+exptype)
        map
      } else {
        map + ((name, exptype))
      }


    case App(s,t) =>
      getTypes(s, getTypes(t, map))

    case Abs(x,t) =>
      getTypes(x,getTypes(t,map))
  }

  def getTypeString(t:TA, outermost : Boolean = true) : String = t match {
    case Ti() => "i"
    case To() => "o"
    case Tindex() => "w"
    case t1 -> t2 =>
      val s = getTypeString(t1,false) + ">" + getTypeString(t2,false)
      if (outermost) s else "("+s+")"
  }

  def getFormulaString(f : HOLExpression, outermost : Boolean = true, escape_latex:Boolean) : String = f match {
    case AllVar(x,t) =>
      val op = if (escape_latex) "\\forall" else "all"
      "("+op+ " "+ getFormulaString(x.asInstanceOf[HOLVar],false, escape_latex) +  " " + getFormulaString(t,false, escape_latex) + ")"
    case ExVar(x,t) =>
      val op = if (escape_latex) "\\exists" else "exists"
      "(" + op+" "+getFormulaString(x.asInstanceOf[HOLVar],false, escape_latex) +  " " + getFormulaString(t,false,escape_latex) + ")"
    case Neg(t1) =>
      val op = if (escape_latex) "\\neg" else "-"
      val str = " "+op+" " + getFormulaString(t1,false,escape_latex)
      if (outermost) str else "(" + str + ")"
    case And(t1,t2) =>
      val op = if (escape_latex) "\\land" else "&"
      val str = getFormulaString(t1,false,escape_latex) +  " " +op+ " " + getFormulaString(t2,false,escape_latex)
      if (outermost) str else "(" + str + ")"
    case Or(t1,t2) =>
      val op = if (escape_latex) "\\lor" else "|"
      val str = getFormulaString(t1,false,escape_latex) +  " " +op+ " " + getFormulaString(t2,false,escape_latex)
      if (outermost) str else "(" + str + ")"
    case Imp(t1,t2) =>
      val op = if (escape_latex) "\\impl" else "->"
      val str = getFormulaString(t1,false,escape_latex) +  " " +op+ " " + getFormulaString(t2,false,escape_latex)
      if (outermost) str else "(" + str + ")"

    case Atom(sym, args) =>
      val str : String =
        if (args.length == 2 && sym.toString.matches("""(<|>|\\leq|\\geq|=|>=|<=)"""))
          "(" + getFormulaString(args(0), false, escape_latex) +" "+nameToLatexString(sym.toString)+" "+
            getFormulaString(args(1),false, escape_latex) + ")"
        else
          nameToLatexString(sym.toString) + (if (args.isEmpty) " " else args.map(getFormulaString(_, false,escape_latex)).mkString("(",", ",")"))
      //if (outermost) str else "(" + str + ")"
      str
    case Function(sym, args, _) =>
        if (args.length == 2 && sym.toString.matches("""[+\-*/]"""))
          "("+getFormulaString(args(0), false,escape_latex) +" "+sym.toString+" "+getFormulaString(args(1),false,escape_latex)+")"
        else {
          if (args.isEmpty)
            nameToLatexString(sym.toString)
          else
            nameToLatexString(sym.toString) + (if (args.isEmpty) " " else args.map(getFormulaString(_, false,escape_latex)).mkString("(",", ",")"))
        }

    case HOLVar(v,_) => v.toString
    case HOLConst(c, _) => c.toString
    case HOLAbs(x,t) =>
      "(\\lambda " + getFormulaString(x.asInstanceOf[HOLVar],false,escape_latex) +  " " + getFormulaString(t,false,escape_latex) + ")"
    case HOLApp(s,t) =>
      if (escape_latex)
        "\\apply{ " + getFormulaString(s,false,escape_latex) + " " + getFormulaString(t,false,escape_latex) + "}"
      else
        "(@ " + getFormulaString(s,false,escape_latex) + " " + getFormulaString(t,false,escape_latex) + ")"

  }

  def fsequentString(fs: FSequent, escape_latex:Boolean) : String =
    fs.antecedent.map(getFormulaString(_, true,escape_latex)).mkString("{",",","}")+
      fs.succedent.map(getFormulaString(_, true, escape_latex)).mkString("{",",","}")

  def generateProof(p: LKProof, s:String, escape_latex:Boolean) : String= p match {
    case Axiom(root) =>
      "\\AX"+fsequentString(root.toFSequent, escape_latex)+"\n"+s
    // unary rules
    case NegLeftRule(p1,root, _,_) =>
      generateProof(p1, "\\NEGL"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case NegRightRule(p1,root, _,_) =>
      generateProof(p1, "\\NEGR"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case AndLeft1Rule(p1,root, _,_) =>
      generateProof(p1, "\\ANDL"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case AndLeft2Rule(p1,root, _,_) =>
      generateProof(p1, "\\ANDL"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case OrRight1Rule(p1,root, _,_) =>
      generateProof(p1, "\\ORR"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case OrRight2Rule(p1,root, _,_) =>
      generateProof(p1, "\\ORR"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case ImpRightRule(p1,root, _,_ , _) =>
      generateProof(p1, "\\IMPR"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    //binary rules
    case AndRightRule(p1,p2, root, _,_,_) =>
      generateProof(p1, generateProof(p2, "\\ANDR"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex), escape_latex)
    case OrLeftRule(p1,p2, root, _,_,_) =>
      generateProof(p1, generateProof(p2, "\\ORL"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex), escape_latex)
    case ImpLeftRule(p1,p2, root, _,_,_) =>
      generateProof(p1, generateProof(p2, "\\IMPL"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex), escape_latex)
    //structural rules
    case CutRule(p1,p2, root, _,_) =>
      generateProof(p1, generateProof(p2, "\\CUT"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex), escape_latex)
    case WeakeningLeftRule(p1,root, _) =>
      generateProof(p1, "\\WEAKL"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case WeakeningRightRule(p1,root, _) =>
      generateProof(p1, "\\WEAKR"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case ContractionLeftRule(p1,root, _,_,_) =>
      generateProof(p1, "\\CONTRL"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case ContractionRightRule(p1,root, _,_,_) =>
      generateProof(p1, "\\CONTRR"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    //quantifier rules
    case ForallLeftRule(p1,root, aux, main, term) =>
      generateProof(p1, "\\ALLL{"+ getFormulaString(term,true, escape_latex) + "}" +fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case ForallRightRule(p1,root, aux, main, term) =>
      generateProof(p1, "\\ALLR{"+ getFormulaString(term,true, escape_latex) + "}" +fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case ExistsLeftRule(p1,root, aux, main, term) =>
      generateProof(p1, "\\EXL{"+ getFormulaString(term,true, escape_latex) + "}" +fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case ExistsRightRule(p1,root, aux, main, term) =>
      generateProof(p1, "\\EXR{"+ getFormulaString(term,true, escape_latex) + "}" +fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    //equality rules
    case EquationLeft1Rule(p1,p2, root, _,_,_) =>
      generateProof(p1, generateProof(p2, "\\EQL"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex), escape_latex)
    case EquationLeft2Rule(p1,p2, root, _,_,_) =>
      generateProof(p1, generateProof(p2, "\\EQL"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex), escape_latex)
    case EquationRight1Rule(p1,p2, root, _,_,_) =>
      generateProof(p1, generateProof(p2, "\\EQR"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex), escape_latex)
    case EquationRight2Rule(p1,p2, root, _,_,_) =>
      generateProof(p1, generateProof(p2, "\\EQR"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex), escape_latex)
    //definition rules
    case DefinitionLeftRule(p1,root, _,_ ) =>
      generateProof(p1, "\\DEF"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)
    case DefinitionRightRule(p1,root, _,_ ) =>
      generateProof(p1, "\\DEF"+fsequentString(root.toFSequent, escape_latex)+"\n"+s, escape_latex)

  }

}

object LatexCode {
  val header =
    """
\documentclass[a4paper]{article}
\\usepackage[utf8]{inputenc}
\\usepackage{amsfonts}
\\usepackage{amsthm}
\\usepackage{mathabx}
\\usepackage{graphicx}
\\usepackage[dvipsnames]{xcolor}
\\usepackage[paperwidth=510mm, paperheight=360mm, left=5mm,right=5mm,top=10mm, bottom=20mm]{geometry}
\\usepackage{bussproofs}
\definecolor{linkcolor}{rgb}{0.1,0.0,0.35}
\definecolor{citecolor}{rgb}{0.1,0.0,0.35}
\newcommand{\iremark}[1]{\todo[inline, color=todocolor]{#1}}

\\usepackage[pdftex,
            colorlinks=true,
            linkcolor=linkcolor,
            filecolor=red,
            citecolor=citecolor,
            pdfauthor={GAPT Prooftool},
            bookmarks, bookmarksnumbered=true]{hyperref}

\newcommand{\lkproves}{\ensuremath{\vdash}}
\renewcommand{\fCenter}{\lkproves}
\renewcommand{\emptyset}{{\font\cmsy = cmsy10 at 10pt \hbox{\cmsy \char 59}}}

\newcommand{\apply}[1]{#1}

\newcommand{\AX}[2]{\AxiomC{\ensuremath{#1} \fCenter \ensuremath{#2}}}
\newcommand{\UI}[2]{\UnaryInfC{\ensuremath{#1} \fCenter \ensuremath{#2}}}
\newcommand{\BI}[2]{\BinaryInfC{\ensuremath{#1} \fCenter \ensuremath{#2}}}
\newcommand{\LL}[1]{\LeftLabel{\footnotesize \ensuremath{#1}}}
\newcommand{\RL}[1]{\RightLabel{\footnotesize \ensuremath{#1}}}
\newcommand{\RLN}[1]{\RightLabel{#1}}


\newcommand{\SALLL}{\RL{\forall:l}}
\newcommand{\SALLR}{\RL{\forall:r}}
\newcommand{\SEXL}{\RL{\exists:l}}
\newcommand{\SEXR}{\RL{\exists:r}}
\newcommand{\SANDL}{\RL{\land:l}}
\newcommand{\SANDR}{\RL{\land:r}}
\newcommand{\SORL}{\RL{\lor:l}}
\newcommand{\SORR}{\RL{\lor:r}}
\newcommand{\SIMPL}{\RL{\impl:l}}
\newcommand{\SIMPR}{\RL{\impl:r}}
\newcommand{\SNEGL}{\RL{\neg:l}}
\newcommand{\SNEGR}{\RL{\neg:r}}
\newcommand{\SEQL}{\RL{=:l}}
\newcommand{\SEQR}{\RL{=:r}}
\newcommand{\SWEAKL}{\RL{w:l}}
\newcommand{\SWEAKR}{\RL{w:r}}
\newcommand{\SCONTRL}{\RL{c:l}}
\newcommand{\SCONTRR}{\RL{c:r}}
\newcommand{\SCUT}{\RL{cut}}
\newcommand{\SDEF}{\RL{def}}
\newcommand{\SBETA}{\RL{\beta}}
\newcommand{\SINSTLEMMA}[1]{\RL{LEMMA: #1}}
\newcommand{\SINSTAXIOM}[1]{\RL{AXIOM: #1}}
\newcommand{\SEQAXIOM}[1]{\RL{EQAX: #1}}

\newcommand{\ALLL}     [3]{\SALLL \UI{#2}{#3} }
\newcommand{\ALLR}     [3]{\SALLR \UI{#2}{#3} }
\newcommand{\EXL}      [3]{\SEXL  \UI{#2}{#3} }
\newcommand{\EXR}      [3]{\SEXR  \UI{#2}{#3} }
\newcommand{\ANDL}     [2]{\SANDL \UI{#1}{#2} }
\newcommand{\ANDR}     [2]{\SANDR \BI{#1}{#2} }
\newcommand{\ORL}      [2]{\SORL  \BI{#1}{#2} }
\newcommand{\ORR}      [2]{\SORR  \UI{#1}{#2} }
\newcommand{\IMPL}     [2]{\SIMPL \BI{#1}{#2}}
\newcommand{\IMPR}     [2]{\SIMPR \UI{#1}{#2}}
\newcommand{\NEGL}     [2]{\SNEGL \UI{#1}{#2}}
\newcommand{\NEGR}     [2]{\SNEGR \UI{#1}{#2}}
\newcommand{\EQL}      [2]{\SEQL  \BI{#1}{#2}}
\newcommand{\EQR}      [2]{\SEQR  \BI{#1}{#2}}
\newcommand{\WEAKL}    [2]{\SWEAKL \UI{#1}{#2}}
\newcommand{\WEAKR}    [2]{\SWEAKR \UI{#1}{#2}}
\newcommand{\CONTRL}   [2]{\SCONTRL \UI{#1}{#2}}
\newcommand{\CONTRR}   [2]{\SCONTRR \UI{#1}{#2}}
\newcommand{\CUT}      [2]{\SCUT    \BI{#1}{#2}}
\newcommand{\DEF}      [2]{\SDEF    \UI{#1}{#2}}
\newcommand{\BETA}     [2]{\SBETA   \UI{#1}{#2}}
\newcommand{\INSTLEMMA}[3]{\SINSTLEMMA{#1} \UI{#2}{#3}}
\newcommand{\INSTAXIOM}[3]{\SINSTAXIOM{#1} \UI{#2}{#3}}
\newcommand{\EQAXIOM}  [3]{\SEQAXIOM{#1}   \UI{#2}{#3}}


\newcommand{\CONTINUEWITH}[1]{
 \noLine
 \UnaryInfC{\ensuremath{(#1)}}
}

\newcommand{\CONTINUEFROM}[3]{
 \AxiomC{\ensuremath{(#1)}}
 \noLine
 \UI{#2}{#3}
}

\newenvironment{declaration}[0]{
\section{Type Declarations}
$
\begin{array}{ll@{: }l}
}{
\end{array}
$
}
\newenvironment{theoryaxioms}[0]{
\section{Theory Axioms}
%$
%\begin{array}{ll@{\vdash }l}
}{
%\end{array}
%$
}

\newcommand{\TYPEDEC}[3]{ #1 & #2 & #3 \\}
\newcommand{\CONSTDEC}[2]{\TYPEDEC{const}{#1}{#2}}
\newcommand{\VARDEC}[2]{\TYPEDEC{var}{#1}{#2}}

\newcommand{\AXIOMDEC}[3]{#1 & #2 & #3 \\}


\newcommand{\ienc}[1]{\ensuremath{\\ulcorner{#1}\\urcorner}}
\newcommand{\benc}[1]{\ensuremath{\llcorner{#1}\lrcorner}}
\newcommand{\impl}{\ensuremath{\rightarrow}}
\newcommand{\bm}{\ensuremath{\dotdiv}}

\begin{document}
    """
}