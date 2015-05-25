package scalan.effects
package impl

import scalan._
import scala.reflect.runtime.universe._
import scalan.monads.{MonadsDslExp, MonadsDslSeq, Monads, MonadsDsl}
import scala.reflect.runtime.universe._
import scala.reflect._
import scalan.common.Default

// Abs -----------------------------------
trait FreeMsAbs extends FreeMs with Scalan {
  self: MonadsDsl =>

  // single proxy for each type family
  implicit def proxyFreeM[F[_], A](p: Rep[FreeM[F, A]]): FreeM[F, A] = {
    proxyOps[FreeM[F, A]](p)(classTag[FreeM[F, A]])
  }

  // familyElem
  class FreeMElem[F[_], A, To <: FreeM[F, A]](implicit val cF: Cont[F], val eA: Elem[A])
    extends EntityElem[To] {
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[FreeM[F, A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Reifiable[_]]) = {
      val conv = fun {x: Rep[FreeM[F, A]] =>  convertFreeM(x) }
      tryConvert(element[FreeM[F, A]], this, x, conv)
    }

    def convertFreeM(x : Rep[FreeM[F, A]]): Rep[To] = {
      assert(x.selfType1.asInstanceOf[Element[_]] match { case _: FreeMElem[_, _, _] => true; case _ => false })
      x.asRep[To]
    }
    override def getDefaultRep: Rep[To] = ???
  }

  implicit def freeMElement[F[_], A](implicit cF: Cont[F], eA: Elem[A]): Elem[FreeM[F, A]] =
    new FreeMElem[F, A, FreeM[F, A]]

  implicit case object FreeMCompanionElem extends CompanionElem[FreeMCompanionAbs] {
    lazy val tag = weakTypeTag[FreeMCompanionAbs]
    protected def getDefaultRep = FreeM
  }

  abstract class FreeMCompanionAbs extends CompanionBase[FreeMCompanionAbs] with FreeMCompanion {
    override def toString = "FreeM"
  }
  def FreeM: Rep[FreeMCompanionAbs]
  implicit def proxyFreeMCompanion(p: Rep[FreeMCompanion]): FreeMCompanion = {
    proxyOps[FreeMCompanion](p)
  }

  // elem for concrete class
  class DoneElem[F[_], A](val iso: Iso[DoneData[F, A], Done[F, A]])(implicit eA: Elem[A], cF: Cont[F])
    extends FreeMElem[F, A, Done[F, A]]
    with ConcreteElem[DoneData[F, A], Done[F, A]] {
    override def convertFreeM(x: Rep[FreeM[F, A]]) = // Converter is not generated by meta
!!!("Cannot convert from FreeM to Done: missing fields List(a)")
    override def getDefaultRep = super[ConcreteElem].getDefaultRep
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[Done[F, A]]
    }
  }

  // state representation type
  type DoneData[F[_], A] = A

  // 3) Iso for concrete class
  class DoneIso[F[_], A](implicit eA: Elem[A], cF: Cont[F])
    extends Iso[DoneData[F, A], Done[F, A]] {
    override def from(p: Rep[Done[F, A]]) =
      p.a
    override def to(p: Rep[A]) = {
      val a = p
      Done(a)
    }
    lazy val defaultRepTo = Default.defaultVal[Rep[Done[F, A]]](Done(element[A].defaultRepValue))
    lazy val eTo = new DoneElem[F, A](this)
  }
  // 4) constructor and deconstructor
  abstract class DoneCompanionAbs extends CompanionBase[DoneCompanionAbs] with DoneCompanion {
    override def toString = "Done"

    def apply[F[_], A](a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Done[F, A]] =
      mkDone(a)
  }
  object DoneMatcher {
    def unapply[F[_], A](p: Rep[FreeM[F, A]]) = unmkDone(p)
  }
  def Done: Rep[DoneCompanionAbs]
  implicit def proxyDoneCompanion(p: Rep[DoneCompanionAbs]): DoneCompanionAbs = {
    proxyOps[DoneCompanionAbs](p)
  }

  implicit case object DoneCompanionElem extends CompanionElem[DoneCompanionAbs] {
    lazy val tag = weakTypeTag[DoneCompanionAbs]
    protected def getDefaultRep = Done
  }

  implicit def proxyDone[F[_], A](p: Rep[Done[F, A]]): Done[F, A] =
    proxyOps[Done[F, A]](p)

  implicit class ExtendedDone[F[_], A](p: Rep[Done[F, A]])(implicit eA: Elem[A], cF: Cont[F]) {
    def toData: Rep[DoneData[F, A]] = isoDone(eA, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoDone[F[_], A](implicit eA: Elem[A], cF: Cont[F]): Iso[DoneData[F, A], Done[F, A]] =
    new DoneIso[F, A]

  // 6) smart constructor and deconstructor
  def mkDone[F[_], A](a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Done[F, A]]
  def unmkDone[F[_], A](p: Rep[FreeM[F, A]]): Option[(Rep[A])]

  // elem for concrete class
  class MoreElem[F[_], A](val iso: Iso[MoreData[F, A], More[F, A]])(implicit eA: Elem[A], cF: Cont[F])
    extends FreeMElem[F, A, More[F, A]]
    with ConcreteElem[MoreData[F, A], More[F, A]] {
    override def convertFreeM(x: Rep[FreeM[F, A]]) = // Converter is not generated by meta
!!!("Cannot convert from FreeM to More: missing fields List(k)")
    override def getDefaultRep = super[ConcreteElem].getDefaultRep
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[More[F, A]]
    }
  }

  // state representation type
  type MoreData[F[_], A] = F[FreeM[F,A]]

  // 3) Iso for concrete class
  class MoreIso[F[_], A](implicit eA: Elem[A], cF: Cont[F])
    extends Iso[MoreData[F, A], More[F, A]] {
    override def from(p: Rep[More[F, A]]) =
      p.k
    override def to(p: Rep[F[FreeM[F,A]]]) = {
      val k = p
      More(k)
    }
    lazy val defaultRepTo = Default.defaultVal[Rep[More[F, A]]](More(cF.lift(element[FreeM[F,A]]).defaultRepValue))
    lazy val eTo = new MoreElem[F, A](this)
  }
  // 4) constructor and deconstructor
  abstract class MoreCompanionAbs extends CompanionBase[MoreCompanionAbs] with MoreCompanion {
    override def toString = "More"

    def apply[F[_], A](k: Rep[F[FreeM[F,A]]])(implicit eA: Elem[A], cF: Cont[F]): Rep[More[F, A]] =
      mkMore(k)
  }
  object MoreMatcher {
    def unapply[F[_], A](p: Rep[FreeM[F, A]]) = unmkMore(p)
  }
  def More: Rep[MoreCompanionAbs]
  implicit def proxyMoreCompanion(p: Rep[MoreCompanionAbs]): MoreCompanionAbs = {
    proxyOps[MoreCompanionAbs](p)
  }

  implicit case object MoreCompanionElem extends CompanionElem[MoreCompanionAbs] {
    lazy val tag = weakTypeTag[MoreCompanionAbs]
    protected def getDefaultRep = More
  }

  implicit def proxyMore[F[_], A](p: Rep[More[F, A]]): More[F, A] =
    proxyOps[More[F, A]](p)

  implicit class ExtendedMore[F[_], A](p: Rep[More[F, A]])(implicit eA: Elem[A], cF: Cont[F]) {
    def toData: Rep[MoreData[F, A]] = isoMore(eA, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoMore[F[_], A](implicit eA: Elem[A], cF: Cont[F]): Iso[MoreData[F, A], More[F, A]] =
    new MoreIso[F, A]

  // 6) smart constructor and deconstructor
  def mkMore[F[_], A](k: Rep[F[FreeM[F,A]]])(implicit eA: Elem[A], cF: Cont[F]): Rep[More[F, A]]
  def unmkMore[F[_], A](p: Rep[FreeM[F, A]]): Option[(Rep[F[FreeM[F,A]]])]

  // elem for concrete class
  class FlatMapElem[F[_], S, B](val iso: Iso[FlatMapData[F, S, B], FlatMap[F, S, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends FreeMElem[F, B, FlatMap[F, S, B]]
    with ConcreteElem[FlatMapData[F, S, B], FlatMap[F, S, B]] {
    override def convertFreeM(x: Rep[FreeM[F, B]]) = // Converter is not generated by meta
!!!("Cannot convert from FreeM to FlatMap: missing fields List(a, f)")
    override def getDefaultRep = super[ConcreteElem].getDefaultRep
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagB = eA.tag
      weakTypeTag[FlatMap[F, S, B]]
    }
  }

  // state representation type
  type FlatMapData[F[_], S, B] = (FreeM[F,S], S => FreeM[F,B])

  // 3) Iso for concrete class
  class FlatMapIso[F[_], S, B](implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends Iso[FlatMapData[F, S, B], FlatMap[F, S, B]]()(pairElement(implicitly[Elem[FreeM[F,S]]], implicitly[Elem[S => FreeM[F,B]]])) {
    override def from(p: Rep[FlatMap[F, S, B]]) =
      (p.a, p.f)
    override def to(p: Rep[(FreeM[F,S], S => FreeM[F,B])]) = {
      val Pair(a, f) = p
      FlatMap(a, f)
    }
    lazy val defaultRepTo = Default.defaultVal[Rep[FlatMap[F, S, B]]](FlatMap(element[FreeM[F,S]].defaultRepValue, fun { (x: Rep[S]) => element[FreeM[F,B]].defaultRepValue }))
    lazy val eTo = new FlatMapElem[F, S, B](this)
  }
  // 4) constructor and deconstructor
  abstract class FlatMapCompanionAbs extends CompanionBase[FlatMapCompanionAbs] with FlatMapCompanion {
    override def toString = "FlatMap"
    def apply[F[_], S, B](p: Rep[FlatMapData[F, S, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[FlatMap[F, S, B]] =
      isoFlatMap(eS, eA, cF).to(p)
    def apply[F[_], S, B](a: Rep[FreeM[F,S]], f: Rep[S => FreeM[F,B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[FlatMap[F, S, B]] =
      mkFlatMap(a, f)
  }
  object FlatMapMatcher {
    def unapply[F[_], S, B](p: Rep[FreeM[F, B]]) = unmkFlatMap(p)
  }
  def FlatMap: Rep[FlatMapCompanionAbs]
  implicit def proxyFlatMapCompanion(p: Rep[FlatMapCompanionAbs]): FlatMapCompanionAbs = {
    proxyOps[FlatMapCompanionAbs](p)
  }

  implicit case object FlatMapCompanionElem extends CompanionElem[FlatMapCompanionAbs] {
    lazy val tag = weakTypeTag[FlatMapCompanionAbs]
    protected def getDefaultRep = FlatMap
  }

  implicit def proxyFlatMap[F[_], S, B](p: Rep[FlatMap[F, S, B]]): FlatMap[F, S, B] =
    proxyOps[FlatMap[F, S, B]](p)

  implicit class ExtendedFlatMap[F[_], S, B](p: Rep[FlatMap[F, S, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]) {
    def toData: Rep[FlatMapData[F, S, B]] = isoFlatMap(eS, eA, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoFlatMap[F[_], S, B](implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Iso[FlatMapData[F, S, B], FlatMap[F, S, B]] =
    new FlatMapIso[F, S, B]

  // 6) smart constructor and deconstructor
  def mkFlatMap[F[_], S, B](a: Rep[FreeM[F,S]], f: Rep[S => FreeM[F,B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[FlatMap[F, S, B]]
  def unmkFlatMap[F[_], S, B](p: Rep[FreeM[F, B]]): Option[(Rep[FreeM[F,S]], Rep[S => FreeM[F,B]])]
}

// Seq -----------------------------------
trait FreeMsSeq extends FreeMsDsl with ScalanSeq {
  self: MonadsDslSeq =>
  lazy val FreeM: Rep[FreeMCompanionAbs] = new FreeMCompanionAbs with UserTypeSeq[FreeMCompanionAbs] {
    lazy val selfType = element[FreeMCompanionAbs]
  }

  case class SeqDone[F[_], A]
      (override val a: Rep[A])
      (implicit eA: Elem[A], cF: Cont[F])
    extends Done[F, A](a)
        with UserTypeSeq[Done[F, A]] {
    lazy val selfType = element[Done[F, A]]
  }
  lazy val Done = new DoneCompanionAbs with UserTypeSeq[DoneCompanionAbs] {
    lazy val selfType = element[DoneCompanionAbs]
  }

  def mkDone[F[_], A]
      (a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Done[F, A]] =
      new SeqDone[F, A](a)
  def unmkDone[F[_], A](p: Rep[FreeM[F, A]]) = p match {
    case p: Done[F, A] @unchecked =>
      Some((p.a))
    case _ => None
  }

  case class SeqMore[F[_], A]
      (override val k: Rep[F[FreeM[F,A]]])
      (implicit eA: Elem[A], cF: Cont[F])
    extends More[F, A](k)
        with UserTypeSeq[More[F, A]] {
    lazy val selfType = element[More[F, A]]
  }
  lazy val More = new MoreCompanionAbs with UserTypeSeq[MoreCompanionAbs] {
    lazy val selfType = element[MoreCompanionAbs]
  }

  def mkMore[F[_], A]
      (k: Rep[F[FreeM[F,A]]])(implicit eA: Elem[A], cF: Cont[F]): Rep[More[F, A]] =
      new SeqMore[F, A](k)
  def unmkMore[F[_], A](p: Rep[FreeM[F, A]]) = p match {
    case p: More[F, A] @unchecked =>
      Some((p.k))
    case _ => None
  }

  case class SeqFlatMap[F[_], S, B]
      (override val a: Rep[FreeM[F,S]], override val f: Rep[S => FreeM[F,B]])
      (implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends FlatMap[F, S, B](a, f)
        with UserTypeSeq[FlatMap[F, S, B]] {
    lazy val selfType = element[FlatMap[F, S, B]]
  }
  lazy val FlatMap = new FlatMapCompanionAbs with UserTypeSeq[FlatMapCompanionAbs] {
    lazy val selfType = element[FlatMapCompanionAbs]
  }

  def mkFlatMap[F[_], S, B]
      (a: Rep[FreeM[F,S]], f: Rep[S => FreeM[F,B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[FlatMap[F, S, B]] =
      new SeqFlatMap[F, S, B](a, f)
  def unmkFlatMap[F[_], S, B](p: Rep[FreeM[F, B]]) = p match {
    case p: FlatMap[F, S, B] @unchecked =>
      Some((p.a, p.f))
    case _ => None
  }
}

// Exp -----------------------------------
trait FreeMsExp extends FreeMsDsl with ScalanExp {
  self: MonadsDslExp =>
  lazy val FreeM: Rep[FreeMCompanionAbs] = new FreeMCompanionAbs with UserTypeDef[FreeMCompanionAbs] {
    lazy val selfType = element[FreeMCompanionAbs]
    override def mirror(t: Transformer) = this
  }

  case class ExpDone[F[_], A]
      (override val a: Rep[A])
      (implicit eA: Elem[A], cF: Cont[F])
    extends Done[F, A](a) with UserTypeDef[Done[F, A]] {
    lazy val selfType = element[Done[F, A]]
    override def mirror(t: Transformer) = ExpDone[F, A](t(a))
  }

  lazy val Done: Rep[DoneCompanionAbs] = new DoneCompanionAbs with UserTypeDef[DoneCompanionAbs] {
    lazy val selfType = element[DoneCompanionAbs]
    override def mirror(t: Transformer) = this
  }

  object DoneMethods {
    // WARNING: Cannot generate matcher for method `flatMap`: Method has function arguments f

    object flatMapBy {
      def unapply(d: Def[_]): Option[(Rep[Done[F, A]], Rep[A => FreeM[F,B]]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Element[_]] match { case _: DoneElem[_, _] => true; case _ => false }) && method.getName == "flatMapBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[Done[F, A]], Rep[A => FreeM[F,B]]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Done[F, A]], Rep[A => FreeM[F,B]]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resume {
      def unapply(d: Def[_]): Option[(Rep[Done[F, A]], Functor[F]) forSome {type F[_]; type A}] = d match {
        case MethodCall(receiver, method, Seq(fF, _*), _) if (receiver.elem.asInstanceOf[Element[_]] match { case _: DoneElem[_, _] => true; case _ => false }) && method.getName == "resume" =>
          Some((receiver, fF)).asInstanceOf[Option[(Rep[Done[F, A]], Functor[F]) forSome {type F[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Done[F, A]], Functor[F]) forSome {type F[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object DoneCompanionMethods {
  }

  def mkDone[F[_], A]
    (a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Done[F, A]] =
    new ExpDone[F, A](a)
  def unmkDone[F[_], A](p: Rep[FreeM[F, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: DoneElem[F, A] @unchecked =>
      Some((p.asRep[Done[F, A]].a))
    case _ =>
      None
  }

  case class ExpMore[F[_], A]
      (override val k: Rep[F[FreeM[F,A]]])
      (implicit eA: Elem[A], cF: Cont[F])
    extends More[F, A](k) with UserTypeDef[More[F, A]] {
    lazy val selfType = element[More[F, A]]
    override def mirror(t: Transformer) = ExpMore[F, A](t(k))
  }

  lazy val More: Rep[MoreCompanionAbs] = new MoreCompanionAbs with UserTypeDef[MoreCompanionAbs] {
    lazy val selfType = element[MoreCompanionAbs]
    override def mirror(t: Transformer) = this
  }

  object MoreMethods {
    object resume {
      def unapply(d: Def[_]): Option[(Rep[More[F, A]], Functor[F]) forSome {type F[_]; type A}] = d match {
        case MethodCall(receiver, method, Seq(fF, _*), _) if (receiver.elem.asInstanceOf[Element[_]] match { case _: MoreElem[_, _] => true; case _ => false }) && method.getName == "resume" =>
          Some((receiver, fF)).asInstanceOf[Option[(Rep[More[F, A]], Functor[F]) forSome {type F[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[More[F, A]], Functor[F]) forSome {type F[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object MoreCompanionMethods {
  }

  def mkMore[F[_], A]
    (k: Rep[F[FreeM[F,A]]])(implicit eA: Elem[A], cF: Cont[F]): Rep[More[F, A]] =
    new ExpMore[F, A](k)
  def unmkMore[F[_], A](p: Rep[FreeM[F, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: MoreElem[F, A] @unchecked =>
      Some((p.asRep[More[F, A]].k))
    case _ =>
      None
  }

  case class ExpFlatMap[F[_], S, B]
      (override val a: Rep[FreeM[F,S]], override val f: Rep[S => FreeM[F,B]])
      (implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends FlatMap[F, S, B](a, f) with UserTypeDef[FlatMap[F, S, B]] {
    lazy val selfType = element[FlatMap[F, S, B]]
    override def mirror(t: Transformer) = ExpFlatMap[F, S, B](t(a), t(f))
  }

  lazy val FlatMap: Rep[FlatMapCompanionAbs] = new FlatMapCompanionAbs with UserTypeDef[FlatMapCompanionAbs] {
    lazy val selfType = element[FlatMapCompanionAbs]
    override def mirror(t: Transformer) = this
  }

  object FlatMapMethods {
    // WARNING: Cannot generate matcher for method `flatMap`: Method has function arguments f1

    object flatMapBy {
      def unapply(d: Def[_]): Option[(Rep[FlatMap[F, S, B]], Rep[B => FreeM[F,R]]) forSome {type F[_]; type S; type B; type R}] = d match {
        case MethodCall(receiver, method, Seq(f1, _*), _) if (receiver.elem.asInstanceOf[Element[_]] match { case _: FlatMapElem[_, _, _] => true; case _ => false }) && method.getName == "flatMapBy" =>
          Some((receiver, f1)).asInstanceOf[Option[(Rep[FlatMap[F, S, B]], Rep[B => FreeM[F,R]]) forSome {type F[_]; type S; type B; type R}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FlatMap[F, S, B]], Rep[B => FreeM[F,R]]) forSome {type F[_]; type S; type B; type R}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object resume {
      def unapply(d: Def[_]): Option[(Rep[FlatMap[F, S, B]], Functor[F]) forSome {type F[_]; type S; type B}] = d match {
        case MethodCall(receiver, method, Seq(fF, _*), _) if (receiver.elem.asInstanceOf[Element[_]] match { case _: FlatMapElem[_, _, _] => true; case _ => false }) && method.getName == "resume" =>
          Some((receiver, fF)).asInstanceOf[Option[(Rep[FlatMap[F, S, B]], Functor[F]) forSome {type F[_]; type S; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FlatMap[F, S, B]], Functor[F]) forSome {type F[_]; type S; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object FlatMapCompanionMethods {
  }

  def mkFlatMap[F[_], S, B]
    (a: Rep[FreeM[F,S]], f: Rep[S => FreeM[F,B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[FlatMap[F, S, B]] =
    new ExpFlatMap[F, S, B](a, f)
  def unmkFlatMap[F[_], S, B](p: Rep[FreeM[F, B]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: FlatMapElem[F, S, B] @unchecked =>
      Some((p.asRep[FlatMap[F, S, B]].a, p.asRep[FlatMap[F, S, B]].f))
    case _ =>
      None
  }

  object FreeMMethods {
    // WARNING: Cannot generate matcher for method `flatMap`: Method has function arguments f

    object flatMapBy {
      def unapply(d: Def[_]): Option[(Rep[FreeM[F, A]], Rep[A => FreeM[F,B]]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Element[_]] match { case _: FreeMElem[_, _, _] => true; case _ => false }) && method.getName == "flatMapBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[FreeM[F, A]], Rep[A => FreeM[F,B]]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FreeM[F, A]], Rep[A => FreeM[F,B]]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object mapBy {
      def unapply(d: Def[_]): Option[(Rep[FreeM[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Element[_]] match { case _: FreeMElem[_, _, _] => true; case _ => false }) && method.getName == "mapBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[FreeM[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FreeM[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    // WARNING: Cannot generate matcher for method `map`: Method has function arguments f

    object resume {
      def unapply(d: Def[_]): Option[(Rep[FreeM[F, A]], Functor[F]) forSome {type F[_]; type A}] = d match {
        case MethodCall(receiver, method, Seq(fF, _*), _) if (receiver.elem.asInstanceOf[Element[_]] match { case _: FreeMElem[_, _, _] => true; case _ => false }) && method.getName == "resume" =>
          Some((receiver, fF)).asInstanceOf[Option[(Rep[FreeM[F, A]], Functor[F]) forSome {type F[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[FreeM[F, A]], Functor[F]) forSome {type F[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object FreeMCompanionMethods {
  }
}
