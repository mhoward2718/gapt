/**********
 * Example proof sequences, usage example from CLI:
 *
 * scala> :load ../examples/ProofSequences.scala
 * scala> val p = LinearExampleProof( 5 )
 **********/

// Functions to construct cut-free FOL LK proofs of the sequents
//
// P(0), \ALL x . P(x) -> P(s(x)) :- P(s^n(0))
//
// where n is an Integer parameter >= 0.
object LinearExampleProof {
  val s = new ConstantStringSymbol("s")
  val p = new ConstantStringSymbol("P")
  val c = new ConstantStringSymbol("0")

  def apply( n: Int ) = proof( 0, n )

  // returns LKProof with end-sequent  P(s^k(0)), \ALL x . P(x) -> P(s(x)) :- P(s^n(0))
  def proof( k: Int, n: Int )  : LKProof =
  {
    val x = FOLVar( VariableStringSymbol("x") )
    val ass = AllVar( x, Imp( Atom( p, x::Nil ), Atom( p, Function( s, x::Nil )::Nil ) ) )
    if ( k == n ) // leaf proof
    {
      val a = Atom( p,  Utils.numeral( n )::Nil )
      WeakeningLeftRule( Axiom( a::Nil, a::Nil ), ass )
    }
    else
    {
      val p1 = Atom( p, Utils.numeral( k )::Nil )
      val p2 = Atom( p, Utils.numeral( k + 1 )::Nil )
      val aux = Imp( p1, p2 )
      ContractionLeftRule( ForallLeftRule( ImpLeftRule( Axiom( p1::Nil, p1::Nil ), proof( k + 1, n ), p1, p2 ), aux, ass, Utils.numeral( k ) ), ass )
    }
  }
}

// Functions to construct cut-free FOL LK proofs of the sequents
//
// P(0,0), \ALL x \ALL y. P(x,y) -> P(s(x),y), \ALL x \ALL y. P(x,y) -> P(x,s(y)) :- P(s^n(0),s^n(0))
//
// where n is an Integer parameter >= 0.
//
// The proofs constructed here go along the diagonal of P, i.e. one X-step, then one Y-step, etc.
object SquareDiagonalExampleProof {
  val s = new ConstantStringSymbol("s")
  val p = new ConstantStringSymbol("P")
  val c = new ConstantStringSymbol("0")

  def apply( n: Int ) = proof( 0, n )

  // returns LKProof with end-sequent  P(s^k(0),s^k(0)), \ALL x \ALL y. P(x,y) -> P(s(x),y), \ALL x \ALL y . P(x,y) -> P(x,s(y)) :- P(s^n(0),s^n(0))
  def proof( k: Int, n: Int )  : LKProof =
  {
    val x = FOLVar( VariableStringSymbol("x") )
    val y = FOLVar( VariableStringSymbol("y") )

    val assx = AllVar( x, AllVar( y, Imp( Atom( p, x::y::Nil ), Atom(p, Function( s, x::Nil )::y::Nil ) ) ) )
    def assx_aux( k: Int ) = AllVar( y, Imp( Atom( p, Utils.numeral( k )::y::Nil ), Atom(p, Utils.numeral( k + 1 )::y::Nil ) ) )

    val assy = AllVar( x, AllVar( y, Imp( Atom( p, x::y::Nil ), Atom(p, x::Function( s, y::Nil )::Nil ) ) ) )
    def assy_aux( k: Int ) = AllVar( y, Imp( Atom( p, Utils.numeral( k )::y::Nil ), Atom(p, Utils.numeral( k )::Function( s, y::Nil )::Nil ) ) )
 
    if ( k == n ) // leaf proof
    {
      val a = Atom( p, Utils.numeral( n )::Utils.numeral( n )::Nil )
      WeakeningLeftRule( WeakeningLeftRule( Axiom( a:: Nil, a::Nil ), assx ), assy )
    }
    else
    {
      val ayl = Atom( p, Utils.numeral( k + 1 )::Utils.numeral( k )::Nil ) // atom y left
      val ayr = Atom( p, Utils.numeral( k + 1 )::Utils.numeral( k + 1 )::Nil )
      val auxy = Imp( ayl, ayr )

      val p1 = ImpLeftRule( Axiom( ayl::Nil, ayl::Nil ), proof( k + 1, n), ayl, ayr )
      val p2 = ForallLeftRule( p1, auxy, assy_aux( k + 1 ), Utils.numeral( k ) )
      val p3 = ForallLeftRule( p2, assy_aux( k + 1 ), assy, Utils.numeral( k + 1) )
      val p4 = ContractionLeftRule( p3, assy )

      val axl = Atom( p, Utils.numeral( k )::Utils.numeral( k )::Nil ) // atom x left
      val axr = Atom( p, Utils.numeral( k + 1 )::Utils.numeral( k )::Nil )
      val auxx = Imp( axl, axr )

      val p5 = ImpLeftRule( Axiom( axl::Nil, axl::Nil ), p4, axl, axr )
      val p6 = ForallLeftRule( p5, auxx, assx_aux( k ), Utils.numeral( k ) )
      val p7 = ForallLeftRule( p6, assx_aux( k ), assx, Utils.numeral( k ) )
      ContractionLeftRule( p7, assx )
    }
  }
}

// Functions to construct cut-free FOL LK proofs of the sequents
//
// P(0,0), \ALL x \ALL y. P(x,y) -> P(s(x),y), \ALL x \ALL y. P(x,y) -> P(x,s(y)) :- P(s^n(0),s^n(0))
//
// where n is an Integer parameter >= 0.
//
// The proofs constructed here go along the edges of P, i.e. first all X-steps are performed, then all Y-steps are performed
object SquareEdgesExampleProof {
  val s = new ConstantStringSymbol("s")
  val p = new ConstantStringSymbol("P")
  val c = new ConstantStringSymbol("0")

  val x = FOLVar( VariableStringSymbol("x") )
  val y = FOLVar( VariableStringSymbol("y") )

  val assx = AllVar( x, AllVar( y, Imp( Atom( p, x::y::Nil ), Atom(p, Function( s, x::Nil )::y::Nil ) ) ) )
  def assx_aux( k: Int ) = AllVar( y, Imp( Atom( p, Utils.numeral( k )::y::Nil ), Atom(p, Utils.numeral( k + 1 )::y::Nil ) ) )

  val assy = AllVar( x, AllVar( y, Imp( Atom( p, x::y::Nil ), Atom(p, x::Function( s, y::Nil )::Nil ) ) ) )
  def assy_aux( k: Int ) = AllVar( y, Imp( Atom( p, Utils.numeral( k )::y::Nil ), Atom(p, Utils.numeral( k )::Function( s, y::Nil )::Nil ) ) )
 
  def apply( n: Int ) = proof( 0, n )

  // returns LKProof with end-sequent  P(s^k(0),0), \ALL x \ALL y. P(x,y) -> P(s(x),y), \ALL x \ALL y. P(x,y) -> P(x,s(y)) :- P(s^n(0),s^n(0))
  def proof( k: Int, n: Int ) : LKProof =
  {
    if ( k == n )
    {
      val p1 = ForallLeftRule( upper_proof( 0, n ), assy_aux( n ), assy, Utils.numeral( n ) )
      WeakeningLeftRule( p1, assx )
    }
    else
    {
      val pk = Atom( p, Utils.numeral( k )::Utils.numeral( 0 )::Nil )
      val pkp1 = Atom( p, Utils.numeral( k + 1 )::Utils.numeral( 0 )::Nil )
      val impl = Imp( pk, pkp1 )

      ContractionLeftRule(
        ForallLeftRule(
          ForallLeftRule(
            ImpLeftRule(
              Axiom( pk::Nil, pk::Nil ),
              proof( k + 1, n ),
            pk, pkp1 ),
          impl, assx_aux( k ), Utils.numeral( 0 )),
        assx_aux( k ), assx, Utils.numeral( k )),
      assx )
    }
  }

  // returns LKProof with end-sequent  P(s^n(0),s^k(0)), \ALL y . P(s^n(0),y) -> P(s^n(0),s(y)) :- P(s^n(0),s^n(0))
  def upper_proof( k: Int, n: Int ) : LKProof =
  {
    if ( k == n ) // leaf proof
    {
      val a = Atom( p,  Utils.numeral( n )::Utils.numeral( n )::Nil )
      WeakeningLeftRule( Axiom( a::Nil, a::Nil ), assy_aux( n ) )
    }
    else
    {
      val pk = Atom( p, Utils.numeral( n )::Utils.numeral( k )::Nil )
      val pkp1 = Atom( p, Utils.numeral( n )::Utils.numeral( k + 1 )::Nil )
      val impl = Imp( pk, pkp1 )

      ContractionLeftRule( ForallLeftRule( ImpLeftRule( Axiom( pk::Nil, pk::Nil ), upper_proof( k + 1, n ), pk, pkp1 ), impl, assy_aux( n ), Utils.numeral( k ) ), assy_aux( n ))
    }
  }
}

// Functions to construct cut-free FOL LK proofs of the sequents
//
// P(a,b), \ALL x \ALL y. P(x,y) -> P(sx(x),y), \ALL x \ALL y. P(x,y) -> P(x,sx(y)) :- P(sx^n(a),sy^n(b))
//
// where n is an Integer parameter >= 0.
//
// The proofs constructed here go along the edges of P, i.e. first all X-steps are performed, then all Y-steps are performed,
// but unlike SquareEdgesExampleProof, different functions are used for the X- and the Y-directions.
object SquareEdges2DimExampleProof {
  //separate sucessor for the x- and y-directions
  val sx = new ConstantStringSymbol("s_x")
  val sy = new ConstantStringSymbol("s_y")
  //0 of the x-axis
  val a= new ConstantStringSymbol("a")
  //0 of the y-axis
  val b = new ConstantStringSymbol("b")

  val p = new ConstantStringSymbol("P")

  val x = FOLVar( VariableStringSymbol("x") )
  val y = FOLVar( VariableStringSymbol("y") )

  //Converts integers into terms consisting of nested application of the successor function to 0
  def numeralX (n: Int) = Utils.iterateTerm(FOLConst( a ), sx, n)
  def numeralY (n: Int) = Utils.iterateTerm(FOLConst( b ), sy, n)

  val assx = AllVar( x, AllVar( y, Imp( Atom( p, x::y::Nil ), Atom(p, Function( sx, x::Nil )::y::Nil ) ) ) )
  def assx_aux( k: Int ) = AllVar( y, Imp( Atom( p, numeralX(k)::y::Nil ), Atom(p, numeralX(k + 1)::y::Nil ) ) )

  val assy = AllVar( x, AllVar( y, Imp( Atom( p, x::y::Nil ), Atom(p, x::Function( sy, y::Nil )::Nil ) ) ) )
  def assy_aux( k: Int ) = AllVar( y, Imp( Atom( p, numeralX( k )::y::Nil ), Atom(p, numeralX( k )::Function( sy, y::Nil )::Nil ) ) )
 
  def apply( n: Int ) = proof( 0, n )

  // returns LKProof with end-sequent  P(sx^k(a),0), \ALL x \ALL y. P(x,y) -> P(sx(x),y), \ALL x \ALL y. P(x,y) -> P(x,sy(y)) :- P(sx^n(a),sy^n(b))
  def proof( k: Int, n: Int ) : LKProof =
  {
    if ( k == n )
    {
      val p1 = ForallLeftRule( upper_proof( 0, n ), assy_aux( n ), assy, numeralX( n ) )
      WeakeningLeftRule( p1, assx )
    }
    else
    {
      val pk = Atom( p, numeralX(k)::numeralY(0)::Nil )
      val pkp1 = Atom( p, numeralX( k + 1)::numeralY(0)::Nil )
      val impl = Imp( pk, pkp1 )

      ContractionLeftRule(
        ForallLeftRule(
          ForallLeftRule(
            ImpLeftRule(
              Axiom( pk::Nil, pk::Nil ),
              proof( k + 1, n ),
            pk, pkp1),
          impl, assx_aux( k ), numeralY( 0 )),  //possibly not correct -> switch?
        assx_aux( k ), assx, numeralX( k )),    //same
      assx )
    }
  }

  // returns LKProof with end-sequent  P(s^n(0),s^k(0)), \ALL y . P(s^n(0),y) -> P(s^n(0),s(y)) :- P(s^n(0),s^n(0))
  //Conjecture: this is the part that goes in the Y-direction.
  def upper_proof( k: Int, n: Int ) : LKProof =
  {
    if ( k == n ) // leaf proof
    {
      val ax = Atom( p,  numeralX( n )::numeralY( n )::Nil )
      WeakeningLeftRule( Axiom( ax::Nil, ax::Nil ), assy_aux( n ) )
    }
    else
    {
      val pk = Atom( p, numeralX( n )::numeralY( k )::Nil )
      val pkp1 = Atom( p, numeralX( n )::numeralY( k + 1 )::Nil )
      val impl = Imp( pk, pkp1 )

      ContractionLeftRule(
        ForallLeftRule(
          ImpLeftRule(
            Axiom( pk::Nil, pk::Nil ),
            upper_proof( k + 1, n ),
            pk,
            pkp1),
          impl,
          assy_aux( n ),
          numeralY( k )), //possibly not correct: switch or maybe restructure.
        assy_aux( n ))
    }
  }
}


// Functions to construct the straightforward cut-free FOL LK proofs of the sequents
//
// P(s^n(0),0), \ALL x \ALL y . P(s(x),y) -> P(x,s(y)) :- P(0,s^n(0))
//
// where n is an Integer parameter >= 0.
object SumExampleProof {
  val s = new ConstantStringSymbol("s")
  val p = new ConstantStringSymbol("P")

  val x = FOLVar( VariableStringSymbol("x") )
  val y = FOLVar( VariableStringSymbol("y") )

  val ass = AllVar( x, AllVar( y, Imp( Atom( p, Function( s, x::Nil )::y::Nil ), Atom( p, x::Function( s, y::Nil )::Nil ) ) ) )
  def ass_inst( x: Int ) = AllVar( y, Imp( Atom( p, Function( s, Utils.numeral( x )::Nil )::y::Nil ), Atom( p, Utils.numeral( x )::Function( s, y::Nil )::Nil ) ) )
  def ass_inst_inst( x: Int, y: Int ) = Imp( Atom( p, Function( s, Utils.numeral( x )::Nil )::Utils.numeral( y )::Nil ), Atom( p, Utils.numeral( x )::Function( s, Utils.numeral( y )::Nil )::Nil ) )

  def apply( n: Int ) = proof( 0, n )

  // returns LKProof with end-sequent  P(s^{n-k}(0),s^k(0)), \ALL x \ALL y. P(s(x),y) -> P(x,s(y)) :- P(0,s^n(0))
  def proof( k: Int, n: Int )  : LKProof =
  {
    if ( k == n ) // leaf proof
    {
      val a = Atom( p, Utils.numeral( 0 )::Utils.numeral( n )::Nil )
      WeakeningLeftRule( Axiom( a::Nil, a::Nil ), ass )
    }
    else
    {
      val a1 = Atom( p, Utils.numeral( n - k )::Utils.numeral( k )::Nil )
      val a2 = Atom( p, Utils.numeral( n - (k + 1) )::Utils.numeral( k + 1 )::Nil )

      ContractionLeftRule(
        ForallLeftRule(
          ForallLeftRule(
            ImpLeftRule(
              Axiom( a1::Nil, a1::Nil ),
              proof( k + 1, n ),
            a1, a2 ),
          ass_inst_inst( n - (k + 1), k ), ass_inst( n - (k + 1) ), Utils.numeral( k ) ),
        ass_inst( n - (k + 1)), ass, Utils.numeral( n - (k + 1) ) ),
      ass )
    }
  }
}

// Functions to construct cut-free FOL LK proofs of the sequents
//
// Refl, Trans, \ALL x. f(x) = x :- f^n(a) = a
//
// where n is an Integer parameter >= 0.
object LinearEqExampleProof {
  val a = new ConstantStringSymbol("a")
  val f = new ConstantStringSymbol("f")
  val eq = new ConstantStringSymbol("=")

  val x = FOLVar( VariableStringSymbol( "x" ))
  val y = FOLVar( VariableStringSymbol( "y" ))
  val z = FOLVar( VariableStringSymbol( "z" ))

  val Refl = AllVar( x, Atom( eq, x::x::Nil ))
  val Ass = AllVar( x, Atom( eq, Function( f, x::Nil )::x::Nil ))
  val Trans = AllVar( x, AllVar( y, AllVar( z, Imp( Atom( eq, x::y::Nil ), Imp( Atom( eq, y::z::Nil ), Atom( eq, x::z::Nil ) ) ) ) ) )

  def apply( n: Int ) = proof( n )

  // returns LKProof with end-sequent  Refl, Trans, \ALL x. f(x) = x :- f^k(a) = a
  def proof( k: Int )  : LKProof = {
    if ( k == 0 ) // leaf proof
    {
      val a_eq_a = Atom( eq, Utils.iterateTerm( FOLConst( a ), f, 0 )::Utils.iterateTerm( FOLConst( a ), f, 0 )::Nil )
      WeakeningLeftRule( WeakeningLeftRule( ForallLeftRule( Axiom( a_eq_a::Nil, a_eq_a::Nil ), a_eq_a, Refl, FOLConst( a ) ), Trans ), Ass )
    }
    else
    {
      // atoms
      val ka_eq_a = Atom( eq, Utils.iterateTerm( FOLConst( a ), f, k )::Utils.iterateTerm( FOLConst( a ), f, 0 )::Nil )
      val ka_eq_ka = Atom( eq, Utils.iterateTerm( FOLConst( a ), f, k )::Utils.iterateTerm( FOLConst( a ), f, k )::Nil )
      val kma_eq_a = Atom( eq, Utils.iterateTerm( FOLConst( a ), f, k-1 )::Utils.iterateTerm( FOLConst( a ), f, 0 )::Nil )
      val ka_eq_kma = Atom( eq, Utils.iterateTerm( FOLConst( a ), f, k )::Utils.iterateTerm( FOLConst( a ), f, k-1 )::Nil )
      val ka_eq_z = Atom( eq, Utils.iterateTerm( FOLConst( a ), f, k )::z::Nil )
      val kma_eq_z = Atom( eq, Utils.iterateTerm( FOLConst( a ), f, k-1 )::z::Nil )
      val y_eq_z = Atom( eq, y::z::Nil )
      val ka_eq_y = Atom( eq, Utils.iterateTerm( FOLConst( a ), f, k )::y::Nil )
      val x_eq_y = Atom( eq, x::y::Nil )
      val x_eq_z = Atom( eq, x::z::Nil )
      
      // prop. formulas
      val Trans2 = Imp( kma_eq_a, ka_eq_a )
      val Trans3 = Imp( ka_eq_kma, Trans2 )

      // quant. formulas
      val Trans3_1 = AllVar( z, Imp( ka_eq_kma, Imp( kma_eq_z, ka_eq_z ) ) )
      val Trans3_2 = AllVar( y, AllVar( z, Imp( ka_eq_y, Imp( y_eq_z, ka_eq_z ) ) ) )
      val Trans3_3 = AllVar( x, AllVar( y, AllVar( z, Imp( x_eq_y, Imp( y_eq_z, x_eq_z ) ) ) ) )

      // prop. proofs
      val p1 = ImpLeftRule( proof( k-1 ), Axiom( ka_eq_a::Nil, ka_eq_a::Nil ), kma_eq_a, ka_eq_a )

      val p0 = Axiom( ka_eq_kma::Nil, ka_eq_kma::Nil )

      val p2 = ImpLeftRule( p0, p1, ka_eq_kma, Trans2 )

      // proofs containing quantifiers
      val p3 = ForallLeftRule( p2, Trans3, Trans3_1, Utils.iterateTerm( FOLConst( a ), f, 0 ) )
      val p4 = ForallLeftRule( p3, Trans3_1, Trans3_2, Utils.iterateTerm( FOLConst( a ), f, k-1 ) )
      val p5 = ForallLeftRule( p4, Trans3_2, Trans3_3, Utils.iterateTerm( FOLConst( a ), f, k ) )

      val p6 = ForallLeftRule( p5, ka_eq_kma, Ass, Utils.iterateTerm( FOLConst( a ), f, k-1 ) )
     
      val p7 = ContractionLeftRule( p6, Ass )
      val p8 = ContractionLeftRule( p7, Trans )

      p8
    }
  }
}

object SumOfOnesF2ExampleProof {
  val eq = new ConstantStringSymbol("=")
  val s = new ConstantStringSymbol("s")
  val zero = new ConstantStringSymbol("0")
  val p = new ConstantStringSymbol("+")
  var f = new ConstantStringSymbol("f")

  val x = FOLVar( VariableStringSymbol( "x" ))
  val y = FOLVar( VariableStringSymbol( "y" ))
  val z = FOLVar( VariableStringSymbol( "z" ))

  //Helpers
  def Fn(n: Int) = Function(f, Utils.numeral(n)::Nil)

  //Forall x.(x + 1 = s(x)) (reversed to avoid the application of the symmetry of =)
  val Plus = AllVar(x, Atom(eq, Function(p, x::Utils.numeral(1)::Nil)::Function(s, x::Nil)::Nil))
  def PlusX(x:FOLTerm) = Atom(eq, Function(p, x::Utils.numeral(1)::Nil)::Function(s, x::Nil)::Nil)

  //Forall xyz.(y=z -> (x+y=x+z))
  val EqPlus = AllVar(x, AllVar(y, AllVar(z, Imp(Atom(eq, y::z::Nil), Atom(eq, Function(p, y::x::Nil)::Function(p, z::x::Nil)::Nil) ) )))
  def EqPlusX(x:FOLTerm) = AllVar(y, AllVar(z, Imp(Atom(eq, y::z::Nil), Atom(eq, Function(p, y::x::Nil)::Function(p, z::x::Nil)::Nil) ) ))
  def EqPlusXY(x:FOLTerm, y:FOLTerm) = AllVar(z, Imp(Atom(eq, y::z::Nil), Atom(eq, Function(p, y::x::Nil)::Function(p, z::x::Nil)::Nil) ) )
  def EqPlusXYZ(x:FOLTerm, y:FOLTerm, z:FOLTerm) = Imp(Atom(eq, y::z::Nil), Atom(eq, Function(p, y::x::Nil)::Function(p, z::x::Nil)::Nil) )

  //Forall xyz.(x = y ^ y = z -> x = z)
  val Trans = AllVar(x, AllVar(y, AllVar(z, Imp(And(Atom(eq, x::y::Nil) , Atom(eq, y::z::Nil) ), Atom(eq, x::z::Nil)))))

  //Definition of f
  //f(0) = 0
  val FZero = Atom(eq, Function(f, Utils.numeral(0)::Nil)::Utils.numeral(0)::Nil)
  //Forall x.f(s(x)) = f(x) + s(0)
  val FSucc = AllVar(x, Atom(eq, Function(f, Function(s, x::Nil)::Nil)::Function(p, Function(f, x::Nil)::Utils.numeral(1)::Nil)::Nil))
  def FSuccX(x:FOLTerm) = Atom(eq, Function(f, Function(s, x::Nil)::Nil)::Function(p, Function(f, x::Nil)::Utils.numeral(1)::Nil)::Nil)

  //The starting axiom f(n) = n |- f(n) = n
  def start(n: Int) = Axiom(Atom(eq, Fn(n)::Utils.numeral(n)::Nil)::Trans::Plus::EqPlus::FSucc::Nil, Atom(eq, Fn(n)::Utils.numeral(n)::Nil)::Nil)

  def apply(n: Int) = RecProof(start(n), n)

  /** Recursively constructs the proof, starting with the proof s1.
    */
  def RecProof(s1: LKProof, n: Int) : LKProof = {
    if (n <= 0) { s1 }
    else {

      val fn_eq_n = Atom(eq, Fn(n-1)::Utils.numeral(n-1)::Nil)
      val fn_s0 = Function(p, Fn(n-1)::Utils.numeral(1)::Nil)
      val n_s0 = Function(p, Utils.numeral(n-1)::Utils.numeral(1)::Nil)

      val tr = TransRule(eq, Fn(n), n_s0, Utils.numeral(n), s1)

      val tr2 = TransRule(eq, Fn(n), fn_s0, n_s0, tr)

      val impl = ImpLeftRule(Axiom(fn_eq_n::Nil, fn_eq_n::Nil), tr2, fn_eq_n, Atom(eq, fn_s0::n_s0::Nil))

      //Instantiate FSucc
      val allQFSucc = ForallLeftRule(impl, FSuccX(Utils.numeral(n-1)) , FSucc, Utils.numeral(n-1))
      val clFSucc = ContractionLeftRule(allQFSucc, FSucc)

      //Instantiate Plus
      val allQPlus = ForallLeftRule(clFSucc, PlusX(Utils.numeral(n-1)) , Plus, Utils.numeral(n-1))
      val clPlus = ContractionLeftRule(allQPlus, Plus)

      //Instantiare EqPlus (x=(s0), y=Fn(n-1), z=n-1)
      val eqx = Utils.numeral(1)
      val eqy = Fn(n-1)
      val eqz = Utils.numeral(n-1)

      val allQEqPlusZ = ForallLeftRule(clPlus, EqPlusXYZ(eqx, eqy, eqz) , EqPlusXY(eqx, eqy), eqz)
      val allQEqPlusYZ = ForallLeftRule(allQEqPlusZ, EqPlusXY(eqx, eqy) , EqPlusX(eqx), eqy)
      val allQEqPlusXYZ = ForallLeftRule(allQEqPlusYZ, EqPlusX(eqx) , EqPlus, eqx)
      val clEqPlus = ContractionLeftRule(allQEqPlusXYZ, EqPlus)

      RecProof(clEqPlus, n-1)
    }
  }
}


/** Constructs the cut-free FOL LK proof of the sequent
  * 
  * AUX, f(0) = 0, Forall x.f(s(x)) = f(x) + s(0) |- f(s^n(0)) = s^n(0)
  * Where AUX is {Transitivity, Symmetry, Reflexity of =,
  *               Forall xy.x=y -> s(x) = s(y), f(0) = 0, Forall x.f(s(x)) = f(x) + s(0)}
  */
object SumOfOnesFExampleProof {
  val eq = new ConstantStringSymbol("=")
  val s = new ConstantStringSymbol("s")
  val zero = new ConstantStringSymbol("0")
  val p = new ConstantStringSymbol("+")
  var f = new ConstantStringSymbol("f")

  val x = FOLVar( VariableStringSymbol( "x" ))
  val y = FOLVar( VariableStringSymbol( "y" ))
  val z = FOLVar( VariableStringSymbol( "z" ))

  //Helpers
  def Fn(n: Int) = Function(f, Utils.numeral(n)::Nil)

  //Forall xyz.(x = y ^ y = z -> x = z)
  val Trans = AllVar(x, AllVar(y, AllVar(z, Imp(And(Atom(eq, x::y::Nil) , Atom(eq, y::z::Nil) ), Atom(eq, x::z::Nil)))))

  //Forall xy.(x=y -> s(x) = s(y))
  val CongSucc = AllVar(x, AllVar(y, Imp( Atom(eq, x::y::Nil), Atom(eq, Function(s, x::Nil)::Function(s, y::Nil)::Nil))))
  def CongSuccX(x:FOLTerm) = AllVar(y, Imp( Atom(eq, x::y::Nil), Atom(eq, Function(s, x::Nil)::Function(s, y::Nil)::Nil)))
  def CongSuccXY(x:FOLTerm, y:FOLTerm) = Imp( Atom(eq, x::y::Nil), Atom(eq, Function(s, x::Nil)::Function(s, y::Nil)::Nil))

  //Forall x.(x + 1 = s(x)) (reversed to avoid the application of the symmetry of =)
  val Plus = AllVar(x, Atom(eq, Function(p, x::Utils.numeral(1)::Nil)::Function(s, x::Nil)::Nil))
  def PlusX(x:FOLTerm) = Atom(eq, Function(p, x::Utils.numeral(1)::Nil)::Function(s, x::Nil)::Nil)

  //Definition of f
  //f(0) = 0
  val FZero = Atom(eq, Function(f, Utils.numeral(0)::Nil)::Utils.numeral(0)::Nil)
  //Forall x.f(s(x)) = f(x) + s(0)
  val FSucc = AllVar(x, Atom(eq, Function(f, Function(s, x::Nil)::Nil)::Function(p, Function(f, x::Nil)::Utils.numeral(1)::Nil)::Nil))
  def FSuccX(x:FOLTerm) = Atom(eq, Function(f, Function(s, x::Nil)::Nil)::Function(p, Function(f, x::Nil)::Utils.numeral(1)::Nil)::Nil)


  //The starting axiom f(n) = n |- f(n) = n
  def start(n: Int) = Axiom(Atom(eq, Fn(n)::Utils.numeral(n)::Nil)::Trans::Plus::CongSucc::FSucc::Nil, Atom(eq, Fn(n)::Utils.numeral(n)::Nil)::Nil)

  def apply(n: Int) = proof(n)
  def proof (n: Int) = TermGenProof(EqChainProof(start(n), n), 0, n)

  /** Generates a sequent containing, in addition to the formulas in the bottommost sequent of s1,
    * the chain of equations f(n) = s(f(n-1)),...,f(1)=s(f(0)), f(0) = 0.s
    * The generates proof employs only the axiom of transitivity and (x=y -> s(x) = s(y)))
    *
    * TODO should be private - but scala shell does not allow access modifiers when :loading a file
    */
  def EqChainProof (s1: LKProof,  n: Int) : LKProof = {
    if (n <= 0) { s1 }
    else {
      val tr = TransRule(eq, Fn(n), Utils.iterateTerm(Fn(n-1), s, 1), Utils.numeral(n), s1)

      val ax2 = Axiom(Atom(eq, Fn(n-1)::Utils.numeral(n-1)::Nil)::Nil, Atom(eq, Fn(n-1)::Utils.numeral(n-1)::Nil)::Nil)

      //Introduces the instantiated form of CongSuc
      val impl = ImpLeftRule(ax2, tr, Atom(eq, Fn(n-1)::Utils.numeral(n-1)::Nil), Atom(eq, Utils.iterateTerm(Fn(n-1), s, 1)::Utils.numeral(n)::Nil))

      //Quantify CongSucc
      val cong1 = ForallLeftRule(impl, CongSuccXY(Fn(n-1), Utils.numeral(n-1)), CongSuccX(Fn(n-1)), Utils.numeral(n-1))
      val cong2 = ForallLeftRule(cong1, CongSuccX(Fn(n-1)), CongSucc, Fn(n-1))

      val cl = ContractionLeftRule(cong2, CongSucc)

      EqChainProof(cl, n-1)
    }
  }

  /** Given a proof s1, produced by EqChainProof, generates a proof that
    * eliminates the chains of equasions and proves the final sequent
    * FZero, FSucc, TR, Plus |- f(n) = n.
    *
    * TODO should be private - but scala shell does not allow access modifiers when :loading a file
    */
  def TermGenProof (s1: LKProof, n: Int, targetN: Int) : LKProof = {
    if (n >= targetN) { s1 }
    else {

      val tr = TransRule(eq, Fn(n+1), Function(p, Fn(n)::Utils.numeral(1)::Nil), Utils.iterateTerm(Fn(n), s, 1), s1)

      //Quantify plus
      val plus = ForallLeftRule(tr, PlusX(Fn(n)), Plus, Fn(n))
      val clPlus = ContractionLeftRule(plus, Plus)

      //Quantify fsucc
      val fsucc = ForallLeftRule(clPlus, FSuccX(Utils.numeral(n)), FSucc, Utils.numeral(n))
      val clFSucc = ContractionLeftRule(fsucc, FSucc)

      TermGenProof(clFSucc, n+1, targetN)
    }

  }


}

// Functions to construct cut-free FOL LK proofs of the sequents
//
// Refl, Trans, CongSuc, ABase, ASuc, :- sum( n ) = s^n(0)
//
// where n is an Integer parameter >= 0.
object SumOfOnesExampleProof {
  val eq = new ConstantStringSymbol("=")
  val s = new ConstantStringSymbol("s")
  val zero = new ConstantStringSymbol("0")
  val p = new ConstantStringSymbol("+")

  val x = FOLVar( VariableStringSymbol( "x" ))
  val y = FOLVar( VariableStringSymbol( "y" ))
  val z = FOLVar( VariableStringSymbol( "z" ))

  // axioms
  val Refl = AllVar( x, Atom( eq, x::x::Nil ))
  val Trans = AllVar( x, AllVar( y, AllVar( z, Imp( Atom( eq, x::y::Nil ), Imp( Atom( eq, y::z::Nil ), Atom( eq, x::z::Nil ) ) ) ) ) )
  val CongSuc = AllVar( x, AllVar( y, Imp( Atom( eq, x::y::Nil ), Atom( eq, Function( s, x::Nil )::Function( s, y::Nil )::Nil ) ) ) )
  val ABase = AllVar( x, Atom( eq, Function( p, x::FOLConst( zero )::Nil )::x::Nil ) )
  val ASuc = AllVar( x, AllVar( y, Atom( eq, Function( p, x::Function( s, y::Nil )::Nil )::Function( s, Function( p, x::y::Nil )::Nil )::Nil ) ) )

  def apply( n: Int ) = proof( n )

  // TODO should be private - but scala shell does not allow access modifiers when :loading a file
  def proof( k: Int ) : LKProof = {
    if ( k == 0 )
    {
      val zero_eq_zero = Atom( eq, Utils.numeral( 0 )::Utils.numeral( 0 )::Nil )
      val p1 = ForallLeftRule( Axiom( zero_eq_zero::Nil, zero_eq_zero::Nil ), zero_eq_zero, Refl, Utils.numeral( 0 ) )
      val p2 = WeakeningLeftRule( p1, Trans )
      val p3 = WeakeningLeftRule( p2, CongSuc )
      val p4 = WeakeningLeftRule( p3, ABase )
      WeakeningLeftRule( p4, ASuc )
    }
    else if ( k == 1 )
    {
      val one_eq_one = Atom( eq, Utils.numeral( 1 )::Utils.numeral( 1 )::Nil )
      val p1 = ForallLeftRule( Axiom( one_eq_one::Nil, one_eq_one::Nil ), one_eq_one, Refl, Utils.numeral( 1 ) )
      val p2 = WeakeningLeftRule( p1, Trans )
      val p3 = WeakeningLeftRule( p2, CongSuc )
      val p4 = WeakeningLeftRule( p3, ABase )
      WeakeningLeftRule( p4, ASuc )
    }
    else
    {
      /// atoms
      val ssumkm1_eq_k = Atom( eq, Function( s, sum( k-1 )::Nil )::Utils.numeral( k )::Nil )
      val ssumkm1_eq_z = Atom( eq, Function( s, sum( k-1 )::Nil )::z::Nil )
      val sumk_eq_k = Atom( eq, sum( k )::Utils.numeral( k )::Nil )
      val sumk_eq_y = Atom( eq, sum( k )::y::Nil )
      val sumk_eq_z = Atom( eq, sum( k )::z::Nil )
      val y_eq_z = Atom( eq, y::z::Nil )
      val sumk_eq_ssumkm1 = Atom( eq, sum( k )::Function( s, sum( k-1 )::Nil )::Nil )
      val sumkm1_eq_km1 = Atom( eq, sum( k-1 )::Utils.numeral( k-1 )::Nil )
      val sumkm1_eq_y = Atom( eq, sum( k-1 )::y::Nil )
      val ssumkm1_eq_sy = Atom( eq, Function( s, sum( k-1 )::Nil )::Function( s, y::Nil )::Nil )
      
      /// prop. formulas
      val Trans2 = Imp( ssumkm1_eq_k, sumk_eq_k )
      val Trans3 = Imp( sumk_eq_ssumkm1, Trans2 )
      val CongSuc2 = Imp( sumkm1_eq_km1, ssumkm1_eq_k )

      /// quant. formulas
      val Trans3_1 = AllVar( z, Imp( sumk_eq_ssumkm1, Imp( ssumkm1_eq_z, sumk_eq_z ) ) )
      val Trans3_2 = AllVar( y, AllVar( z, Imp( sumk_eq_y, Imp( y_eq_z, sumk_eq_z ) ) ) )
      val CongSuc2_1 = AllVar( y, Imp( sumkm1_eq_y, ssumkm1_eq_sy ) )

      /// proof
      // transitivity (using aux_proof)
      val p1 = Axiom( ssumkm1_eq_k::Nil, ssumkm1_eq_k::Nil )
      val p2 = Axiom( sumk_eq_k::Nil, sumk_eq_k::Nil )
      val p3 = ImpLeftRule( p1, p2, ssumkm1_eq_k, sumk_eq_k)
      val p4 = aux_proof( k-1 )
      val p5 = ImpLeftRule( p4, p3, sumk_eq_ssumkm1, Trans2 )
      val p6 = ForallLeftRule( p5, Trans3, Trans3_1, Utils.numeral( k ) )
      val p7 = ForallLeftRule( p6, Trans3_1, Trans3_2, Function( s, sum( k-1 )::Nil ) )
      val p8 = ForallLeftRule( p7, Trans3_2, Trans, sum( k ) )
      val p9 = ContractionLeftRule( p8, Trans )

      // congruence sucessor (using IH)
      val p10 = proof( k-1 )
      val p11 = ImpLeftRule( p10, p9, sumkm1_eq_km1, ssumkm1_eq_k )
      val p12 = ContractionLeftRule( p11, Trans )
      val p13 = ContractionLeftRule( p12, CongSuc )
      val p14 = ContractionLeftRule( p13, ASuc )
      val p15 = ContractionLeftRule( p14, ABase )
      val p16 = ForallLeftRule( p15, CongSuc2, CongSuc2_1, Utils.numeral( k-1 ) )
      val p17 = ForallLeftRule( p16, CongSuc2_1, CongSuc, sum( k-1 ) )
      ContractionLeftRule( p17, CongSuc )
    }
  }

  // constructs proof of: Trans, CongSuc, ASuc, ABase :- sum( k + 1 ) = s( sum( k ) )
  // TODO should be private - but scala shell does not allow access modifiers when :loading a file
  def aux_proof( k: Int ) : LKProof = {
    /// atoms
    val ssumkp0_eq_ssumk = Atom( eq, Function( s, Function( p, sum( k )::Utils.numeral( 0 )::Nil )::Nil )::Function( s, sum( k )::Nil )::Nil )
    val sumkp1_eq_ssumk = Atom( eq, sum( k+1 )::Function( s, sum( k )::Nil )::Nil )
    val sumkp1_eq_ssumkp0 = Atom( eq, sum( k+1 )::Function( s, Function( p, sum( k )::Utils.numeral( 0 )::Nil )::Nil )::Nil )
    val ssumkp0_eq_z = Atom( eq, Function( s, Function( p, sum( k )::Utils.numeral( 0 )::Nil )::Nil )::z::Nil )
    val sumkp1_eq_z = Atom( eq, sum( k+1 )::z::Nil )
    val sumkp1_eq_y = Atom( eq, sum( k+1 )::y::Nil )
    val y_eq_z = Atom( eq, y::z::Nil )
    val sumkp0_eq_sumk = Atom( eq, Function( p, sum( k )::Utils.numeral( 0 )::Nil )::sum( k )::Nil )
    val sumkp0_eq_y = Atom( eq, Function( p, sum( k )::Utils.numeral( 0 )::Nil )::y::Nil )
    val ssumkp0_eq_sy = Atom( eq, Function( s, Function( p, sum( k )::Utils.numeral( 0 )::Nil )::Nil )::Function( s, y::Nil )::Nil )
    val sumkpsy_eq_ssumkpy = Atom( eq, Function( p, sum( k )::Function( s, y::Nil)::Nil )::Function( s, Function( p, sum( k )::y::Nil )::Nil )::Nil )
 
    /// prop. formulas
    val Trans2 = Imp( ssumkp0_eq_ssumk, sumkp1_eq_ssumk )
    val Trans3 = Imp( sumkp1_eq_ssumkp0, Trans2 )
    val Cong2 = Imp( sumkp0_eq_sumk, ssumkp0_eq_ssumk )

    /// quant. formulas
    val Trans3_1 = AllVar( z, Imp( sumkp1_eq_ssumkp0, Imp( ssumkp0_eq_z, sumkp1_eq_z ) ) )
    val Trans3_2 = AllVar( y, AllVar( z, Imp( sumkp1_eq_y, Imp( y_eq_z, sumkp1_eq_z ) ) ) )
    val Cong2_1 = AllVar( y, Imp( sumkp0_eq_y, ssumkp0_eq_sy ) )
    val ASuc_1 = AllVar( y, sumkpsy_eq_ssumkpy )

    /// proof
    // transitivity
    val p1 = Axiom( ssumkp0_eq_ssumk::Nil, ssumkp0_eq_ssumk::Nil )
    val p2 = Axiom( sumkp1_eq_ssumk::Nil, sumkp1_eq_ssumk::Nil )
    val p3 = ImpLeftRule( p1, p2, ssumkp0_eq_ssumk, sumkp1_eq_ssumk )
    val p4 = Axiom( sumkp1_eq_ssumkp0::Nil, sumkp1_eq_ssumkp0::Nil )
    val p5 = ImpLeftRule( p4, p3, sumkp1_eq_ssumkp0, Trans2 )
    val p6 = ForallLeftRule( p5, Trans3, Trans3_1, Function( s, sum( k )::Nil ) )
    val p7 = ForallLeftRule( p6, Trans3_1, Trans3_2, Function( s, Function( p, sum( k )::Utils.numeral( 0 )::Nil )::Nil ) )
    val p8 = ForallLeftRule( p7, Trans3_2, Trans, sum( k+1 ) )

    // congruence sucessor
    val p9 = Axiom( sumkp0_eq_sumk::Nil, sumkp0_eq_sumk::Nil )
    val p10 = ImpLeftRule( p9, p8, sumkp0_eq_sumk, ssumkp0_eq_ssumk )
    val p11 = ForallLeftRule( p10, Cong2, Cong2_1, sum( k ) )
    val p12 = ForallLeftRule( p11, Cong2_1, CongSuc, Function( p, sum( k )::Utils.numeral( 0 )::Nil ) )

    // addition sucessor case
    val p13 = ForallLeftRule( p12, sumkp1_eq_ssumkp0, ASuc_1, Utils.numeral( 0 ) )
    val p14 = ForallLeftRule( p13, ASuc_1, ASuc, sum( k ) )

    // addition base case
    ForallLeftRule( p14, sumkp0_eq_sumk, ABase, sum( k ) )
  }

  // the term (.((1 + 1) + 1 ) + ... + 1 ), k must be at least 1
  // TODO should be private - but scala shell does not allow access modifiers when :loading a file
  def sum( k: Int ) : FOLTerm = {
    if ( k == 1 )  Utils.numeral( 1 )
    else           Function( p, sum( k-1 )::Utils.numeral( 1 )::Nil )
  }
}
