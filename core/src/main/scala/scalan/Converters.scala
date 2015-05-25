package scalan

import scalan.staged.Expressions

trait Converters extends Views { self: Scalan =>

  type Conv[T,R] = Rep[Converter[T,R]]
  trait Converter[T,R] extends Reifiable[Converter[T,R]] {
    implicit def eT: Elem[T]
    implicit def eR: Elem[R]
    def convFun: Rep[T => R]
    def apply(x: Rep[T]): Rep[R]
  }
  trait ConverterCompanion

  abstract class BaseConverter[T,R](val convFun: Rep[T => R])(implicit val eT: Elem[T], val eR: Elem[R])
    extends Converter[T,R] {
    def apply(x: Rep[T]): Rep[R] = convFun(x)
    override def toString: String = s"${eT.name} --> ${eR.name}"
    override def equals(other: Any): Boolean = other match {
      case c: Converters#Converter[_, _] => eT == c.eT && eR == c.eR
      case _ => false
    }
  }
  trait BaseConverterCompanion

  abstract class PairConverter[A1, A2, B1, B2]
    (val conv1: Conv[A1, B1], val conv2: Conv[A2, B2])
    (implicit val eA1: Elem[A1], val eA2: Elem[A2], val eB1: Elem[B1], val eB2: Elem[B2])
    extends Converter[(A1, A2), (B1, B2)] {

    val eT = pairElement(eA1, eA2)
    val eR = pairElement(eB1, eB2)
    def apply(x: Rep[(A1,A2)]) = { val Pair(a1, a2) = x; Pair(conv1(a1), conv2(a2)) }
    lazy val convFun = fun { x: Rep[(A1,A2)] => apply(x) }
  }
  trait PairConverterCompanion

  abstract class SumConverter[A1, A2, B1, B2]
    (val conv1: Conv[A1, B1], val conv2: Conv[A2, B2])
    (implicit val eA1: Elem[A1], val eA2: Elem[A2], val eB1: Elem[B1], val eB2: Elem[B2])
    extends Converter[(A1 | A2), (B1 | B2)] {

    val eT = sumElement(eA1, eA2)
    val eR = sumElement(eB1, eB2)
    def apply(x: Rep[(A1|A2)]) = { x.mapSumBy(conv1.convFun, conv2.convFun) }
    lazy val convFun = fun { x: Rep[(A1 | A2)] => apply(x) }
  }
  trait SumConverterCompanion
}

trait ConvertersDsl extends impl.ConvertersAbs { self: Scalan =>
  def tryConvert[From,To](eFrom: Elem[From], eTo: Elem[To], x: Rep[Reifiable[_]], conv: Rep[From => To]): Rep[To]
}

trait ConvertersDslSeq extends impl.ConvertersSeq { self: ScalanSeq =>
  def tryConvert[From,To](eFrom: Elem[From], eTo: Elem[To], x: Rep[Reifiable[_]], conv: Rep[From => To]): Rep[To] = conv(x.asRep[From])
}

trait ConvertersDslExp extends impl.ConvertersExp with Expressions { self: ScalanExp =>

  case class Convert[From,To](eFrom: Elem[From], eTo: Elem[To], x: Rep[Reifiable[_]], conv: Rep[From => To])
    extends BaseDef[To]()(eTo) {
    def uniqueOpId: String = name(eFrom, eTo)
    def mirror(f: Transformer): Rep[To] = Convert(eFrom, eTo, f(x), f(conv))
  }

  def tryConvert[From, To](eFrom: Elem[From], eTo: Elem[To], x: Rep[Reifiable[_]], conv: Rep[From => To]): Rep[To] = {
    if (x.elem <:< eFrom)
      conv(x.asRep[From])
    else
      Convert(eFrom, eTo, x, conv)
  }

  object HasConv {
    def unapply[A,B](elems: (Elem[A], Elem[B])): Option[Conv[A,B]] = hasConverter(elems._1, elems._2)
  }

  object IsConvertible {
    def unapply[A,B](elems: (Elem[A], Elem[B])): Option[(Conv[A,B], Conv[B,A])] =
      for {
        c1 <- HasConv.unapply(elems)
        c2 <- HasConv.unapply(elems.swap)
      }
      yield (c1, c2)
  }

  def hasConverter[A,B](eA: Elem[A], eB: Elem[B]): Option[Conv[A,B]] = {
    (eA, eB) match {
      case (e1, e2) if e1 == e2 =>
        implicit val ea = e1
        Some(BaseConverter(identityFun[A]).asRep[Converter[A,B]])
      case (pA: PairElem[a1,a2], pB: PairElem[b1,b2]) =>
        implicit val ea1 = pA.eFst
        implicit val eb1 = pB.eFst
        implicit val ea2 = pA.eSnd
        implicit val eb2 = pB.eSnd
        for {
          c1 <- hasConverter(ea1, eb1)
          c2 <- hasConverter(ea2, eb2)
        }
        yield PairConverter(c1, c2)
      case (pA: SumElem[a1,a2], pB: SumElem[b1,b2]) =>
        implicit val ea1 = pA.eLeft
        implicit val eb1 = pB.eLeft
        implicit val ea2 = pA.eRight
        implicit val eb2 = pB.eRight
        for {
          c1 <- hasConverter(ea1, eb1)
          c2 <- hasConverter(ea2, eb2)
        }
        yield SumConverter(c1, c2)
      case (eEntity: EntityElem[_], eClass: ConcreteElem[tData,tClass]) =>
        val convOpt = eClass.getConverterFrom(eEntity)
        convOpt
      case _ => None
    }
  }

  override def rewriteDef[T](d: Def[T]) = d match {
    // Rule: convert(eFrom, eTo, x, conv) if x.elem <:< eFrom  ==>  conv(x)
    case Convert(eFrom: Elem[from], eTo: Elem[to], x,  conv) if x.elem <:< eFrom =>
      conv(x)
    case _ => super.rewriteDef(d)
  }
}