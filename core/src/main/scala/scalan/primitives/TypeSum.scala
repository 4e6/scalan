package scalan.primitives

import scalan.staged.{BaseExp}
import scalan.{ScalanStaged, Scalan, ScalanSeq}
import scala.language.{implicitConversions}

trait TypeSum {  self: Scalan =>

  trait SumOps[A, B] {
    //def eA: Elem[A]
    //def eB: Elem[B]
    def isLeft: Rep[Boolean]
    def isRight: Rep[Boolean]
    def fold[R: Elem](l: Rep[A] => Rep[R], r: Rep[B] => Rep[R]): Rep[R]
  }

  implicit def pimpSum[A, B](s: Rep[(A | B)]): SumOps[A, B]

  def toLeft[A](a: Rep[A]): Rep[(A|Unit)]

  def toRight[A](a: Rep[A]): Rep[R[A]]

  def toLeftSum[A, B: Elem](a: Rep[A]): Rep[(A | B)]

  def toRightSum[A: Elem, B](a: Rep[B]): Rep[(A | B)]
}

trait TypeSumSeq extends TypeSum { self: ScalanSeq =>

  def toLeft[A](a: Rep[A]): Rep[(A|Unit)] = Left(a)

  def toRight[A](a: Rep[A]): Rep[R[A]] = Right(a)

  def toLeftSum[A, B: Elem](a: Rep[A]): Rep[(A | B)] = Left[A,B](a)

  def toRightSum[A: Elem, B](a: Rep[B]): Rep[(A | B)] = Right[A,B](a)

  class SeqSumOps[A, B](s: Rep[(A | B)]) extends SumOps[A,B] {
    //def eA = element[A]; def eB = element[B]
    def fold[R: Elem](l: Rep[A] => Rep[R], r: Rep[B] => Rep[R]): Rep[R] = s.fold(l,r)
    def isLeft = s.isLeft
    def isRight = s.isRight
  }
  implicit def pimpSum[A, B](s: Rep[(A | B)]): SumOps[A, B] = new SeqSumOps[A,B](s)

}

trait TypeSumExp extends TypeSum with BaseExp { self: ScalanStaged =>

  case class Left[A, B](left: Exp[A])(implicit val eB: Elem[B]) extends Def[(A | B)] {
    implicit def eA = left.elem
    lazy val selfType =  element[(A|B)]
    override def mirror(t: Transformer) = Left[A,B](t(left))
    lazy val uniqueOpId = name(eA, eB)
  }

  case class Right[A, B](right: Exp[B])(implicit val eA: Elem[A]) extends Def[(A | B)] {
    implicit def eB = right.elem
    lazy val selfType =  element[(A|B)]
    override def mirror(t: Transformer) = Right[A,B](t(right))
    lazy val uniqueOpId = name(eA, eB)
  }

  def toLeft[A](a: Rep[A]): Rep[L[A]] = withElemOf(a){ implicit e => Left[A,Unit](a) }
  def toRight[A](a: Rep[A]): Rep[R[A]] = withElemOf(a){ implicit e => Right[Unit,A](a) }
  def toLeftSum[A,B:Elem](a: Rep[A]): Rep[(A|B)] = withElemOf(a){ implicit e => Left[A,B](a) }
  def toRightSum[A:Elem,B](b: Rep[B]): Rep[(A|B)] = withElemOf(b){ implicit e => Right[A,B](b) }

  case class IsLeft[A, B](sum: Exp[(A | B)]) extends Def[Boolean] {
    override def mirror(t: Transformer) = IsLeft(t(sum))
    def selfType = element[Boolean]
    lazy val uniqueOpId = name(sum.elem.ea, sum.elem.eb)
  }

  case class IsRight[A, B](sum: Exp[(A | B)]) extends Def[Boolean] {
    override def mirror(t: Transformer) = IsRight(t(sum))
    def selfType = element[Boolean]
    lazy val uniqueOpId = name(sum.elem.ea, sum.elem.eb)
  }

  case class SumFold[A, B, R](sum: Exp[(A | B)], left: Exp[A => R], right: Exp[B => R])
                             (implicit val selfType: Elem[R]) extends Def[R] {
    override def mirror(t: Transformer) = SumFold(t(sum), t(left), t(right))
    lazy val uniqueOpId = name(sum.elem.ea, sum.elem.eb)
  }

  class StagedSumOps[A, B](s: Rep[(A | B)]) extends SumOps[A,B] {
    implicit def eA = s.elem.ea
    implicit def eB = s.elem.eb
    def fold[R: Elem](l: Rep[A] => Rep[R], r: Rep[B] => Rep[R]): Rep[R] = SumFold(s, fun(l), fun(r))
    def isLeft = IsLeft(s)
    def isRight = IsRight(s)
  }
  implicit def pimpSum[A, B](s: Rep[(A | B)]): SumOps[A, B] = new StagedSumOps[A,B](s)

  override def rewrite[T](d: Exp[T])(implicit eT: LElem[T]) = d match {
    case Def(f@SumFold(Def(Left(left)), l, _)) => {
      val eR: Elem[T] = f.selfType.asInstanceOf[Elem[T]]
      mkApply(l, left)
    }
    case Def(f@SumFold(Def(Right(right)), _, r)) => {
      val eR: Elem[T] = f.selfType.asInstanceOf[Elem[T]]
      mkApply(r, right)
    }
    //TODO case IsLeft, IsRight
    case _ => super.rewrite(d)
  }
}