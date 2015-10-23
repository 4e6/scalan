package scalan.stream

import scalan._
import scala.reflect.runtime.universe._
import scalan.monads.{MonadsDsl, Monads}
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait ProcessesAbs extends Processes with scalan.Scalan {
  self: ProcessesDsl =>

  // single proxy for each type family
  implicit def proxyProcess[F[_], O](p: Rep[Process[F, O]]): Process[F, O] = {
    proxyOps[Process[F, O]](p)(scala.reflect.classTag[Process[F, O]])
  }

  // familyElem
  class ProcessElem[F[_], O, To <: Process[F, O]](implicit _cF: Cont[F], _eO: Elem[O])
    extends EntityElem[To] {
    def cF = _cF
    def eO = _eO
    lazy val parent: Option[Elem[_]] = None
    lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("F" -> Right(cF.asInstanceOf[SomeCont]), "O" -> Left(eO))
    }
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagO = eO.tag
      weakTypeTag[Process[F, O]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[Process[F, O]] => convertProcess(x) }
      tryConvert(element[Process[F, O]], this, x, conv)
    }

    def convertProcess(x: Rep[Process[F, O]]): Rep[To] = {
      x.selfType1.asInstanceOf[Element[_]] match {
        case _: ProcessElem[_, _, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have ProcessElem[_, _, _], but got $e")
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def processElement[F[_], O](implicit cF: Cont[F], eO: Elem[O]): Elem[Process[F, O]] =
    cachedElem[ProcessElem[F, O, Process[F, O]]](cF, eO)

  implicit case object ProcessCompanionElem extends CompanionElem[ProcessCompanionAbs] {
    lazy val tag = weakTypeTag[ProcessCompanionAbs]
    protected def getDefaultRep = Process
  }

  abstract class ProcessCompanionAbs extends CompanionDef[ProcessCompanionAbs] with ProcessCompanion {
    def selfType = ProcessCompanionElem
    override def toString = "Process"
  }
  def Process: Rep[ProcessCompanionAbs]
  implicit def proxyProcessCompanion(p: Rep[ProcessCompanion]): ProcessCompanion =
    proxyOps[ProcessCompanion](p)

  abstract class AbsAwait[F[_], A, O]
      (req: Rep[F[A]], recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F])
    extends Await[F, A, O](req, recv) with Def[Await[F, A, O]] {
    lazy val selfType = element[Await[F, A, O]]
  }
  // elem for concrete class
  class AwaitElem[F[_], A, O](val iso: Iso[AwaitData[F, A, O], Await[F, A, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F])
    extends ProcessElem[F, O, Await[F, A, O]]
    with ConcreteElem[AwaitData[F, A, O], Await[F, A, O]] {
    override lazy val parent: Option[Elem[_]] = Some(processElement(container[F], element[O]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("F" -> Right(cF.asInstanceOf[SomeCont]), "A" -> Left(eA), "O" -> Left(eO))
    }

    override def convertProcess(x: Rep[Process[F, O]]) = // Converter is not generated by meta
!!!("Cannot convert from Process to Await: missing fields List(req, recv)")
    override def getDefaultRep = Await(cF.lift(element[A]).defaultRepValue, constFun[$bar[Throwable, A], Process[F, O]](element[Process[F, O]].defaultRepValue))
    override lazy val tag = {
      implicit val tagA = eA.tag
      implicit val tagO = eO.tag
      weakTypeTag[Await[F, A, O]]
    }
  }

  // state representation type
  type AwaitData[F[_], A, O] = (F[A], $bar[Throwable, A] => Process[F, O])

  // 3) Iso for concrete class
  class AwaitIso[F[_], A, O](implicit eA: Elem[A], eO: Elem[O], cF: Cont[F])
    extends Iso[AwaitData[F, A, O], Await[F, A, O]]()(pairElement(implicitly[Elem[F[A]]], implicitly[Elem[$bar[Throwable, A] => Process[F, O]]])) {
    override def from(p: Rep[Await[F, A, O]]) =
      (p.req, p.recv)
    override def to(p: Rep[(F[A], $bar[Throwable, A] => Process[F, O])]) = {
      val Pair(req, recv) = p
      Await(req, recv)
    }
    lazy val eTo = new AwaitElem[F, A, O](this)
  }
  // 4) constructor and deconstructor
  class AwaitCompanionAbs extends CompanionDef[AwaitCompanionAbs] with AwaitCompanion {
    def selfType = AwaitCompanionElem
    override def toString = "Await"
    def apply[F[_], A, O](p: Rep[AwaitData[F, A, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]): Rep[Await[F, A, O]] =
      isoAwait(eA, eO, cF).to(p)
    def apply[F[_], A, O](req: Rep[F[A]], recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]): Rep[Await[F, A, O]] =
      mkAwait(req, recv)
  }
  object AwaitMatcher {
    def unapply[F[_], A, O](p: Rep[Process[F, O]]) = unmkAwait(p)
  }
  lazy val Await: Rep[AwaitCompanionAbs] = new AwaitCompanionAbs
  implicit def proxyAwaitCompanion(p: Rep[AwaitCompanionAbs]): AwaitCompanionAbs = {
    proxyOps[AwaitCompanionAbs](p)
  }

  implicit case object AwaitCompanionElem extends CompanionElem[AwaitCompanionAbs] {
    lazy val tag = weakTypeTag[AwaitCompanionAbs]
    protected def getDefaultRep = Await
  }

  implicit def proxyAwait[F[_], A, O](p: Rep[Await[F, A, O]]): Await[F, A, O] =
    proxyOps[Await[F, A, O]](p)

  implicit class ExtendedAwait[F[_], A, O](p: Rep[Await[F, A, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]) {
    def toData: Rep[AwaitData[F, A, O]] = isoAwait(eA, eO, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoAwait[F[_], A, O](implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]): Iso[AwaitData[F, A, O], Await[F, A, O]] =
    cachedIso[AwaitIso[F, A, O]](eA, eO, cF)

  // 6) smart constructor and deconstructor
  def mkAwait[F[_], A, O](req: Rep[F[A]], recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]): Rep[Await[F, A, O]]
  def unmkAwait[F[_], A, O](p: Rep[Process[F, O]]): Option[(Rep[F[A]], Rep[$bar[Throwable, A] => Process[F, O]])]

  abstract class AbsEmit[F[_], O]
      (head: Rep[O], tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F])
    extends Emit[F, O](head, tail) with Def[Emit[F, O]] {
    lazy val selfType = element[Emit[F, O]]
  }
  // elem for concrete class
  class EmitElem[F[_], O](val iso: Iso[EmitData[F, O], Emit[F, O]])(implicit eO: Elem[O], cF: Cont[F])
    extends ProcessElem[F, O, Emit[F, O]]
    with ConcreteElem[EmitData[F, O], Emit[F, O]] {
    override lazy val parent: Option[Elem[_]] = Some(processElement(container[F], element[O]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("F" -> Right(cF.asInstanceOf[SomeCont]), "O" -> Left(eO))
    }

    override def convertProcess(x: Rep[Process[F, O]]) = // Converter is not generated by meta
!!!("Cannot convert from Process to Emit: missing fields List(head, tail)")
    override def getDefaultRep = Emit(element[O].defaultRepValue, element[Process[F, O]].defaultRepValue)
    override lazy val tag = {
      implicit val tagO = eO.tag
      weakTypeTag[Emit[F, O]]
    }
  }

  // state representation type
  type EmitData[F[_], O] = (O, Process[F, O])

  // 3) Iso for concrete class
  class EmitIso[F[_], O](implicit eO: Elem[O], cF: Cont[F])
    extends Iso[EmitData[F, O], Emit[F, O]]()(pairElement(implicitly[Elem[O]], implicitly[Elem[Process[F, O]]])) {
    override def from(p: Rep[Emit[F, O]]) =
      (p.head, p.tail)
    override def to(p: Rep[(O, Process[F, O])]) = {
      val Pair(head, tail) = p
      Emit(head, tail)
    }
    lazy val eTo = new EmitElem[F, O](this)
  }
  // 4) constructor and deconstructor
  class EmitCompanionAbs extends CompanionDef[EmitCompanionAbs] with EmitCompanion {
    def selfType = EmitCompanionElem
    override def toString = "Emit"
    def apply[F[_], O](p: Rep[EmitData[F, O]])(implicit eO: Elem[O], cF: Cont[F]): Rep[Emit[F, O]] =
      isoEmit(eO, cF).to(p)
    def apply[F[_], O](head: Rep[O], tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F]): Rep[Emit[F, O]] =
      mkEmit(head, tail)
  }
  object EmitMatcher {
    def unapply[F[_], O](p: Rep[Process[F, O]]) = unmkEmit(p)
  }
  lazy val Emit: Rep[EmitCompanionAbs] = new EmitCompanionAbs
  implicit def proxyEmitCompanion(p: Rep[EmitCompanionAbs]): EmitCompanionAbs = {
    proxyOps[EmitCompanionAbs](p)
  }

  implicit case object EmitCompanionElem extends CompanionElem[EmitCompanionAbs] {
    lazy val tag = weakTypeTag[EmitCompanionAbs]
    protected def getDefaultRep = Emit
  }

  implicit def proxyEmit[F[_], O](p: Rep[Emit[F, O]]): Emit[F, O] =
    proxyOps[Emit[F, O]](p)

  implicit class ExtendedEmit[F[_], O](p: Rep[Emit[F, O]])(implicit eO: Elem[O], cF: Cont[F]) {
    def toData: Rep[EmitData[F, O]] = isoEmit(eO, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoEmit[F[_], O](implicit eO: Elem[O], cF: Cont[F]): Iso[EmitData[F, O], Emit[F, O]] =
    cachedIso[EmitIso[F, O]](eO, cF)

  // 6) smart constructor and deconstructor
  def mkEmit[F[_], O](head: Rep[O], tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F]): Rep[Emit[F, O]]
  def unmkEmit[F[_], O](p: Rep[Process[F, O]]): Option[(Rep[O], Rep[Process[F, O]])]

  abstract class AbsHalt[F[_], O]
      (err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F])
    extends Halt[F, O](err) with Def[Halt[F, O]] {
    lazy val selfType = element[Halt[F, O]]
  }
  // elem for concrete class
  class HaltElem[F[_], O](val iso: Iso[HaltData[F, O], Halt[F, O]])(implicit eO: Elem[O], cF: Cont[F])
    extends ProcessElem[F, O, Halt[F, O]]
    with ConcreteElem[HaltData[F, O], Halt[F, O]] {
    override lazy val parent: Option[Elem[_]] = Some(processElement(container[F], element[O]))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("F" -> Right(cF.asInstanceOf[SomeCont]), "O" -> Left(eO))
    }

    override def convertProcess(x: Rep[Process[F, O]]) = // Converter is not generated by meta
!!!("Cannot convert from Process to Halt: missing fields List(err)")
    override def getDefaultRep = Halt(element[Throwable].defaultRepValue)
    override lazy val tag = {
      implicit val tagO = eO.tag
      weakTypeTag[Halt[F, O]]
    }
  }

  // state representation type
  type HaltData[F[_], O] = Throwable

  // 3) Iso for concrete class
  class HaltIso[F[_], O](implicit eO: Elem[O], cF: Cont[F])
    extends Iso[HaltData[F, O], Halt[F, O]] {
    override def from(p: Rep[Halt[F, O]]) =
      p.err
    override def to(p: Rep[Throwable]) = {
      val err = p
      Halt(err)
    }
    lazy val eTo = new HaltElem[F, O](this)
  }
  // 4) constructor and deconstructor
  class HaltCompanionAbs extends CompanionDef[HaltCompanionAbs] with HaltCompanion {
    def selfType = HaltCompanionElem
    override def toString = "Halt"

    def apply[F[_], O](err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F]): Rep[Halt[F, O]] =
      mkHalt(err)
  }
  object HaltMatcher {
    def unapply[F[_], O](p: Rep[Process[F, O]]) = unmkHalt(p)
  }
  lazy val Halt: Rep[HaltCompanionAbs] = new HaltCompanionAbs
  implicit def proxyHaltCompanion(p: Rep[HaltCompanionAbs]): HaltCompanionAbs = {
    proxyOps[HaltCompanionAbs](p)
  }

  implicit case object HaltCompanionElem extends CompanionElem[HaltCompanionAbs] {
    lazy val tag = weakTypeTag[HaltCompanionAbs]
    protected def getDefaultRep = Halt
  }

  implicit def proxyHalt[F[_], O](p: Rep[Halt[F, O]]): Halt[F, O] =
    proxyOps[Halt[F, O]](p)

  implicit class ExtendedHalt[F[_], O](p: Rep[Halt[F, O]])(implicit eO: Elem[O], cF: Cont[F]) {
    def toData: Rep[HaltData[F, O]] = isoHalt(eO, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoHalt[F[_], O](implicit eO: Elem[O], cF: Cont[F]): Iso[HaltData[F, O], Halt[F, O]] =
    cachedIso[HaltIso[F, O]](eO, cF)

  // 6) smart constructor and deconstructor
  def mkHalt[F[_], O](err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F]): Rep[Halt[F, O]]
  def unmkHalt[F[_], O](p: Rep[Process[F, O]]): Option[(Rep[Throwable])]

  registerModule(Processes_Module)
}

// Seq -----------------------------------
trait ProcessesSeq extends ProcessesDsl with scalan.ScalanSeq {
  self: ProcessesDslSeq =>
  lazy val Process: Rep[ProcessCompanionAbs] = new ProcessCompanionAbs {
  }

  case class SeqAwait[F[_], A, O]
      (override val req: Rep[F[A]], override val recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F])
    extends AbsAwait[F, A, O](req, recv) {
  }

  def mkAwait[F[_], A, O]
    (req: Rep[F[A]], recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]): Rep[Await[F, A, O]] =
    new SeqAwait[F, A, O](req, recv)
  def unmkAwait[F[_], A, O](p: Rep[Process[F, O]]) = p match {
    case p: Await[F, A, O] @unchecked =>
      Some((p.req, p.recv))
    case _ => None
  }

  case class SeqEmit[F[_], O]
      (override val head: Rep[O], override val tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F])
    extends AbsEmit[F, O](head, tail) {
  }

  def mkEmit[F[_], O]
    (head: Rep[O], tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F]): Rep[Emit[F, O]] =
    new SeqEmit[F, O](head, tail)
  def unmkEmit[F[_], O](p: Rep[Process[F, O]]) = p match {
    case p: Emit[F, O] @unchecked =>
      Some((p.head, p.tail))
    case _ => None
  }

  case class SeqHalt[F[_], O]
      (override val err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F])
    extends AbsHalt[F, O](err) {
  }

  def mkHalt[F[_], O]
    (err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F]): Rep[Halt[F, O]] =
    new SeqHalt[F, O](err)
  def unmkHalt[F[_], O](p: Rep[Process[F, O]]) = p match {
    case p: Halt[F, O] @unchecked =>
      Some((p.err))
    case _ => None
  }
}

// Exp -----------------------------------
trait ProcessesExp extends ProcessesDsl with scalan.ScalanExp {
  self: ProcessesDslExp =>
  lazy val Process: Rep[ProcessCompanionAbs] = new ProcessCompanionAbs {
  }

  case class ExpAwait[F[_], A, O]
      (override val req: Rep[F[A]], override val recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F])
    extends AbsAwait[F, A, O](req, recv)

  object AwaitMethods {
    // WARNING: Cannot generate matcher for method `map`: Method has function arguments f

    // WARNING: Cannot generate matcher for method `onHalt`: Method has function arguments f
  }

  object AwaitCompanionMethods {
  }

  def mkAwait[F[_], A, O]
    (req: Rep[F[A]], recv: Rep[$bar[Throwable, A] => Process[F, O]])(implicit eA: Elem[A], eO: Elem[O], cF: Cont[F]): Rep[Await[F, A, O]] =
    new ExpAwait[F, A, O](req, recv)
  def unmkAwait[F[_], A, O](p: Rep[Process[F, O]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: AwaitElem[F, A, O] @unchecked =>
      Some((p.asRep[Await[F, A, O]].req, p.asRep[Await[F, A, O]].recv))
    case _ =>
      None
  }

  case class ExpEmit[F[_], O]
      (override val head: Rep[O], override val tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F])
    extends AbsEmit[F, O](head, tail)

  object EmitMethods {
    // WARNING: Cannot generate matcher for method `map`: Method has function arguments f

    // WARNING: Cannot generate matcher for method `onHalt`: Method has function arguments f
  }

  object EmitCompanionMethods {
  }

  def mkEmit[F[_], O]
    (head: Rep[O], tail: Rep[Process[F, O]])(implicit eO: Elem[O], cF: Cont[F]): Rep[Emit[F, O]] =
    new ExpEmit[F, O](head, tail)
  def unmkEmit[F[_], O](p: Rep[Process[F, O]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: EmitElem[F, O] @unchecked =>
      Some((p.asRep[Emit[F, O]].head, p.asRep[Emit[F, O]].tail))
    case _ =>
      None
  }

  case class ExpHalt[F[_], O]
      (override val err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F])
    extends AbsHalt[F, O](err)

  object HaltMethods {
    // WARNING: Cannot generate matcher for method `map`: Method has function arguments f

    // WARNING: Cannot generate matcher for method `onHalt`: Method has function arguments f
  }

  object HaltCompanionMethods {
  }

  def mkHalt[F[_], O]
    (err: Rep[Throwable])(implicit eO: Elem[O], cF: Cont[F]): Rep[Halt[F, O]] =
    new ExpHalt[F, O](err)
  def unmkHalt[F[_], O](p: Rep[Process[F, O]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: HaltElem[F, O] @unchecked =>
      Some((p.asRep[Halt[F, O]].err))
    case _ =>
      None
  }

  object ProcessMethods {
    // WARNING: Cannot generate matcher for method `map`: Method has function arguments f

    // WARNING: Cannot generate matcher for method `onHalt`: Method has function arguments f

    object ++ {
      def unapply(d: Def[_]): Option[(Rep[Process[F, O]], Th[Process[F, O]]) forSome {type F[_]; type O}] = d match {
        case MethodCall(receiver, method, Seq(p, _*), _) if (receiver.elem.asInstanceOf[Element[_]] match { case _: ProcessElem[_, _, _] => true; case _ => false }) && method.getName == "$plus$plus" =>
          Some((receiver, p)).asInstanceOf[Option[(Rep[Process[F, O]], Th[Process[F, O]]) forSome {type F[_]; type O}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Process[F, O]], Th[Process[F, O]]) forSome {type F[_]; type O}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object ProcessCompanionMethods {
    object emit {
      def unapply(d: Def[_]): Option[(Rep[O], Rep[Process[F, O]]) forSome {type F[_]; type O}] = d match {
        case MethodCall(receiver, method, Seq(head, tail, _*), _) if receiver.elem == ProcessCompanionElem && method.getName == "emit" =>
          Some((head, tail)).asInstanceOf[Option[(Rep[O], Rep[Process[F, O]]) forSome {type F[_]; type O}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[O], Rep[Process[F, O]]) forSome {type F[_]; type O}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    // WARNING: Cannot generate matcher for method `await`: Method has function arguments recv

    object eval {
      def unapply(d: Def[_]): Option[Rep[F[A]] forSome {type F[_]; type A}] = d match {
        case MethodCall(receiver, method, Seq(a, _*), _) if receiver.elem == ProcessCompanionElem && method.getName == "eval" =>
          Some(a).asInstanceOf[Option[Rep[F[A]] forSome {type F[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[F[A]] forSome {type F[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object evalIO {
      def unapply(d: Def[_]): Option[Rep[IO[A]] forSome {type A}] = d match {
        case MethodCall(receiver, method, Seq(a, _*), _) if receiver.elem == ProcessCompanionElem && method.getName == "evalIO" =>
          Some(a).asInstanceOf[Option[Rep[IO[A]] forSome {type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[IO[A]] forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object Try {
      def unapply(d: Def[_]): Option[Th[Process[F, O]] forSome {type F[_]; type O}] = d match {
        case MethodCall(receiver, method, Seq(p, _*), _) if receiver.elem == ProcessCompanionElem && method.getName == "Try" =>
          Some(p).asInstanceOf[Option[Th[Process[F, O]] forSome {type F[_]; type O}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Th[Process[F, O]] forSome {type F[_]; type O}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }
}

object Processes_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAANVXTWwbRRR+XttxbKdJ+FFRW7UJkQsCgR0hpB4itXJTG4rcOMrmgEyhGq8nzpb9y8w4tTn0wBFuiAsHhHrvjQsnbkgICU4VIHHiwKmUQwX0BOqb8e56/bNORAsRPox2Zt++9+b7vvdmfPsepDmD57hBLOIUbSpIUVfPZS4KesURpuhdcVsdi16iO+8f/8K44lzkGiw0YGaX8EvcakC2/1DpeuGzTvdqkCWOQblwGRfwbE1FKBmuZVFDmK5TMm27I0jToqWaycVaDVJNt9Xbg5uQqMGi4ToGo4Lq6xbhnHJ/fZbKjMxwnlXzXt0bxHBKchelyC62GTEFpo8xFvv2W9TTe47r9GwB835qdU+mhTYZ0/ZcJoIQGXS367aCacohuABP1q6TfVLCEO2SLpjptPHLvEeMd0mbbqCJNE9hwpxaO9s9T82TNchxuocAXbY9S610PQBABl5RSRQH+BRDfIoSn4JOmUks8z0iX24yt9uD/i+RBOh66OKlA1wEHmjFaRU+uGq89UDP25r8uCtTyagdzqCjpRg1KCoQx6+3PuL3X7t1ToNcA3ImLze5YMQQUcp9tPLEcVyhcg4BJKyNbK3EsaWilNFmRBJZw7U94qAnH8o55MkyDVNIY7k257MTA31GeDQwTXS9RLjf5Zj9Kt2sE8vavHvi5bO/Vt7UQBsOkUWXOgqfBU4FZJAbxID73uW4ICBRHUAsp3U1lUO2OxgzU5IJYXn+7m+tr1bhqhaC6cc+HH/oIs1//D5/54ULGsw2lNqrFmk3EE9esahdZ+uuIxow6+5T1n+T2SeWfJrIZ6ZFd0jHEj7KUXiSCI+A5di69KjEbk3VQCIAIN+X8Ybr0EJ1s/Cn/s3Ht6VKGcz13/QL9W/z3F8/ze8IJWABSUb3AnSTWN6j8I/My2P4q1cnw0TksCQgxaixP8EvgzNxovFoteMYdy5/8tTC6Ws/K8nMtFybmEq3p2qQZtg0FESnfNLQd6rQJGw4yez2LnNvyPId1k4092DxUVQ30F6uD7Du2vSJlfvmO7c+FEplie5ww6s3r2OHWVPfnZkiuKDx/tFY1X4/8cNnGmRRV01T2MQrrB6yXfyLLQBCPAbDEoppvnxDVn402NIAyWciIJ9MjAhGo+WQUllNU3QX56A+zUH9YAdGNXQgK3miJKLsC0ir/arvQ32fjtc3Inh8q/a0de/Clxqk34D0DvYGjsJuuh2nFVCDJ7igXXExWEsMU4NUEEbskAr1W4YB1iNV+/ZEi2ujcEw2Kw/Ym2wwBms+MYzbo7X1MdmM9ZldSlpT+tc47WMeBDGteA+Po0HI8bway3GVc6xi/+PC+e91n5LZRmUfL5ADpCiH5uG0eNRSS1LG4nQih1enOD6I/deJ9T9iX2Z7lOxHjGd8YIfTTuLZ+djL+NBsLvqxJhAauecePYBy/HbYFxpm/fzwfgHH/LMM/6ZQ/8g5j0fcSswRp/uXBkT/5oNPN1787vNf1DUuJ68feC11wv+GgwOtO9Jv58Lw+G8vkjLKTt5JVLoPAVB50Th9DwAA"
}
}

